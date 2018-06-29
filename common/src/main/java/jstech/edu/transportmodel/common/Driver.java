package jstech.edu.transportmodel.common;

import java.util.Set;

// This is basically a class to create concrete object representing Person, even though there are no specific fields.
// In general, person is an abstract entity and shouldn't be creatable directly.
public class Driver extends Person {

    private Driver(int id, String firstName, String lastName, String nickName, String phoneNumber, String email, String keyProviderUserName,
                     Set<String> keyProviderRoles, UserRole role) {
        super(id, firstName, lastName, nickName, phoneNumber, email, keyProviderUserName, keyProviderRoles, role);
    }

    public static class Builder extends Person.Builder {
        public Driver build() {
            return new Driver(id, firstName, lastName, nickName, phoneNumber, email, keyProviderUserName, keyProviderRoles, role);
        }
    }
}
