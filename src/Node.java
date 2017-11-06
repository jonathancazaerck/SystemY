import java.net.InetAddress;

public class Node {
    public InetAddress ip;
    public String name;
    public int id;
    public NodeOperations nodeOperations;

    public Node(String name, InetAddress ip, NodeOperations nodeOperations) {
        this.name = name;
        this.id = Util.hash(name);
        this.nodeOperations = nodeOperations;
    }
}
