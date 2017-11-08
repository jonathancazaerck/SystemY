import java.net.InetAddress;

public class Node {
    public InetAddress ip;
    public String name;
    public static int id;
    public static int nextNodeId;
    public static int prevNodeId;
    public NodeOperations nodeOperations;

    public Node(String name, InetAddress ip, NodeOperations nodeOperations) {
        this.name = name;
        this.id = Util.hash(name);
        this.nodeOperations = nodeOperations;
    }


}
