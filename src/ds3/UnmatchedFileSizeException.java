package ds3;

public class UnmatchedFileSizeException extends Exception {
    public UnmatchedFileSizeException(String name, long expectedSize, long actualSize) {
        super("File " + name + " had expected size " + expectedSize + " and actual size" + actualSize);
    }
}
