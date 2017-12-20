package ds3;

import java.util.ArrayList;

public class FilesAgent implements Agent {
    private ArrayList<FileRef> fileList = new ArrayList<>();
    private transient Node currentNode;

    public void setCurrentNode(Node node) {
        this.currentNode = node;
    }

    @Override
    public void run() {
        log("starting");

        ArrayList<FileRef> nodeFileList = this.currentNode.getFileList();

        for(FileRef nodeFileRef : nodeFileList) {
            FileRef foundFileRef = null;

            for(FileRef fileRef : fileList) {
                if (nodeFileRef.getFileName().equals(fileRef.getFileName())) {
                    foundFileRef = fileRef;
                    break;
                }
            }

            if (foundFileRef == null) nodeFileList.add(new FileRef(nodeFileRef.getFileName()));
        }
    }

    @Override
    public boolean shouldProceed() {
        return true;
    }

    private void log(String str) {
        System.out.println("[files_agent@" + currentNode.getName() + "] " + str);
    }
}
