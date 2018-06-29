package jstech.edu.transportmodel.common;

import java.util.Objects;

public class ParentNotificationSetting {
    private UserInfo userInfo;
    private int notificationMinutes;
    private boolean enabled;

    public UserInfo getUserInfo() {
        return userInfo;
    }

    public int getNotificationMinutes() {
        return notificationMinutes;
    }

    public boolean isEnabled() {
        return enabled;
    }

    private ParentNotificationSetting(UserInfo userInfo, int notificationMinutes, boolean enabled) {
        this.userInfo = userInfo;
        this.notificationMinutes = notificationMinutes;
        this.enabled = enabled;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ParentNotificationSetting that = (ParentNotificationSetting) o;
        return notificationMinutes == that.notificationMinutes &&
                enabled == that.enabled &&
                Objects.equals(userInfo, that.userInfo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userInfo, notificationMinutes, enabled);
    }

    @Override
    public String toString() {
        return "ParentNotificationSetting{" +
                "userInfo=" + userInfo +
                ", notificationMinutes=" + notificationMinutes +
                ", enabled=" + enabled +
                '}';
    }

    public static class Builder {
        private UserInfo userInfo;
        private int notificationMinutes;
        private boolean enabled;

        public Builder setUserInfo(UserInfo userInfo) {
            this.userInfo = userInfo;
            return this;
        }

        public Builder setNotificationMinutes(int notificationMinutes) {
            this.notificationMinutes = notificationMinutes;
            return this;
        }

        public Builder setEnabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public ParentNotificationSetting build() {
            return new ParentNotificationSetting(userInfo, notificationMinutes, enabled);
        }
    }
}
