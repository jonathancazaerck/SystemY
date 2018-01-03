package ds3;

import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Paths;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Random;

public class NodeMain {
    private static Gui gui;

    public static void main(String[] args) {
        System.setProperty("java.net.preferIPv4Stack", "true");

        String name;
        if (System.getProperty("ds3.enableGui").equals("true")) {
            gui = Gui.start();
            name = "system-y-app-"+new Random().nextInt(99);
        } else {
            name = args[0];
        }
        if((args.length > 4 && args[4].equals("gui"))) gui = Gui.start();
        if(args.length > 3) Node.setFilesPath(Paths.get(args[3]));
        int port = args.length > 2 ? Integer.parseInt(args[2]) : Constants.DEFAULT_PORT;
        InetSocketAddress address = new InetSocketAddress(args[1], port);

        try {
            Node node;
            if (gui != null) {
                node = gui.getNode();
            } else {
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
                    try {
                        node.sendFileDownloadRequest(gui.getSelectedFile());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
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
//                gui.onClickDelete(() -> {
//                    try {
//                        node.sendFileDownloadRequest(gui.getSelectedFile());
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                });
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
