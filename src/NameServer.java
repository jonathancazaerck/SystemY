import org.json.simple.JSONObject;

import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;

public class NameServer {
    public HashMap<Integer, InetAddress> nodeIpMap;

    public NameServer() {
        this.nodeIpMap = new HashMap<Integer, InetAddress>();
    }

    public void registerNodeByName(String name, InetAddress ip) {
        nodeIpMap.put(Util.hash(name), ip);
    }

    public InetAddress getIpByName(String name) {
        return nodeIpMap.get(Util.hash(name));
    }

    public static void main(String[] args) {
    }
    public void toJSON(HashMap<Integer, InetAddress> map){
        JSONObject obj = new JSONObject(map);

        try (FileWriter file = new FileWriter("test.txt")) {
            file.write(obj.toJSONString());
            System.out.println("Successfully Copied JSON Object to File...");
            System.out.println("\nJSON Object: " + obj);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
