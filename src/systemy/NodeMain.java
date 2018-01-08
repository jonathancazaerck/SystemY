package systemy;

import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Paths;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.ArrayList;

public class NodeMain {
    private static Gui gui;

    public static void main(String[] args) throws CliException {
        mainGeneral(args, false);
    }

    public static void mainWithGui(String[] args) throws CliException {
        mainGeneral(args, true);
    }

    public static void mainGeneral(String[] args, boolean enableGui) throws CliException {
        System.setProperty("java.net.preferIPv4Stack", "true");

        try {
            Node node;
            if (enableGui) {
                gui = Gui.start();
                node = gui.getNode();
            } else {
                if (args.length < 1) throw new CliException("Missing argument: name");
                if (args.length < 2) throw new CliException("Missing argument: IP");
                String name = args[0];
                int port = args.length > 2 ? Integer.parseInt(args[2]) : Constants.DEFAULT_PORT;
                InetSocketAddress address = new InetSocketAddress(args[1], port);
                if (args.length > 3) Node.setFilesPath(Paths.get(args[3]));
                if ((args.length > 4 && args[4].equals("gui"))) gui = Gui.start();
                node = new Node(name, address);
            }

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

            if (gui != null) {
                gui.setStatusLabel("Searching for nameserver");
                node.onBound(() -> {
                    gui.setStatusLabel("Bound");
                });
                node.onReady(() -> {
                    gui.setStatusLabel("Ready");
                });
                node.onNeighboursChanged(() -> {
                    gui.setStatusLabel("Neighbours changed");
                });
                node.onFilesReplicated(() -> {
                    gui.setStatusLabel("Replicated files");
                });
                node.onShutdown(() -> {
                    gui.setStatusLabel("Shutting down");
                });
                node.onFileListChanged(() -> {
                    if (gui != null) {
                        gui.setFileList(new ArrayList<FileRef>(node.getFileList().values()));
                    }
                });
                gui.onClickDownload(() -> {
                    node.setLockRequest(gui.getSelectedFile());
                });
                gui.onClickOpen(() -> {
                    System.out.println("Opening file " + gui.getSelectedFile().getFileName());
                    try {
                        File file = node.fileRefToFile(gui.getSelectedFile(), true);
                        OSHelper.openFile(file);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            }
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
