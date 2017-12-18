package ds3;

import java.net.InetSocketAddress;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface NameServerOperations extends Remote {
    int getNodeHashToReplicateTo(int hash) throws RemoteException;
    InetSocketAddress getAddressByHash(int hash) throws RemoteException;
    void exportJSON() throws RemoteException;
    void importJSON() throws RemoteException;
    Ring getRing() throws RemoteException;
}
