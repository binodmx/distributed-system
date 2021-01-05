package server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

public class Main {
    public static void main(String[] args) {
        DatagramSocket datagramSocket = null;
        int DEFAULT_PORT = 55555;
        int MAX_NODES = 10;
        ArrayList<Node> nodes = new ArrayList<>();
        try {
            if (args.length == 1) {
                DEFAULT_PORT = Integer.parseInt(args[0]);
            } else if (args.length == 2) {
                MAX_NODES = Integer.parseInt(args[1]);
            }
            datagramSocket = new DatagramSocket(DEFAULT_PORT);
            System.out.println("Bootstrap Server created at port " + DEFAULT_PORT + ". Waiting for incoming data...");
        } catch (SocketException e) {
            System.out.println("Error: Couldn't initialize the socket.");
            System.exit(0);
        }
        while (true) {
            String response = "";
            byte[] buffer = new byte[65536];
            DatagramPacket incomingDatagramPacket = new DatagramPacket(buffer, buffer.length);
            try {
                datagramSocket.receive(incomingDatagramPacket);
                String request = new String(incomingDatagramPacket.getData(), 0,
                        incomingDatagramPacket.getLength());
                System.out.println(incomingDatagramPacket.getAddress() + ":" + incomingDatagramPacket.getPort() +
                        " - " + request);
                StringTokenizer stringTokenizer = new StringTokenizer(request.trim(), " ");
                String length = stringTokenizer.nextToken();
                String command = stringTokenizer.nextToken();
                String ipAddress;
                String port;
                String username;
                switch (command) {
                    case "REG":
                        ipAddress = stringTokenizer.nextToken();
                        port = stringTokenizer.nextToken();
                        username = stringTokenizer.nextToken();
                        response += "REGOK ";
                        if (nodes.size() == MAX_NODES) {
                            throw new ServerFullException("9996");
                        }
                        for (Node node : nodes) {
                            if (!node.getIpAddress().equals(ipAddress)) {
                                continue;
                            } else if (!node.getPort().equals(port)) {
                                continue;
                            } else if (!node.getUsername().equals(username)) {
                                throw new AlreadyAssignedException("9997");
                            } else {
                                throw new AlreadyRegisteredException("9998");
                            }
                        }
                        response += Integer.toString(nodes.size());
                        for (Node node : nodes) {
                            response +=  " " + node.getIpAddress() + " " + node.getPort();
                        }
                        nodes.add(new Node(ipAddress, port, username));
                        break;
                    case "UNREG":
                        ipAddress = stringTokenizer.nextToken();
                        port = stringTokenizer.nextToken();
                        username = stringTokenizer.nextToken();
                        response += "UNROK ";
                        for (Node node : nodes) {
                            if (node.getIpAddress().equals(ipAddress) && node.getPort().equals(port)) {
                                nodes.remove(node);
                                response += "0";
                                break;
                            }
                        }
                    case "PRINT":
                        response += "PRINTOK " + Integer.toString(nodes.size());
                        for (Node node : nodes) {
                            response +=  " " + node.getIpAddress() + " " + node.getPort() + " " + node.getUsername();
                        }
                        break;
                    default:
                        throw new CommandErrorException("9999");
                }
            } catch (IOException | AssertionError | NoSuchElementException e) {
                response = "ERROR";
            } catch (CommandErrorException | AlreadyAssignedException | AlreadyRegisteredException |
                    ServerFullException e) {
                response += e.getMessage();
            } finally {
                response = String.format("%04d", response.length() + 5) + " " + response + "\n";
                DatagramPacket outgoingDatagramPacket = new DatagramPacket(response.getBytes(),
                        response.getBytes().length, incomingDatagramPacket.getAddress(),
                        incomingDatagramPacket.getPort());
                try {
                    datagramSocket.send(outgoingDatagramPacket);
                } catch (IOException e) {
                    System.out.println("Error: Unable to send response.");
                }
            }
        }
    }
}
