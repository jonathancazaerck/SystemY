package ds3;

public class ExistingNodeException extends Exception {
    public ExistingNodeException(int hash) {
        super("A node with hash " + hash + " already exists!");
    }
}
