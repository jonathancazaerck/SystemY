import java.net.InetAddress;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.TreeMap;

public interface NameServerOperations extends Remote {

    void registerNodeByName(String name, InetAddress ip) throws RemoteException;
    InetAddress getIpByName(String name) throws RemoteException;
    void printTreemap() throws RemoteException;
    void removeNodeByName(String name) throws RemoteException;
    void toJSON(TreeMap<Integer, InetAddress> map) throws RemoteException;
}
