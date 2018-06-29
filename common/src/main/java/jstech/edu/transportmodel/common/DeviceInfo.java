package jstech.edu.transportmodel.common;

import java.util.Objects;

public class DeviceInfo {
    private int deviceInfoId;
    private int personId;
    private Platform platform;
    private String deviceId;
    private String appToken;
    private String endPointArn;



    public int getDeviceInfoId() {
        return deviceInfoId;
    }

    public int getPersonId() {
        return personId;
    }

    public Platform getPlatform() {
        return platform;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getAppToken() {
        return appToken;
    }

    public String getEndPointArn() { return endPointArn;  }


    private DeviceInfo(int deviceInfoId, int personId, Platform platform, String deviceId, String appToken, String endPointArn) {
        this.deviceInfoId = deviceInfoId;
        this.personId = personId;
        this.platform = platform;
        this.deviceId = deviceId;
        this.appToken = appToken;
        this.endPointArn = endPointArn;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DeviceInfo that = (DeviceInfo) o;
        return personId == that.personId &&
                Objects.equals(platform, that.platform) &&
                Objects.equals(deviceId, that.deviceId) &&
                Objects.equals(appToken, that.appToken) &&
                Objects.equals(endPointArn, that.endPointArn);
    }

    @Override
    public int hashCode() {

        return Objects.hash(personId, platform, deviceId, appToken,endPointArn);
    }

    @Override
    public String toString() {
        return "DeviceInfo{" +
                "deviceInfoId=" + deviceInfoId +
                ", personId=" + personId +
                ", platform='" + platform + '\'' +
                ", deviceId='" + deviceId + '\'' +
                ", appToken='" + appToken + '\'' +
                ", endPointArn='" + endPointArn + '\'' +
                '}';
    }

    public static class Builder {
        private int deviceInfoId;
        private int personId;
        private Platform platform;
        private String deviceId;
        private String appToken;
        private String endPointArn;

        public Builder setDeviceInfoId(int deviceInfoId) {
            this.deviceInfoId = deviceInfoId;
            return this;
        }

        public Builder setPersonId(int personId) {
            this.personId = personId;
            return this;
        }

        public Builder setPlatform(Platform platform) {
            this.platform = platform;
            return this;
        }

        public Builder setDeviceId(String deviceId) {
            this.deviceId = deviceId;
            return this;
        }

        public Builder setAppToken(String appToken) {
            this.appToken = appToken;
            return this;
        }

        public Builder setEndPointArn(String endPointArn) {
            this.endPointArn = endPointArn;
            return this;
        }


        public DeviceInfo build() {
            return new DeviceInfo(deviceInfoId, personId, platform, deviceId, appToken, endPointArn);
        }
    }
}
