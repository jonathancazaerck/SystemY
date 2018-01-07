package ds3;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.rmi.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class NameServer extends UnicastRemoteObject implements NameServerOperations, NameServerLifecycleHooks {

    //allRing is a type Ring and contains all the nodes
    private final Ring allRing = new Ring();
    //readyRing is a type Ring and contains all the nodes which are fully integrated and ready
    private final Ring readyRing = new Ring();
    private final InetAddress ip;

    //Runnable = functions that needs to be excecuted
    private final ArrayList<Runnable> onReadyRunnables = new ArrayList<Runnable>();
    private final ArrayList<Runnable> onShutdownRunnables = new ArrayList<Runnable>();

    private MulticastSocket multicastSocket;

    private boolean isShuttingDown = false;

    //Registry for RMI
    private Registry registry;

    //NameServer constructor
    public NameServer(InetAddress ip) throws RemoteException {
        super();
        this.ip = ip;
    }

    public void start() throws IOException, AlreadyBoundException, ParseException, UnknownMessageException, ExistingNodeException {

        //Disable IPv6
        System.setProperty("java.net.preferIPv4Stack", "true");

        //REGISTRY FOR RMI
        //--------------------------------------------------------------------------------------------------------------------------------------
        log("Creating registry");
        registry = LocateRegistry.createRegistry(Constants.REGISTRY_PORT); //create a remote object registry that accepts calls on REGISTRY_PORT
        log("Created registry");

        log("Binding this to registry");
        registry.bind("NameServer", this); //binds a remote reference to the specified name in this registry
        log("Bound this to registry");
        //--------------------------------------------------------------------------------------------------------------------------------------

        //MULTICASTS
        //--------------------------------------------------------------------------------------------------------------------------------------
        InetAddress multicastIp = InetAddress.getByName(Constants.MULTICAST_IP); //determines the ip adres of the host
        multicastSocket = new MulticastSocket(Constants.MULTICAST_PORT); //make new multicastSocket
        multicastSocket.joinGroup(multicastIp); //let the ip adres of the host join the multicastSocket group
        log("Ready to receive multicasts");
        //--------------------------------------------------------------------------------------------------------------------------------------


        log("Running ready hooks");
        onReadyRunnables.forEach(Runnable::run); //do each function in the list onReadyRunnables

        while(!this.isShuttingDown) { //if isShuttingDown is false:
            byte[] buffer = new byte[Constants.MAX_MESSAGE_SIZE]; //byte array with MAX_MESSAGE_SIZE size
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length); //constructs a DatagramPacket for receiving packets of length MAX_MESSAGE_SIZE
            try {
                multicastSocket.receive(packet); //when a message is received from any of the multicastIp's, it will be stored in packet
                handleMulticastPacket(packet);
            } catch (SocketException e) {
                log("Closed socket, stopping");
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }

        onShutdownRunnables.forEach(Runnable::run);
    }

    //With this method, the nameserver can response to multicasts. Nodes can send four kinds of multicasts.
    private void handleMulticastPacket(DatagramPacket packet) throws IOException, ParseException, UnknownMessageException, InterruptedException {
        JSONObject obj = Util.extractJSONFromPacket(packet);

        String msgType = (String) obj.get("type");
        int nodeHash = ((int) (long) obj.get("hash"));

        switch (msgType) {
            case "node_hello": //multicast from node to nameserver with parameters type, hash, name, ip and port
                String nodeName = (String) obj.get("name"); //get name into nodeName
                InetAddress nodeIp = InetAddress.getByName((String) obj.get("ip")); //get ip into nodeIp
                int nodePort = ((Long) obj.get("port")).intValue(); //get port into nodePort
                InetSocketAddress nodeAddress = new InetSocketAddress(nodeIp, nodePort); //creates socket address from nodeIp en nodePort

                try {
                    this.registerNode(nodeHash, nodeAddress); //add node to allRing
                    log("Registered node " + nodeName + " with address " + nodeAddress.toString());
                } catch (ExistingNodeException e) {
                    log(e.getMessage());
                    return;
                }

                // make a response message to answer to the nodes multicast
                JSONObject responseMsg = new JSONObject();
                responseMsg.put("type", "nameserver_hello");
                responseMsg.put("ip", this.ip.getHostAddress());
                responseMsg.put("amount", getNumberOfNodes()); // how many nodes are there in the network
                String responseStr = responseMsg.toJSONString();

                DatagramSocket datagramSocket = new DatagramSocket();
                TimeUnit.MILLISECONDS.sleep(100); // between sending and listening

                // send response to nodeIP with nodePort
                log("Sending nameserver hello to " + nodeName + " at " + nodeAddress.toString());
                datagramSocket.send(new DatagramPacket(responseStr.getBytes(), responseStr.length(), nodeIp, nodePort));
                datagramSocket.close();

                break;
            case "node_bound":
                break; // do nothing
            case "node_ready":
                this.readyRing.put(nodeHash, this.allRing.get(nodeHash)); // node is ready and can put into readyRing
                break;
            case "node_shutdown":
                this.allRing.remove(nodeHash); // remove node from allRing
                this.readyRing.remove(nodeHash); // remove node from readyRing
                break;
            default:
                throw new UnknownMessageException(msgType);
        }
    }

    //Method to add new node. If the node allready exist, an exception will be throwed.
    //Otherwise the node is add to the allRing of type Ring (treemap)
    private void registerNode(int hash, InetSocketAddress address) throws ExistingNodeException {
        if(this.allRing.containsKey(hash)) {
            throw new ExistingNodeException(hash);
        } else {
            this.allRing.put(hash, address);
        }
    }

    //Methode to remove a node when it fails, given the hash of the node.
    public void notifyFailure(int nodeHash) {
        this.allRing.remove(nodeHash);
        this.readyRing.remove(nodeHash);
    }

    //Get the IP adres from node with nodeHash
    public InetSocketAddress getAddressByHash(int nodeHash) {
        return this.allRing.get(nodeHash);
    }

    //We always replicate to a lower hash ID
    public int getNodeHashToReplicateTo(int fileHash) {
        return this.readyRing.lowerModularKey(fileHash);
    }

    //Get number of all nodes
    public int getNumberOfNodes() {
        return this.allRing.size();
    }

    public void exportJSON() {
        JSONObject obj = new JSONObject();

        for(Map.Entry<Integer, InetSocketAddress> entry : allRing.entrySet()) {
            InetSocketAddress address = entry.getValue();
            obj.put(entry.getKey().toString(), address.getAddress().getHostAddress() + ":" + address.getPort());
        }

        File file = new File("tmp/nameserver.json");
        file.mkdirs();
        try {
            FileWriter fileWriter = new FileWriter(file);
            fileWriter.write(obj.toJSONString());
            log("Successfully wrote nameserver.json\n");
            log("JSON Object: " + obj);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void importJSON() {
        try {
            String str = new String(Files.readAllBytes(Paths.get("tmp/nameserver.json")));
            JSONObject obj = (JSONObject) JSONValue.parseWithException(str);

            for(Object objEntry : obj.entrySet()) {
                Map.Entry<String, String> entry = (Map.Entry<String, String>) objEntry;
                log(entry.toString());
                String[] v = entry.getValue().split(":");
                InetAddress ip = InetAddress.getByName(v[0]);
                int port = Integer.parseInt(v[1]);
                allRing.put(Integer.parseInt(entry.getKey()), new InetSocketAddress(ip, port));
            }
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
    }

    @Override
    public ShallowRing getShallowRing() throws RemoteException {
        return ShallowRing.fromRing(readyRing);
    }

    public void shutdown() throws RemoteException, NotBoundException {
        this.isShuttingDown = true;
        this.multicastSocket.close();
        this.registry.unbind("NameServer");
        UnicastRemoteObject.unexportObject(this.registry, true);
    }

    //Method to add to the list onReadyRunnables
    @Override
    public void onReady(Runnable runnable) {
        onReadyRunnables.add(runnable);
    }

    //Method to add to the list onShutdownRunnables
    @Override
    public void onShutdown(Runnable runnable) {
        onShutdownRunnables.add(runnable);
    }

    //Method to print
    private void log(String str) {
        System.out.println("[nameserver] " + str);
    }
}
