package node;

import common.Constants;
import common.File;
import common.Node;
import common.MessageBroker;
import common.AlreadyAssignedException;
import common.AlreadyRegisteredException;
import common.CommandErrorException;
import common.ServerFullException;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.*;

public class QueryHandler implements Runnable {
    private DatagramSocket datagramSocket;
    private ArrayList<Node> nodes;
    private ArrayList<File> files;
    private MessageBroker messageBroker;
    private final String NODE_IP;
    private final int NODE_PORT;
    private final String NODE_USERNAME;
    private final String SERVER_IP;
    private final int SERVER_PORT;

    public QueryHandler(ArrayList<Node> nodes, ArrayList<File> files, String NODE_IP, int NODE_PORT, String NODE_USERNAME, String SERVER_IP, int SERVER_PORT) {
        this.nodes = nodes;
        this.files = files;
        this.NODE_IP = NODE_IP;
        this.NODE_PORT = NODE_PORT;
        this.NODE_USERNAME = NODE_USERNAME;
        this.SERVER_IP = SERVER_IP;
        this.SERVER_PORT = SERVER_PORT;

        // initializing datagram socket
        try {
            this.datagramSocket = new DatagramSocket(NODE_PORT);
        } catch (SocketException e) {
            System.out.println("Error: Couldn't initialize the datagram socket.");
        }

        // initializing message broker
        this.messageBroker = new MessageBroker(datagramSocket);
    }

    @Override
    public void run() {
        while (true) {
            StringBuilder response = new StringBuilder();
            DatagramPacket incomingDatagramPacket = null;
            try {
                incomingDatagramPacket = messageBroker.receive(Constants.NODE_REQUEST_TIMEOUT);
            } catch (IOException e) {
                continue;
            }
            try {
                String request = new String(incomingDatagramPacket.getData(), 0,
                        incomingDatagramPacket.getLength());
                System.out.println(incomingDatagramPacket.getAddress() + ":" + incomingDatagramPacket.getPort() +
                        " - " + request.replace("\n", ""));
                StringTokenizer stringTokenizer = new StringTokenizer(request.trim(), " ");
                String length = stringTokenizer.nextToken();
                String command = stringTokenizer.nextToken();
                String ipAddress;
                int port;
                ArrayList<String> searchTerms;
                int hops;
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
                        response.append("SEROK ");
                        ipAddress = stringTokenizer.nextToken();
                        port = Integer.parseInt(stringTokenizer.nextToken());

                        // extracting search terms from the search query
                        searchTerms = new ArrayList<>();
                        String temp = stringTokenizer.nextToken();
                        if (temp.startsWith("\"")) {
                            if (temp.endsWith("\"")) {
                                searchTerms.add(temp.substring(1, temp.lastIndexOf("\"")));
                            } else {
                                searchTerms.add(temp.substring(1));
                                while (!(temp = stringTokenizer.nextToken()).endsWith("\"")) {
                                    searchTerms.add(temp);
                                }
                                searchTerms.add(temp.substring(0,temp.length() - 1));
                            }
                        } else {
                            response.append("9998");
                            break;
                        }

                        hops = Integer.parseInt(stringTokenizer.nextToken());

                        // searching for matching file names
                        ArrayList<File> foundFiles = new ArrayList<>();
                        for (File file : files) {
                            boolean isMatching = true;
                            ArrayList<String> fileNameSlices = new ArrayList<>(Arrays.asList(file.getName().toLowerCase().split("\\s+")));
                            for (String searchTerm : searchTerms) {
                                if (!fileNameSlices.contains(searchTerm.toLowerCase())) {
                                    isMatching = false;
                                    break;
                                }
                            }
                            if (isMatching) {
                                foundFiles.add(file);
                            }
                        }

                        // output found files or send the SER request for 2 neighbour nodes
                        if (foundFiles.size() > 0) {
                            response.append(foundFiles.size() + " " + NODE_IP + " " + NODE_PORT + " " + hops);
                            for (File file : foundFiles) {
                                response.append(" " + file.getName());
                            }
                        } else if (hops < Constants.MAX_HOPS && nodes.size() > 0) {
                            request = "SER " + NODE_IP + " " + NODE_PORT + " \"" + String.join(" ", searchTerms) + "\" " + (hops + 1);
                            request = String.format("%04d", request.length() + 5) + " " + request + "\n";
                            Collections.shuffle(nodes);
                            for (int i = 0; i < Math.min(2, nodes.size()); i++) {
                                Node node = nodes.get(i);
                                try {
                                    String mbResponse = messageBroker.sendAndReceive(request, node.getIpAddress(), node.getPort(), Constants.NODE_SEARCH_TIMEOUT);
                                    response = new StringBuilder(mbResponse);
                                    break;
                                } catch (IOException e) {
                                    System.out.println("Error: Couldn't connect the node at " + node.getIpAddress() + ":" + node.getPort());
                                }
                            }
                            if (response.toString().equals("SEROK ")) {
                                response.append("9999");
                            }
                        } else {
                            response.append("0");
                        }
                        break;
                    case "START":
                        response.append("STARTOK ");
                        // registering with the server
                        try {
                            request = "REG " + NODE_IP + " " + NODE_PORT + " " + NODE_USERNAME;
                            request = String.format("%04d", request.length() + 5) + " " + request + "\n";
                            String mbResponse = messageBroker.sendAndReceive(request, SERVER_IP, SERVER_PORT, Constants.SERVER_REG_TIMEOUT);
                            System.out.println(mbResponse.trim());
                            // Here onwards, assume that node always gets a correct response from the server.
                            // Otherwise stringTokenizer will return NoSuchElement exception
                            stringTokenizer = new StringTokenizer(mbResponse.trim(), " ");
                            length = stringTokenizer.nextToken();
                            command = stringTokenizer.nextToken();
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
                                    while (stringTokenizer.hasMoreTokens()) {
                                        ipAddress = stringTokenizer.nextToken();
                                        port = Integer.parseInt(stringTokenizer.nextToken());
                                        nodes.add(new common.Node(ipAddress, port));
                                    }
                                    System.out.println("Registered successfully.");
                            }
                        } catch (CommandErrorException | AlreadyRegisteredException | AlreadyAssignedException | ServerFullException e) {
                            System.out.println(e.getMessage());
                            response.append("9999");
                        } catch (IOException e) {
                            System.out.println("Error: Couldn't register with the server.");
                            response.append("9999");
                        }
                        // joining the network
                        for (common.Node node : nodes) {
                            try {
                                request = "JOIN " + NODE_IP + " " + NODE_PORT;
                                request = String.format("%04d", request.length() + 5) + " " + request + "\n";
                                String mbResponse = messageBroker.sendAndReceive(request, node.getIpAddress(), node.getPort(), Constants.NODE_JOIN_TIMEOUT);
                                System.out.println(mbResponse.trim());
                            } catch (IOException e) {
                                System.out.println("Error: Couldn't join the node at " + node.getIpAddress() + ":" + node.getPort());
                                if (!response.toString().contains("9999")) {
                                    response.append("9999");
                                }
                            }
                        }
                        if (!response.toString().contains("9999")) {
                            response.append("0");
                        }
                        break;
                    case "STOP":
                        response.append("STOPOK ");
                        // leaving from the network
                        for (common.Node node : nodes) {
                            try {
                                request = "LEAVE " + NODE_IP + " " + NODE_PORT;
                                request = String.format("%04d", request.length() + 5) + " " + request + "\n";
                                String mbResponse = messageBroker.sendAndReceive(request, node.getIpAddress(), node.getPort(), Constants.NODE_LEAVE_TIMEOUT);
                                System.out.println(mbResponse.trim());
                            } catch (IOException e) {
                                System.out.println("Error: Couldn't leave the node at " + node.getIpAddress() + ":" + node.getPort());
                                response.append("9999");
                            }
                        }
                        // unregistering from the server
                        try {
                            request = "UNREG " + NODE_IP + " " + NODE_PORT + " " + NODE_USERNAME;
                            request = String.format("%04d", request.length() + 5) + " " + request + "\n";
                            String mbResponse = messageBroker.sendAndReceive(request, SERVER_IP, SERVER_PORT, Constants.SERVER_UNREG_TIMEOUT);
                            System.out.println(mbResponse.trim());
                            stringTokenizer = new StringTokenizer(mbResponse);
                            length = stringTokenizer.nextToken();
                            command = stringTokenizer.nextToken();
                            String value = stringTokenizer.nextToken();
                            switch (value) {
                                case "0":
                                    System.out.println("Unregistered successfully.");
                                    break;
                                case "9999":
                                    throw new CommandErrorException("Error: Unable to unregister.");
                            }
                        } catch (CommandErrorException e) {
                            System.out.println(e.getMessage());
                            if (!response.toString().contains("9999")) {
                                response.append("9999");
                            }
                        } catch (IOException e) {
                            System.out.println("Error: Couldn't unregister from the server.");
                            if (!response.toString().contains("9999")) {
                                response.append("9999");
                            }
                        }
                        if (!response.toString().contains("9999")) {
                            response.append("0");
                            nodes.clear();
                        }
                        break;
                    case "DOWNLOAD":
                        response.append("DOWNLOADOK ");
                        ipAddress = stringTokenizer.nextToken();
                        port = Integer.parseInt(stringTokenizer.nextToken());

                        // extracting file name from the request
                        String fileName = "";
                        String tempNextToken = stringTokenizer.nextToken();
                        if (tempNextToken.startsWith("\"")) {
                            if (tempNextToken.endsWith("\"")) {
                                fileName += tempNextToken.substring(1, tempNextToken.lastIndexOf("\""));
                            } else {
                                fileName += tempNextToken.substring(1);
                                while (!(tempNextToken = stringTokenizer.nextToken()).endsWith("\"")) {
                                    fileName += " " + tempNextToken;
                                }
                                fileName += " " + tempNextToken.substring(0, tempNextToken.lastIndexOf("\""));
                            }
                        } else {
                            response.append("9999");
                            break;
                        }

                        try {
                            FTPClient ftpClient = new FTPClient(ipAddress, port + Constants.FTP_PORT_OFFSET, fileName);
                        } catch (IOException e) {
                            System.out.println("Error: Couldn't download the file.");
                            response.append("9999");
                        }
                        response.append("0");
                        break;
                    case "PRINT":
                        response.append("PRINTOK ").append(nodes.size());
                        for (common.Node node : nodes) {
                            response.append(" ").append(node.getIpAddress()).append(" ").append(node.getPort());
                        }
                        break;
                    case "PRINTF":
                        response.append("PRINTFOK ").append(nodes.size());
                        for (File file : files) {
                            response.append(" \"").append(file.getName()).append("\"");
                        }
                        break;
                    default:
                        throw new IOException();
                }
            } catch (IOException | NoSuchElementException e) {
                response = new StringBuilder("ERROR");
            } finally {
                response = new StringBuilder(String.format("%04d", response.length() + 5) + " " + response + "\n");
                try {
                    DatagramPacket responseDatagramPacket = new DatagramPacket(
                            response.toString().getBytes(),
                            response.length(),
                            incomingDatagramPacket.getAddress(),
                            incomingDatagramPacket.getPort()
                    );
                    messageBroker.send(responseDatagramPacket, Constants.NODE_RESPONSE_TIMEOUT);
                } catch (IOException e) {
                    System.out.println("Error: Unable to send response.");
                }
            }
        }
    }
}
