package node;

import common.File;
import common.Node;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

public class QueryHandler implements Runnable {
    private DatagramSocket datagramSocket;
    private ArrayList<Node> nodes;
    private ArrayList<File> files;
    private MessageBroker messageBroker;
    private final String NODE_IP;
    private final int NODE_PORT;

    public QueryHandler(ArrayList<Node> nodes, ArrayList<File> files, String NODE_IP, int NODE_PORT) {
        this.nodes = nodes;
        this.files = files;
        this.NODE_IP = NODE_IP;
        this.NODE_PORT = NODE_PORT;

        // initializing datagram socket
        try {
            this.datagramSocket = new DatagramSocket(NODE_PORT);
        } catch (SocketException e) {
            System.out.println("Error: Couldn't initialize the datagram socket.");
            System.exit(0);
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
                incomingDatagramPacket = messageBroker.receive();
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
                        String temp;
                        if ((temp = stringTokenizer.nextToken()).startsWith("\"")) {
                            searchTerms.add(temp.substring(1));
                        } else {
                            response.append("9998");
                            break;
                        }
                        while ((temp = stringTokenizer.nextToken()).endsWith("\"")) {
                            searchTerms.add(temp);
                        }
                        hops = Integer.parseInt(temp);

                        // searching for matching file names
                        ArrayList<File> foundFiles = new ArrayList<>();
                        for (File file : files) {
                            boolean isMatching = true;
                            String fileName = file.getName();
                            for (String searchTerm : searchTerms) {
                                if (!fileName.toLowerCase().contains(searchTerm.toLowerCase())) {
                                    isMatching = false;
                                }
                            }
                            if (isMatching) {
                                foundFiles.add(file);
                            }
                        }

                        if (foundFiles.size() > 0) {
                            response.append(foundFiles.size() + " " + NODE_IP + " " + NODE_PORT + " " + hops);
                            for (File file : foundFiles) {
                                response.append(" " + file.getName());
                            }
                        } else if (hops < Constants.MAX_HOPS) {
                            request = "SER " + NODE_IP + " " + NODE_PORT + "\"" + String.join(" ", searchTerms) + "\"" + " " + (hops + 1);
                            request = String.format("%04d", request.length() + 5) + " " + request + "\n";
                            // todo: change here in order to make sure random numbers are not equal
                            common.Node node1 = nodes.get((int) (Math.random() * nodes.size()));
                            try {
                                String response1 = messageBroker.sendAndReceive(request, node1.getIpAddress(), node1.getPort(), Constants.NODE_SEARCH_TIMEOUT);
                                response = new StringBuilder(response1);
                            } catch (IOException e1) {
                                System.out.println("Error: Couldn't connect the node at " + node1.getIpAddress() + ":" + node1.getPort());
                                common.Node node2 = nodes.get((int) (Math.random() * nodes.size()));
                                try {
                                    String response2 = messageBroker.sendAndReceive(request, node2.getIpAddress(), node2.getPort(), Constants.NODE_SEARCH_TIMEOUT);
                                    response = new StringBuilder(response2);
                                } catch (IOException e2) {
                                    System.out.println("Error: Couldn't connect the node at " + node2.getIpAddress() + ":" + node2.getPort());
                                }
                            }
                        } else {
                            response.append("0");
                        }
                        break;
                    case "PRINT":
                        response.append("PRINTOK ").append(nodes.size());
                        for (common.Node node : nodes) {
                            response.append(" ").append(node.getIpAddress()).append(" ").append(node.getPort()).append(" ").append(node.getUsername());
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
