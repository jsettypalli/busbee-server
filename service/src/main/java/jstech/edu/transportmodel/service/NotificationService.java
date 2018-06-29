package jstech.edu.transportmodel.service;

import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.model.*;
import jstech.edu.transportmodel.common.*;
import jstech.edu.transportmodel.dto.NotificationMessageDto;

import java.util.List;
import java.util.Map;

public interface NotificationService {

    //AmazonSNS snsClient();
    CreateTopicResult createTopic(String topicName);
    boolean exists(String topicName);
    /*PublishResult publish(String endpointArn, Platform platform,
                          Map<Platform, Map<String, MessageAttributeValue>> attributesMap);*/
    CreatePlatformApplicationResult createPlatformApplication(
            String applicationName, Platform platform, String principal,
            String credential);
    CreatePlatformEndpointResult createPlatformEndpoint(
            Platform platform, String customData, String platformToken,
            String applicationArn);
    //void sendAndroidAppNotification();
    String registerWithSNS(DeviceInfo devInfo);
    SubscribeResult singleSubscribe(String endpointArn, String topicName,String protocolToUse);
    boolean singleUnsubscribe(String endpointArn, String topicName);
    List<UnsubscribeResult> unsubscribeTopic(String endpointArn, String topicName);
    SubscribeResult subscribe(String topicArn, String protocolToUse, String endpointArn);

    // Main Functions
    void pushNotification(String topicName,String Message);
    void pushNotification(String topicName, Map<String, Object> dataMap);
    boolean subscribe(List<DeviceInfo> devices, List<String> topics);
    boolean unsubscribe(List<DeviceInfo> devices, List<String> topics);

    // wrapper methods around push notification
    boolean pushBusArrivalNotificationMessage(String topicName, NotificationMessageDto notificationMessage);
    boolean pushDriverReminderNotificationMessage(String topicName, NotificationMessageDto notificationMessage);

    // Web socket related notifications
    void publishBusPosition(BusPosition busPosition, BusStop nextBusStopInTheRoute);
    void publishStopSendingBusPositionMessage(int tripId, int busId);
    void publishNextBusStopLocationMessage(int tripId, int busId, BusStop nextBusStop);
}
