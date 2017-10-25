import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

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

            //RMI client
            Registry vRegistry  = LocateRegistry.getRegistry();
            NodeOperations vNodeOperations = (NodeOperations) vRegistry.lookup(NodeOperations.class.getName());
            Node vConnection = new NodeMain().createNode(vNodeOperations);

        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public Node createNode(NodeOperations vNodeOperations){
        return new Node(vNodeOperations);
    }
}
