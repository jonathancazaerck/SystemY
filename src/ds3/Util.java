package ds3;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;

import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.util.Arrays;

public final class Util {
    public static int hash(String str) {
        return Math.abs(str.hashCode()%32768);
    }

    public static String getNodeRegistryName(int hash) {
        return "Node:" + hash;
    }

    public static JSONObject extractJSONFromPacket(DatagramPacket packet) throws UnsupportedEncodingException, ParseException {
        String msg = new String(packet.getData(), 0, packet.getLength(), "UTF-8");

        return (JSONObject) JSONValue.parseWithException(msg + "\n");
    }

    public static byte[] trimByteArray(byte[] bytes) {
        int i = bytes.length - 1;
        while (i >= 0 && bytes[i] == 0)
        {
            --i;
        }
        return Arrays.copyOf(bytes, i + 1);
    }
}
