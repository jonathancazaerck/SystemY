package ds3;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.*;

public class FilesAgent implements Agent {
    private final TreeMap<Integer, FileRef> fileList = new TreeMap<>();
    private transient Node currentNode;

    private Integer lockedFileNodeHash = null;
    private FileRef lockedFile = null;
    private Integer nodeToSendToHash = null;

    public void setCurrentNode(Node node) {
        this.currentNode = node;
    }

    @Override
    public void run() {
        log("starting");

        File[] localFiles = currentNode.getLocalFilesPath().toFile().listFiles();
        if(localFiles == null) return;
        TreeSet<FileRef> updatedFileRefs = new TreeSet<>();

        for (FileRef fileRef : fileList.values()) {
            if (fileRef.isLocationDisappeared() && fileRef.getLocationHash() == currentNode.getHash()) {
                fileRef.setOverrideLocationHash(null);
                updatedFileRefs.add(fileRef);
            }
        }

        for (File localFile : localFiles) {
            String name = localFile.getName();
            int fileHash = Util.hash(name);

            if (!fileList.containsKey(fileHash)) {
                FileRef fileRef = new FileRef(name, currentNode.getHash());
                fileList.put(fileHash, fileRef);
                updatedFileRefs.add(fileRef);
            }
        }

        if(lockedFileNodeHash == currentNode.getHash()){

        }

        if(lockedFileNodeHash==null && lockedFile==null){
            lockedFile = currentNode.getLockRequest();
            if(lockedFile!=null){
                lockedFileNodeHash = currentNode.getHash();
                for (FileRef updatedFileRef : updatedFileRefs){
                    if(lockedFile.getFileNameHash() == updatedFileRef.getFileNameHash()){
                        updatedFileRef.lock();
                    }
                }
            }
        }

        if(lockedFile!=null){
            if(lockedFile.getActualLocationHash() == currentNode.getHash()){
                nodeToSendToHash = currentNode.getHash();
            }
        }

        currentNode.setFileList(fileList);
        currentNode.notifyUpdatedFiles(updatedFileRefs);
    }

    @Override
    public boolean shouldProceed() {
        return true;
    }

    @Override
    public String getSort() {
        return "files";
    }

    private void log(String str) {
        System.out.println("[files_agent@" + currentNode.getName() + "] " + str);
    }
}
