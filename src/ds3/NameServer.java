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
import java.util.concurrent.TimeUnit;

public class NameServer extends UnicastRemoteObject implements NameServerOperations, NameServerLifecycleHooks {
    private final Ring allRing = new Ring();
    private final Ring readyRing = new Ring();
    private final InetAddress ip;

    private final ArrayList<Runnable> onReadyRunnables = new ArrayList<Runnable>();
    private final ArrayList<Runnable> onShutdownRunnables = new ArrayList<Runnable>();

    private MulticastSocket multicastSocket;

    private boolean isShuttingDown = false;

    private Registry registry;

    public NameServer(InetAddress ip) throws RemoteException {
        super();
        this.ip = ip;
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
            byte[] buffer = new byte[Constants.MAX_MESSAGE_SIZE];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            try {
                multicastSocket.receive(packet);
                handleMulticastPacket(packet);
            } catch (SocketException e) {
                log("Closed socket, stopping");
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        onShutdownRunnables.forEach(Runnable::run);
    }

    private void handleMulticastPacket(DatagramPacket packet) throws IOException, ParseException, UnknownMessageException, InterruptedException {
        JSONObject obj = Util.extractJSONFromPacket(packet);

        String msgType = (String) obj.get("type");
        int nodeHash = ((int) (long) obj.get("hash"));

        switch (msgType) {
            case "node_hello":
                String nodeName = (String) obj.get("name");
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
                responseMsg.put("ip", this.ip.getHostAddress());
                responseMsg.put("amount", getNumberOfNodes());
                String responseStr = responseMsg.toJSONString();

                DatagramSocket datagramSocket = new DatagramSocket();

                TimeUnit.MILLISECONDS.sleep(100); // between sending and listening
                log("Sending nameserver hello to " + nodeName + " at " + nodeAddress.toString());
                datagramSocket.send(new DatagramPacket(responseStr.getBytes(), responseStr.length(), nodeIp, nodePort));
                datagramSocket.close();

                break;
            case "node_bound":
                break;
            case "node_ready":
                this.readyRing.put(nodeHash, this.allRing.get(nodeHash));
                break;
            case "node_shutdown":
                this.allRing.remove(nodeHash);
                this.readyRing.remove(nodeHash);
                break;
            default:
                throw new UnknownMessageException(msgType);
        }
    }

    private void registerNode(int hash, InetSocketAddress address) throws ExistingNodeException {
        if(this.allRing.containsKey(hash)) {
            throw new ExistingNodeException(hash);
        } else {
            this.allRing.put(hash, address);
        }
    }

    public void notifyFailure(int nodeHash) {
        this.allRing.remove(nodeHash);
        this.readyRing.remove(nodeHash);
    }

    public InetSocketAddress getAddressByHash(int hash) {
        return this.allRing.get(hash);
    }

    public int getNodeHashToReplicateTo(int fileHash) {
        return this.readyRing.lowerModularKey(fileHash);
    }

    public int getNumberOfNodes() {
        return this.allRing.size();
    }

    public void exportJSON() {
        JSONObject obj = new JSONObject();

        for(Map.Entry<Integer, InetSocketAddress> entry : allRing.entrySet()) {
            InetSocketAddress address = entry.getValue();
            obj.put(entry.getKey().toString(), address.getAddress().getHostAddress() + ":" + address.getPort());
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
                allRing.put(Integer.parseInt(entry.getKey()), new InetSocketAddress(ip, port));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    @Override
    public ShallowRing getShallowRing() throws RemoteException {
        return ShallowRing.fromRing(readyRing);
    }

    public void shutdown() throws RemoteException, NotBoundException {
        this.isShuttingDown = true;
        this.multicastSocket.close();
        this.registry.unbind("NameServer");
        UnicastRemoteObject.unexportObject(this.registry, true);
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
