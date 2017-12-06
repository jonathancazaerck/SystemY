package ds3;

public class FileRef {
    private String fileName;
    private boolean locked = false;
    private FileRefLocation location;

    public FileRef(String fileName) {
        this.fileName = fileName;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public FileRefLocation getLocation() {
        return location;
    }

    public void setLocation(FileRefLocation location) {
        this.location = location;
    }

    public String getFileName() {
        return fileName;
    }
}
