package ds3;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;

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
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;

public class Node extends UnicastRemoteObject implements NodeOperations, LifecycleHooks {
    private InetSocketAddress address;
    private String name;
    private int hash;

    private int nextNodeHash;
    private int prevNodeHash;

    private Registry registry;

    private boolean isShuttingDown;

    private ArrayList<Runnable> onReadyRunnables;
    private ArrayList<Runnable> onShutdownRunnables;
    private ArrayList<Runnable> onFilesReplicatedRunnables;
    private ArrayList<Runnable> onNeighbourChangedRunnables;


    private static Path filesPath = Paths.get("tmp/files");

    private Path localFilesPath;
    private Path replicatedFilesPath;

    private ServerSocket serverSocket;

    public Node(String name, InetSocketAddress address) throws RemoteException {
        super();
        this.name = name;
        this.address = address;
        this.hash = Util.hash(name);

        this.isShuttingDown = false;

        this.onReadyRunnables = new ArrayList<Runnable>();
        this.onShutdownRunnables = new ArrayList<Runnable>();
        this.onFilesReplicatedRunnables = new ArrayList<Runnable>();
        this.onNeighbourChangedRunnables = new ArrayList<Runnable>();

        this.localFilesPath = Paths.get(filesPath.toAbsolutePath().toString(), name, "local");
        this.replicatedFilesPath = Paths.get(filesPath.toAbsolutePath().toString(), name, "replicated");

        this.localFilesPath.toFile().mkdirs();
        this.replicatedFilesPath.toFile().mkdirs();
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

    public void start() throws AlreadyBoundException, IOException, NotBoundException, ParseException, UnknownMessageException {
        System.setProperty("java.net.preferIPv4Stack", "true");

        DatagramSocket datagramSocket = openNameServerHelloSocket(address);

        log("Multicasting node hello: " + name + " at " + address.toString());
        sendNodeHello(name, address);

        log("Listening for nameserver hello");
        InetAddress nameServerIp = waitForNameServerHello(datagramSocket);

        log("Locating registry at " + nameServerIp.toString());
        registry = LocateRegistry.getRegistry(nameServerIp.getHostAddress(), Constants.REGISTRY_PORT);
        log("Located registry at " + nameServerIp.toString());
        NameServerOperations nameServer = (NameServerOperations) registry.lookup("NameServer");

        int amount = nameServer.getNumberOfNodes();
        log("Number of nodes: " + amount);

        if (amount == 1) {
            this.prevNodeHash = this.hash;
            this.nextNodeHash = this.hash;
        } else if (amount == 2) {
            this.nextNodeHash = this.prevNodeHash = nameServer.getPrevHash(this.hash);

            NodeOperations prevNode = (NodeOperations) registry.lookup(Util.getNodeRegistryName(this.prevNodeHash));

            prevNode.notifyNewNeighbour(this.hash);
        } else {
            this.prevNodeHash = nameServer.getPrevHash(this.hash);
            this.nextNodeHash = nameServer.getNextHash(this.hash);

            NodeOperations prevNode = (NodeOperations) registry.lookup(Util.getNodeRegistryName(this.prevNodeHash));
            NodeOperations nextNode = (NodeOperations) registry.lookup(Util.getNodeRegistryName(this.nextNodeHash));

            prevNode.notifyNewNeighbour(this.hash);
            nextNode.notifyNewNeighbour(this.hash);
        }

        String registryName = Util.getNodeRegistryName(hash);
        log("Binding this to registry: " + registryName);
        registry.bind(registryName, this);
        log("Bound to registry: " + registryName);

        log("Running ready hooks for node " + this.name + " with registry name " + registryName);
        onReadyRunnables.forEach(Runnable::run);

        this.onNeighbourChangedRunnables.add(() -> {
            try {
                this.replicateFiles();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (NotBoundException e) {
                e.printStackTrace();
            }
        });

        onFilesReplicatedRunnables.forEach(Runnable::run);

        listenForFiles();
    }

    private void sendNodeHello(String name, InetSocketAddress address) throws IOException {
        JSONObject msg = new JSONObject();
        msg.put("type", "node_hello");
        msg.put("name", name);
        msg.put("ip", address.getHostString());
        msg.put("port", address.getPort());
        String msgStr = msg.toJSONString();
        InetAddress multicastIp = InetAddress.getByName(Constants.MULTICAST_IP);
        MulticastSocket socket = new MulticastSocket(Constants.MULTICAST_PORT);
        socket.joinGroup(multicastIp);
        DatagramPacket msgPacket = new DatagramPacket(msgStr.getBytes(), msgStr.length(), multicastIp, Constants.MULTICAST_PORT);
        socket.send(msgPacket);
        socket.close();
    }

    private DatagramSocket openNameServerHelloSocket(InetSocketAddress address) throws SocketException {
        return new DatagramSocket(address.getPort());
    }

    private InetAddress waitForNameServerHello(DatagramSocket datagramSocket) throws IOException, ParseException, UnknownMessageException {
        byte[] buffer = new byte[1000];

        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        datagramSocket.receive(packet);

        String msg = new String(packet.getData(), 0, packet.getLength(), "UTF-8");
        JSONObject obj = (JSONObject) JSONValue.parseWithException(msg + "\n");
        String msgType = (String) obj.get("type");

        datagramSocket.close();

        switch (msgType) {
            case "nameserver_hello":
                InetAddress nameServerIp = InetAddress.getByName((String) obj.get("ip"));
                log("Received nameserver hello: " + nameServerIp.toString());
                return nameServerIp;
            default:
                throw new UnknownMessageException(msgType, msg);
        }
    }

    public void notifyNewNeighbour(int newNodeHash) {
        boolean isAlone = hash == prevNodeHash;
        boolean isFirstNode = hash < prevNodeHash;
        boolean isLastNode = nextNodeHash < hash;

        log("Getting notified of new neighbour: this = " + hash + ", next = " + nextNodeHash + ", prev = " + prevNodeHash + ", new = " + newNodeHash);

        if (isAlone || (hash < newNodeHash && newNodeHash < nextNodeHash) || (isLastNode && (hash < newNodeHash || newNodeHash < nextNodeHash))) {
            this.nextNodeHash = newNodeHash;
            log(newNodeHash + " is new next");
        }
        // can be both prev and next
        if (isAlone || (prevNodeHash < newNodeHash && newNodeHash < hash) || (isFirstNode && (prevNodeHash < newNodeHash || newNodeHash < hash))) {
            this.prevNodeHash = newNodeHash;
            log(newNodeHash + " is new prev");
        }

        this.onNeighbourChangedRunnables.forEach(Runnable::run);
    }

    public void shutdown() throws IOException, NotBoundException {
        this.isShuttingDown = true;

        NodeOperations prevNode = (NodeOperations) this.registry.lookup(Util.getNodeRegistryName(this.prevNodeHash));
        NodeOperations nextNode = (NodeOperations) this.registry.lookup(Util.getNodeRegistryName(this.nextNodeHash));

        prevNode.notifyNeighbourShutdown(false, this.nextNodeHash);
        nextNode.notifyNeighbourShutdown(true, this.prevNodeHash);

        this.registry.unbind(Util.getNodeRegistryName(this.hash));

        onShutdownRunnables.forEach(Runnable::run);

        this.serverSocket.close();
    }

    public void notifyNeighbourShutdown(boolean prev, int newNeighbourHash) {
        if (prev) {
            this.nextNodeHash = newNeighbourHash;
        } else {
            this.prevNodeHash = newNeighbourHash;
        }
    }

    public void replicateFiles() throws IOException, NotBoundException {
        File[] localFiles = localFilesPath.toFile().listFiles();
        if(localFiles == null) return;
        if(hash == prevNodeHash) return;

        int count;
        byte[] buffer = new byte[(int) Math.pow(2, 10)];

        NameServerOperations nameServer = (NameServerOperations) registry.lookup("NameServer");

        for (File child : localFiles) {
            String name =  child.getName();
            int fileHash = Util.hash(name);

            int nodeHashToDupl = nameServer.getPrevHash(fileHash);

            if (nodeHashToDupl == hash) nodeHashToDupl = prevNodeHash;

            InetSocketAddress addressToDupl = nameServer.getAddressByHash(nodeHashToDupl);

            log("Replicating file " + child.getName() + " to " + nodeHashToDupl + " with address " + addressToDupl);

            Socket socket = new Socket(addressToDupl.getAddress(), addressToDupl.getPort());

            try {
                OutputStream out = socket.getOutputStream();
                FileInputStream fis = new FileInputStream(child);
                BufferedInputStream bfis = new BufferedInputStream(fis);

                long fileSize = fis.getChannel().size();

                JSONObject metadata = new JSONObject();
                metadata.put("type", "file_metadata");
                metadata.put("name", name);
                metadata.put("size", fileSize);

                ByteBuffer metadataBuffer = ByteBuffer.allocate(Constants.FILE_METADATA_LENGTH);
                metadataBuffer.put(metadata.toJSONString().getBytes());
                out.write(metadataBuffer.array(), 0, Constants.FILE_METADATA_LENGTH);

                while ((count = bfis.read(buffer)) >= 0) {
                    out.write(buffer, 0, count);
                    out.flush();
                }

                out.close();
                bfis.close();
                System.out.println("File is transferred!");
            } catch (IOException e) {
                e.printStackTrace();
            }

            socket.close();
        }
    }

    private void listenForFiles() throws IOException {
        log("Listening for files on port " + this.address.getPort());
        this.serverSocket = new ServerSocket(this.address.getPort());

        while(!this.isShuttingDown) {
            Socket request;
            try {
                request = this.serverSocket.accept();
                handleFileRequest(request);
                request.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleFileRequest(Socket request) throws IOException {
        InputStream in = request.getInputStream();
        byte[] fileSizeBuffer = new byte[Constants.FILE_METADATA_LENGTH];
        in.read(fileSizeBuffer, 0, Constants.FILE_METADATA_LENGTH);
        JSONObject metadata = (JSONObject) JSONValue.parse(new String(fileSizeBuffer)+"\n");

        log("Incoming metadata " + metadata);

        in.close();

//        FileOutputStream fos = new FileOutputStream(replicatedFilesPath.toString() + "/");
//        this.readAndWriteFile(fos);
    }

    @Override
    public void onReady(Runnable runnable) {
        onReadyRunnables.add(runnable);
    }

    @Override
    public void onShutdown(Runnable runnable) {
        onShutdownRunnables.add(runnable);
    }

    public void onFilesReplicated(Runnable runnable) {
        onFilesReplicatedRunnables.add(runnable);
    }

    private void log(String str) {
        System.out.println("[" + name + "@" + hash + "] " + str);
    }
}
