package server;

public class AlreadyAssignedException extends Exception {
    public AlreadyAssignedException(String message) {
        super(message);
    }
}
