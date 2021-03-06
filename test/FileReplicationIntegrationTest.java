import systemy.NameServer;
import systemy.Node;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class FileReplicationIntegrationTest {
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

    private int replicationCount;

    private volatile Exception asyncException;

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
                asyncException = e;
            }
        });
        for (Node node : nodes) {
            nodeThreads.add(new Thread(() -> {
                try {
                    System.out.println("Starting node thread: " + node.getName());
                    node.start();
                } catch (Exception e) {
                    asyncException = e;
                }
            }));
        }

        this.eliasThread = nodeThreads.get(nodes.indexOf(elias));
        this.hansThread = nodeThreads.get(nodes.indexOf(hans));
        this.jonathanThread = nodeThreads.get(nodes.indexOf(jonathan));
        this.jillThread = nodeThreads.get(nodes.indexOf(jill));

        this.replicationCount = 0;
    }

    @AfterEach
    void checkForThreadException() {
        if (asyncException != null) {
            asyncException.printStackTrace();
            fail(asyncException);
        }
    }

    @Disabled
    @Test
    void testFiles() throws InterruptedException {
        nameServer.onReady(jillThread::start);
        jill.onReady(hansThread::start);
        hans.onReady(jonathanThread::start);
        jonathan.onReady(eliasThread::start);

        Runnable afterFilesReplicated = () -> {
            incrementReplicationCount();

            System.out.println("RC" + replicationCount);

            // There should be 9 batch file transfers
            if (replicationCount == 9) {
                try {
                    TimeUnit.SECONDS.sleep(5);
                } catch (InterruptedException e) {
                    asyncException = e;
                }
                try {
                    nameServer.shutdown();
                    elias.shutdown();
                    hans.shutdown();
                    jonathan.shutdown();
                    jill.shutdown();
                } catch (Exception e) {
                    asyncException = e;
                }
            }
        };

        jill.onFilesReplicated(afterFilesReplicated);
        hans.onFilesReplicated(afterFilesReplicated);
        jonathan.onFilesReplicated(afterFilesReplicated);
        elias.onFilesReplicated(afterFilesReplicated);

        nameServerThread.start();

        jillThread.join();
        hansThread.join();
        jonathanThread.join();
        eliasThread.join();
        nameServerThread.join();
    }

    private synchronized void incrementReplicationCount() {
        this.replicationCount += 1;
    }
}
