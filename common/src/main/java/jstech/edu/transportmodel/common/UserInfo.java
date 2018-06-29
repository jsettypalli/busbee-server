package jstech.edu.transportmodel.common;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class UserInfo {
    private String phoneNumber;
    private String email;

    private String keyProviderUserName;
    private Set<String> keyProviderRoles;
    private UserRole role;

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getEmail() {
        return email;
    }

    public String getKeyProviderUserName() {
        return keyProviderUserName;
    }

    public Set<String> getKeyProviderRoles() {
        return keyProviderRoles;
    }

    public UserRole getRole() {
        return role;
    }

    protected UserInfo(String phoneNumber, String email, String keyProviderUserName, Set<String> keyProviderRoles, UserRole role) {
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.keyProviderUserName = keyProviderUserName;
        this.keyProviderRoles = keyProviderRoles;
        this.role = role;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserInfo userInfo = (UserInfo) o;
        return Objects.equals(phoneNumber, userInfo.phoneNumber) ||
                Objects.equals(email, userInfo.email);
    }

    @Override
    public int hashCode() {

        return Objects.hash(phoneNumber, email);
    }

    @Override
    public String toString() {
        return "UserInfo{" +
                "phoneNumber='" + phoneNumber + '\'' +
                ", email='" + email + '\'' +
                ", keyProviderUserName='" + keyProviderUserName + '\'' +
                ", keyProviderRoles=" + keyProviderRoles +
                '}';
    }

    public static class Builder {
        private String phoneNumber;
        protected String email;

        private String keyProviderUserName;
        private Set<String> keyProviderRoles = new HashSet<>();
        protected UserRole role;

        public Builder setPhoneNumber(String phoneNumber) {
            this.phoneNumber = phoneNumber;
            return this;
        }

        public Builder setEmail(String email) {
            this.email = email;
            return this;
        }

        public Builder setKeyProviderUserName(String keyProviderUserName) {
            this.keyProviderUserName = keyProviderUserName;
            return this;
        }

        public Builder setKeyProviderRoles(Set<String> keyProviderRoles) {
            this.keyProviderRoles = keyProviderRoles;
            return this;
        }

        public Builder addKeyProviderRole(String role) {
            keyProviderRoles.add(role);
            return this;
        }

        public Builder setRole(UserRole role) {
            this.role = role;
            return this;
        }

        public UserInfo build() {
            return new UserInfo(phoneNumber, email, keyProviderUserName, keyProviderRoles, role);
        }
    }
}
