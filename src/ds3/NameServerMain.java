package ds3;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.RemoteException;

public class NameServerMain {
    public static void main(String[] args) {
        System.setProperty("java.net.preferIPv4Stack", "true");


        try {
            InetAddress ip = InetAddress.getByName(args[0]);

            NameServer nameServer = new NameServer(ip);
            nameServer.start();
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }
}
