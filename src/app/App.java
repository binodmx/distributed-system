package app;

import common.Constants;
import common.MessageBroker;

import java.io.IOException;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.StringTokenizer;

public class App {
    public static void main(String[] args) {
        String NODE_IP = "localhost";
        int NODE_PORT = 55556;
        MessageBroker messageBroker = null;
        Scanner scanner = new Scanner(System.in);
        String option;
        String request;
        String response;
        for (String arg : args) {
            if (arg.toLowerCase().startsWith("-node=")) {
                StringTokenizer stringTokenizer = new StringTokenizer(arg.substring(6), ":");
                NODE_IP = stringTokenizer.nextToken();
                NODE_PORT = Integer.parseInt(stringTokenizer.nextToken());
            } else if (arg.toLowerCase().equals("-help")) {
                System.out.println("Usage: java app [-node=<ip>:<port>] [-help]\n");
                System.out.println("Default node\t= localhost:55556");
            } else {
                System.out.println("Error: Invalid arguments.\nUse 'java node -help' command for help.");
                System.exit(0);
            }
        }
        // initializing message broker
        try {
            messageBroker = new MessageBroker();
        } catch (SocketException e) {
            System.out.println("Error: Couldn't initiate the message broker.");
            System.exit(0);
        }
        loop1: while (true) {
            System.out.println("\nChoose an option to continue:");
            System.out.println("1. Start node");
            System.out.println("2. Exit");
            System.out.print("\nPlease enter the option number: ");
            option = scanner.nextLine();
            switch (option) {
                case "1":
                    request = "START";
                    request = String.format("%04d", request.length() + 5) + " " + request + "\n";
                    response = null;
                    try {
                        response = messageBroker.sendAndReceive(request, NODE_IP, NODE_PORT, Constants.NODE_REQUEST_TIMEOUT).trim();
                    } catch (IOException e) {
                        System.out.println("Error: Unable to start the node.");
                    }
                    if (response.equals("0014 STARTOK 0")) {
                        System.out.println("Node started successfully.");
                        loop2: while (true) {
                            System.out.println("\nChoose an option to continue:");
                            System.out.println("1. Search a file");
                            System.out.println("2. Print routing table");
                            System.out.println("3. Print available files");
                            System.out.println("4. Stop node");
                            System.out.print("\nPlease enter the option number: ");
                            option = scanner.nextLine();
                            switch (option) {
                                case "1":
                                    System.out.print("Please enter the file name to search: ");
                                    String fileName = scanner.nextLine();
                                    request = "SER " + NODE_IP + " " + NODE_PORT + " \"" + fileName + "\" 0";
                                    request = String.format("%04d", request.length() + 5) + " " + request + "\n";
                                    response = null;
                                    try {
                                        response = messageBroker.sendAndReceive(request, NODE_IP, NODE_PORT, Constants.NODE_SEARCH_TIMEOUT).trim();
                                        StringTokenizer stringTokenizer = new StringTokenizer(response);
                                        String length = stringTokenizer.nextToken();
                                        String command = stringTokenizer.nextToken();
                                        int fileCount = Integer.parseInt(stringTokenizer.nextToken());
                                        if (fileCount == 9999 || fileCount == 9998) {
                                            System.out.println("\nNo files available for the search term '" + fileName + "'");
                                            break;
                                        }
                                        String ipAddress = stringTokenizer.nextToken();
                                        int port = Integer.parseInt(stringTokenizer.nextToken());
                                        int hops = Integer.parseInt(stringTokenizer.nextToken());
                                        ArrayList<String> fileNames = new ArrayList<>();
                                        for (int i = 0; i < fileCount; i++) {
                                            String tempToken = stringTokenizer.nextToken();
                                            if (tempToken.startsWith("\"")) {
                                                if (tempToken.endsWith("\"")) {
                                                    fileNames.add(tempToken.substring(1, tempToken.lastIndexOf("\"")));
                                                } else {
                                                    String tempFileName = tempToken.substring(1);
                                                    while (!(tempToken = stringTokenizer.nextToken()).endsWith("\"")) {
                                                        tempFileName += " " + tempToken;
                                                    }
                                                    tempFileName += " " + tempToken.substring(0, tempToken.length() - 1);
                                                    fileNames.add(tempFileName);
                                                }
                                            } else {
                                                System.out.println("Error: Invalid search result.");
                                                break;
                                            }
                                        }
                                        if (fileNames.size() > 0) {
                                            System.out.println("\nAvailable files for the search term '" + fileName + "':");
                                            for (int i = 0; i < fileNames.size(); i++) {
                                                System.out.println((i + 1) + ". " + fileNames.get(i));
                                            }
                                            //todo : implement donwload
                                            System.out.print("\nSelect a file to download: ");
                                            int downloadOption = Integer.parseInt(scanner.nextLine());
                                            request = "DOWNLOAD " + ipAddress + " " + port + " \"" + fileNames.get(downloadOption-1) + "\"";
                                            request = String.format("%04d", request.length() + 5) + " " + request + "\n";
                                            response = messageBroker.sendAndReceive(request, ipAddress, port, Constants.NODE_SEARCH_TIMEOUT).trim();
                                            System.out.println(response);

                                        } else {
                                            System.out.println("\nNo files available for the search term '" + fileName + "':");
                                        }
                                    } catch (IOException e) {
                                        System.out.println("Error: Unable to search.");
                                    }
                                    break;
                                case "2":
                                    request = "PRINT";
                                    request = String.format("%04d", request.length() + 5) + " " + request + "\n";
                                    response = null;
                                    try {
                                        response = messageBroker.sendAndReceive(request, NODE_IP, NODE_PORT, Constants.NODE_REQUEST_TIMEOUT).trim();
                                    } catch (IOException e) {
                                        System.out.println("Error: Unable to print the routing table.");
                                    }
                                    // todo: handle response here
                                    System.out.println(response);
                                    break;
                                case "3":
                                    request = "PRINTF";
                                    request = String.format("%04d", request.length() + 5) + " " + request + "\n";
                                    response = null;
                                    try {
                                        response = messageBroker.sendAndReceive(request, NODE_IP, NODE_PORT, Constants.NODE_REQUEST_TIMEOUT).trim();
                                    } catch (IOException e) {
                                        System.out.println("Error: Unable to print available files.");
                                    }
                                    // todo: handle response here
                                    System.out.println(response);
                                    break;
                                case "4":
                                    request = "STOP";
                                    request = String.format("%04d", request.length() + 5) + " " + request + "\n";
                                    response = null;
                                    try {
                                        response = messageBroker.sendAndReceive(request, NODE_IP, NODE_PORT, Constants.NODE_REQUEST_TIMEOUT).trim();
                                    } catch (IOException e) {
                                        System.out.println("Error: Unable to stop the server.");
                                    }
                                    if (response.equals("0013 STOPOK 0")) {
                                        System.out.println("Node stopped successfully.");
                                        break loop2;
                                    } else if (response.equals("0016 STOPOK 9999")) {
                                        System.out.println("Error: An error occurred while stopping the node.");
                                        break loop2;
                                    } else {
                                        System.out.println("Error: An unknown error occurred while stopping the node.");
                                        break loop2;
                                    }
                                default:
                                    System.out.println("Invalid option number. Try again.\n");
                            }
                        }
                    } else if (response.equals("0017 STARTOK 9999")) {
                        System.out.println("Error: An error occurred while starting the node.");
                    } else {
                        System.out.println("Error: An unknown error occurred while starting the node.");
                    }
                    break;
                case "2":
                    break loop1;
                default:
                    System.out.println("\nInvalid option number. Try again.\n");
            }
        }

    }
}
