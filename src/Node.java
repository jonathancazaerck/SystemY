import java.net.InetAddress;

public class Node {
    public InetAddress ip;
    public String name;
    public int id;

    public Node(String name, InetAddress ip) {
        this.name = name;
        this.id = Util.hash(name);
    }

    public static void main(String[] args){

    }
}
