import ds3.NameServer;
import ds3.Node;
import ds3.NodeMain;
import org.junit.Test;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.rmi.RemoteException;

public class NodeTest {
    @Test
    public void test() throws RemoteException, UnknownHostException {
        Node node = new Node("dalk", new InetSocketAddress("localhost",6666));
        NameServer nameserver = new NameServer(InetAddress.getByName("localhost"));
        Node node2 = new Node("hans", new InetSocketAddress("localhost",6667));


        Thread tNS = new Thread(){
            public void run(){
                nameserver.start();
            }
        };

        Thread tN1 = new Thread(){
            public void run(){
                node.start();
            }
        };

        Thread tN2 = new Thread(){
            public void run(){
                node2.start();
            }
        };

        tNS.start();
        tN1.start();
        tN2.start();
    }


}


