package ds3;

import java.net.InetSocketAddress;

public class Node {
    public InetSocketAddress address;
    public String name;
    public static int id;
    public static int nextNodeId;
    public static int prevNodeId;
    public NodeOperations nodeOperations;

    public Node(String name, InetSocketAddress ip, NodeOperations nodeOperations) {
        this.name = name;
        this.id = Util.hash(name);
        this.nodeOperations = nodeOperations;
    }


}
