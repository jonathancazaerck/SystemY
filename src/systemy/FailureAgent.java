package systemy;

public class FailureAgent implements Agent {
    private int failedNodeHash;
    private final int startedOnNodeHash;

    private final ShallowRing shallowRing;
    private final ShallowRing shallowRingWithFailedNode;

    private transient Node currentNode;

    public FailureAgent(int failedNodeHash, int startedOnNodeHash, ShallowRing shallowRing) {
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
        return shallowRing.higherModular(currentNode.getHash()) != startedOnNodeHash;
    }

    @Override
    public void run() {
        int currentNodeHash = currentNode.getHash();
        if (currentNode.getNextNodeHash() == failedNodeHash) {
            currentNode.setNextNodeHash(shallowRing.higherModular(currentNodeHash));
        }
        if (currentNode.getPrevNodeHash() == failedNodeHash) {
            currentNode.setPrevNodeHash(shallowRing.lowerModular(currentNodeHash));
        }
        for(FileRef fileRef : currentNode.getFileList().values()) {
            boolean fileBelongsOnFailedNode = fileRef.getLocationHash() == failedNodeHash;
            boolean currentNodeHasFile = shallowRing.lowerModular(Util.hash(fileRef.getFileName())) == currentNodeHash;
            if (fileBelongsOnFailedNode && currentNodeHasFile) {
                fileRef.setOverrideLocationHash(currentNodeHash);
                log("File " + fileRef.getFileName() + " belongs to failed node, i'm taking over.");
            }
        }
    }

    @Override
    public String getSort() {
        return "failure";
    }

    private void log(String str) {
        System.out.println("[failure_agent@" + currentNode.getName() + "] " + str);
    }
}
