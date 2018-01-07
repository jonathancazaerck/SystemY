package systemy;

import java.awt.*;
import java.io.File;
import java.io.IOException;

public abstract class OSHelper {
    public static void openFile(File file) throws IOException {
        if(!Desktop.isDesktopSupported()){
            System.out.println("Desktop is not supported");
            return;
        }

        Desktop desktop = Desktop.getDesktop();
        System.out.println("Opening " + file.getAbsolutePath());
        desktop.open(file);
    }
}
