package ds3;

import java.net.InetSocketAddress;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface NodeOperations extends Remote {
    InetSocketAddress getAddress() throws RemoteException;
    String getName() throws RemoteException;
    void notifyNewNeighbour(int hash) throws RemoteException;
    void notifyNeighbourShutdown(boolean prev, int newNeighbourHash) throws RemoteException;
}
