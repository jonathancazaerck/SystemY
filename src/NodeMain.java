import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class NodeMain {
    public static void main(String[] args){
        try {
            Registry registry = LocateRegistry.getRegistry();
            NameServerOperations nameServerOperations = (NameServerOperations) registry.lookup("NameServerOperations");
            nameServerOperations.registerNodeByName(args[0], InetAddress.getByName(args[1]));
            System.out.println("Registered node " + args[0] + " with ip " + args[1]);

            try {
                String msg = "hallo ik ben de nameserver haha";
                InetAddress multicastIp = InetAddress.getByName(Constants.MULTICAST_IP);
                MulticastSocket socket = new MulticastSocket(Constants.MULTICAST_PORT);
                socket.joinGroup(multicastIp);
                DatagramPacket hi = new DatagramPacket(msg.getBytes(), msg.length(), multicastIp, Constants.MULTICAST_PORT);
                socket.send(hi);
            } catch (IOException e) {
                e.printStackTrace();
            }

        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (NotBoundException e) {
            e.printStackTrace();
        }
    }
}
