package ds3;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.ArrayList;

public class FailureAgent implements Agent {
    private int failedNodeHash;
    private final int startedOnNodeHash;

    private final ShallowRing shallowRing;
    private final ShallowRing shallowRingWithFailedNode;

    private transient Node currentNode;

    private ArrayList<FileRef> fileList;

    public FailureAgent(int failedNodeHash, int startedOnNodeHash, ShallowRing shallowRing){
        this.failedNodeHash = failedNodeHash;
        this.startedOnNodeHash = startedOnNodeHash;
        this.shallowRing = shallowRing;
        this.shallowRingWithFailedNode = ((ShallowRing) shallowRing.clone());
        this.shallowRingWithFailedNode.add(failedNodeHash);
    }

    public void setCurrentNode(Node node) {
        this.currentNode = node;
    }

    @Override
    public boolean shouldProceed() {
        return shallowRing.higherModular(currentNode.getHash()) == startedOnNodeHash;
    }

    @Override
    public void run(){
        log("Running failure agent");
        int currentNodeHash = currentNode.getHash();
        if (currentNode.getNextNodeHash() == failedNodeHash) {
            currentNode.setNextNodeHash(shallowRing.higherModular(currentNodeHash));
        }
        if (currentNode.getPrevNodeHash() == failedNodeHash) {
            currentNode.setPrevNodeHash(shallowRing.lowerModular(currentNodeHash));
        }
        for(FileRef fileRef : currentNode.getFileList()) {
            if(fileRef.getLocation() == FileRefLocation.LOCAL){
                if(shallowRingWithFailedNode.lowerModular(Util.hash(fileRef.getFileName())) == failedNodeHash){
                    try {
                        currentNode.replicateFile(fileRef);
                    } catch (java.io.IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        log("Running failure agent done");
    }

    private void log(String str) {
        System.out.println("[failure_agent@" + currentNode.getName() + "] " + str);
    }
}
