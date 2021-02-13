package node;

import exceptions.AlreadyAssignedException;
import exceptions.AlreadyRegisteredException;
import exceptions.CommandErrorException;
import exceptions.ServerFullException;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

public class Node {
    public static void main(String[] args) {
        ArrayList<common.Node> nodes = new ArrayList<>();
        DatagramSocket datagramSocket = null;
        MessageBroker messageBroker = null;
        String USERNAME = "node@" + (int) (Math.random() * 10000);
        String MY_IP = "localhost";
        int MY_PORT = 55556;
        String SERVER_IP = "localhost";
        int SERVER_PORT = 55555;

        // creating datagram socket
        try {
            for (String arg : args) {
                if (arg.toLowerCase().startsWith("port=")) {
                    MY_PORT = Integer.parseInt(arg.substring(5));
                } else if (arg.toLowerCase().startsWith("server=")) {
                    StringTokenizer stringTokenizer = new StringTokenizer(arg.substring(7), ":");
                    SERVER_IP = stringTokenizer.nextToken();
                    SERVER_PORT = Integer.parseInt(stringTokenizer.nextToken());
                }
            }
            datagramSocket = new DatagramSocket(MY_PORT);
            System.out.println("Node " + USERNAME + " is created at port " + MY_PORT + ".");
        } catch (SocketException e) {
            System.out.println("Error: Couldn't initialize the socket.");
            System.exit(0);
        }

//        try(final DatagramSocket socket = new DatagramSocket()){
//            socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
//            System.out.println(socket.getLocalAddress().getHostAddress());
//            System.out.println(datagramSocket.getLocalSocketAddress());
//        } catch (SocketException | UnknownHostException e) {
//            e.printStackTrace();
//        }

        // initializing message broker
        try {
            messageBroker = new MessageBroker();
        } catch (SocketException e) {
            System.out.println("Error: Couldn't initialize the message broker.");
            System.exit(0);
        }

        // registering with the server
        try {
            String request = "REG " + MY_IP + " " + MY_PORT + " " + USERNAME;
            request = String.format("%04d", request.length() + 5) + " " + request + "\n";
            String response = messageBroker.send(request, SERVER_IP, SERVER_PORT);
            System.out.println(response.trim());
            StringTokenizer stringTokenizer = new StringTokenizer(response.trim(), " ");
            String length = stringTokenizer.nextToken();
            String command = stringTokenizer.nextToken();
            String nodesCount = stringTokenizer.nextToken();
            switch (nodesCount) {
                case "0":
                    System.out.println("Registered successfully. No nodes in the system yet.");
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
                    // here, assume that server node always gets a correct response from the server.
                    while (stringTokenizer.hasMoreTokens()) {
                        String ipAddress = stringTokenizer.nextToken();
                        int port = Integer.parseInt(stringTokenizer.nextToken());
                        nodes.add(new common.Node(ipAddress, port));
                    }
                    System.out.println("Registered successfully.");
            }
        } catch (CommandErrorException | AlreadyRegisteredException | AlreadyAssignedException | ServerFullException e) {
            System.out.println(e.getMessage());
            System.exit(0);
        } catch (IOException | NoSuchElementException e) {
            System.out.println("Error: Couldn't register with the server.");
            System.exit(0);
        }
        
        // joining the network
        for (common.Node node : nodes) {
            try {
                String request = "JOIN " + MY_IP + " " + MY_PORT;
                request = String.format("%04d", request.length() + 5) + " " + request + "\n";
                String response = messageBroker.send(request, node.getIpAddress(), node.getPort());
                System.out.println(response.trim());
            } catch (IOException e) {
                System.out.println("Error: Couldn't join the node at " + node.getIpAddress() + ":" + node.getPort());
            }
        }

        // waiting for incoming requests
        System.out.println("Waiting for incoming requests...");
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
                String length = stringTokenizer.nextToken();
                String command = stringTokenizer.nextToken();
                String ipAddress;
                int port;
                String filename;
                String hops;
                switch (command.toUpperCase()) {
                    case "JOIN":
                        response.append("JOINOK ");
                        ipAddress = stringTokenizer.nextToken();
                        port = Integer.parseInt(stringTokenizer.nextToken());
                        try {
                            nodes.add(new common.Node(ipAddress, port));
                            response.append("0");
                        } catch (Exception e) {
                            response.append("9999");
                        }
                        break;
                    case "LEAVE":
                        response.append("LEAVEOK ");
                        ipAddress = stringTokenizer.nextToken();
                        port = Integer.parseInt(stringTokenizer.nextToken());
                        boolean left = false;
                        for (common.Node node : nodes) {
                            if (node.getIpAddress().equals(ipAddress) && node.getPort() == port) {
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
                        port = Integer.parseInt(stringTokenizer.nextToken());
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
                    case "SHUTDOWN":
                        // leaving the network
                        for (common.Node node : nodes) {
                            try {
                                request = "LEAVE " + MY_IP + " " + MY_PORT;
                                request = String.format("%04d", request.length() + 5) + " " + request + "\n";
                                response.append(messageBroker.send(request, node.getIpAddress(), node.getPort()));
                                System.out.println(response.toString().trim());
                            } catch (IOException e) {
                                System.out.println("Error: Couldn't leave the node at " + node.getIpAddress() + ":" + node.getPort());
                            }
                        }

                        // unregistering with the server
                        try {
                            request = "UNREG " + MY_IP + " " + MY_PORT + " " + USERNAME;
                            request = String.format("%04d", request.length() + 5) + " " + request + "\n";
                            response.append(messageBroker.send(request, SERVER_IP, SERVER_PORT));
                            System.out.println(response.toString().trim());
                            System.exit(0);
                        } catch (IOException e) {
                            System.out.println("Error: Couldn't unregister from the server.");
                        }
                        break;
                    default:
                        throw new IOException();
                }
            } catch (IOException | AssertionError | NoSuchElementException e) {
                response = new StringBuilder("ERROR");
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
