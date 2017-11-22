package ds3;

public class ExistingNodeException extends Exception {
    public ExistingNodeException(String name) {
        super("A node with name " + name + " already exists!");
    }
}
