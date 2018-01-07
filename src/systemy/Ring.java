package systemy;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.TreeMap;

//This class represents the node ring. It's a subclass from Treemap. The treemap contains the hash and the ip-adres
public class Ring extends TreeMap<Integer, InetSocketAddress> {

    //Method to get the hash of the previous node

    public int lowerModularKey(int key) {
        Map.Entry<Integer, InetSocketAddress> prevEntry = this.lowerEntry(key);

        //If we search for the previous node from the first node, we will return the last node hash
        if (prevEntry == null) {
            return this.lastKey();
        } else {
            return prevEntry.getKey();
        }
    }

    //Method to get the hash of the next node
    public int higherModularKey(int key) {
        Map.Entry<Integer, InetSocketAddress> nextEntry = this.higherEntry(key);

        //If we search the next node from the last node, we will return the first node hash
        if (nextEntry == null) {
            return this.firstKey();
        } else {
            return nextEntry.getKey();
        }
    }
}
