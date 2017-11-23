package ds3;

import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.AlreadyBoundException;
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
        } catch (AlreadyBoundException e) {
            e.printStackTrace();
        } catch (ExistingNodeException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (UnknownMessageException e) {
            e.printStackTrace();
        }
    }
}
