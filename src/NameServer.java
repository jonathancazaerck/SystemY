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

    public static void main(String[] args){
    }
}
