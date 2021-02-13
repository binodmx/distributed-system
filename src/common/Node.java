package common;

public class Node {
    private final String ipAddress;
    private final int port;
    private String username;

    public Node(String ipAddress, int port) {
        this.ipAddress = ipAddress;
        this.port = port;
    }

    public Node(String ipAddress, int port, String username) {
        this.ipAddress = ipAddress;
        this.port = port;
        this.username = username;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public int getPort() {
        return port;
    }

    public String getUsername() {
        return username;
    }
}
