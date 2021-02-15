package node;

import java.io.IOException;
import java.net.Socket;

public class FTPClient {
    public FTPClient(String ipAddress, int port, String fileName) throws IOException {
        Socket serverSocket = new Socket(ipAddress, port);
        Thread t = new Thread(new DataReceivingOperation(serverSocket, fileName));
        t.start();
    }
}