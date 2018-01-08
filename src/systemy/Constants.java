package systemy;

public class Constants {
    public static String MULTICAST_IP = "228.5.6.9";
    public static int MULTICAST_PORT = 6789;
    public static int DEFAULT_PORT = 6788; //when there is no port given, the default port is used
    public static int REGISTRY_PORT = 1099;
    public static int TCP_METADATA_LENGTH = 200;
    public static int MAX_FILE_SIZE = (int) Math.pow(2, 22);
    public static int MAX_MESSAGE_SIZE = (int) Math.pow(2, 10);
    public static int AGENT_DELAY = 1000;
    public static int AGENT_TIMEOUT = 5000;
}
