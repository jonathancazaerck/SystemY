package ds3;

public class UnknownMessageException extends Exception {
    public UnknownMessageException(String type) {
        super("Unknown message type: " + type);
    }
}
