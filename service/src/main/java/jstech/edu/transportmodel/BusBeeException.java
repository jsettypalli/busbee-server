package jstech.edu.transportmodel;

public class BusBeeException extends Exception {

    public BusBeeException(String message) {
        super(message);
    }

    public BusBeeException(String message, Throwable cause) {
        super(message, cause);
    }

    public BusBeeException(Throwable cause) {
        super(cause);
    }
}
