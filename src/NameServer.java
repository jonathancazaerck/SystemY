import java.net.InetAddress;
import java.util.HashMap;

public class NameServer {
    public HashMap<Integer, InetAddress> nodeIpMap;

    public NameServer() {
        this.nodeIpMap = new HashMap<Integer, InetAddress>();
    }
}
