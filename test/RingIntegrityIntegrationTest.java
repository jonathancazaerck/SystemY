import ds3.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.fail;

class RingIntegrityIntegrationTest {

    private NameServer nameServer;
    private ArrayList<Node> nodes;

    private Thread nameServerThread;
    private ArrayList<Thread> nodeThreads;

    private Node elias;
    private Node hans;
    private Node jonathan;
    private Node jill;

    private Thread eliasThread;
    private Thread hansThread;
    private Thread jonathanThread;
    private Thread jillThread;

    private volatile Exception threadException;

    @BeforeEach
    void setUp() throws IOException {
        System.setProperty("java.net.preferIPv4Stack", "true");

        Node.setFilesPath(Paths.get("fixtures/files"));

        nodeThreads = new ArrayList<Thread>();
        nodes = new ArrayList<Node>();

        nameServer = new NameServer(InetAddress.getByName("localhost"));
        nodes.add(this.elias = new Node("elias", new InetSocketAddress("localhost",6666)));
        nodes.add(this.hans = new Node("hans", new InetSocketAddress("localhost",6667)));
        nodes.add(this.jonathan = new Node("jonathan", new InetSocketAddress("localhost",6668)));
        nodes.add(this.jill = new Node("jill", new InetSocketAddress("localhost",6669)));

        nameServerThread = new Thread(() -> {
            try {
                System.out.println("Starting nameserver thread: ");
                nameServer.start();
            } catch (Exception e) {
                threadException = e;
            }
        });
        for (Node node : nodes) {
            nodeThreads.add(new Thread(() -> {
                try {
                    System.out.println("Starting node thread: " + node.getName());
                    node.start();
                } catch (Exception e) {
                    threadException = e;
                }
            }));
        }

        this.eliasThread = nodeThreads.get(nodes.indexOf(elias));
        this.hansThread = nodeThreads.get(nodes.indexOf(hans));
        this.jonathanThread = nodeThreads.get(nodes.indexOf(jonathan));
        this.jillThread = nodeThreads.get(nodes.indexOf(jill));
    }

    @AfterEach
    void checkForThreadException() {
        if (threadException != null) {
            threadException.printStackTrace();
            fail(threadException);
        }
    }

    @Test
    void testTwoNodes() throws RemoteException, UnknownHostException, InterruptedException {
        nameServer.onReady(eliasThread::start);

        elias.onReady(hansThread::start);

        hans.onNeighbourChanged(() -> {
            assertEquals(elias.getHash(), hans.getPrevNodeHash());
            assertEquals(elias.getHash(), hans.getNextNodeHash());

            assertEquals(hans.getHash(), elias.getPrevNodeHash());
            assertEquals(hans.getHash(), elias.getNextNodeHash());

            nameServer.shutdown();
            try {
                elias.shutdown();
                hans.shutdown();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        nameServerThread.start();

        eliasThread.join();
        hansThread.join();
        nameServerThread.join();

        assertEquals("elias", elias.getName());
        assertEquals("hans", hans.getName());
    }

    @Test
    void testThreeNodes() throws InterruptedException {
        nameServer.onReady(eliasThread::start);

        elias.onReady(hansThread::start);

        hans.onReady(jonathanThread::start);

        jonathan.onReady(() -> {
            assertEquals(3, nameServer.getNumberOfNodes());
            if (elias.getNextNodeHash() == hans.getHash()) {
                assertEquals(jonathan.getHash(), elias.getPrevNodeHash());
                assertEquals(jonathan.getHash(), hans.getNextNodeHash());
            } else if (elias.getNextNodeHash() == jonathan.getHash()) {
                assertEquals(hans.getHash(), elias.getPrevNodeHash());
                assertEquals(hans.getHash(), jonathan.getNextNodeHash());
            } else {
                fail("Ring incomplete");
            }

            nameServer.shutdown();
        });

        nameServerThread.start();

        eliasThread.join();
        hansThread.join();
        jonathanThread.join();
        nameServerThread.join();
    }

    @Test
    void testFourNodes() throws InterruptedException {
        nameServer.onReady(jillThread::start);
        jill.onReady(hansThread::start);
        hans.onReady(jonathanThread::start);
        jonathan.onReady(eliasThread::start);

        elias.onReady(() -> {
            assertEquals(4, nameServer.getNumberOfNodes());
            for(Node node : nodes) {
                assertNotEquals(node.getHash(), node.getPrevNodeHash());
                assertNotEquals(node.getHash(), node.getNextNodeHash());
            }

            TreeMap<Integer, Node> map = new TreeMap<Integer, Node>();

            for(Node node : nodes) {
                map.put(node.getHash(), node);
            }

            for(Node initialNode : nodes) {
                Node node = initialNode;
                for (int i = 0; i <= 3; i++) {
                    node = map.get(node.getPrevNodeHash());
                }

                assertEquals(node, initialNode);

                for (int i = 0; i <= 3; i++) {
                    node = map.get(node.getNextNodeHash());
                }

                assertEquals(node, initialNode);
            }

            nameServer.shutdown();
        });

        nameServerThread.start();

        eliasThread.join();
        hansThread.join();
        jonathanThread.join();
        jillThread.join();
        nameServerThread.join();
    }
}