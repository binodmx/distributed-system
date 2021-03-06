package node;

import common.Constants;
import common.File;

import java.io.IOException;
import java.util.*;

public class Node {
    public static void main(String[] args) {
        String MY_IP = "localhost";
        int MY_PORT = 55556;
        String MY_USERNAME = "node@" + (int) (Math.random() * 10000);
        String SERVER_IP = "localhost";
        int SERVER_PORT = 55555;
        ArrayList<common.Node> nodes = new ArrayList<>();
        ArrayList<File> files = new ArrayList<>();

        for (String arg : args) {
            if (arg.toLowerCase().startsWith("--node=")) {
                StringTokenizer stringTokenizer = new StringTokenizer(arg.substring(7), ":");
                MY_IP = stringTokenizer.nextToken();
                MY_PORT = Integer.parseInt(stringTokenizer.nextToken());
            } else if (arg.toLowerCase().startsWith("--server=")) {
                StringTokenizer stringTokenizer = new StringTokenizer(arg.substring(9), ":");
                SERVER_IP = stringTokenizer.nextToken();
                SERVER_PORT = Integer.parseInt(stringTokenizer.nextToken());
            } else if (arg.equalsIgnoreCase("--help")) {
                System.out.println("Usage: java node.Node [--port=<port>] [--server=<ip>:<port>] | [--help]\n");
                System.out.println("Default port\t= 55556\nDefault server\t= localhost:55555");
            } else {
                System.out.println("Error: Invalid arguments.\nUse 'java node.Node --help' command for help.");
                System.exit(0);
            }
        }

        // starting query handler
        QueryHandler queryHandler = new QueryHandler(nodes, files, MY_IP, MY_PORT, MY_USERNAME, SERVER_IP, SERVER_PORT);
        Thread thread1 = new Thread(queryHandler);
        thread1.start();

        // starting ftp server
        try {
            FTPServer ftpServer = new FTPServer(MY_PORT + Constants.FTP_PORT_OFFSET, MY_USERNAME);
            Thread thread2 = new Thread(ftpServer);
            thread2.start();
        } catch (IOException e) {
            System.out.println("Error: Couldn't start the FTP server.");
            System.exit(0);
        }

        System.out.println("Node " + MY_USERNAME + " created at " + MY_IP + ":" + MY_PORT + ".");

        // randomly assigning file names
        ArrayList<String> fileNames = new ArrayList<>(Arrays.asList(
                "Adventures of Tintin",
                "Jack and Jill",
                "Glee",
                "The Vampire Diarie",
                "King Arthur",
                "Windows XP",
                "Harry Potter",
                "Kung Fu Panda",
                "Lady Gaga",
                "Twilight",
                "Windows 8",
                "Mission Impossible",
                "Turn Up The Music",
                "Super Mario",
                "American Pickers",
                "Microsoft Office 2010",
                "Happy Feet",
                "Modern Family",
                "American Idol",
                "Hacking for Dummies"
        ));
        Collections.shuffle(fileNames);
        System.out.println("Available Files:");
        for (int i = 0; i < Math.min(Constants.MAX_FILES, (int) (Math.random() * 10) + Constants.MIN_FILES); i++) {
            files.add(new File(fileNames.get(i)));
            System.out.print( "\"" + fileNames.get(i) + "\" ");
        }

        System.out.println("\nWaiting for incoming requests...");
    }
}