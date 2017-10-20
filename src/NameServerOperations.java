import java.net.InetAddress;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.HashMap;

public interface NameServerOperations extends Remote{

    void registerNodeByName(String name, InetAddress ip) throws RemoteException;
    InetAddress getIpByName(int name) throws RemoteException;
    void printTreemap() throws RemoteException;
    void removeNodeByName(String name) throws RemoteException;
    void toJSON(HashMap<Integer, InetAddress> map) throws RemoteException;
}
