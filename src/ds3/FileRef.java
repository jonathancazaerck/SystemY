package ds3;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileRef {
    private String fileName;
    private boolean locked = false;
    private FileRefLocation location;

    private static Path filesPath = Paths.get("tmp/files");

    public FileRef(String fileName) {
        this.fileName = fileName;
    }

    public boolean isLocked() {
        return locked;
    }

    public void lock() {
        this.locked = true;
    }

    public void unlock() {
        this.locked = false;
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

    public File getFile(String name){
        Path localFilesPath = Paths.get(filesPath.toAbsolutePath().toString(), name, "local", fileName);
        File file = localFilesPath.toFile();
        return file;
    }
}
