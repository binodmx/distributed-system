package server;

import common.Node;
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

public class Server {
    public static void main(String[] args) {
        DatagramSocket datagramSocket = null;
        int DEFAULT_PORT = 55555;
        int MAX_NODES = 10;
        ArrayList<Node> nodes = new ArrayList<>();
        try {
            for (String arg : args) {
                if (arg.toLowerCase().startsWith("port=")) {
                    DEFAULT_PORT = Integer.parseInt(arg.substring(5));
                } else if (arg.toLowerCase().startsWith("max=")) {
                    MAX_NODES = Integer.parseInt(arg.substring(4));
                }
            }
            datagramSocket = new DatagramSocket(DEFAULT_PORT);
            System.out.println("Bootstrap Server is created at port " + DEFAULT_PORT + ". Waiting for incoming requests...");
        } catch (SocketException e) {
            System.out.println("Error: Couldn't initialize the socket.");
            System.exit(0);
        }
        while (true) {
            StringBuilder response = new StringBuilder();
            byte[] buffer = new byte[65536];
            DatagramPacket incomingDatagramPacket = new DatagramPacket(buffer, buffer.length);
            try {
                datagramSocket.receive(incomingDatagramPacket);
                String request = new String(incomingDatagramPacket.getData(), 0,
                        incomingDatagramPacket.getLength());
                System.out.println(incomingDatagramPacket.getAddress() + ":" + incomingDatagramPacket.getPort() +
                        " - " + request.replace("\n", ""));
                StringTokenizer stringTokenizer = new StringTokenizer(request.trim(), " ");
                String length;
                String command;
                String ipAddress;
                String username;
                int port;
                try {
                    length = stringTokenizer.nextToken();
                    command = stringTokenizer.nextToken();
                } catch (NoSuchElementException e) {
                    throw new IOException();
                }
                switch (command.toUpperCase()) {
                    case "REG":
                        response.append("REGOK ");
                        if (nodes.size() == MAX_NODES) {
                            throw new ServerFullException("9996");
                        }
                        try {
                            ipAddress = stringTokenizer.nextToken();
                            port = Integer.parseInt(stringTokenizer.nextToken());
                            username = stringTokenizer.nextToken();
                        } catch (NoSuchElementException e) {
                            throw new CommandErrorException("9999");
                        }
                        for (Node node : nodes) {
                            if (!node.getIpAddress().equals(ipAddress)) {
                                continue;
                            } else if (node.getPort() != port) {
                                continue;
                            } else if (!node.getUsername().equals(username)) {
                                throw new AlreadyAssignedException("9997");
                            } else {
                                throw new AlreadyRegisteredException("9998");
                            }
                        }
                        response.append(nodes.size());
                        for (Node node : nodes) {
                            response.append(" ").append(node.getIpAddress()).append(" ").append(node.getPort());
                        }
                        nodes.add(new Node(ipAddress, port, username));
                        break;
                    case "UNREG":
                        response.append("UNROK ");
                        try {
                            ipAddress = stringTokenizer.nextToken();
                            port = Integer.parseInt(stringTokenizer.nextToken());
                            username = stringTokenizer.nextToken();
                        } catch (NoSuchElementException e) {
                            throw new CommandErrorException("9999");
                        }
                        boolean unregistered = false;
                        for (Node node : nodes) {
                            if (node.getIpAddress().equals(ipAddress) && node.getPort() == port) {
                                nodes.remove(node);
                                unregistered = true;
                                break;
                            }
                        }
                        if (unregistered) {
                            response.append("0");
                        } else {
                            response.append("9999");
                        }
                        break;
                    case "PRINT":
                        response.append("PRINTOK ").append(nodes.size());
                        for (Node node : nodes) {
                            response.append(" ").append(node.getIpAddress()).append(" ").append(node.getPort()).append(" ").append(node.getUsername());
                        }
                        break;
                    case "SHUTDOWN":
                        System.out.println("Server stopped.");
                        System.exit(0);
                    default:
                        throw new IOException();
                }
            } catch (IOException | AssertionError e) {
                response = new StringBuilder("ERROR");
            } catch (CommandErrorException | AlreadyAssignedException | AlreadyRegisteredException |
                    ServerFullException e) {
                response.append(e.getMessage());
            } finally {
                response = new StringBuilder(String.format("%04d", response.length() + 5) + " " + response + "\n");
                DatagramPacket outgoingDatagramPacket = new DatagramPacket(
                        response.toString().getBytes(),
                        response.toString().getBytes().length,
                        incomingDatagramPacket.getAddress(),
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
