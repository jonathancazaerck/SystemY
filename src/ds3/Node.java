package ds3;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
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

    private ArrayList<Runnable> onReadyRunnables;
    private ArrayList<Runnable> onShutdownRunnables;
    private ArrayList<Runnable> onFilesReplicatedRunnables;

    public Node(String name, InetSocketAddress address) throws RemoteException {
        super();
        this.name = name;
        this.address = address;
        this.hash = Util.hash(name);
        this.onReadyRunnables = new ArrayList<Runnable>();
        this.onShutdownRunnables = new ArrayList<Runnable>();
        this.onFilesReplicatedRunnables = new ArrayList<Runnable>();
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

        replicateFiles();

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
    }

    public void shutdown() throws RemoteException, NotBoundException {
        NodeOperations prevNode = (NodeOperations) this.registry.lookup(Util.getNodeRegistryName(this.prevNodeHash));
        NodeOperations nextNode = (NodeOperations) this.registry.lookup(Util.getNodeRegistryName(this.nextNodeHash));

        prevNode.notifyNeighbourShutdown(false, this.nextNodeHash);
        nextNode.notifyNeighbourShutdown(true, this.prevNodeHash);

        this.registry.unbind(Util.getNodeRegistryName(this.hash));

        onShutdownRunnables.forEach(Runnable::run);
    }

    public void notifyNeighbourShutdown(boolean prev, int newNeighbourHash) {
        if (prev) {
            this.nextNodeHash = newNeighbourHash;
        } else {
            this.prevNodeHash = newNeighbourHash;
        }
    }

    public void replicateFiles() throws IOException, NotBoundException {
        File localFilesDir = new File("tmp/files/" + name + "/local");
        File replicatedFilesDir = new File("tmp/files/" + name + "/replicated");
        localFilesDir.mkdirs();
        replicatedFilesDir.mkdirs();

        File[] localFiles = localFilesDir.listFiles();
        if(localFiles == null) return;

        int count;
        byte[] buffer = new byte[(int) Math.pow(2, 10)];

        NameServerOperations nameServer = (NameServerOperations) registry.lookup("NameServer");

        for (File child : localFiles) {
            String name =  child.getName();
            int fileHash = Util.hash(name);

            int nodeHashToDupl = nameServer.getPrevHash(fileHash);

            if (nodeHashToDupl == hash) nodeHashToDupl = prevNodeHash;

            InetSocketAddress addressToDupl = nameServer.getAddressByHash(nodeHashToDupl);

            Socket socket = new Socket(addressToDupl.getAddress(), addressToDupl.getPort());

            try{
                OutputStream out = socket.getOutputStream();
                FileInputStream fis = new FileInputStream(child);
                BufferedInputStream bfis = new BufferedInputStream(fis);

                long fileSize = fis.getChannel().size();

                JSONObject metadataObj = new JSONObject();
                metadataObj.put("type", "file_metadata");
                metadataObj.put("name", name);
                metadataObj.put("size", fileSize);

                ByteBuffer metadataBuffer = ByteBuffer.allocate(200);
                metadataBuffer.put(metadataObj.toJSONString().getBytes());
                out.write(metadataBuffer.array(), 0, 200);

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
        }
    }

    private void listenForFiles() {
        
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
