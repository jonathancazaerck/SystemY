package ds3;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;

import java.io.FileWriter;
import java.io.IOException;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;
import java.util.TreeMap;

public class NameServer extends UnicastRemoteObject implements NameServerOperations {
    private TreeMap<Integer, InetSocketAddress> nodeAddressMap;
    private InetAddress ip;

    public NameServer(InetAddress ip) throws RemoteException {
        super();
        this.ip = ip;
        this.nodeAddressMap = new TreeMap<Integer, InetSocketAddress>();
    }

    public void start() {
        Registry registry;
        try {
            System.out.println("Creating registry");
            registry = LocateRegistry.createRegistry(Constants.REGISTRY_PORT);
            System.out.println("Created registry");

            System.out.println("Binding this to registry");
            registry.bind("NameServer", this);
            System.out.println("Bound this to registry");

            InetAddress multicastIp = InetAddress.getByName(Constants.MULTICAST_IP);
            MulticastSocket multicastSocket = new MulticastSocket(Constants.MULTICAST_PORT);
            multicastSocket.joinGroup(multicastIp);

            System.out.println("Ready to receive multicasts");

            while(true) {
                byte[] buffer = new byte[1000];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                multicastSocket.receive(packet);

                handleMulticastPacket(packet);
            }
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

    private void handleMulticastPacket(DatagramPacket packet) throws IOException, ParseException, UnknownMessageException, ExistingNodeException {
        String msg = new String(packet.getData(), 0, packet.getLength(), "UTF-8");

        System.out.println("Received message: " + msg);

        JSONObject obj = (JSONObject) JSONValue.parseWithException(msg + "\n");

        String msgType = (String) obj.get("type");

        switch (msgType) {
            case "node_hello":
                String nodeName = (String) obj.get("name");
                InetAddress nodeIp = InetAddress.getByName((String) obj.get("ip"));
                int nodePort = ((Long) obj.get("port")).intValue();
                InetSocketAddress nodeAddress = new InetSocketAddress(nodeIp, nodePort);


                this.registerNodeByName(nodeName, nodeAddress);

                JSONObject responseMsg = new JSONObject();
                responseMsg.put("type", "nameserver_hello");
                responseMsg.put("ip", "localhost");
                String responseStr = responseMsg.toJSONString();

                DatagramSocket datagramSocket = new DatagramSocket();

                System.out.println("Sending nameserver hello to " + nodeName + " at " + nodeAddress.toString());
                datagramSocket.send(new DatagramPacket(responseStr.getBytes(), responseStr.length(), nodeIp, nodePort));

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
            System.out.println("Registered node " + name + " with address " + address.toString());
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
            System.out.println("Key: "+entry.getKey()+". Value: "+entry.getValue());
        }
    }

    private void removeNodeByName(String name) {
        nodeAddressMap.remove(Util.hash(name));
    }

    public InetSocketAddress getAddressByFileName(String fileName) {
        if (nodeAddressMap.isEmpty()) {
            System.out.println("No nodes!");
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
        // TODO: fixen
        return hash;
    }

    public int getNextHash(int hash) {
        // TODO: fixen
        return hash;
    }

    @SuppressWarnings("unchecked")
    public void exportJSON() {
        JSONObject obj = new JSONObject();

        for(Map.Entry<Integer, InetSocketAddress> entry : nodeAddressMap.entrySet()) {
            InetSocketAddress address = entry.getValue();
            obj.put(entry.getKey().toString(), address.getHostName()+":"+address.getPort());
        }

        try (FileWriter file = new FileWriter("nameserver.json")) {
            file.write(obj.toJSONString());
            System.out.println("Successfully Copied JSON Object to ds3.File...");
            System.out.println("\nJSON Object: " + obj);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    public void importJSON() {
        try {
            String str = new String(Files.readAllBytes(Paths.get("nameserver.json")));
            JSONObject obj = (JSONObject) JSONValue.parseWithException(str);

            for(Object objEntry : obj.entrySet()) {
                Map.Entry<String, String> entry = (Map.Entry<String, String>) objEntry;
                System.out.println(entry);
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
}
