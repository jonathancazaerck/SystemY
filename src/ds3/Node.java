package ds3;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class Node extends UnicastRemoteObject implements NodeOperations {
    private InetSocketAddress address;
    private String name;
    private int hash;

    private int nextNodeHash;
    private int prevNodeHash;

    private Registry registry;

    public Node(String name, InetSocketAddress address) throws RemoteException {
        super();
        this.name = name;
        this.address = address;
        this.hash = Util.hash(name);
    }

    public InetSocketAddress getAddress() {
        return address;
    }

    public String getName() {
        return name;
    }

    public void start() {
        try {
            System.out.println("Multicasting node hello: " + name + " at " + address.toString());
            sendNodeHello(name, address);

            System.out.println("Listening for nameserver hello");
            InetAddress nameServerIp = listenForNameServerHello(address);

            System.out.println("Locating registry at " + nameServerIp.toString());
            registry = LocateRegistry.getRegistry(nameServerIp.getHostAddress());
            System.out.println("Located registry at " + nameServerIp.toString());
            NameServerOperations nameServer = (NameServerOperations) registry.lookup("NameServer");

            int amount = nameServer.getNumberOfNodes();
            System.out.println("Number of nodes: " + amount);

            if (amount == 1) {
                this.prevNodeHash = this.hash;
                this.nextNodeHash = this.hash;
            } else if (amount == 2) {
                this.prevNodeHash = nameServer.getPrevHash(this.hash);
                this.nextNodeHash = prevNodeHash;

                Node prevNode = (Node) registry.lookup(Util.getNodeRegistryName(this.prevNodeHash));

                prevNode.onNewNeighbour(this.hash);
            } else {
                this.prevNodeHash = nameServer.getPrevHash(this.hash);
                this.nextNodeHash = nameServer.getNextHash(this.hash);

                Node prevNode = (Node) registry.lookup(Util.getNodeRegistryName(this.prevNodeHash));
                Node nextNode = (Node) registry.lookup(Util.getNodeRegistryName(this.nextNodeHash));

                prevNode.onNewNeighbour(this.hash);
                nextNode.onNewNeighbour(this.hash);
            }

            String registryName = Util.getNodeRegistryName(hash);
            System.out.println("Binding this to registry with name: " + registryName);
            registry.bind(registryName, this);
            System.out.println("Bound to registry");

        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        } catch (NotBoundException e) {
            e.printStackTrace();
        } catch (AlreadyBoundException e) {
            e.printStackTrace();
        } catch (UnknownMessageException e) {
            e.printStackTrace();
        }
    }

    private void sendNodeHello(String name, InetSocketAddress address) throws IOException {
        JSONObject messageObj = new JSONObject();
        messageObj.put("type", "node_hello");
        messageObj.put("name", name);
        messageObj.put("ip", address.getHostString());
        messageObj.put("port", address.getPort());
            String msg = messageObj.toJSONString();
        InetAddress multicastIp = InetAddress.getByName(Constants.MULTICAST_IP);
        MulticastSocket socket = new MulticastSocket(Constants.MULTICAST_PORT);
        socket.joinGroup(multicastIp);
        DatagramPacket hi = new DatagramPacket(msg.getBytes(), msg.length(), multicastIp, Constants.MULTICAST_PORT);
        socket.send(hi);
    }

    private InetAddress listenForNameServerHello(InetSocketAddress address) throws IOException, ParseException, UnknownMessageException {
        byte[] buffer = new byte[1000];

        DatagramSocket datagramSocket = new DatagramSocket(address.getPort());
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        datagramSocket.receive(packet);

        String msg = new String(packet.getData(), 0, packet.getLength(), "UTF-8");
        System.out.println(msg);
        JSONObject obj = (JSONObject) JSONValue.parseWithException(msg + "\n");
        String msgType = (String) obj.get("type");

        switch (msgType) {
            case "nameserver_hello":
                InetAddress nameServerIp = InetAddress.getByName((String) obj.get("ip"));
                System.out.println("Received nameserver hello: " + nameServerIp.toString());
                return nameServerIp;
            default:
                throw new UnknownMessageException(msgType, msg);
        }
    }

    public void onNewNeighbour(int newNodeHash) {
         boolean isFirstNode = hash < prevNodeHash;
         boolean isLastNode = nextNodeHash < hash;

         if ((hash < newNodeHash && newNodeHash < nextNodeHash) || (isLastNode && (newNodeHash < nextNodeHash))) {
             this.nextNodeHash = newNodeHash;
         }
         // can be both prev and next
         if ((prevNodeHash < newNodeHash && newNodeHash < hash) || (isFirstNode && (prevNodeHash < newNodeHash))) {
             this.prevNodeHash = newNodeHash;
         }
    }

    public void shutdown() throws RemoteException, NotBoundException {
        Node prevNode = (Node) this.registry.lookup(Util.getNodeRegistryName(this.prevNodeHash));
        Node nextNode = (Node) this.registry.lookup(Util.getNodeRegistryName(this.nextNodeHash));

        prevNode.onNeighbourShutdown(false, this.nextNodeHash);
        nextNode.onNeighbourShutdown(true, this.prevNodeHash);

        this.registry.unbind(Util.getNodeRegistryName(this.hash));
    }

    public void onNeighbourShutdown(boolean prev, int newNeighbourHash) {
        if (prev) {
            this.nextNodeHash = newNeighbourHash;
        } else {
            this.prevNodeHash = newNeighbourHash;
        }
    }

    public  void replicateFiles() throws IOException, NotBoundException {
        File dir = new File("nodeOwnFiles");
        File[] directoryListing = dir.listFiles();

        int hashToDupl;
        InetSocketAddress addressToDupl;

        int count;
        byte[] buffer = new byte[(int) Math.pow(2, 10)];



        if (directoryListing != null) {
            for (File child : directoryListing) {
                String name =  child.getName();
                int hash = Util.hash(name);

                NameServerOperations nameServer = (NameServerOperations) registry.lookup("NameServer");
                hashToDupl = nameServer.getPrevHash(hash);

                if(hashToDupl == hash) {hashToDupl = prevNodeHash;}
                addressToDupl = nameServer.getAddressByHash(hashToDupl);


                Socket socket = new Socket(addressToDupl.getAddress(),addressToDupl.getPort());

                try{
                    OutputStream out = socket.getOutputStream();
                    FileInputStream fis = new FileInputStream(child);
                    BufferedInputStream bfis = new BufferedInputStream(fis);

                    long fileSize = fis.getChannel().size();

                    JSONObject messageObj = new JSONObject();
                    messageObj.put("type", "file_metadata");
                    messageObj.put("name", name);
                    messageObj.put("size", fileSize);

                    ByteBuffer fileSizeBuffer = ByteBuffer.allocate(Long.BYTES);
                    fileSizeBuffer.putLong(fileSize);
                    out.write(fileSizeBuffer.array(), 0, Long.BYTES);

                    while ((count = bfis.read(buffer)) >= 0) {
                        out.write(buffer, 0, count);
                        out.flush();
                    }

                    out.close();
                    bfis.close();
                    System.out.println("File is transferred!");
                }catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
