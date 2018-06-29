package jstech.edu.transportmodel.auth;

import jstech.edu.transportmodel.BusBeeException;

public interface AuthKeyLoader {
    void loadKeys() throws BusBeeException;
}
