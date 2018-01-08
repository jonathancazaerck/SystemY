package systemy;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class Gui {
    private JList<String> fileList;
    private JButton downloadButton;
    private JButton openButton;
    private JPanel jpanel;
    private JLabel selectedItemLabel;
    private JLabel fileLocationLabel;
    private JLabel statusLabel;
    private JLabel nodeInfoLabel;
    private ArrayList<FileRef> fileRefs;
    private FileRef selectedFile;

    private String nodeName;
    private InetSocketAddress nodeAddress;
    private Path nodeFilesPath;

    private Runnable onClickDownloadRunnable;
    private Runnable onClickOpenRunnable;


    public Gui() {
        fileList.addListSelectionListener(e -> {
            int index = fileList.getSelectedIndex();
            if (index == -1) {
                selectedFile = null;
                selectedItemLabel.setText("/");
                fileLocationLabel.setText("/");
            } else {
                selectedFile = fileRefs.get(index);
                selectedItemLabel.setText(selectedFile.getFileName());
                int actualLocationHash = selectedFile.getActualLocationHash();
                String locationStr = String.valueOf(selectedFile.getActualLocationHash());
                if (actualLocationHash == Util.hash(nodeName)) {
                    locationStr += " (this)";
                }
                fileLocationLabel.setText(locationStr);
            }
        });

        downloadButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (selectedFile != null) {
                    JOptionPane.showMessageDialog(jpanel, "Het bestand: " + selectedFile.getFileName() + " wordt gedownloaded.", "Bericht", JOptionPane.INFORMATION_MESSAGE);
                    onClickDownloadRunnable.run();
                } else {
                    showError();
                }
            }
        });

        openButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (selectedFile != null) {
                    JOptionPane.showMessageDialog(jpanel, "Het bestand: " + selectedFile.getFileName() + " wordt geopend.", "Bericht", JOptionPane.INFORMATION_MESSAGE);
                    onClickOpenRunnable.run();
                } else {
                    showError();
                }
            }
        });
    }

    public static Gui start() {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }
        Gui gui = new Gui();
        JFrame frame = new JFrame("SystemY");
        frame.setContentPane(gui.jpanel);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
        return gui;
    }

    public void reload() {
        DefaultListModel<String> listModel = new DefaultListModel<>();
        for (FileRef fileRef : fileRefs) {
            listModel.addElement(fileRef.getFileName());
            System.out.println("[gui] added element " + fileRef.getFileName());
        }
        fileList.setModel(listModel);
    }

    public void setFileList(ArrayList<FileRef> fileRefs) {
        this.fileRefs = fileRefs;
        reload();
    }

    public void showError() {
        JOptionPane.showMessageDialog(jpanel, "Er is geen bestand geselecteerd. Het proces wordt afgebroken.", "Error", JOptionPane.ERROR_MESSAGE);
    }

    public Node getNode() throws IOException {
        JPanel panel = new JPanel(new BorderLayout(5, 5));

        JPanel label = new JPanel(new GridLayout(0, 1, 2, 2));
        label.add(new JLabel("Naam:", SwingConstants.RIGHT));
        label.add(new JLabel("IP-adres:", SwingConstants.RIGHT));
        label.add(new JLabel("Poort:", SwingConstants.RIGHT));
        label.add(new JLabel("Map:", SwingConstants.RIGHT));
        panel.add(label, BorderLayout.WEST);

        JPanel controls = new JPanel(new GridLayout(0, 1, 2, 2));
        JTextField FNodeName = new JTextField();
        controls.add(FNodeName);
        JTextField FNodeIP = new JTextField();
        FNodeIP.setText("localhost");
        controls.add(FNodeIP);
        JTextField FNodePort = new JTextField();
        FNodePort.setText("6790");
        controls.add(FNodePort);
        JTextField FNodeFolder = new JTextField();
        FNodeFolder.setText("tmp/files");
        controls.add(FNodeFolder);

//        JFileChooser f = new JFileChooser();
//        f.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
//        f.showSaveDialog(null);
//        //nodeFolder = f.getCurrentDirectory() + f.getSelectedFile()
//        nodeFolder = new StringBuilder().append(f.getCurrentDirectory()).append("/").append(f.getSelectedFile()).toString();
//        controls.add(f);

        panel.add(controls, BorderLayout.CENTER);

        JOptionPane.showMessageDialog(jpanel, panel, "Node settings", JOptionPane.QUESTION_MESSAGE);

        String nodeIpName = FNodeIP.getText();
        int nodePort = Integer.parseInt(FNodePort.getText());
        nodeFilesPath = Paths.get(FNodeFolder.getText());

        nodeName = FNodeName.getText();
        try {
            nodeAddress = new InetSocketAddress(InetAddress.getByName(nodeIpName), nodePort);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        Node.setFilesPath(nodeFilesPath);

        nodeInfoLabel.setText(nodeName + " @ " + nodeAddress.getHostString());

        return new Node(nodeName, nodeAddress);
    }

    public void setStatusLabel(String text) {
        this.statusLabel.setText(text);
    }

    public void onClickDownload(Runnable r) {
        onClickDownloadRunnable = r;
    }

    public void onClickOpen(Runnable r) {
        this.onClickOpenRunnable = r;
    }

    public FileRef getSelectedFile() {
        return selectedFile;
    }


    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        jpanel = new JPanel();
        jpanel.setLayout(new GridLayoutManager(6, 3, new Insets(20, 20, 20, 20), -1, -1));
        jpanel.setPreferredSize(new Dimension(800, 600));
        fileList = new JList();
        final DefaultListModel defaultListModel1 = new DefaultListModel();
        fileList.setModel(defaultListModel1);
        jpanel.add(fileList, new GridConstraints(0, 0, 5, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(283, 50), null, 0, false));
        downloadButton = new JButton();
        downloadButton.setText("Download");
        jpanel.add(downloadButton, new GridConstraints(2, 1, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(110, -1), null, null, 0, false));
        openButton = new JButton();
        openButton.setText("Open");
        jpanel.add(openButton, new GridConstraints(3, 1, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(110, -1), null, null, 0, false));
        nodeInfoLabel = new JLabel();
        nodeInfoLabel.setText("Cazaerck Jonathan - Claes Jill - Havermans Elias - Wirtz Hans Otto");
        jpanel.add(nodeInfoLabel, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        selectedItemLabel = new JLabel();
        selectedItemLabel.setText("/");
        jpanel.add(selectedItemLabel, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(100, -1), new Dimension(100, 22), null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("Geselecteerd:");
        label1.setVerticalAlignment(0);
        label1.setVerticalTextPosition(1);
        jpanel.add(label1, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(87, 22), null, 0, false));
        statusLabel = new JLabel();
        statusLabel.setText("Status");
        jpanel.add(statusLabel, new GridConstraints(5, 1, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("File locatie:");
        jpanel.add(label2, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(68, 33), null, 0, false));
        fileLocationLabel = new JLabel();
        fileLocationLabel.setText("/");
        jpanel.add(fileLocationLabel, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(5, 33), null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return jpanel;
    }
}




