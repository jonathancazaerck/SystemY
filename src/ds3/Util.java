package ds3;

import static java.lang.Math.abs;

public class Util {
    public static int hash(String str) {
        return Math.abs(str.hashCode()%32768);
    }
}