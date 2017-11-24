package ds3;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.rmi.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Map;

public class NameServer extends UnicastRemoteObject implements NameServerOperations, NameServerLifecycleHooks {
    private Ring ring;
    private InetAddress ip;

    private ArrayList<Runnable> onReadyRunnables;
    private ArrayList<Runnable> onShutdownRunnables;

    private MulticastSocket multicastSocket;

    private boolean isShuttingDown;

    private Registry registry;

    public NameServer(InetAddress ip) throws RemoteException {
        super();
        this.ip = ip;
        this.ring = new Ring();
        this.onReadyRunnables = new ArrayList<Runnable>();
        this.onShutdownRunnables = new ArrayList<Runnable>();
        this.isShuttingDown = false;
    }

    public void start() throws IOException, AlreadyBoundException, ParseException, UnknownMessageException, ExistingNodeException {
        System.setProperty("java.net.preferIPv4Stack", "true");

        log("Creating registry");
        registry = LocateRegistry.createRegistry(Constants.REGISTRY_PORT);
        log("Created registry");

        log("Binding this to registry");
        registry.bind("NameServer", this);
        log("Bound this to registry");

        InetAddress multicastIp = InetAddress.getByName(Constants.MULTICAST_IP);
        multicastSocket = new MulticastSocket(Constants.MULTICAST_PORT);
        multicastSocket.joinGroup(multicastIp);

        log("Ready to receive multicasts");

        log("Running ready hooks");
        onReadyRunnables.forEach(Runnable::run);

        while(!this.isShuttingDown) {
            byte[] buffer = new byte[1000];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            try {
                multicastSocket.receive(packet);
                handleMulticastPacket(packet);
            } catch (SocketException e) {
                log("Closed socket, stopping");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        onShutdownRunnables.forEach(Runnable::run);
    }

    private void handleMulticastPacket(DatagramPacket packet) throws IOException, ParseException, UnknownMessageException {
        JSONObject obj = Util.extractJSONFromPacket(packet);

        String msgType = (String) obj.get("type");

        switch (msgType) {
            case "node_hello":
                String nodeName = (String) obj.get("name");
                int nodeHash = ((int) (long) obj.get("hash"));
                InetAddress nodeIp = InetAddress.getByName((String) obj.get("ip"));
                int nodePort = ((Long) obj.get("port")).intValue();
                InetSocketAddress nodeAddress = new InetSocketAddress(nodeIp, nodePort);

                try {
                    this.registerNode(nodeHash, nodeAddress);
                    log("Registered node " + nodeName + " with address " + nodeAddress.toString());
                } catch (ExistingNodeException e) {
                    log(e.getMessage());
                    return;
                }

                JSONObject responseMsg = new JSONObject();
                responseMsg.put("type", "nameserver_hello");
                responseMsg.put("ip", this.ip.getHostName());
                responseMsg.put("amount", getNumberOfNodes());
                String responseStr = responseMsg.toJSONString();

                DatagramSocket datagramSocket = new DatagramSocket();

                log("Sending nameserver hello to " + nodeName + " at " + nodeAddress.toString());
                datagramSocket.send(new DatagramPacket(responseStr.getBytes(), responseStr.length(), nodeIp, nodePort));
                datagramSocket.close();

                break;
            case "node_ready":
                break;
            default:
                throw new UnknownMessageException(msgType);
        }
    }

    private void registerNode(int hash, InetSocketAddress address) throws ExistingNodeException {
        if(this.ring.containsKey(hash)) {
            throw new ExistingNodeException(hash);
        } else {
            this.ring.put(hash, address);
        }
    }

    public InetSocketAddress getAddressByHash(int hash) {
        return this.ring.get(hash);
    }

    public int getNodeHashToReplicateTo(int fileHash) {
        return this.ring.lowerModularEntry(fileHash);
    }

    public int getNumberOfNodes() {
        return this.ring.size();
    }

    public void exportJSON() {
        JSONObject obj = new JSONObject();

        for(Map.Entry<Integer, InetSocketAddress> entry : ring.entrySet()) {
            InetSocketAddress address = entry.getValue();
            obj.put(entry.getKey().toString(), address.getHostName()+":"+address.getPort());
        }

        File file = new File("tmp/nameserver.json");
        file.mkdirs();
        try {
            FileWriter fileWriter = new FileWriter(file);
            fileWriter.write(obj.toJSONString());
            log("Successfully wrote nameserver.json\n");
            log("JSON Object: " + obj);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void importJSON() {
        try {
            String str = new String(Files.readAllBytes(Paths.get("tmp/nameserver.json")));
            JSONObject obj = (JSONObject) JSONValue.parseWithException(str);

            for(Object objEntry : obj.entrySet()) {
                Map.Entry<String, String> entry = (Map.Entry<String, String>) objEntry;
                log(entry.toString());
                String[] v = entry.getValue().split(":");
                InetAddress ip = InetAddress.getByName(v[0]);
                int port = Integer.parseInt(v[1]);
                ring.put(Integer.parseInt(entry.getKey()), new InetSocketAddress(ip, port));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    public void shutdown() {
        this.isShuttingDown = true;
        this.multicastSocket.close();
        try {
            this.registry.unbind("NameServer");
            UnicastRemoteObject.unexportObject(this.registry, true);
        } catch (NoSuchObjectException e) {
            e.printStackTrace();
        } catch (AccessException e) {
            e.printStackTrace();
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (NotBoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onReady(Runnable runnable) {
        onReadyRunnables.add(runnable);
    }

    @Override
    public void onShutdown(Runnable runnable) {
        onShutdownRunnables.add(runnable);
    }

    private void log(String str) {
        System.out.println("[nameserver] " + str);
    }
}
