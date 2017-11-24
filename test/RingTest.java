import ds3.Ring;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.net.InetSocketAddress;

class RingTest {
    @Test
    void testModularity() {
        Ring ring = new Ring();
        ring.put(1, new InetSocketAddress(1));
        ring.put(2, new InetSocketAddress(2));
        ring.put(3, new InetSocketAddress(3));
        ring.put(4, new InetSocketAddress(4));
        assertEquals(3, ring.lowerModularEntry(4));
        assertEquals(2, ring.lowerModularEntry(3));
        assertEquals(1, ring.higherModularEntry(4));
        assertEquals(4, ring.lowerModularEntry(1));
    }
}
