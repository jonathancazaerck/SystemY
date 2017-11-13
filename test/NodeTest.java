import static org.junit.jupiter.api.Assertions.assertEquals;
import com.sun.tools.internal.jxc.ap.Const;
import org.junit.jupiter.api.Test;
public class NodeTest {
    @Test
    public void test(){
        NodeMain.id = 10;
        NodeMain.nextNodeId = 13;
        NodeMain.prevNodeId = 5;
        NodeMain.handleNewNode(12);
        assertEquals(12, NodeMain.nextNodeId);
    }
}


