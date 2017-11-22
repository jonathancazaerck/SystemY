package ds3;

public class UnknownMessageException extends Exception {
    public UnknownMessageException(String type, String data) {
        super("Unknown message type: " + type + " of message " + data);
    }
}
