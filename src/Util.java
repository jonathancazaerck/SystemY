import static java.lang.Math.abs;

public class Util {
    public static int hash(String str) {
        int hash = 7;
        for (int i = 0; i < str.length(); i++) {
            hash = hash*31 + (str.charAt(i));
        }
        hash = Math.abs(hash);
        return hash % 32768;
    }
}
