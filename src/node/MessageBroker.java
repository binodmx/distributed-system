package node;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.StringTokenizer;

public class MessageBroker {
    private final DatagramSocket datagramSocket;

    public MessageBroker() throws SocketException {
        datagramSocket = new DatagramSocket();
    }

    public String send(String request, String ipAddress, int port) throws IOException {
        DatagramPacket requestDatagramPacket = new DatagramPacket(request.getBytes(), request.length(), InetAddress.getByName(ipAddress), port);
        datagramSocket.setSoTimeout(10000);
        datagramSocket.send(requestDatagramPacket);
        byte[] buffer = new byte[65536];
        DatagramPacket responseDatagramPacket = new DatagramPacket(buffer, buffer.length);
        datagramSocket.receive(responseDatagramPacket);
        return new String(responseDatagramPacket.getData(), 0, responseDatagramPacket.getLength());
    }

}
