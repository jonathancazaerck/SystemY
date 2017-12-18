package ds3;

import javax.swing.*;
import java.util.ArrayList;

public class SystemY_GUI {
    private JList fileList;
    private ArrayList<String> inputList;
    DefaultListModel<String> model;

    public SystemY_GUI (){
        inputList = new ArrayList<String>();
    }

    public void updateJList(){
        model = new DefaultListModel<String>();
        for(String filename: inputList){
            model.addElement(filename);
        }
        fileList.setModel(model);
        fileList.setSelectedIndex(0);
    }

    public void addToList(String in){
        inputList.add(in);
    }


}

