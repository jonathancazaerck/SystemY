package ds3;

public class FileNotPresentException extends Exception {
    FileNotPresentException() {
        super("File is not present (not in local, or not in replicated when overridden)");
    }
}
