package server;

public class CommandErrorException extends Exception {
    public CommandErrorException(String message) {
        super(message);
    }
}
