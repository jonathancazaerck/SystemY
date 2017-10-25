import org.json.simple.JSONObject;

import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.rmi.AlreadyBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;
import java.util.TreeMap;

public class NameServer extends UnicastRemoteObject implements NameServerOperations {
    private TreeMap<Integer, InetAddress> nodeIpMap;

    public NameServer() throws RemoteException {
        super();
        this.nodeIpMap = new TreeMap<Integer, InetAddress>();
    }

    public void registerNodeByName(String name, InetAddress ip) {
        Integer hash = Util.hash(name);
        // @Hans: Try en Catch niet beter? En zo ja, kan je dit implementeren?
        if(nodeIpMap.containsKey(hash)) {
            System.out.println("Naam " + name + " bestaat reeds!");
        } else {
            System.out.println("Registered node "+ name + " with ip " + ip.toString());
            nodeIpMap.put(hash, ip);
            exportToJSON();
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


    public void exportToJSON() {
        JSONObject obj = new JSONObject();

        for(Map.Entry<Integer, InetAddress> entry : nodeIpMap.entrySet()) {
            obj.put(entry.getKey(), entry.getValue().getHostAddress());
        }

        try (FileWriter file = new FileWriter("nameserver.json")) {
            file.write(obj.toJSONString());
            System.out.println("Successfully Copied JSON Object to File...");
            System.out.println("\nJSON Object: " + obj);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
