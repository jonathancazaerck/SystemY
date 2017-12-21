package ds3;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

public class Gui {
    private JList<String> fileList;
    private JButton herladenButton;
    private JButton downloadButton;
    private JButton openButton;
    private JButton deleteButton;
    private JPanel jpanel;
    private JLabel labelSelectedItem;
    private Collection<FileRef> fileRefs;
    private String selectedItem;

    public Gui() {
        herladenButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                reload();
            }
        });

        fileList.addListSelectionListener(e -> {
            selectedItem = fileList.getModel().getElementAt(fileList.getSelectedIndex());
            labelSelectedItem.setText(selectedItem);
        });

        downloadButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (selectedItem != null) {
                    JOptionPane.showMessageDialog(jpanel, "Het bestand: " + selectedItem + " wordt gedownloaded.", "Bericht", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    showError();
                }
            }
        });

        openButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (selectedItem != null) {
                    JOptionPane.showMessageDialog(jpanel, "Het bestand: " + selectedItem + " wordt geopend.", "Bericht", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    showError();
                }
            }
        });

        deleteButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (selectedItem != null) {
                    String[] options = {"Ja", "Nee"};
                    int i = JOptionPane.showOptionDialog(jpanel, "Wilt u het bestand: " + selectedItem + " verwijderen?", "Bericht", 0, JOptionPane.WARNING_MESSAGE, null, options, null);
                    if (i == 0)
                        System.out.println("File will be deleted!");
                    else
                        System.out.println("User makes a mistake!");
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

    public void setFileList(Collection<FileRef> fileRefs) {
        this.fileRefs = fileRefs;
        reload();
    }

    public void showError() {
        JOptionPane.showMessageDialog(jpanel, "Er is geen bestand geselecteerd. Het proces wordt afgebroken.", "Error", JOptionPane.ERROR_MESSAGE);
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
        jpanel.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(6, 3, new Insets(20, 20, 20, 20), -1, -1));
        jpanel.setPreferredSize(new Dimension(800, 600));
        fileList = new JList<String>();
        final DefaultListModel defaultListModel1 = new DefaultListModel();
        fileList.setModel(defaultListModel1);
        jpanel.add(fileList, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 5, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(283, 50), null, 0, false));
        herladenButton = new JButton();
        herladenButton.setEnabled(true);
        herladenButton.setText("Herladen");
        jpanel.add(herladenButton, new com.intellij.uiDesigner.core.GridConstraints(1, 1, 1, 2, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        downloadButton = new JButton();
        downloadButton.setText("Download");
        jpanel.add(downloadButton, new com.intellij.uiDesigner.core.GridConstraints(2, 1, 1, 2, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        openButton = new JButton();
        openButton.setText("Open");
        jpanel.add(openButton, new com.intellij.uiDesigner.core.GridConstraints(3, 1, 1, 2, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("Cazaerck Jonathan - Claes Jill - Havermans Elias - Wirtz Hans Otto");
        jpanel.add(label1, new com.intellij.uiDesigner.core.GridConstraints(5, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        deleteButton = new JButton();
        deleteButton.setText("Delete");
        jpanel.add(deleteButton, new com.intellij.uiDesigner.core.GridConstraints(4, 1, 1, 2, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, new Dimension(30, -1), null, null, 0, false));
        labelSelectedItem = new JLabel();
        labelSelectedItem.setForeground(new Color(-16776961));
        labelSelectedItem.setText("");
        jpanel.add(labelSelectedItem, new com.intellij.uiDesigner.core.GridConstraints(0, 2, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, new Dimension(100, -1), new Dimension(100, -1), null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("Geselecteerd:");
        label2.setVerticalAlignment(0);
        label2.setVerticalTextPosition(1);
        jpanel.add(label2, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(87, 16), null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return jpanel;
    }
}




