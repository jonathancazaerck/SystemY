import java.rmi.AlreadyBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class NameServerMain {
    public static void main(String[] args) {
        try{
            Registry vRegistry = LocateRegistry.getRegistry();
            vRegistry.bind(NameServerOperations.class.getName(), (Remote) new NameServer());
            System.out.println("NameServer ready.");
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (AlreadyBoundException e) {
            e.printStackTrace();
        }
    }


}
