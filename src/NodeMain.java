import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class NodeMain {
    public static void main(String[] args){
        try {
            Registry vRegistry = LocateRegistry.getRegistry();
            NameServerOperations vNameServerOperations = (NameServerOperations) vRegistry.lookup("NameServerOperations");
            //Node vConnection = new Node(args[0], InetAddress.getByName(args[1]), vNameServerOperations);
            vNameServerOperations.registerNodeByName(args[0], InetAddress.getByName(args[1]));
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (NotBoundException e) {
            e.printStackTrace();
        }
    }
}
