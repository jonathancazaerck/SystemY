package ds3;

import java.io.File;
import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileRef implements Serializable, Comparable<FileRef> {
    private String fileName;
    private boolean locked = false;
    private final Integer locationHash;
    private Integer overrideLocationHash;

    public FileRef(String fileName, Integer locationHash) {
        this.fileName = fileName;
        this.locationHash = locationHash;
    }

    public boolean isLocked() {
        return locked;
    }

    public void lock() {
        this.locked = true;
    }

    public int getFileNameHash() { return Util.hash(fileName); }

    public void unlock() {
        this.locked = false;
    }

    public int getLocationHash() {
        return locationHash;
    }

    public String getFileName() {
        return fileName;
    }

    public boolean isLocationDisappeared() {
        return overrideLocationHash != null;
    }

    public Integer getOverrideLocationHash() {
        return overrideLocationHash;
    }

    public void setOverrideLocationHash(Integer overrideLocationHash) {
        this.overrideLocationHash = overrideLocationHash;
    }

    public int getActualLocationHash() {
        return overrideLocationHash != null ? overrideLocationHash : locationHash;
    }

    @Override
    public int compareTo(FileRef other) {
        return Integer.compare(getFileNameHash(), other.getFileNameHash());
    }
}
