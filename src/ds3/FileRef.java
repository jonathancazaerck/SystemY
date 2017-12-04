package ds3;

public class FileRef {
    private String fileName;
    private boolean locked = false;

    public FileRef(String fileName){
        this.fileName = fileName;
    }
}
