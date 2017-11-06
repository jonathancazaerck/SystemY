import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class NameServerMain {
    public static void main(String[] args) {
        try{
            if (false) {
                Registry vRegistry = LocateRegistry.getRegistry();
                vRegistry.bind("NameServerOperations", new NameServer());
            }
            new NameServer();
            System.out.println("NameServer ready.");
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (AlreadyBoundException e) {
            e.printStackTrace();
        }
    }
}
