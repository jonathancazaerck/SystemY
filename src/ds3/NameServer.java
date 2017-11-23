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
import java.rmi.AlreadyBoundException;
import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

public class NameServer extends UnicastRemoteObject implements NameServerOperations, LifecycleHooks {
    private TreeMap<Integer, InetSocketAddress> nodeAddressMap;
    private InetAddress ip;

    private ArrayList<Runnable> onReadyRunnables;
    private ArrayList<Runnable> onShutdownRunnables;

    private MulticastSocket multicastSocket;

    private boolean isShuttingDown;

    private Registry registry;

    public NameServer(InetAddress ip) throws RemoteException {
        super();
        this.ip = ip;
        this.nodeAddressMap = new TreeMap<Integer, InetSocketAddress>();
        this.onReadyRunnables = new ArrayList<Runnable>();
        this.onShutdownRunnables = new ArrayList<Runnable>();
        this.isShuttingDown = false;
    }

    public void start() throws IOException, AlreadyBoundException, ParseException, UnknownMessageException, ExistingNodeException {
        System.setProperty("java.net.preferIPv4Stack", "true");

        log("Creating registry");
        registry = LocateRegistry.createRegistry(Constants.REGISTRY_PORT);
        log("Created registry");

        log("Binding this to registry");
        registry.bind("NameServer", this);
        log("Bound this to registry");

        InetAddress multicastIp = InetAddress.getByName(Constants.MULTICAST_IP);
        multicastSocket = new MulticastSocket(Constants.MULTICAST_PORT);
        multicastSocket.joinGroup(multicastIp);

        log("Ready to receive multicasts");

        log("Running ready hooks");
        onReadyRunnables.forEach(Runnable::run);

        while(!this.isShuttingDown) {
            byte[] buffer = new byte[1000];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            try {
                multicastSocket.receive(packet);
                handleMulticastPacket(packet);
            } catch (IOException e) {
                log("Closed socket, stopping");
            }
        }

        onShutdownRunnables.forEach(Runnable::run);
    }

    public void startWithoutExceptions() {
        try {
            start();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (AlreadyBoundException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        } catch (UnknownMessageException e) {
            e.printStackTrace();
        } catch (ExistingNodeException e) {
            e.printStackTrace();
        }
    }

    private void handleMulticastPacket(DatagramPacket packet) throws IOException, ParseException, UnknownMessageException {
        String msg = new String(packet.getData(), 0, packet.getLength(), "UTF-8");

        log("Received message: " + msg);

        JSONObject obj = (JSONObject) JSONValue.parseWithException(msg + "\n");

        String msgType = (String) obj.get("type");

        switch (msgType) {
            case "node_hello":
                String nodeName = (String) obj.get("name");
                InetAddress nodeIp = InetAddress.getByName((String) obj.get("ip"));
                int nodePort = ((Long) obj.get("port")).intValue();
                InetSocketAddress nodeAddress = new InetSocketAddress(nodeIp, nodePort);

                try {
                    this.registerNodeByName(nodeName, nodeAddress);
                } catch (ExistingNodeException e) {
                    log(e.getMessage());
                    return;
                }

                JSONObject responseMsg = new JSONObject();
                responseMsg.put("type", "nameserver_hello");
                responseMsg.put("ip", this.ip.getHostName());
                String responseStr = responseMsg.toJSONString();

                DatagramSocket datagramSocket = new DatagramSocket();

                log("Sending nameserver hello to " + nodeName + " at " + nodeAddress.toString());
                datagramSocket.send(new DatagramPacket(responseStr.getBytes(), responseStr.length(), nodeIp, nodePort));
                datagramSocket.close();

                break;
            default:
                throw new UnknownMessageException(msgType, msg);
        }
    }

    private void registerNodeByName(String name, InetSocketAddress address) throws ExistingNodeException {
        Integer hash = Util.hash(name);
        if(nodeAddressMap.containsKey(hash)) {
            throw new ExistingNodeException(name);
        } else {
            log("Registered node " + name + " with address " + address.toString());
            nodeAddressMap.put(hash, address);
        }
    }

    private InetSocketAddress getAddressToReplicateTo(Integer filehash){
        Integer foundKey;
        foundKey = nodeAddressMap.floorKey(filehash); //FloorKey returns a key-value mapping associated with the greatest key less than or equal to the given key, or null if there is no such key.
        if(foundKey == null){
            foundKey = nodeAddressMap.lastKey();
        }
        return nodeAddressMap.get(foundKey);
    }

    public InetSocketAddress getAddressByName(String name) {
        return nodeAddressMap.get(Util.hash(name));
    }

    private void printTreemap() {
        for(Map.Entry<Integer, InetSocketAddress> entry : nodeAddressMap.entrySet()){
            log("Key: "+entry.getKey()+". Value: "+entry.getValue());
        }
    }

    private void removeNodeByName(String name) {
        nodeAddressMap.remove(Util.hash(name));
    }

    public InetSocketAddress getAddressByFileName(String fileName) {
        if (nodeAddressMap.isEmpty()) {
            log("No nodes!");
            return null;
        } else {
            Integer fileNameHash = Util.hash(fileName);
            Integer closestHash = null;
            Integer biggestHash = null;
            for (Map.Entry<Integer, InetSocketAddress> entry : nodeAddressMap.entrySet()) {
                int nodeHash = entry.getKey();
                int diff = fileNameHash - nodeHash;
                if (diff < 0 && (biggestHash == null || nodeHash > biggestHash)) {
                    biggestHash = nodeHash;
                } else if (closestHash == null || diff < closestHash) {
                    closestHash = nodeHash;
                }
            }
            return closestHash != null ? nodeAddressMap.get(closestHash) : nodeAddressMap.get(biggestHash);
        }
    }

    public int getNumberOfNodes() {
        return nodeAddressMap.size();
    }

    public int getPrevHash(int hash) {
        Map.Entry<Integer, InetSocketAddress> prevEntry = nodeAddressMap.lowerEntry(hash);

        if (prevEntry == null) {
            return nodeAddressMap.lastKey();
        } else {
            return prevEntry.getKey();
        }
    }

    public int getNextHash(int hash) {
        Map.Entry<Integer, InetSocketAddress> nextEntry = nodeAddressMap.higherEntry(hash);

        if (nextEntry == null) {
            return nodeAddressMap.firstKey();
        } else {
            return nextEntry.getKey();
        }
    }

    public void exportJSON() {
        JSONObject obj = new JSONObject();

        for(Map.Entry<Integer, InetSocketAddress> entry : nodeAddressMap.entrySet()) {
            InetSocketAddress address = entry.getValue();
            obj.put(entry.getKey().toString(), address.getHostName()+":"+address.getPort());
        }

        File file = new File("tmp/nameserver.json");
        file.mkdirs();
        try (FileWriter fileWriter = new FileWriter(file)) {
            fileWriter.write(obj.toJSONString());
            log("Successfully wrote nameserver.json\n");
            log("JSON Object: " + obj);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void importJSON() {
        try {
            String str = new String(Files.readAllBytes(Paths.get("nameserver.json")));
            JSONObject obj = (JSONObject) JSONValue.parseWithException(str);

            for(Object objEntry : obj.entrySet()) {
                Map.Entry<String, String> entry = (Map.Entry<String, String>) objEntry;
                log(entry.toString());
                String[] v = entry.getValue().split(":");
                InetAddress ip = InetAddress.getByName(v[0]);
                int port = Integer.parseInt(v[1]);
                nodeAddressMap.put(Integer.parseInt(entry.getKey()), new InetSocketAddress(ip, port));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    public void shutdown() {
        this.isShuttingDown = true;
        this.multicastSocket.close();
        try {
            UnicastRemoteObject.unexportObject(this.registry, true);
        } catch (NoSuchObjectException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onReady(Runnable runnable) {
        onReadyRunnables.add(runnable);
    }

    @Override
    public void onShutdown(Runnable runnable) {
        onShutdownRunnables.add(runnable);
    }

    private void log(String str) {
        System.out.println("[nameserver] " + str);
    }
}
