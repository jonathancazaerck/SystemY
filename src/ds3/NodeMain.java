package ds3;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.net.*;

public class NodeMain {
    public static void main(String[] args) {
        // Registry registry = LocateRegistry.getRegistry();
        // ds3.NameServerOperations nameServerOperations = (ds3.NameServerOperations) registry.lookup("ds3.NameServerOperations");
        // nameServerOperations.registerNodeByName(args[0], InetAddress.getByName(args[1]));
        // System.out.println("Registered node " + args[0] + " with ip " + args[1] + " and port " + args[2]);


        String name = args[0];
        int port = args.length > 2 ? Integer.parseInt(args[2]) : Constants.DEFAULT_PORT;
        InetSocketAddress address = new InetSocketAddress(args[1], port);

        try {
            sendMulticast(name, address);
            listenForAmount();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void sendMulticast(String name, InetSocketAddress address) throws IOException {
        System.out.println("Multicasting node " + name + " with address " + address.toString());

        JSONObject messageObj = new JSONObject();
        messageObj.put("type", "node_register");
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

    public static int listenForAmount() throws IOException {
        System.out.println("Listening for amount of nodes");

        byte[] buffer = new byte[1000];

        DatagramSocket datagramSocket = new DatagramSocket();
        DatagramPacket recvPacket = new DatagramPacket(buffer, buffer.length);
        datagramSocket.receive(recvPacket);
        System.out.println(new String(recvPacket.getData()));

        return 0;
    }


    public static void receiveNode(){

        try {
            InetAddress multicastIp = InetAddress.getByName(Constants.MULTICAST_IP);
            MulticastSocket multicastSocket = new MulticastSocket(Constants.MULTICAST_PORT);
            DatagramSocket datagramSocket = new DatagramSocket();
            multicastSocket.joinGroup(multicastIp);

            byte[] buffer = new byte[1000];

            while(true) {

                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                multicastSocket.receive(packet);

                String msg = new String(packet.getData(), 0, packet.getLength(), "UTF-8");

                System.out.println(msg);

                JSONObject obj = (JSONObject) JSONValue.parseWithException(msg+"\n");

                String nodeName = (String) obj.get("name");
                InetAddress nodeIp = InetAddress.getByName((String) obj.get("ip"));

                int hashNode = Util.hash(nodeName);

//                if(id < hashNode && hashNode < nextNodeId){
//                    nextNodeId = hashNode;
//
//                    JSONObject responseObj = new JSONObject();
//                    responseObj.put("type", "Neighbour update");
//                    responseObj.put("selfId", id);
//                    responseObj.put("nextNodeId", nextNodeId);
//                    String responseStr = responseObj.toJSONString();
//
//                    datagramSocket.send(new DatagramPacket(responseStr.getBytes(), responseStr.length(), nodeIp, Constants.MULTICAST_PORT));
//                } else if(prevNodeId < hashNode && id < hashNode){
//                    prevNodeId = hashNode;
//                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }


    public static void deleteThisNode(){
        try {
            InetAddress NextIP = InetAddress.getByName(Constants.MULTICAST_IP); //Change this to the next ip
            InetAddress PreviousIP = InetAddress.getByName(Constants.MULTICAST_IP); //Change this to the prev ip
            JSONObject neighboursChangeMsg = new JSONObject();
            neighboursChangeMsg.put("current",Node.id);
            neighboursChangeMsg.put("nextNeighbour", Node.nextNodeId);
            neighboursChangeMsg.put("previousNeighbour", Node.prevNodeId);
            String neighboursChangeMsgStr = neighboursChangeMsg.toString();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

    }

//    public static void receiveDelete(){
//
//        //Hieronder komt code om datagrampakket te ontvangen. Moet nog aangepast worden!!!!!!!
//        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
//        multicastSocket.receive(packet);
//
//        String msg = new String(packet.getData(), 0, packet.getLength(), "UTF-8");
//
//        System.out.println("Received message: " + msg);
//
//        JSONObject obj = (JSONObject) JSONValue.parseWithException(msg+"\n");
//
//        int ToBeDeleted = (int) obj.get("current");
//        int prevNeighbour = (int) obj.get("previousNeighbour");
//        int nextNeighbour = (int) obj.get("nextNeighbour");
//
//        if(Node.id < ToBeDeleted){
//            //Vorige buur wijzigen
//            //Verander veld volgende buur
//            Node.nextNodeId = nextNeighbour;
//        }
//
//        else{
//            //Volgende buur wijzigen
//            //Verander veld vorige buur
//            Node.prevNodeId = prevNeighbour;
//        }
//    }
}