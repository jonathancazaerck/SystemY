import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.RemoteException;

public class NodeMain {
    public static void main(String[] args){
        try {
            NameServer nameServer = new NameServer();

            nameServer.registerNodeByName("jefke", InetAddress.getByName("127.0.0.1"));

            //Proberen of er twee dezelfde nodes kunnen aangemaakt worden
            nameServer.registerNodeByName("jefke", InetAddress.getByName("127.1.1.1"));

            //Rij uit treemap printen
            //System.out.println(nameServer.getIpByName("jefke"));

            //Node deleten
            nameServer.removeNodeByName("jefke");

            //Print treemap
            nameServer.printTreemap();

            //Print en schrijf JSON-file
            nameServer.toJSON(nameServer.nodeIpMap);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}
