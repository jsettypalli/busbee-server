package jstech.edu.transportmodel.common;

public enum Platform {
    // Apple Push Notification Service - BusBee Platform
    APNS,
    // Sandbox version of Apple Push Notification Service - BusBee Platform
    APNS_SANDBOX,
    // Amazon Device Messaging
    ADM,
    // Google Cloud Messaging - BusBee Platform
    GCM,
    // Baidu CloudMessaging Service
    BAIDU,
    // Windows Notification Service
    WNS,
    // Microsoft Push Notificaion Service
    MPNS;

    public static Platform getInstance(String devicePlatform) {
        if(devicePlatform.equalsIgnoreCase("android")) {
            return GCM;
        } else if(devicePlatform.equalsIgnoreCase("ios")) {
            return APNS;
        } else if(devicePlatform.equalsIgnoreCase("ios_dev")) {
            return APNS_SANDBOX;
        }

        return null;
    }
}