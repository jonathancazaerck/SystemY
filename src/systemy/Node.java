package systemy;

import org.apache.commons.io.FileUtils;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.net.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class Node {
    private final InetSocketAddress address;
    private final String name;
    private final int hash;

    private int nextNodeHash;
    private int prevNodeHash;

    private Registry registry;

    private boolean isShuttingDown = false;

    private final ArrayList<Runnable> onReadyRunnables = new ArrayList<>();
    private final ArrayList<Runnable> onBoundRunnables = new ArrayList<>();
    private final ArrayList<Runnable> onShutdownRunnables = new ArrayList<>();
    private final ArrayList<Runnable> onFilesReplicatedRunnables = new ArrayList<>();
    private final ArrayList<Runnable> onNeighboursChangedRunnables = new ArrayList<>();
    private final ArrayList<Runnable> onListeningForFilesRunnables = new ArrayList<>();
    private final ArrayList<Runnable> onListeningForMulticastsRunnables = new ArrayList<>();
    private final ArrayList<Runnable> onFileListChangedRunnables = new ArrayList<>();

    private TreeMap<Integer, FileRef> fileList = new TreeMap<>();

    private FileRef lockRequest = null;

    private Thread fileListenerThread;
    private Thread multicastListenerThread;

    private boolean listeningToFiles = false;
    private boolean listeningToMulticasts = false;
    private long lastFilesAgentSeenTime = 0;

    public static Path getFilesPath() {
        return filesPath;
    }

    public static void setFilesPath(Path filesPath) {
        Node.filesPath = filesPath;
    }

    private static Path filesPath = Paths.get("tmp/files");

    private final Path localFilesPath;
    private final Path replicatedFilesPath;
    private final Path downloadsFilesPath;

    private ServerSocket serverSocket;
    private MulticastSocket multicastSocket;
    private DatagramSocket unicastSocket;

    private InetAddress nameServerIp;

    private NameServerOperations nameServer;

    private boolean receivedNameServerHello;

    public Node(String name, InetSocketAddress address) throws IOException {
        super();
        this.name = name;
        this.address = address;
        this.hash = Util.hash(name); //make a hash of the name of the node
        this.prevNodeHash = this.nextNodeHash = this.hash;

        this.localFilesPath = Paths.get(filesPath.toAbsolutePath().toString(), name, "local"); //e.g. tmp/files/jill/local
        this.replicatedFilesPath = Paths.get(filesPath.toAbsolutePath().toString(), name, "replicated"); //e.g. tmp/files/jill/replicated
        this.downloadsFilesPath = Paths.get(filesPath.toAbsolutePath().toString(), name, "downloads"); //e.g. tmp/files/jill/downloads

        this.localFilesPath.toFile().mkdirs(); //creates directory with this pathname
        this.replicatedFilesPath.toFile().mkdirs(); //creates directory with this pathname
        this.downloadsFilesPath.toFile().mkdirs();

        FileUtils.cleanDirectory(this.replicatedFilesPath.toFile()); //cleans directory without deleting it
    }

    //method to get the address of the node
    public InetSocketAddress getAddress() {
        return address;
    }

    //method to get the name of the node
    public String getName() {
        return name;
    }

    //method to get the hash of the node
    public int getHash() {
        return hash;
    }

    //method to get the previous node hash
    public int getPrevNodeHash() {
        return prevNodeHash;
    }

    //method to get the next node hash
    public int getNextNodeHash() {
        return nextNodeHash;
    }

    public void setPrevNodeHash(int prevNodeHash) {
        this.prevNodeHash = prevNodeHash;
    }

    public void setNextNodeHash(int nextNodeHash) {
        this.nextNodeHash = nextNodeHash;
    }

    public Path getLocalFilesPath() {
        return localFilesPath;
    }

    public void start() throws AlreadyBoundException, IOException, NotBoundException, ParseException, UnknownMessageException, InterruptedException {

        System.setProperty("java.net.preferIPv4Stack", "true"); //Disable IPv6

        unicastSocket = new DatagramSocket(address.getPort());

        sendRetryingHello();
        log("Listening for nameserver hello");
        int nodeAmount = waitForNameServerHello(); //wait for answer from nameserver
        log("Received nameserver hello");

        setupRegistry();
        setupInternalRunnables();
        onBoundRunnables.forEach(Runnable::run);

        if (nodeAmount > 1) {
            log("Starting wait for reveals");
            waitForReveals(nodeAmount);
            log("Received reveals and setup neighbours");
        }

        unicastSocket.close();

        setupInitialFileList();

        if (nodeAmount == 2) {
            new Thread(this::spawnFilesAgent).start();
        }

        startAgentTimeoutChecker();

        startListeners();
    }


    private void sendRetryingHello() {
        new Thread(() -> {
            while(!receivedNameServerHello) {
                try {
                    log("Multicasting node hello: " + name + " at " + address.toString());
                    sendMulticast("node_hello", true);
                    TimeUnit.SECONDS.sleep(2);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void setupInitialFileList() {
        File[] localFiles = this.getLocalFilesPath().toFile().listFiles();

        if (localFiles == null) return;

        for (File localFile : localFiles) {
            String name = localFile.getName();
            int fileHash = Util.hash(name);
            FileRef fileRef = new FileRef(name, this.getHash());
            this.fileList.put(fileHash, fileRef);
        }

        onFileListChangedRunnables.forEach(Runnable::run);
    }

    private void setupRegistry() throws RemoteException, NotBoundException {
        log("Locating registry at " + this.nameServerIp.toString());
        this.registry = LocateRegistry.getRegistry(this.nameServerIp.getHostAddress(), Constants.REGISTRY_PORT);
        log("Located registry at " + this.nameServerIp.toString());
        this.nameServer = (NameServerOperations) this.registry.lookup("NameServer");
        log("Looked up nameserver");
    }

    private void setupInternalRunnables() {
        this.onReadyRunnables.add(() -> {
            try {
                sendMulticast("node_ready", false);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        this.onBoundRunnables.add(() -> {
            try {
                sendMulticast("node_bound", false);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        this.onListeningForFilesRunnables.add(() -> {
            listeningToFiles = true;
            if (listeningToMulticasts) {
                log("Running ready hooks for node " + this.name);
                onReadyRunnables.forEach(Runnable::run);
            }
        });

        this.onListeningForMulticastsRunnables.add(() -> {
            listeningToMulticasts = true;
            if (listeningToFiles) {
                log("Running ready hooks for node " + this.name);
                onReadyRunnables.forEach(Runnable::run);
            }
        });
    }

    private void startListeners() throws InterruptedException {
        this.fileListenerThread = new Thread(() -> {
            try {
                listenForFiles();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        this.fileListenerThread.start();

        this.multicastListenerThread = new Thread(() -> {
            try {
                listenForMulticasts();
            } catch (IOException | UnknownMessageException | InterruptedException | ParseException e) {
                e.printStackTrace();
            }
        });
        this.multicastListenerThread.start();

        this.fileListenerThread.join();
        this.multicastListenerThread.join();
    }

    //Method to send multicasts
    private void sendMulticast(String type, boolean fullInfo) throws IOException {
        JSONObject msg = new JSONObject(); //Making a new JSONObject to send uni/multicasts
        msg.put("type", type);
        msg.put("hash", hash);
        //When fullInfo is true, more information is given because the node sends to the nameserver
        //When fullInfo is false, only type and hash are send because the node only sends to other nodes
        if(fullInfo) {
            msg.put("name", name);
            msg.put("ip", address.getHostString());
            msg.put("port", address.getPort());
        }
        //Make and send the multicast message
        String msgStr = msg.toJSONString();
        InetAddress multicastIp = InetAddress.getByName(Constants.MULTICAST_IP);
        MulticastSocket socket = new MulticastSocket(Constants.MULTICAST_PORT);
        socket.joinGroup(multicastIp);
        DatagramPacket msgPacket = new DatagramPacket(msgStr.getBytes(), msgStr.length(), multicastIp, Constants.MULTICAST_PORT);
        socket.send(msgPacket);
        socket.close();
    }

    //Method to wait for answer of nameserver to multicast from type "node_hello"
    private int waitForNameServerHello() throws IOException, ParseException, UnknownMessageException {
        byte[] buffer = new byte[Constants.MAX_MESSAGE_SIZE];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        unicastSocket.receive(packet);

        JSONObject obj = Util.extractJSONFromPacket(packet);
        String msgType = (String) obj.get("type"); //get the type of the message

        switch (msgType) {
            case "nameserver_hello":
                receivedNameServerHello = true;
                this.nameServerIp = InetAddress.getByName((String) obj.get("ip")); //get nameserver IP-address
                int nodeAmount = (int) (long) obj.get("amount"); //get the amount of the nodes in the network
                log("Received nameserver hello: " + nameServerIp.toString() + " amount: " + nodeAmount);
                return nodeAmount;
            default:
                throw new UnknownMessageException(msgType);
        }
    }

    // Method to handle a "node_reveal" message
    private void waitForReveals(int nodeAmount) throws IOException, ParseException, UnknownMessageException, InterruptedException, NotBoundException {
        int revealCount = 0;
        int revealCountNeeded = Math.min(nodeAmount - 1, 2); //math.min returns the smallest number

        byte[] buffer = new byte[Constants.MAX_MESSAGE_SIZE];
        log("Listening for " + revealCountNeeded + " reveals");

        //
        while(revealCount < revealCountNeeded) {
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length); // new datagrampacket with length from buffer
            unicastSocket.receive(packet);

            JSONObject obj = Util.extractJSONFromPacket(packet);
            String msgType = (String) obj.get("type");

            switch (msgType) {
                case "node_reveal":
                    revealCount += 1; // revealCount + 1
                    int sourceNodeHash = (int) (long) obj.get("hash"); // hash from the node that wants to reveal himself
                    int sourceNodePrevHash = (int) (long) obj.get("prev_hash"); // previous hash ""
                    int sourceNodeNextHash = (int) (long) obj.get("next_hash"); // next hash ""
                    log("Received reveal: " + obj.toJSONString());

                    boolean changed = false;

                    //if the hash of this node is the same as the previous hash of the revealed node
                    if (this.hash == sourceNodePrevHash) {
                        this.nextNodeHash = sourceNodeHash;
                        changed = true;
                    }

                    //if the hash of this node is the same as the next hash of the revealed node
                    if (this.hash == sourceNodeNextHash) {
                        this.prevNodeHash = sourceNodeHash;
                        changed = true;
                    }
                    if (changed && this.hash != this.prevNodeHash && this.hash != this.nextNodeHash) {
                        this.onNeighboursChangedRunnables.forEach(Runnable::run);
                    }
                    break;
                default:
                    throw new UnknownMessageException(msgType);
            }
        }

        unicastSocket.close();
    }

    // Method to handle the neighbours of an existing node when a new node is made
    // Also let the new node know what is changed
    private void handleNodeBound(int newNodeHash) throws IOException, NotBoundException, InterruptedException {
        boolean isAlone = hash == prevNodeHash; // this node is alone in the network
        boolean isFirstNode = hash < prevNodeHash; // this node is the first
        boolean isLastNode = nextNodeHash < hash; // this node is the last
        Integer changedHash = null;

        log("Getting notified of new node: this = " + hash + ", next = " + nextNodeHash + ", prev = " + prevNodeHash + ", new = " + newNodeHash);

        // if this node is alone OR newNodeHash is between hash and nextNodeHash OR this hash is the last node and the newNodeHash is bigger than this hash or smaller than the next hash
        if (isAlone || (hash < newNodeHash && newNodeHash < nextNodeHash) || (isLastNode && (hash < newNodeHash || newNodeHash < nextNodeHash))) {
            this.nextNodeHash = changedHash = newNodeHash;
            log(newNodeHash + " is new next");
        }
        // can be both prev and next
        // if this node is alone OR newNodeHash is between prevNodeHash and hash OR this hash is the first node and the newNodeHash is bigger than prev hash or smaller than this hash
        if (isAlone || (prevNodeHash < newNodeHash && newNodeHash < hash) || (isFirstNode && (prevNodeHash < newNodeHash || newNodeHash < hash))) {
            this.prevNodeHash = changedHash = newNodeHash;
            log(newNodeHash + " is new prev");
        }

        // if something changed, a new message is made (type: "node_reveal")
        // this message is send to the new node to let him know:
        // - which node hash this is
        // - what the previous node hash is
        // - what the next node hash is
        // in other words: the already existing node reveals himself to the new node
        if (changedHash != null) {
            JSONObject responseMsg = new JSONObject();
            responseMsg.put("type", "node_reveal");
            responseMsg.put("hash", this.hash);
            responseMsg.put("prev_hash", this.prevNodeHash);
            responseMsg.put("next_hash", this.nextNodeHash);
            String responseStr = responseMsg.toJSONString();

            DatagramSocket datagramSocket = new DatagramSocket();

            InetSocketAddress nodeAddress = this.nameServer.getAddressByHash(changedHash);

            this.onNeighboursChangedRunnables.forEach(Runnable::run);
            TimeUnit.MILLISECONDS.sleep(100);  // between sending and listening

            log("Sending node reveal to " + nodeAddress.toString());
            datagramSocket.send(new DatagramPacket(responseStr.getBytes(), responseStr.length(), nodeAddress.getAddress(), nodeAddress.getPort()));
            datagramSocket.close();
        }
    }

    // Method to shutdown the node
    public void shutdown() throws IOException, InterruptedException, NodeNotReadyException {

        this.isShuttingDown = true;
        this.onShutdownRunnables.forEach(Runnable::run);

        if (this.serverSocket == null || this.multicastSocket == null) {
            throw new NodeNotReadyException(this.name);
        }

        this.serverSocket.close();
        this.multicastSocket.close();

        this.fileListenerThread.join();
        this.multicastListenerThread.join();
    }
    // Method to replicate a file.
    // Invoked by replicateFiles method
    // Input is the reference to the file we want to replicate, destination is to be determined.
    public void replicateFile(FileRef fileRef) throws IOException, FileNotPresentException {
        int fileHash = Util.hash(fileRef.getFileName()); //calculate the hash of the file

        if (fileRef.getActualLocationHash() != hash) throw new FileNotPresentException();

        int nodeHashToDupl = this.nameServer.getNodeHashToReplicateTo(fileHash); //node hash where we want to replicate to

        if (nodeHashToDupl == hash) nodeHashToDupl = prevNodeHash; //if the nodehash is the same as this node, replicate to the previous node

        sendFileToNodeHash(fileRef, nodeHashToDupl, false);
    }

    public void replicateFiles(Collection<FileRef> fileRefs) throws IOException, FileNotPresentException {
        for(FileRef fileRef : fileRefs) {
            replicateFile(fileRef);
        }
    }

    public void sendFileToNodeHash(FileRef fileRef, int nodeHashToDupl, boolean isDownload) throws IOException {
        InetSocketAddress addressToDupl = this.nameServer.getAddressByHash(nodeHashToDupl);

        File file = fileRefToFile(fileRef);

        log("Replicating file " + file.getName() + " to " + nodeHashToDupl + " with address " + addressToDupl);

        sendFileToAddress(file, addressToDupl, isDownload);
    }

    private void sendFileToAddress(File file, InetSocketAddress addressToDupl, boolean isDownload) throws IOException {
        FileInputStream fis = new FileInputStream(file);
        long fileSize = fis.getChannel().size();

        JSONObject metadata = new JSONObject();
        metadata.put("type", "file_metadata");
        metadata.put("name", file.getName());
        metadata.put("size", fileSize);
        metadata.put("download", isDownload);

        BufferedInputStream bfis = new BufferedInputStream(fis);

        TCPHelper.sendRequest(addressToDupl, metadata, bfis);

        log("File " + file.getName() + " is transferred!");
    }

    // Method to listen for files in the server.
    // A server socket waits for requests to come in over the network
    // and returns a result to the requester.
    private void listenForFiles() throws IOException {
        this.serverSocket = new ServerSocket(this.address.getPort());
        this.serverSocket.setReuseAddress(true);
        log("Listening for files on port " + this.address.getPort());
        this.onListeningForFilesRunnables.forEach(Runnable::run);

        while(!this.isShuttingDown) {
            Socket request;
            try {
                request = this.serverSocket.accept();
                handleUnicast(request);
                request.close();
            } catch(SocketException e) {
                log("Closed socket, stopping file listener");
            } catch (IOException | UnknownMessageException | UnmatchedFileSizeException | ParseException e) {
                e.printStackTrace();
            }
        }
        this.serverSocket.close();
    }

    // Method to listen on the right port to multicasts
    // If a multicast is received, jump to handleMulticastPacket
    // This method is invoked in the thread multicastListenerThread
    private void listenForMulticasts() throws IOException, UnknownMessageException, ParseException, InterruptedException {
        InetAddress multicastIp = InetAddress.getByName(Constants.MULTICAST_IP);
        this.multicastSocket = new MulticastSocket(Constants.MULTICAST_PORT);
        this.multicastSocket.joinGroup(multicastIp);
        log("Listening for multicasts");
        this.onListeningForMulticastsRunnables.forEach(Runnable::run);

        while(!this.isShuttingDown) {
            byte[] buffer = new byte[Constants.MAX_MESSAGE_SIZE];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            try {
                this.multicastSocket.receive(packet);
                handleMulticastPacket(packet);
            } catch (SocketException e) {
                log("Closed socket, stopping multicast listener");
            } catch (IOException | NotBoundException e) {
                e.printStackTrace();
            }
        }

        this.multicastSocket.close();
    }

    // Method to handle four kinds of multicasts
    // Invoked in method listenForMulticasts
    private void handleMulticastPacket(DatagramPacket packet) throws UnknownMessageException, IOException, ParseException, NotBoundException, InterruptedException {

        JSONObject msg = Util.extractJSONFromPacket(packet); //make JSONObject from multicast packet
        String msgType = (String) msg.get("type"); //save "type" into msgType
        int sourceNodeHash = (int) (long) msg.get("hash"); //save hash into sourceNodeHash

        log("Received multicast message from " + sourceNodeHash + ": " + msgType);

        if (sourceNodeHash == this.hash) return; //all hashes are unique, so if we are sending to ourself => return

        switch (msgType) {
            case "node_hello":
                break; // do nothing
            case "node_bound":
                handleNodeBound(sourceNodeHash); // choose new neighbours
                break;
            case "node_ready":
                removeRedunantFiles(sourceNodeHash); // check later
                break;
            case "node_shutdown": // this is unused
                if (this.prevNodeHash == sourceNodeHash) {
                    this.prevNodeHash = (int) (long) msg.get("prev_hash");
                }
                if (this.nextNodeHash == sourceNodeHash) {
                    this.nextNodeHash = (int) (long) msg.get("next_hash");
                }
                break;
            default:
                throw new UnknownMessageException(msgType);
        }
    }

    private void removeRedunantFiles(int sourceNodeHash) throws RemoteException {
        File[] replicatedFiles = replicatedFilesPath.toFile().listFiles();
        if(replicatedFiles == null) return;

        for (File replicatedFile : replicatedFiles) {
            if (this.nameServer.getNodeHashToReplicateTo(Util.hash(replicatedFile.getName())) == sourceNodeHash) {
                replicatedFile.delete();
            }
        }
    }

    private void handleUnicast(Socket request) throws IOException, ParseException, UnmatchedFileSizeException, UnknownMessageException {
        InputStream in = request.getInputStream();
        byte[] fileMetadataBuffer = new byte[Constants.TCP_METADATA_LENGTH];
        in.read(fileMetadataBuffer, 0, Constants.TCP_METADATA_LENGTH);
        String metadataStr = new String(Util.trimByteArray(fileMetadataBuffer));

        log("Incoming metadata " + metadataStr);
        JSONObject metadata = (JSONObject) JSONValue.parseWithException(metadataStr+"\n");

        String metadataType = (String) metadata.get("type");

        switch (metadataType) {
            case "file_metadata":
                long expectedFileSize = (long) metadata.get("size");
                String fileName = (String) metadata.get("name");
                boolean isDownload = (Boolean) metadata.get("download");
                handleIncomingFile(fileName, expectedFileSize, in, isDownload);
                in.close();
                break;
            case "agent_metadata":
                ObjectInputStream ois = new ObjectInputStream(in);
                try {
                    Agent agent = (Agent) ois.readObject();
                    if (agent.getSort().equals("files")) {
                        lastFilesAgentSeenTime = System.currentTimeMillis();
                    }
                    startAgent(agent);
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
                in.close();
                break;
            case "node_download_request":
                System.out.println("Received NDR: " + metadata);
                int fileHash = (int) (long) metadata.get("file_hash");
                FileRef file = fileList.get(fileHash);
                int nodeHash = (int) (long) metadata.get("hash");
                sendFileToNodeHash(file, nodeHash, true);
                break;
            default:
                in.close();
                throw new UnknownMessageException(metadataType);
        }

    }

    private void handleIncomingFile(String fileName, long expectedFileSize, InputStream in, boolean isDownload) throws IOException, UnmatchedFileSizeException {
        Path filePath = Paths.get((isDownload ? downloadsFilesPath : replicatedFilesPath).toString(), fileName);

        log("Writing file " + fileName + " to " + filePath);

        FileOutputStream fos = new FileOutputStream(filePath.toString());

        byte[] buffer = new byte[Constants.MAX_FILE_SIZE];
        int count;

        long actualFileSize = 0;
        while((count = in.read(buffer)) >= 0){
            fos.write(buffer, 0, count);
            actualFileSize += count;
        }
        if(actualFileSize != expectedFileSize) {
            throw new UnmatchedFileSizeException(fileName, expectedFileSize, actualFileSize);
        }
    }

    private boolean isAlone() {
        return hash == prevNodeHash;
    }

    public void onReady(Runnable runnable) {
        onReadyRunnables.add(runnable);
    }
    public void onShutdown(Runnable runnable) {
        onShutdownRunnables.add(runnable);
    }
    public void onFilesReplicated(Runnable runnable) {
        onFilesReplicatedRunnables.add(runnable);
    }
    public void onNeighboursChanged(Runnable runnable) {
        onNeighboursChangedRunnables.add(runnable);
    }
    public void onFileListChanged(Runnable runnable) {
        onFileListChangedRunnables.add(runnable);
    }
    public void onBound(Runnable runnable) {
        onBoundRunnables.add(runnable);
    }

    private void log(String str) {
        System.out.println("[" + name + "@" + hash + "] " + str);
    }

    public TreeMap<Integer, FileRef> getFileList() {
        return fileList;
    }

    public void setFileList(TreeMap<Integer, FileRef> fileList) {
        if(this.fileList != null) {
            Set<Integer> orgSet = this.fileList.keySet();
            Set<Integer> newSet = fileList.keySet();
            boolean nothingChanged = orgSet.containsAll(newSet) && newSet.containsAll(orgSet);
            if (nothingChanged) return; // if no differences
        }

        this.fileList = fileList;
        onFileListChangedRunnables.forEach(Runnable::run);
    }

    private void startAgent(Agent agent) {
        agent.setCurrentNode(this);
        new Thread(() -> {
            boolean isAlone = isAlone();
            log("Running agent");
            agent.run();
            log("Running agent done");
            if (isAlone) return;
            try {
                TimeUnit.MILLISECONDS.sleep(Constants.AGENT_DELAY);
                if (agent.shouldProceed()) {
                    sendAgentToNextNode(agent);
                } else {
                    log("Not sending agent");
                    if (agent.getSort().equals("failure")) spawnFilesAgent();
                }
            } catch (SocketException e) {
                log("Can't send agent to " + nextNodeHash + ", starting failure agent");
                handleFailedAgentSending(nextNodeHash);
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void handleFailedAgentSending(int failedNodeHash) {
        try {
            this.nameServer.notifyFailure(failedNodeHash);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        spawnFailureAgent(failedNodeHash);
    }

    private void sendAgentToNextNode(Agent agent) throws IOException {
        InetSocketAddress address = this.nameServer.getAddressByHash(this.nextNodeHash);

        JSONObject metadata = new JSONObject();
        metadata.put("type", "agent_metadata");
        metadata.put("sort", agent.getSort());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(agent);
        oos.close();

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());

        BufferedInputStream bis = new BufferedInputStream(bais);

        TCPHelper.sendRequest(address, metadata, bis);

        log("sent agent");
    }

    private void spawnFilesAgent() {
        log("spawning files agent");
        Agent agent = new FilesAgent();
        startAgent(agent);
        lastFilesAgentSeenTime = System.currentTimeMillis();
    }

    private void spawnFailureAgent(int failedNodeHash) {
        log("spawning failure agent");
        try {
            // shallow ring does not have the failed node because of notifyFailure()
            FailureAgent agent = new FailureAgent(failedNodeHash, hash, this.nameServer.getShallowRing());
            startAgent(agent);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void notifyUpdatedFiles(Collection<FileRef> updatedFileRefs) {
        try {
            replicateFiles(updatedFileRefs);
        } catch (IOException | FileNotPresentException e) {
            e.printStackTrace();
        }
    }

    public File fileRefToFile(FileRef fileRef) {
        return fileRefToFile(fileRef, false);
    }

    public File fileRefToFile(FileRef fileRef, boolean isDownload) {
        String dirName;
        if (isDownload) {
            dirName = "downloads";
        } else {
            dirName = fileRef.isLocationDisappeared() ? "replicated" : "local";
        }
        Path filePath = Paths.get(
            Node.getFilesPath().toAbsolutePath().toString(),
            name,
            dirName,
            fileRef.getFileName());

        return filePath.toFile();
    }

    public void setLockRequest(FileRef fileRef){
        this.lockRequest = fileRef;
    }

    public FileRef getLockRequest() {
        return lockRequest;
    }

    public void removeLockRequest() {
        lockRequest = null;
    }

    private void startAgentTimeoutChecker() {
        // timeout needs to be quite random in order not to trigger multiple nodes
        int timeout = Constants.AGENT_TIMEOUT + new Random().nextInt(20 * 1000);
        new Thread(() -> {
            while (true) {
                try {
                    if (lastFilesAgentSeenTime != 0 &&
                            System.currentTimeMillis() - lastFilesAgentSeenTime > timeout) {
                        log("Haven't seen files agent in " + timeout + " seconds, spawning new");
                        spawnFilesAgent();
                    }
                    TimeUnit.MILLISECONDS.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
