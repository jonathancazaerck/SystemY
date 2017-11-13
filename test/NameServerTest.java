import static org.junit.jupiter.api.Assertions.assertEquals;
import com.sun.tools.internal.jxc.ap.Const;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.rmi.RemoteException;

public class NameServerTest {
    @Test
    public void addAndRemove() throws RemoteException, UnknownHostException {
        NameServer ns = new NameServer();
        assertEquals(0, ns.getNumberOfNodes());
        ns.registerNodeByName("jill", new InetSocketAddress("192.168.0.1", Constants.DEFAULT_PORT));
        assertEquals(1, ns.getNumberOfNodes());
        ns.removeNodeByName("jill");
        assertEquals(0, ns.getNumberOfNodes());
    }
}
