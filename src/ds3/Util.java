package ds3;

public class Util {
    public static int hash(String str) {
        return Math.abs(str.hashCode()%32768);
    }

    public static String getNodeRegistryName(int hash) {
        return "Node:" + hash;
    }
}
