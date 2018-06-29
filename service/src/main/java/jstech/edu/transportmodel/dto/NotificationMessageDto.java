package jstech.edu.transportmodel.dto;

import jstech.edu.transportmodel.common.Driver;
import jstech.edu.transportmodel.common.SchoolBus;

public class NotificationMessageDto {
    private String driverName;
    private String driverThumbNailUrl;
    private String driverFullImageUrl;
    private String busNumber;
    private String busRegistrationNumber;
    private int expectedTimeInMins;
    private String message;
    private String shortMessage;
    private String event;

    private NotificationMessageDto(String driverName, String driverThumbNailUrl, String driverFullImageUrl,
                                   String busNumber, String busRegistrationNumber, int expectedTimeInMins,
                                   String message, String shortMessage, String event) {
        this.driverName = driverName;
        this.driverThumbNailUrl = driverThumbNailUrl;
        this.driverFullImageUrl = driverFullImageUrl;
        this.busNumber = busNumber;
        this.busRegistrationNumber = busRegistrationNumber;
        this.expectedTimeInMins = expectedTimeInMins;
        this.message = message;
        this.shortMessage = shortMessage;
        this.event = event;
    }

    public String getDriverName() {
        return driverName;
    }

    public String getDriverThumbNailUrl() {
        return driverThumbNailUrl;
    }

    public String getDriverFullImageUrl() {
        return driverFullImageUrl;
    }

    public String getBusNumber() {
        return busNumber;
    }

    public String getBusRegistrationNumber() {
        return busRegistrationNumber;
    }

    public int getExpectedTimeInMins() {
        return expectedTimeInMins;
    }

    public String getMessage() {
        return message;
    }

    public String getShortMessage() {
        return shortMessage;
    }

    public String getEvent() {
        return event;
    }

    @Override
    public String toString() {
        return "NotificationMessageDto{" +
                "driverName='" + driverName + '\'' +
                ", driverThumbNailUrl='" + driverThumbNailUrl + '\'' +
                ", driverFullImageUrl='" + driverFullImageUrl + '\'' +
                ", busNumber='" + busNumber + '\'' +
                ", busRegistrationNumber='" + busRegistrationNumber + '\'' +
                ", expectedTimeInMins=" + expectedTimeInMins +
                ", message='" + message + '\'' +
                ", shortMessage='" + shortMessage + '\'' +
                ", event='" + event + '\'' +
                '}';
    }

    public static class Builder {
        private String driverName;
        private String driverThumbNailUrl;
        private String driverFullImageUrl;
        private String busNumber;
        private String busRegistrationNumber;
        private int expectedTimeInMins;
        private String message;
        private String shortMessage;
        private String event;

        public Builder setDriverName(String driverName) {
            this.driverName = driverName;
            return this;
        }

        public Builder setDriverThumbNailUrl(String driverThumbNailUrl) {
            this.driverThumbNailUrl = driverThumbNailUrl;
            return this;
        }

        public Builder setDriverFullImageUrl(String driverFullImageUrl) {
            this.driverFullImageUrl = driverFullImageUrl;
            return this;
        }

        public Builder setBusNumber(String busNumber) {
            this.busNumber = busNumber;
            return this;
        }

        public Builder setBusRegistrationNumber(String busRegistrationNumber) {
            this.busRegistrationNumber = busRegistrationNumber;
            return this;
        }

        public Builder setExpectedTimeInMins(int expectedTimeInMins) {
            this.expectedTimeInMins = expectedTimeInMins;
            return this;
        }

        public Builder setShortMessage(String shortMessage) {
            this.shortMessage = shortMessage;
            return this;
        }

        public Builder setMessage(String message) {
            this.message = message;
            return this;
        }

        public Builder setEvent(String event) {
            this.event = event;
            return this;
        }

        public NotificationMessageDto build() {
            return new NotificationMessageDto(driverName, driverThumbNailUrl, driverFullImageUrl,
                                                busNumber, busRegistrationNumber, expectedTimeInMins, message, shortMessage, event);
        }

        public static NotificationMessageDto build(SchoolBus schoolBus, Driver driver, String message, String shortMessage) {
            return build(schoolBus, driver, message, shortMessage, null, -1);
        }

        public static NotificationMessageDto build(SchoolBus schoolBus, Driver driver, String message, String shortMessage, String event) {
            return build(schoolBus, driver, message, shortMessage, event, -1);
        }

        public static NotificationMessageDto build(SchoolBus schoolBus, Driver driver, String message, String shortMessage, String event, long expectedTimeInMins) {
            NotificationMessageDto.Builder builder = new NotificationMessageDto.Builder()
                                                        .setMessage(message)
                                                        .setShortMessage(shortMessage)
                                                        .setBusNumber(schoolBus.getBusNumber())
                                                        .setBusRegistrationNumber(schoolBus.getRegistrationNumber())
                                                        .setDriverName(driver == null ? null : driver.getNickName())
                                                        .setDriverThumbNailUrl(driver == null ? null : driver.getThumbnailUrl())
                                                        .setDriverFullImageUrl(driver == null ? null : driver.getRegularImageUrl());

            if(expectedTimeInMins >= 0) {
                builder.setExpectedTimeInMins(new Long(expectedTimeInMins).intValue());
            }

            if(event != null) {
                builder.setEvent(event);
            }

            return builder.build();
        }
    }
}
