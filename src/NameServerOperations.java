import java.net.InetAddress;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface NameServerOperations extends Remote{

    InetAddress getIpByName(int name) throws RemoteException;
    void registerNodeByName(String name, InetAddress ip);
    
}
