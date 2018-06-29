package jstech.edu.transportmodel;

public class GeoException extends Exception {
    public GeoException(String message) {
        super(message);
    }

    public GeoException(String message, Exception e) {
        super(message, e);
    }
}
