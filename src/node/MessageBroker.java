package node;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class MessageBroker {
    private final DatagramSocket datagramSocket;

    public MessageBroker() throws SocketException {
        this.datagramSocket = new DatagramSocket();
    }

    public MessageBroker(DatagramSocket datagramSocket) {
        this.datagramSocket = datagramSocket;
    }

    public void send(DatagramPacket datagramPacket, int timeout) throws IOException {
        datagramSocket.setSoTimeout(timeout);
        datagramSocket.send(datagramPacket);
    }

    public DatagramPacket receive() throws IOException {
        byte[] buffer = new byte[65536];
        DatagramPacket requestDatagramPacket = new DatagramPacket(buffer, buffer.length);
        datagramSocket.setSoTimeout(Constants.NODE_REQUEST_TIMEOUT);
        datagramSocket.receive(requestDatagramPacket);
        return requestDatagramPacket;
    }

    public String sendAndReceive(String request, String ipAddress, int port, int timeout) throws IOException {
        DatagramPacket requestDatagramPacket = new DatagramPacket(
                request.getBytes(),
                request.length(),
                InetAddress.getByName(ipAddress),
                port
        );
        datagramSocket.setSoTimeout(timeout);
        datagramSocket.send(requestDatagramPacket);
        byte[] buffer = new byte[65536];
        DatagramPacket responseDatagramPacket = new DatagramPacket(buffer, buffer.length);
        datagramSocket.receive(responseDatagramPacket);
        return new String(responseDatagramPacket.getData(), 0, responseDatagramPacket.getLength());
    }
}
