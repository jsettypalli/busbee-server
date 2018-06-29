package jstech.edu.transportmodel.common;

import java.util.Set;

public abstract class Person extends UserInfo {

    private int id;
    private String firstName;
    private String lastName;
    private String nickName;
    private String thumbnailUrl;
    private String regularImageUrl;

    public int getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getNickName() {
        return nickName;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public String getRegularImageUrl() {
        return regularImageUrl;
    }

    protected Person(int id, String firstName, String lastName, String nickName, String phoneNumber, String email, String keyProviderUserName,
                     Set<String> keyProviderRoles, UserRole role) {
        super(phoneNumber, email, keyProviderUserName, keyProviderRoles, role);
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.nickName = nickName;
        this.thumbnailUrl = "/app/users/"+keyProviderUserName+"/thumbnail";
        this.regularImageUrl = "/app/users/"+keyProviderUserName+"/regular";
    }

    public static class Builder extends UserInfo.Builder {

        protected  int id;
        private String firstName;
        private String lastName;
        private String nickName;
        private String thumbnailUrl;
        private String regularImageUrl;

        public Builder setId(int id) {
            this.id = id;
            return this;
        }

        public Builder setFirstName(String firstName) {
            this.firstName = firstName;
            return this;
        }

        public Builder setLastName(String lastName) {
            this.lastName = lastName;
            return this;
        }

        public Builder setNickName(String nickName) {
            this.nickName = nickName;
            return this;
        }

        public Builder setThumbnailUrl(String thumbnailUrl) {
            this.thumbnailUrl = thumbnailUrl;
            return this;
        }

        public Builder setRegularImageUrl(String regularImageUrl) {
            this.regularImageUrl = regularImageUrl;
            return this;
        }
    }
}
