package node;

import common.File;
import exceptions.AlreadyAssignedException;
import exceptions.AlreadyRegisteredException;
import exceptions.CommandErrorException;
import exceptions.ServerFullException;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.StringTokenizer;

public class Node {
    private final String MY_IP;
    private final int MY_PORT;
    private final String MY_USERNAME;
    private final String SERVER_IP;
    private final int SERVER_PORT;
    private ArrayList<common.Node> nodes;
    private ArrayList<File> files;
    private MessageBroker messageBroker;
    private QueryHandler queryHandler;

    public Node(String MY_IP, int MY_PORT, String MY_USERNAME, String SERVER_IP, int SERVER_PORT) {
        this.MY_IP = MY_IP;
        this.MY_PORT = MY_PORT;
        this.MY_USERNAME = MY_USERNAME;
        this.SERVER_IP = SERVER_IP;
        this.SERVER_PORT = SERVER_PORT;
        this.nodes = new ArrayList<>();
        this.files = new ArrayList<>();

        // initializing message broker
        try {
            this.messageBroker = new MessageBroker();
        } catch (SocketException e) {
            System.out.println("Error: Couldn't initialize the message broker.");
            System.exit(0);
        }

        // registering with the server
        this.register();
        
        // joining the network
        this.join();

        // starting query handler
        queryHandler = new QueryHandler(nodes, files, MY_IP, MY_PORT);
        Thread thread = new Thread(queryHandler);
        thread.start();

        // starting file handler
        // todo: add ftp server code here

        System.out.println("Node " + MY_USERNAME + " is created at " + MY_IP + ":" + MY_PORT + ". Waiting for incoming requests...");
    }

    public void register() {
        String request;
        String response;
        try {
            request = "REG " + MY_IP + " " + MY_PORT + " " + MY_USERNAME;
            request = String.format("%04d", request.length() + 5) + " " + request + "\n";
            response = messageBroker.sendAndReceive(request, SERVER_IP, SERVER_PORT, Constants.SERVER_REG_TIMEOUT);
            System.out.println(response.trim());
            // Here onwards, assume that node always gets a correct response from the server.
            // Otherwise stringTokenizer will return NoSuchElement exception
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
        } catch (IOException e) {
            System.out.println("Error: Couldn't register with the server.");
            System.exit(0);
        }
    }

    public void unregister() {
        String request;
        String response;
        try {
            request = "UNREG " + MY_IP + " " + MY_PORT + " " + MY_USERNAME;
            request = String.format("%04d", request.length() + 5) + " " + request + "\n";
            response = messageBroker.sendAndReceive(request, SERVER_IP, SERVER_PORT, Constants.SERVER_UNREG_TIMEOUT);
            System.out.println(response.toString().trim());
            // todo: handle unregister error codes
        } catch (IOException e) {
            System.out.println("Error: Couldn't unregister from the server.");
        }
    }

    public void join() {
        String request;
        String response;
        for (common.Node node : nodes) {
            try {
                request = "JOIN " + MY_IP + " " + MY_PORT;
                request = String.format("%04d", request.length() + 5) + " " + request + "\n";
                response = messageBroker.sendAndReceive(request, node.getIpAddress(), node.getPort(), Constants.NODE_JOIN_TIMEOUT);
                System.out.println(response.trim());
            } catch (IOException e) {
                System.out.println("Error: Couldn't join the node at " + node.getIpAddress() + ":" + node.getPort());
            }
        }
    }

    public void leave() {
        String request;
        String response;
        for (common.Node node : nodes) {
            try {
                request = "LEAVE " + MY_IP + " " + MY_PORT;
                request = String.format("%04d", request.length() + 5) + " " + request + "\n";
                response = messageBroker.sendAndReceive(request, node.getIpAddress(), node.getPort(), Constants.NODE_LEAVE_TIMEOUT);
                System.out.println(response.toString().trim());
            } catch (IOException e) {
                System.out.println("Error: Couldn't leave the node at " + node.getIpAddress() + ":" + node.getPort());
            }
        }
    }

    public void search(String query) {
        String request;
        String response1;
        String response2;
        int hops = 0;
        ArrayList<File> foundFiles = new ArrayList<>();
        for (File file : files) {
            boolean isMatching = true;
            String fileName = file.getName();
            for (String searchTerm : query.split("\\s+")) {
                if (!fileName.toLowerCase().contains(searchTerm.toLowerCase())) {
                    isMatching = false;
                }
            }
            if (isMatching) {
                foundFiles.add(file);
            }
        }
        if (foundFiles.size() > 0) {
            for (File file : foundFiles) {
                System.out.println(file.getName());
            }
        } else if (hops < Constants.MAX_HOPS) {
            request = "SER " + MY_IP + " " + MY_PORT + query + " " + (hops + 1);
            request = String.format("%04d", request.length() + 5) + " " + request + "\n";
            // todo: change here in order to make sure random numbers are not equal
            common.Node node1 = nodes.get((int) (Math.random() * nodes.size()));
            try {
                response1 = messageBroker.sendAndReceive(request, node1.getIpAddress(), node1.getPort(), Constants.NODE_SEARCH_TIMEOUT);
                System.out.println(response1.trim());
            } catch (IOException e1) {
                System.out.println("Error: Couldn't connect the node at " + node1.getIpAddress() + ":" + node1.getPort());
                common.Node node2 = nodes.get((int) (Math.random() * nodes.size()));
                try {
                    response2 = messageBroker.sendAndReceive(request, node2.getIpAddress(), node2.getPort(), Constants.NODE_SEARCH_TIMEOUT);
                    System.out.println(response2.trim());
                } catch (IOException e2) {
                    System.out.println("Error: Couldn't connect the node at " + node2.getIpAddress() + ":" + node2.getPort());
                }
            }
        } else {
            System.out.println("File not found.");
        }
    }

    public void print() {
        if (nodes.size() > 0) {
            for (common.Node node : nodes) {
                System.out.println(node.getIpAddress() + "\t" + node.getPort());
            }
        } else {
            System.out.println("No nodes in the routing table.");
        }
    }
}
