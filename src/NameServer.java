import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.io.FileWriter;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;
import java.util.TreeMap;

public class NameServer extends UnicastRemoteObject implements NameServerOperations {
    private TreeMap<Integer, InetAddress> nodeIpMap;

    public NameServer() throws RemoteException {
        super();
        this.nodeIpMap = new TreeMap<Integer, InetAddress>();
        printTreemap();

        try {
            InetAddress multicastIp = InetAddress.getByName(Constants.MULTICAST_IP);
            MulticastSocket socket = new MulticastSocket(Constants.MULTICAST_PORT);
            socket.joinGroup(multicastIp);

            byte[] buffer = new byte[1000];

            while(true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                System.out.println(packet.getAddress());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void registerNodeByName(String name, InetAddress ip) {
        Integer hash = Util.hash(name);
        // @Hans: Try en Catch niet beter? En zo ja, kan je dit implementeren?
        if(nodeIpMap.containsKey(hash)) {
            System.out.println("A node with name " + name + " already exists!");
        } else {
            System.out.println("Registered node "+ name + " with ip " + ip.toString());
            nodeIpMap.put(hash, ip);
        }
    }

    public InetAddress getIpByName(String name) {
        return nodeIpMap.get(Util.hash(name));
    }

    public void printTreemap() {
        for(Map.Entry<Integer, InetAddress> entry : nodeIpMap.entrySet()){
            System.out.println("Key: "+entry.getKey()+". Value: "+entry.getValue());
        }
    }

    public void removeNodeByName(String name) {
        nodeIpMap.remove(Util.hash(name));
    }

    public InetAddress getIpByFileName(String fileName) {
        if (nodeIpMap.isEmpty()) {
            System.out.println("No nodes!");
            return null;
        } else {
            Integer fileNameHash = Util.hash(fileName);
            Integer closestHash = null;
            Integer biggestHash = null;
            for (Map.Entry<Integer, InetAddress> entry : nodeIpMap.entrySet()) {
                int nodeHash = entry.getKey();
                int diff = fileNameHash - nodeHash;
                if (diff < 0 && (biggestHash == null || nodeHash > biggestHash)) {
                    biggestHash = nodeHash;
                } else if (closestHash == null || diff < closestHash) {
                    closestHash = nodeHash;
                }
            }
            return closestHash != null ? nodeIpMap.get(closestHash) : nodeIpMap.get(biggestHash);
        }
    }

    public int getNumberOfNodes() {
        return nodeIpMap.size();
    }

    @SuppressWarnings("unchecked")
    public void exportJSON() {
        JSONObject obj = new JSONObject();

        for(Map.Entry<Integer, InetAddress> entry : nodeIpMap.entrySet()) {
           obj.put(entry.getKey().toString(), entry.getValue().getHostAddress());
        }

        try (FileWriter file = new FileWriter("nameserver.json")) {
            file.write(obj.toJSONString());
            System.out.println("Successfully Copied JSON Object to File...");
            System.out.println("\nJSON Object: " + obj);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    public void importJSON() {
        try {
            String str = new String(Files.readAllBytes(Paths.get("nameserver.json")));
            JSONObject obj = (JSONObject) JSONValue.parse(str);

            for(Object objEntry : obj.entrySet()) {
                Map.Entry<String, String> entry = (Map.Entry<String, String>) objEntry;
                System.out.println(entry);
                nodeIpMap.put(Integer.parseInt(entry.getKey()), InetAddress.getByName(entry.getValue()));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
