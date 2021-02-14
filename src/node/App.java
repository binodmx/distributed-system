package node;

import java.util.Scanner;
import java.util.StringTokenizer;

public class App {
    public static void main(String[] args) {
        String NODE_IP = "localhost";
        int NODE_PORT = 55556;
        String NODE_USERNAME = "node@" + (int) (Math.random() * 10000);
        String SERVER_IP = "localhost";
        int SERVER_PORT = 55555;

        for (String arg : args) {
            if (arg.toLowerCase().startsWith("-port=")) {
                NODE_PORT = Integer.parseInt(arg.substring(6));
            } else if (arg.toLowerCase().startsWith("-server=")) {
                StringTokenizer stringTokenizer = new StringTokenizer(arg.substring(8), ":");
                SERVER_IP = stringTokenizer.nextToken();
                SERVER_PORT = Integer.parseInt(stringTokenizer.nextToken());
            } else if (arg.toLowerCase().equals("-help")) {
                System.out.println("Usage: java app [-port=<port>] [-server=<ip>:<port>] [-help]\n");
                System.out.println("Default port\t= 55556\nDefault server\t= localhost:55555");
            } else {
                System.out.println("Error: Invalid arguments.\nUse 'java app -help' command for help.");
                System.exit(0);
            }
        }
        
        Node node = new Node(NODE_IP, NODE_PORT, NODE_USERNAME, SERVER_IP, SERVER_PORT);

        while (true) {
            Scanner scanner = new Scanner(System.in);
            System.out.println("\nChoose an option to continue:");
            System.out.println("1. Search a file");
            System.out.println("2. Print routing table");
            System.out.println("3. Exit network");
            System.out.print("\nPlease enter the option number: ");
            String option = scanner.nextLine();
            switch (option) {
                case "1":
                    System.out.print("\nPlease enter the file name: ");
                    String fileName = scanner.nextLine();
                    node.search(fileName);
                    break;
                case "2":
                    node.print();
                    break;
                case "3":
                    node.leave();
                    node.unregister();
                    System.exit(0);
                    break;
                default:
                    System.out.println("\nInvalid option number. Try again.\n");
            }

        }

    }
}
