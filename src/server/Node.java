package server;

public class Node {
    private final String ipAddress;
    private final String port;
    private final String username;

    public Node(String ipAddress, String port, String username) {
        this.ipAddress = ipAddress;
        this.port = port;
        this.username = username;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getPort() {
        return port;
    }

    public String getUsername() {
        return username;
    }
}
