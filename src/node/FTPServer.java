package node;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Logger;


public class FTPServer implements Runnable {
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private final Logger LOG = Logger.getLogger(FTPServer.class.getName());
    private String userName;

    public FTPServer(int port, String userName) throws IOException {
        this.serverSocket = new ServerSocket(port);
        this.userName = userName;
    }

    public int getPort() {
        return serverSocket.getLocalPort();
    }

    @Override
    public void run() {
        while (true) {
            try {
                clientSocket = serverSocket.accept();
            } catch (IOException e) {
                e.printStackTrace();
            }
            Thread t = new Thread(new DataSendingOperation(clientSocket, userName));
            t.start();
        }
    }
}