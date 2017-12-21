package ds3;

import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Paths;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

public class NodeMain {
    private static Gui gui;

    public static void main(String[] args) {
        System.setProperty("java.net.preferIPv4Stack", "true");

        String name = args[0];
        if(args.length > 4 && args[4].equals("gui")) new Thread(() -> { gui = Gui.start(); }).start();
        if(args.length > 3) Node.setFilesPath(Paths.get(args[3]));
        int port = args.length > 2 ? Integer.parseInt(args[2]) : Constants.DEFAULT_PORT;
        InetSocketAddress address = new InetSocketAddress(args[1], port);

        try {
            Node node = new Node(name, address);

            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    try {
                        node.shutdown();
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (NodeNotReadyException e) {
                        e.printStackTrace();
                    }
                }
            });

            node.onFileListChanged(() -> {
                if (gui != null) {
                    gui.setFileList(node.getFileList().values());
                }
            });
            node.start();
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (AlreadyBoundException e) {
            e.printStackTrace();
        } catch (NotBoundException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (UnknownMessageException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
