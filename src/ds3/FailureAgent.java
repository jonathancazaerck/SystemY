package ds3;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.ArrayList;

public class FailureAgent implements Agent {
    private int failedNodeHash;
    private int startedOnNodeHash;

    private Ring ring;

    private transient Node currentNode;

    private ArrayList<FileRef> fileList;

    public FailureAgent(int failedNodeHash, int startedOnNodeHash, Ring ring){
        this.failedNodeHash = failedNodeHash;
        this.startedOnNodeHash = startedOnNodeHash;
        this.ring = ring;
    }

    public void setCurrentNode(Node node) { this.currentNode = node; }

    @Override
    public boolean shouldProceed() {
        return false;
    }

    public void setFileList(ArrayList<FileRef> fileList){this.fileList = fileList;}

    @Override
    public void run(){
        int hashNode;
        for(FileRef fileRef : fileList) {
            if(fileRef.getLocation() == FileRefLocation.LOCAL){
                if(ring.lowerModularEntry(Util.hash(fileRef.getFileName())) == failedNodeHash){
                    try {
                        currentNode.replicateFile(fileRef);
                    } catch (java.io.IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private void log(String str) {
        System.out.println("[failure_agent@" + currentNode.getName() + "] " + str);
    }
}
