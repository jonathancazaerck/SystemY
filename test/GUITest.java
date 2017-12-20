import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.fail;


public class GUITest {
    @Test
    void test() {
       SystemY_GUI gui = new SystemY_GUI();
       gui.addToList("test1.txt");
       gui.addToList("test2.txt");
       gui.addToList("test3.txt");
       gui.updateJList();
    }
}
