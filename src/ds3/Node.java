package ds3;

import org.apache.commons.io.FileUtils;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;

import javax.xml.crypto.Data;
import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class Node implements NodeLifecycleHooks {
    private InetSocketAddress address;
    private String name;
    private int hash;

    private int nextNodeHash;
    private int prevNodeHash;

    private Registry registry;

    private boolean isShuttingDown = false;

    private ArrayList<Runnable> onReadyRunnables = new ArrayList<Runnable>();
    private ArrayList<Runnable> onBoundRunnables = new ArrayList<Runnable>();
    private ArrayList<Runnable> onShutdownRunnables = new ArrayList<Runnable>();
    private ArrayList<Runnable> onFilesReplicatedRunnables = new ArrayList<Runnable>();
    private ArrayList<Runnable> onNeighboursChangedRunnables = new ArrayList<Runnable>();
    private ArrayList<Runnable> onListeningForFilesRunnables = new ArrayList<Runnable>();
    private ArrayList<Runnable> onListeningForMulticastsRunnables = new ArrayList<Runnable>();

    private ArrayList<FileRef> fileList = new ArrayList<FileRef>();

    private Thread fileListenerThread;
    private Thread multicastListenerThread;

    private boolean listeningToFiles = false;
    private boolean listeningToMulticasts = false;

    private static Path filesPath = Paths.get("tmp/files");

    private Path localFilesPath;
    private Path replicatedFilesPath;

    private ServerSocket serverSocket;
    private MulticastSocket multicastSocket;
    private DatagramSocket unicastSocket;

    private InetAddress nameServerIp;

    private NameServerOperations nameServer;

    public Node(String name, InetSocketAddress address) throws IOException {
        super();
        this.name = name;
        this.address = address;
        this.hash = Util.hash(name);
        this.prevNodeHash = this.nextNodeHash = this.hash;

        this.localFilesPath = Paths.get(filesPath.toAbsolutePath().toString(), name, "local");
        this.replicatedFilesPath = Paths.get(filesPath.toAbsolutePath().toString(), name, "replicated");

        this.localFilesPath.toFile().mkdirs();
        this.replicatedFilesPath.toFile().mkdirs();

        FileUtils.cleanDirectory(this.replicatedFilesPath.toFile());
    }

    public InetSocketAddress getAddress() {
        return address;
    }

    public String getName() {
        return name;
    }

    public int getHash() {
        return hash;
    }

    public int getPrevNodeHash() {
        return prevNodeHash;
    }

    public int getNextNodeHash() {
        return nextNodeHash;
    }

    public static void setFilesPath(Path filesPath) {
        Node.filesPath = filesPath;
    }

    public void start() throws AlreadyBoundException, IOException, NotBoundException, ParseException, UnknownMessageException, InterruptedException {
        System.setProperty("java.net.preferIPv4Stack", "true");

        unicastSocket = new DatagramSocket(address.getPort());

        log("Multicasting node hello: " + name + " at " + address.toString());
        sendMulticast("node_hello", true);

        log("Listening for nameserver hello");
        int nodeAmount = waitForNameServerHello();
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

        startListeners();
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
                replicateFiles();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (NotBoundException e) {
                e.printStackTrace();
            }
        });

        this.onBoundRunnables.add(() -> {
            try {
                sendMulticast("node_bound", false);
                replicateFiles();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (NotBoundException e) {
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
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ParseException e) {
                e.printStackTrace();
            } catch (UnknownMessageException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        this.multicastListenerThread.start();

        this.fileListenerThread.join();
        this.multicastListenerThread.join();
    }

    private void sendMulticast(String type, boolean fullInfo) throws IOException {
        JSONObject msg = new JSONObject();
        msg.put("type", type);
        msg.put("hash", hash);
        if(fullInfo) {
            msg.put("name", name);
            msg.put("ip", address.getHostString());
            msg.put("port", address.getPort());
        }
        String msgStr = msg.toJSONString();
        InetAddress multicastIp = InetAddress.getByName(Constants.MULTICAST_IP);
        MulticastSocket socket = new MulticastSocket(Constants.MULTICAST_PORT);
        socket.joinGroup(multicastIp);
        DatagramPacket msgPacket = new DatagramPacket(msgStr.getBytes(), msgStr.length(), multicastIp, Constants.MULTICAST_PORT);
        socket.send(msgPacket);
        socket.close();
    }

    private int waitForNameServerHello() throws IOException, ParseException, UnknownMessageException {
        byte[] buffer = new byte[Constants.MAX_MESSAGE_SIZE];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        unicastSocket.receive(packet);

        JSONObject obj = Util.extractJSONFromPacket(packet);
        String msgType = (String) obj.get("type");

        switch (msgType) {
            case "nameserver_hello":
                this.nameServerIp = InetAddress.getByName((String) obj.get("ip"));
                int nodeAmount = (int) (long) obj.get("amount");
                log("Received nameserver hello: " + nameServerIp.toString() + " amount: " + nodeAmount);
                return nodeAmount;
            default:
                throw new UnknownMessageException(msgType);
        }
    }

    private void waitForReveals(int nodeAmount) throws IOException, ParseException, UnknownMessageException, InterruptedException, NotBoundException {
        int revealCount = 0;
        int revealCountNeeded = Math.min(nodeAmount - 1, 2);

        byte[] buffer = new byte[Constants.MAX_MESSAGE_SIZE];
        log("Listening for " + revealCountNeeded + " reveals");

        while(revealCount < revealCountNeeded) {
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            unicastSocket.receive(packet);

            JSONObject obj = Util.extractJSONFromPacket(packet);
            String msgType = (String) obj.get("type");

            switch (msgType) {
                case "node_reveal":
                    revealCount += 1;
                    int sourceNodeHash = (int) (long) obj.get("hash");
                    int sourceNodePrevHash = (int) (long) obj.get("prev_hash");
                    int sourceNodeNextHash = (int) (long) obj.get("next_hash");
                    log("Received reveal: " + obj.toJSONString());

                    boolean changed = false;
                    if (this.hash == sourceNodePrevHash) {
                        this.nextNodeHash = sourceNodeHash;
                        changed = true;
                    }
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

    private void handleNodeBound(int newNodeHash) throws IOException, NotBoundException, InterruptedException {
        boolean isAlone = hash == prevNodeHash;
        boolean isFirstNode = hash < prevNodeHash;
        boolean isLastNode = nextNodeHash < hash;
        Integer changedHash = null;

        log("Getting notified of new node: this = " + hash + ", next = " + nextNodeHash + ", prev = " + prevNodeHash + ", new = " + newNodeHash);

        if (isAlone || (hash < newNodeHash && newNodeHash < nextNodeHash) || (isLastNode && (hash < newNodeHash || newNodeHash < nextNodeHash))) {
            this.nextNodeHash = changedHash = newNodeHash;
            log(newNodeHash + " is new next");
        }
        // can be both prev and next
        if (isAlone || (prevNodeHash < newNodeHash && newNodeHash < hash) || (isFirstNode && (prevNodeHash < newNodeHash || newNodeHash < hash))) {
            this.prevNodeHash = changedHash = newNodeHash;
            log(newNodeHash + " is new prev");
        }

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

    private void replicateFiles() throws IOException, NotBoundException {
        replicateFiles(null);
    }

    private void replicateFiles(Integer limitHash) throws IOException, NotBoundException {
        if(isAlone()) return;

        File[] localFiles = localFilesPath.toFile().listFiles();
        if(localFiles == null) return;

        int count;
        byte[] fileOutputBuffer = new byte[Constants.MAX_FILE_SIZE];

        for (File localFile : localFiles) {
            int fileHash = Util.hash(name);

            int nodeHashToDupl = this.nameServer.getNodeHashToReplicateTo(fileHash);

            if (nodeHashToDupl == hash) nodeHashToDupl = prevNodeHash;
            if (limitHash != null && nodeHashToDupl != limitHash) continue;

            InetSocketAddress addressToDupl = this.nameServer.getAddressByHash(nodeHashToDupl);

            log("Replicating file " + localFile.getName() + " to " + nodeHashToDupl + " with address " + addressToDupl);

            Socket socket = new Socket(addressToDupl.getAddress(), addressToDupl.getPort());

            try {
                OutputStream out = socket.getOutputStream();
                FileInputStream fis = new FileInputStream(localFile);
                BufferedInputStream bfis = new BufferedInputStream(fis);

                long fileSize = fis.getChannel().size();

                JSONObject metadata = new JSONObject();
                metadata.put("type", "file_metadata");
                metadata.put("name", localFile.getName());
                metadata.put("size", fileSize);

                ByteBuffer metadataBuffer = ByteBuffer.allocate(Constants.FILE_METADATA_LENGTH);
                metadataBuffer.put(metadata.toJSONString().getBytes());
                out.write(metadataBuffer.array(), 0, Constants.FILE_METADATA_LENGTH);

                while ((count = bfis.read(fileOutputBuffer)) >= 0) {
                    out.write(fileOutputBuffer, 0, count);
                    out.flush();
                }

                out.close();
                bfis.close();
                log("File " + localFile.getName() + " is transferred!");
            } catch (IOException e) {
                e.printStackTrace();
            }

            socket.close();
        }

        this.onFilesReplicatedRunnables.forEach(Runnable::run);
    }

    private void listenForFiles() throws IOException {
        this.serverSocket = new ServerSocket(this.address.getPort());
        this.serverSocket.setReuseAddress(true);
        log("Listening for files on port " + this.address.getPort());
        this.onListeningForFilesRunnables.forEach(Runnable::run);

        while(!this.isShuttingDown) {
            Socket request;
            try {
                request = this.serverSocket.accept();
                handleFileRequest(request);
                request.close();
            } catch(SocketException e) {
                log("Closed socket, stopping file listener");
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ParseException e) {
                e.printStackTrace();
            } catch (UnmatchedFileSizeException e) {
                e.printStackTrace();
            }
        }
        this.serverSocket.close();
    }

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
            } catch (IOException e) {
                e.printStackTrace();
            } catch (NotBoundException e) {
                e.printStackTrace();
            }
        }

        this.multicastSocket.close();
    }

    private void handleMulticastPacket(DatagramPacket packet) throws UnknownMessageException, IOException, ParseException, NotBoundException, InterruptedException {
        JSONObject obj = Util.extractJSONFromPacket(packet);

        String msgType = (String) obj.get("type");

        int sourceNodeHash = (int) (long) obj.get("hash");
        log("Received multicast message from " + sourceNodeHash + ": " + msgType);

        if (sourceNodeHash == this.hash) return;

        switch (msgType) {
            case "node_hello":
                break;
            case "node_bound":
                handleNodeBound(sourceNodeHash);
                break;
            case "node_ready":
                replicateFiles(sourceNodeHash);
                removeRedunantFiles(sourceNodeHash);
                break;
            case "node_shutdown":
                if (this.prevNodeHash == sourceNodeHash) {
                    this.prevNodeHash = (int) (long) obj.get("prev_hash");
                }
                if (this.nextNodeHash == sourceNodeHash) {
                    this.nextNodeHash = (int) (long) obj.get("next_hash");
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

    private void handleFileRequest(Socket request) throws IOException, ParseException, UnmatchedFileSizeException {
        InputStream in = request.getInputStream();
        byte[] fileSizeBuffer = new byte[Constants.FILE_METADATA_LENGTH];
        in.read(fileSizeBuffer, 0, Constants.FILE_METADATA_LENGTH);
        String metadataStr = new String(Util.trimByteArray(fileSizeBuffer));

        log("Incoming metadata " + metadataStr);
        JSONObject metadata = (JSONObject) JSONValue.parseWithException(metadataStr+"\n");

        long expectedFileSize = (long) metadata.get("size");
        String fileName = (String) metadata.get("name");
        Path filePath = Paths.get(replicatedFilesPath.toString(), fileName);

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

        in.close();
    }

    private boolean isAlone() {
        return hash == prevNodeHash;
    }

    @Override
    public void onReady(Runnable runnable) {
        onReadyRunnables.add(runnable);
    }

    @Override
    public void onShutdown(Runnable runnable) {
        onShutdownRunnables.add(runnable);
    }

    @Override
    public void onFilesReplicated(Runnable runnable) {
        onFilesReplicatedRunnables.add(runnable);
    }

    @Override
    public void onNeighboursChanged(Runnable runnable) {
        onNeighboursChangedRunnables.add(runnable);
    }

    @Override
    public void onBound(Runnable runnable) {
        onBoundRunnables.add(runnable);
    }

    private void log(String str) {
        System.out.println("[" + name + "@" + hash + "] " + str);
    }
}
