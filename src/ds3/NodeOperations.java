package ds3;

import java.net.InetSocketAddress;
import java.rmi.RemoteException;

public interface NodeOperations {
    InetSocketAddress getAddress() throws RemoteException;
    String getName() throws RemoteException;
    void onNewNeighbour(int hash) throws RemoteException;
    void onNeighbourShutdown(boolean prev, int newNeighbourHash) throws RemoteException;
}
