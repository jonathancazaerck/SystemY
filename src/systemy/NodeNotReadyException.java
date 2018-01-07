package systemy;

public class NodeNotReadyException extends Exception {
    NodeNotReadyException(String name) {
        super("Node " + name + " isn't ready yet!");
    }
}
