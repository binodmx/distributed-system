package node;

import exceptions.AlreadyAssignedException;
import exceptions.AlreadyRegisteredException;
import exceptions.CommandErrorException;
import exceptions.ServerFullException;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

public class Node {
    public static void main(String[] args) {
        ArrayList<common.Node> nodes = new ArrayList<>();
        DatagramSocket datagramSocket = null;
        MessageBroker messageBroker = null;
        int DEFAULT_PORT = 55556;
        try {
            for (String arg : args) {
                if (arg.toLowerCase().startsWith("port=")) {
                    DEFAULT_PORT = Integer.parseInt(arg.substring(5));
                }
            }
            datagramSocket = new DatagramSocket(DEFAULT_PORT);
            System.out.println("Node is created at port " + DEFAULT_PORT + ".");
        } catch (SocketException e) {
            System.out.println("Error: Couldn't initialize the socket.");
            System.exit(0);
        }
        try {
            messageBroker = new MessageBroker();
        } catch (SocketException e) {
            System.out.println("Error: Couldn't initialize the message broker.");
            System.exit(0);
        }
        try {
            String response = messageBroker.send("12 REG 12.12.12.12 9898 ABC", "localhost", 55555);
            System.out.println(response.trim());
            StringTokenizer stringTokenizer = new StringTokenizer(response.trim(), " ");
            String length = stringTokenizer.nextToken();
            String command = stringTokenizer.nextToken();
            String nodesCount = stringTokenizer.nextToken();
            switch (nodesCount) {
                case "0":
                    System.out.println("Registration successful. No nodes in the system yet.");
                    break;
                case "9999":
                    throw new CommandErrorException("Error: Registration failed. There is some error in the command.");
                case "9998":
                    throw new AlreadyRegisteredException("Error: Registration failed. Already registered to you. Unregister first to register again.");
                case "9997":
                    throw new AlreadyAssignedException("Error: Registration failed. Already registered to another user. Try different IP and port");
                case "9996":
                    throw new ServerFullException("Error: Registration failed. Can't register, server is full.");
                default:
                    while (stringTokenizer.hasMoreTokens()) {
                        String ipAddress = stringTokenizer.nextToken();
                        String port = stringTokenizer.nextToken();
                        nodes.add(new common.Node(ipAddress, port));
                    }
                    System.out.println("Registration successful.");
            }
        } catch (CommandErrorException | AlreadyRegisteredException | AlreadyAssignedException | ServerFullException e) {
            System.out.println(e.getMessage());
            System.exit(0);
        } catch (IOException | NoSuchElementException e) {
            System.out.println("Error: Couldn't register with the server.");
            System.exit(0);
        }
        System.out.println("Waiting for incoming data...");
        while (true) {
            StringBuilder response = new StringBuilder();
            byte[] buffer = new byte[65536];
            DatagramPacket incomingDatagramPacket = new DatagramPacket(buffer, buffer.length);
            try {
                datagramSocket.receive(incomingDatagramPacket);
                String request = new String(incomingDatagramPacket.getData(), 0,
                        incomingDatagramPacket.getLength());
                System.out.print(incomingDatagramPacket.getAddress() + ":" + incomingDatagramPacket.getPort() +
                        " - " + request.replace("\n", ""));
                StringTokenizer stringTokenizer = new StringTokenizer(request.trim(), " ");
                String length = stringTokenizer.nextToken();
                String command = stringTokenizer.nextToken();
                String ipAddress;
                String port;
                String filename;
                String hops;
                switch (command.toUpperCase()) {
                    case "JOIN":
                        response.append("JOINOK ");
                        ipAddress = stringTokenizer.nextToken();
                        port = stringTokenizer.nextToken();
                        try {
                            nodes.add(new common.Node(ipAddress, port));
                        } catch (Exception e) {
                            response.append("9999");
                        }
                        break;
                    case "LEAVE":
                        response.append("LEAVEOK ");
                        ipAddress = stringTokenizer.nextToken();
                        port = stringTokenizer.nextToken();
                        boolean left = false;
                        for (common.Node node : nodes) {
                            if (node.getIpAddress().equals(ipAddress) && node.getPort().equals(port)) {
                                nodes.remove(node);
                                left = true;
                                break;
                            }
                        }
                        if (left) {
                            response.append("0");
                        } else {
                            response.append("9999");
                        }
                        break;
                    case "SER":
                        ipAddress = stringTokenizer.nextToken();
                        port = stringTokenizer.nextToken();
                        filename = stringTokenizer.nextToken();
                        hops = stringTokenizer.nextToken();
                        response.append("SEROK ");
//                        String data3 = messageBroker.send("SEARCH message", "localhost", 55555);
                        break;
                    case "PRINT":
                        response.append("PRINTOK ").append(nodes.size());
                        for (common.Node node : nodes) {
                            response.append(" ").append(node.getIpAddress()).append(" ").append(node.getPort()).append(" ").append(node.getUsername());
                        }
                        break;
                    default:
                        throw new CommandErrorException("9999");
                }
            } catch (IOException | AssertionError | NoSuchElementException e) {
                response = new StringBuilder("ERROR");
            } catch (CommandErrorException e) {
                response.append(e.getMessage());
            } finally {
                response = new StringBuilder(String.format("%04d", response.length() + 5) + " " + response + "\n");
                DatagramPacket outgoingDatagramPacket = new DatagramPacket(
                        response.toString().getBytes(),
                        response.toString().getBytes().length,
                        incomingDatagramPacket.getAddress(),
                        incomingDatagramPacket.getPort()
                );
                try {
                    datagramSocket.send(outgoingDatagramPacket);
                } catch (IOException e) {
                    System.out.println("Error: Unable to send response.");
                }
            }
        }
    }
}
