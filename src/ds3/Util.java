package ds3;

import static java.lang.Math.abs;

public class Util {
    public static int hash(String str) {
        return Math.abs(str.hashCode()%32768);
    }

    public static String getNodeRegistryName(int hash) {
        return "Node:0x" + String.format("%04x", hash);
    }
}
