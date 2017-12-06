package ds3;

import java.io.Serializable;
import java.util.ArrayList;

public class FilesAgent implements Serializable, Runnable {
    private ArrayList<FileRef> fileList = new ArrayList<FileRef>();
    private Node currentNode;

    public void setCurrentNode(Node node) {
        this.currentNode = node;
    }

    @Override
    public void run() {
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

    private void log(String str) {
        System.out.println("[files_agent] " + str);
    }
}
