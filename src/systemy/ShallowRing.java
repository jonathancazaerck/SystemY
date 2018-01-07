package systemy;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.TreeSet;

public class ShallowRing extends TreeSet<Integer> {
    static public ShallowRing fromRing(Ring ring) {
        ShallowRing shallowRing = new ShallowRing();
        for(Map.Entry<Integer, InetSocketAddress> entry : ring.entrySet()) {
            shallowRing.add(entry.getKey());
        }
        return shallowRing;
    }

    public Integer higherModular(int key) {
        Integer nextKey = higher(key);
        return nextKey == null ? first() : nextKey;
    }

    public Integer lowerModular(int key) {
        Integer prevKey = lower(key);
        return prevKey == null ? last() : prevKey;
    }
}
