package ds3;

import java.net.InetSocketAddress;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface NameServerOperations extends Remote {
    InetSocketAddress getAddressByHash(int hash) throws RemoteException;
    InetSocketAddress getAddressByFileName(String fileName) throws RemoteException;
    int getNumberOfNodes() throws RemoteException;
    int getPrevHash(int hash) throws RemoteException;
    int getNextHash(int hash) throws RemoteException;
    void exportJSON() throws RemoteException;
    void importJSON() throws RemoteException;
}
