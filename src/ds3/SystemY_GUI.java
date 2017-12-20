package ds3;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import javax.swing.JComponent;

public class SystemY_GUI {
    private JList fileList;
    private JButton button1;
    private ArrayList<String> inputList;
    DefaultListModel<String> model;

    public SystemY_GUI() {
        JFrame frame = new JFrame("System Y");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }

    public void updateJList() {
        /*model = new DefaultListModel<String>();
        for (String filename : inputList) {
            model.addElement(filename);
        }
        fileList.setModel(model);
        fileList.setSelectedIndex(0);*/
    }

    public void addToList(String in) {
        //inputList.add(in);
    }


}

