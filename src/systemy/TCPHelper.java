package systemy;

import org.json.simple.JSONObject;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;

public abstract class TCPHelper {
    public static byte[] sendRequest(InetSocketAddress address, JSONObject metadata, BufferedInputStream bis) throws IOException {
        int count;
        byte[] fileOutputBuffer = new byte[Constants.MAX_FILE_SIZE];

        Socket socket = new Socket(address.getAddress(), address.getPort());

        try {
            OutputStream out = socket.getOutputStream();

            ByteBuffer metadataBuffer = ByteBuffer.allocate(Constants.FILE_METADATA_LENGTH);
            metadataBuffer.put(metadata.toJSONString().getBytes());
            out.write(metadataBuffer.array(), 0, Constants.FILE_METADATA_LENGTH);

            if (bis != null) {
                while ((count = bis.read(fileOutputBuffer)) >= 0) {
                    out.write(fileOutputBuffer, 0, count);
                    out.flush();
                }
            }
            out.close();
            if (bis != null) bis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        socket.close();

        return fileOutputBuffer;
    }

    public static void sendRequest(InetSocketAddress address, JSONObject metadata) throws IOException {
        sendRequest(address, metadata, null);
    }
}
