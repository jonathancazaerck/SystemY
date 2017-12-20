package ds3;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class Ring extends TreeMap<Integer, InetSocketAddress> {
    public int lowerModularKey(int key) {
        Map.Entry<Integer, InetSocketAddress> prevEntry = this.lowerEntry(key);

        if (prevEntry == null) {
            return this.lastKey();
        } else {
            return prevEntry.getKey();
        }
    }

    public int higherModularKey(int key) {
        Map.Entry<Integer, InetSocketAddress> nextEntry = this.higherEntry(key);

        if (nextEntry == null) {
            return this.firstKey();
        } else {
            return nextEntry.getKey();
        }
    }
}
