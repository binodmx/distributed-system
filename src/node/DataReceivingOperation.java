package node;

import java.io.*;
import java.net.Socket;

public class DataReceivingOperation implements Runnable {
    private Socket serverSocket;
    private BufferedReader in = null;
    private String fileName;

    public DataReceivingOperation(Socket socket, String fileName) {
        this.serverSocket = socket;
        this.fileName = fileName;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(serverSocket.getInputStream()));
            DataOutputStream dOut = new DataOutputStream(serverSocket.getOutputStream());
            dOut.writeUTF(fileName);
            dOut.flush();
            receiveFile();
            in.close();
        } catch (IOException e) {
            System.out.println("Error: Server error. Connection closed.");
        }
    }

    public void receiveFile() {
        try {
            int bytesRead;
            DataInputStream serverData = new DataInputStream(serverSocket.getInputStream());
            String fileName = serverData.readUTF();
            OutputStream output = new FileOutputStream(fileName);
            long size = serverData.readLong();
            byte[] buffer = new byte[1024];
            while (size > 0 && (bytesRead = serverData.read(buffer, 0, (int) Math.min(buffer.length, size))) != -1) {
                output.write(buffer, 0, bytesRead);
                size -= bytesRead;
            }
            output.close();
            serverData.close();
        } catch (IOException e) {
            System.out.println("Error: Server error. Connection closed.");
        }
    }
}
