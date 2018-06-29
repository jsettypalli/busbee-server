package jstech.edu.transportmodel.service;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.amazonaws.services.sns.model.*;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import jstech.edu.transportmodel.common.BusPosition;
import jstech.edu.transportmodel.common.BusStop;
import jstech.edu.transportmodel.common.DeviceInfo;
import jstech.edu.transportmodel.common.Platform;
import jstech.edu.transportmodel.dao.UserDao;
import jstech.edu.transportmodel.dto.NotificationMessageDto;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.springframework.util.StringUtils.isEmpty;


@Service
public class NotificationServiceImpl implements NotificationService  {

    private static final Logger logger = LoggerFactory.getLogger(NotificationServiceImpl.class);

    @Value("${aws.access_key:}")
    private String accessKey;

    @Value("${aws.secret_key:}")
    private String secretKey;

    @Value("${aws.region:}")
    private String region;

    @Value("${fcm.serverAPIKey:}") // Principal to send to createplatformApplication
    private String fcmServerAPIKey;

    @Value("${aws.platformapplicationname:}")
    private String platformapplicationname;

    private AmazonSNS snsClient;

    //String token = "eqoyJdehTR8:APA91bETSKRB-8XmXKxWVh3rE508jS7dWcVwKV4ptHL5VWzwzm2wr7KCbCaH3hsCCUqtKwVCERTt5sR_w2eLXttcSS4NCnXLvSVgRh2KFKqoyTLX5gK76p___xSqsoSwZh_l48mbxAbl";
    private String token;
    //String topicArn="";
    private String platformApplicationArn = "";
    private Map<String, String> topicToArnMap = new ConcurrentHashMap<>();
    //String platformEndPointArn ="";

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private SimpMessagingTemplate template;

    @Autowired
    private UserDao userDao;

    private static final Map<Platform, Map<String, MessageAttributeValue>> attributesMap = new HashMap<Platform, Map<String, MessageAttributeValue>>();
    static {
        attributesMap.put(Platform.GCM, null);
        attributesMap.put(Platform.APNS, null);
        attributesMap.put(Platform.APNS_SANDBOX, null);

    }

    @PostConstruct
    public void initialize() {
        snsClient();

        // BUSBEE_ANDROID_APP - Enum
        // BUSBEE_IOS_APP

        CreatePlatformApplicationResult platformApplicationResult =   createPlatformApplication(
                "BUSBEE_ANDROID_DEV_APP", Platform.GCM, "","AIzaSyC4YosbLYfgu8ALF1FCnFW3MgzOdov0Eus");

        //CreatePlatformApplicationResult platformApplicationResult =   createPlatformApplication(
        //        platformapplicationname, Platform.GCM, "",fcmServerAPIKey);
        this.platformApplicationArn = platformApplicationResult.getPlatformApplicationArn();
    }


    private AmazonSNS snsClient() {
        AWSCredentials awsCredentials = new BasicAWSCredentials(accessKey, secretKey);

        snsClient =  AmazonSNSClientBuilder.standard().withRegion(region).withCredentials(new AWSStaticCredentialsProvider(awsCredentials)).build();

        //this.snsClient = snsClient;

        return snsClient;
    }

    @Override
    public CreateTopicResult createTopic(String topicName) {

        CreateTopicResult result = null;

        // OPTIONAL: Check the topic already created or not
        // Create a new topic if it doesn't exist
        if(!exists(topicName)) {
            CreateTopicRequest createTopicReq = new CreateTopicRequest(topicName);
            result = snsClient.createTopic(createTopicReq);
            String topicArn = result.getTopicArn();
            topicToArnMap.put(topicName, topicArn);
        }
        return  result;
    }

    @Override
    public boolean exists(String topicName) {
        if(topicToArnMap.containsKey(topicName)) {
            return true;
        }

        ListTopicsResult result = snsClient.listTopics();
        List<Topic> topics = new ArrayList<>(result.getTopics());

        while (result.getNextToken() != null) {
            result = snsClient.listTopics(result.getNextToken());
            topics.addAll(result.getTopics());
        }

        for(Topic topic:topics){
            String locTopicName = getTopicNameFromArn(topic.getTopicArn());
            topicToArnMap.putIfAbsent(locTopicName, topic.getTopicArn());
        }

        return topicToArnMap.containsKey(topicName);
    }


    // Delete Topic
    //delete an SNS topic - Deletes all the subscribed endpoints too.
    public DeleteTopicResult deleteTopic(String topicArn) {
        DeleteTopicRequest deleteTopicRequest = new DeleteTopicRequest(topicArn);
        DeleteTopicResult result = snsClient.deleteTopic(deleteTopicRequest);
        topicToArnMap.remove(getTopicNameFromArn(topicArn));
        //get request id for DeleteTopicRequest from SNS metadata
        logger.debug("DeleteTopicRequest - " + snsClient.getCachedResponseMetadata(deleteTopicRequest));
        return result;
    }


    public SubscribeResult singleSubscribe(String endpointArn, String topicName, String protocolToUse) {

        String topicArn = topicToArnMap.get(topicName);
        if(!StringUtils.hasText(topicArn)) {
            createTopic(topicName);
            topicArn = topicToArnMap.get(topicName);
        }

        if(topicArn == null) {
            logger.error("topicArn couldn't be created for topic name:{}.", topicName);
            return null;
        }

        /*List<Topic> topicArns = new ArrayList<>();
        ListTopicsResult result = snsClient.listTopics();
        topicArns.addAll(result.getTopics());

        while (result.getNextToken() != null) {
            result = snsClient.listTopics(result.getNextToken());
            topicArns.addAll(result.getTopics());
        }

        for (Topic resulttopicArn : topicArns) {
            String existTopicName = getTopicNameFromArn(resulttopicArn.getTopicArn());
            if (existTopicName.equals(topicName)) {
                topicArn = resulttopicArn.getTopicArn();
            }
        }

        //if the topic does not exist, create it.
        if(topicArn == null){
            createTopic(topicName); // this.topicArn gets initialized in createTopic
        }*/

        SubscribeResult subscribeResult = subscribe(topicArn, protocolToUse, endpointArn);
        return subscribeResult;
    }

    public SubscribeResult subscribe(String topicArn, String endpointArn, String protocolToUse, Platform platform) {

        //subscribe to an SNS topic
        SubscribeRequest subRequest = new SubscribeRequest(topicArn, protocolToUse, endpointArn);
        SubscribeResult subResult;
        subResult = snsClient.subscribe(subRequest);

        //get request id for SubscribeRequest from SNS metadata
        logger.debug("SubscribeRequest - " + snsClient.getCachedResponseMetadata(subRequest));
        return subResult;
    }

    public SubscribeResult subscribe(String topicArn, String protocolToUse, String endpointArn) {

        //subscribe to an SNS topic
        SubscribeRequest subRequest = new SubscribeRequest(topicArn, protocolToUse,endpointArn );
        SubscribeResult subResult;
        subResult = snsClient.subscribe(subRequest);

        //get request id for SubscribeRequest from SNS metadata
        logger.debug("SubscribeRequest - " + snsClient.getCachedResponseMetadata(subRequest));
        return subResult;

    }

    public boolean singleUnsubscribe(String endpointArn, String topicName) {

        String topicArn = topicToArnMap.get(topicName);
        if(!StringUtils.hasText(topicArn)) {
            createTopic(topicName);
            topicArn = topicToArnMap.get(topicName);
        }

        if(topicArn == null) {
            logger.error("topicArn couldn't be created for topic name:{}.", topicName);
            return false;
        }

        /*List<Topic> topicArns = new ArrayList<>();
        ListTopicsResult result = snsClient.listTopics();
        topicArns.addAll(result.getTopics());

        while (result.getNextToken() != null) {
            result = snsClient.listTopics(result.getNextToken());
            topicArns.addAll(result.getTopics());
        }

        for (Topic resulttopicArn : topicArns) {
            String existTopicName = getTopicNameFromArn(resulttopicArn.getTopicArn());
            if (existTopicName.equals(topicName)) {
                this.topicArn = resulttopicArn.getTopicArn();
            }
        }

        if(this.topicArn == null) {
            return false;
        }*/

        //List<Subscription> subscriptions = new ArrayList<>();
        ListSubscriptionsByTopicResult subscriptionsByTopicResultresult = snsClient.listSubscriptionsByTopic(topicArn);
        List<Subscription> subscriptions = new ArrayList<>(subscriptionsByTopicResultresult.getSubscriptions());
        //subscriptions.addAll(subscriptionsByTopicResultresult.getSubscriptions());

        while (subscriptionsByTopicResultresult.getNextToken() != null) {
            subscriptionsByTopicResultresult = snsClient.listSubscriptionsByTopic(subscriptionsByTopicResultresult.getNextToken());
            subscriptions.addAll(subscriptionsByTopicResultresult.getSubscriptions());
        }

        for (Subscription subscribtion : subscriptions) {
            String subcriptionEndPoint = subscribtion.getEndpoint();
            if(endpointArn.equals(subcriptionEndPoint)) {
                //unsubscribe an endpoint to an SNS topic
                UnsubscribeRequest unsubRequest;
                unsubRequest = new UnsubscribeRequest(subscribtion.getSubscriptionArn());
                UnsubscribeResult unsubResult;
                unsubResult = snsClient.unsubscribe(unsubRequest);
                //get request id for UnSubscribeRequest from SNS metadata
                logger.debug("UnSubscribeRequest - " + snsClient.getCachedResponseMetadata(unsubRequest));
            }
        }
        return true;

    }

    public List<UnsubscribeResult> unsubscribeTopic(String endpointArn, String topicName) {

        String topicArn = topicToArnMap.get(topicName);
        if(!StringUtils.hasText(topicArn)) {
            createTopic(topicName);
            topicArn = topicToArnMap.get(topicName);
        }

        if(topicArn == null) {
            logger.error("topicArn couldn't be created for topic name:{}.", topicName);
            return null;
        }

        /*List<Topic> topicArns = new ArrayList<>();
        ListTopicsResult result = snsClient.listTopics();
        topicArns.addAll(result.getTopics());

        while (result.getNextToken() != null) {
            result = snsClient.listTopics(result.getNextToken());
            topicArns.addAll(result.getTopics());
        }

        for (Topic resulttopicArn : topicArns) {
            String existTopicName = getTopicNameFromArn(resulttopicArn.getTopicArn());
            if (existTopicName.equals(topicName)) {
                this.topicArn = resulttopicArn.getTopicArn();
            }
        }

        if(this.topicArn == null) {
            return null;
        }*/

        List<Subscription> subscriptionArns = new ArrayList<>();
        ListSubscriptionsByTopicResult subscriptionsByTopicResultresult = snsClient.listSubscriptionsByTopic(topicArn);

        subscriptionArns.addAll(subscriptionsByTopicResultresult.getSubscriptions());

        while (subscriptionsByTopicResultresult.getNextToken() != null) {
            subscriptionsByTopicResultresult = snsClient.listSubscriptionsByTopic(subscriptionsByTopicResultresult.getNextToken());
            subscriptionArns.addAll(subscriptionsByTopicResultresult.getSubscriptions());
        }

        List<UnsubscribeResult> unsubResultList = new ArrayList();

        for (Subscription resultSubscribtionArn : subscriptionArns) {

            //unsubscribe an endpoint to an SNS topic
            UnsubscribeRequest unsubRequest;
            unsubRequest = new UnsubscribeRequest(resultSubscribtionArn.getSubscriptionArn());
            UnsubscribeResult unsubResult;
            unsubResult = snsClient.unsubscribe(unsubRequest);
            //get request id for UnSubscribeRequest from SNS metadata
            logger.debug("UnSubscribeRequest - " + snsClient.getCachedResponseMetadata(unsubRequest));
            unsubResultList.add(unsubResult);
        }

        return unsubResultList;

    }


/*
    public SubscribeResult confirmSubcribtion(String topicArn, String endpointArn, String protocolToUse, Platform platform) {


        //subscribe to an SNS topic
        SubscribeRequest subRequest = new SubscribeRequest(topicArn, protocolToUse, endpointArn);
        SubscribeResult subResult;
        subResult = snsClient.subscribe(subRequest);

        //get request id for SubscribeRequest from SNS metadata
        logger.debug("SubscribeRequest - " + snsClient.getCachedResponseMetadata(subRequest));
        return subResult;

    }
*/

/*
    public PublishResult publish(String endpointArn, Platform platform,
                                 Map<Platform, Map<String, MessageAttributeValue>> attributesMap) {
        PublishRequest publishRequest = new PublishRequest();
        Map<String, MessageAttributeValue> notificationAttributes = getValidNotificationAttributes(attributesMap
                .get(platform));
        if (notificationAttributes != null && !notificationAttributes.isEmpty()) {
            publishRequest.setMessageAttributes(notificationAttributes);
        }
        publishRequest.setMessageStructure("json");
        // If the message attributes are not set in the requisite method,
        // notification is sent with default attributes
        String message = getPlatformMessage(platform);
        Map<String, String> messageMap = new HashMap<String, String>();
        if(endpointArn == this.topicArn) {
            messageMap.put("default", message);
        }
        messageMap.put(platform.name(), message);
        message = jsonify(messageMap);
        // For direct publish to mobile end points, topicArn is not relevant.
        publishRequest.setTargetArn(endpointArn);

        // Display the message that will be sent to the endpoint/
        logger.debug("{Message Body: " + message + "}");
        StringBuilder builder = new StringBuilder();
        builder.append("{Message Attributes: ");
        for (Map.Entry<String, MessageAttributeValue> entry : notificationAttributes
                .entrySet()) {
            builder.append("(\"" + entry.getKey() + "\": \""
                    + entry.getValue().getStringValue() + "\"),");
        }
        builder.deleteCharAt(builder.length() - 1);
        builder.append("}");
        logger.debug(builder.toString());

        publishRequest.setMessage(message);
        return snsClient.publish(publishRequest);
    }
*/

    public PublishResult publish(String endpointArn, Platform platform, String message) {
        PublishRequest publishRequest = new PublishRequest();
        publishRequest.setMessageStructure("json");
        // If the message attributes are not set in the requisite method,
        // notification is sent with default attributes
        String finalMessage = getPlatformMessage(platform, message);
        Map<String, String> messageMap = new HashMap<>();

        for(String topicArn: topicToArnMap.values()) {
            if (endpointArn.equals(topicArn)) {
                messageMap.put("default", finalMessage);
                break;
            }
        }
        messageMap.put(platform.name(), finalMessage);
        message = jsonify(messageMap);
        // For direct publish to mobile end points, topicArn is not relevant.
        publishRequest.setTargetArn(endpointArn);

        // Display the message that will be sent to the endpoint/
        logger.debug("{Message Body: " + message + "}");
        publishRequest.setMessage(message);
        return snsClient.publish(publishRequest);
    }

    private PublishResult publish(String endpointArn, String message) {
        PublishRequest publishRequest = new PublishRequest();
        publishRequest.setMessageStructure("json");

        // For direct publish to mobile end points, topicArn is not relevant.
        publishRequest.setTargetArn(endpointArn);

        // Display the message that will be sent to the endpoint/
        logger.debug("{Message Body: " + message + "}");
        publishRequest.setMessage(message);
        return snsClient.publish(publishRequest);
    }

    public CreatePlatformApplicationResult createPlatformApplication(
            String applicationName, Platform platform, String principal,
            String credential) {
        CreatePlatformApplicationRequest platformApplicationRequest = new CreatePlatformApplicationRequest();
        Map<String, String> attributes = new HashMap<>();
        attributes.put("PlatformPrincipal", principal);
        attributes.put("PlatformCredential", credential);
        platformApplicationRequest.setAttributes(attributes);
        platformApplicationRequest.setName(applicationName);
        platformApplicationRequest.setPlatform(platform.name());
        return snsClient.createPlatformApplication(platformApplicationRequest);
    }


    private void deletePlatformApplication(String applicationArn) {
        DeletePlatformApplicationRequest request = new DeletePlatformApplicationRequest();
        request.setPlatformApplicationArn(applicationArn);
        snsClient.deletePlatformApplication(request);
    }

    public CreatePlatformEndpointResult createPlatformEndpoint(
            Platform platform, String customData, String platformToken,
            String applicationArn) {
        CreatePlatformEndpointRequest platformEndpointRequest = new CreatePlatformEndpointRequest();
        platformEndpointRequest.setCustomUserData(customData);
        String token = platformToken;
        String userId = null;
        if (platform == Platform.BAIDU) {
            String[] tokenBits = platformToken.split("\\|");
            token = tokenBits[0];
            userId = tokenBits[1];
            Map<String, String> endpointAttributes = new HashMap<>();
            endpointAttributes.put("UserId", userId);
            endpointAttributes.put("ChannelId", token);
            platformEndpointRequest.setAttributes(endpointAttributes);
        }
        platformEndpointRequest.setToken(token);
        platformEndpointRequest.setPlatformApplicationArn(applicationArn);
        return snsClient.createPlatformEndpoint(platformEndpointRequest);
    }

    public List<Endpoint> getEndPointsList(String applicationArn) {
        ListEndpointsByPlatformApplicationRequest listEndpointsByPlatformApplicationReq = new ListEndpointsByPlatformApplicationRequest();
        listEndpointsByPlatformApplicationReq.setPlatformApplicationArn(applicationArn);
        ListEndpointsByPlatformApplicationResult listResult = snsClient.listEndpointsByPlatformApplication(listEndpointsByPlatformApplicationReq);
        List<Endpoint> endpointsList = listResult.getEndpoints();
        return endpointsList;

    }

    public String  registerWithSNS(DeviceInfo deviceInfo) {

        String endpointArn = deviceInfo.getEndPointArn();

        /* "Retrieved from the mobile operating system"; */
        String token = deviceInfo.getAppToken();
        this.token = token;

        boolean updateNeeded = false;
        boolean createNeeded = (null == endpointArn);

        if (createNeeded) {
            // No platform endpoint ARN is stored; need to call createEndpoint.
            endpointArn = createEndpoint();
            DeviceInfo deviceInfo1 = new DeviceInfo.Builder()
                    .setAppToken(deviceInfo.getAppToken())
                    .setDeviceId(deviceInfo.getDeviceId())
                    .setPlatform(deviceInfo.getPlatform())
                    .setEndPointArn(endpointArn)
                    .setDeviceInfoId(deviceInfo.getDeviceInfoId())
                    .setPersonId(deviceInfo.getPersonId())
                    .build();


            DeviceInfo out = userDao.updateDevice(null,deviceInfo1);

            logger.debug("After updating device info - AppToken is  :" + out.getAppToken());
            // Updatedevice
            createNeeded = false;
        }

        logger.debug("Retrieving platform endpoint data...");
        // Look up the platform endpoint and make sure the data in it is current, even if
        // it was just created.
        try {
            GetEndpointAttributesRequest geaReq =
                    new GetEndpointAttributesRequest()
                            .withEndpointArn(endpointArn);
            GetEndpointAttributesResult geaRes =
                    snsClient.getEndpointAttributes(geaReq);

            updateNeeded = !geaRes.getAttributes().get("Token").equals(token)
                    || !geaRes.getAttributes().get("Enabled").equalsIgnoreCase("true");

        } catch (NotFoundException nfe) {
            // We had a stored ARN, but the platform endpoint associated with it
            // disappeared. Recreate it.
            createNeeded = true;
        }

        if (createNeeded) {
            // No platform endpoint ARN is stored; need to call createEndpoint.
            endpointArn = createEndpoint();
            DeviceInfo deviceInfo1 = new DeviceInfo.Builder()
                    .setAppToken(deviceInfo.getAppToken())
                    .setDeviceId(deviceInfo.getDeviceId())
                    .setPlatform(deviceInfo.getPlatform())
                    .setEndPointArn(endpointArn)
                    .setDeviceInfoId(deviceInfo.getDeviceInfoId())
                    .setPersonId(deviceInfo.getPersonId())
                    .build();


            userDao.updateDevice(null,deviceInfo1);
            // Updatedevice
            createNeeded = false;
        }

        logger.debug("updateNeeded = " + updateNeeded);

        if (updateNeeded) {
            // The platform endpoint is out of sync with the current data;
            // update the token and enable it.
            logger.debug("Updating platform endpoint " + endpointArn);
            Map attribs = new HashMap();
            attribs.put("Token", token);
            attribs.put("Enabled", "true");
            SetEndpointAttributesRequest saeReq =
                    new SetEndpointAttributesRequest()
                            .withEndpointArn(endpointArn)
                            .withAttributes(attribs);
            snsClient.setEndpointAttributes(saeReq);
        }

        return endpointArn;

    }


    /**
     * @return never null
     * */
    private String createEndpoint() {

        String endpointArn = null;
        try {
            logger.debug("Creating platform endpoint with token " + token);
            CreatePlatformEndpointRequest cpeReq =
                    new CreatePlatformEndpointRequest()
                            .withPlatformApplicationArn(platformApplicationArn)
                            .withToken(token);
            CreatePlatformEndpointResult cpeRes = snsClient.createPlatformEndpoint(cpeReq);
            endpointArn = cpeRes.getEndpointArn();
        } catch (InvalidParameterException ipe) {
            String message = ipe.getErrorMessage();
            logger.debug("Exception message: " + message);
            Pattern p = Pattern
                    .compile(".*Endpoint (arn:aws:sns[^ ]+) already exists " +
                            "with the same token.*");
            Matcher m = p.matcher(message);
            if (m.matches()) {
                // The platform endpoint already exists for this token, but with
                // additional custom data that
                // createEndpoint doesn't want to overwrite. Just use the
                // existing platform endpoint.
                endpointArn = m.group(1);
            } else {
                // Rethrow the exception, the input is actually bad.
                throw ipe;
            }
        }
        //storeEndpointArn(endpointArn);
        return endpointArn;
    }


    /**
     * @return the ARN the app was registered under previously, or null if no
     *         platform endpoint ARN is stored.
     */
    /*private String retrieveEndpointArn() {
        // Retrieve the platform endpoint ARN from permanent storage,
        // or return null if null is stored.
        //return arnStorage;
        return platformEndPointArn;
    }*/


    /**
     * Stores the platform endpoint ARN in permanent storage for lookup next time.
     * */
    /*private void storeEndpointArn(String endpointArn) {
        // Write the platform endpoint ARN to permanent storage.
        //arnStorage = endpointArn;
        platformEndPointArn = endpointArn;
    }*/




    public static Map<String, MessageAttributeValue> getValidNotificationAttributes(
            Map<String, MessageAttributeValue> notificationAttributes) {
        Map<String, MessageAttributeValue> validAttributes = new HashMap<String, MessageAttributeValue>();

        if (notificationAttributes == null) return validAttributes;

        for (Map.Entry<String, MessageAttributeValue> entry : notificationAttributes
                .entrySet()) {
            if (!isEmpty(entry.getValue().getStringValue().trim())) {
                validAttributes.put(entry.getKey(), entry.getValue());
            }
        }
        return validAttributes;
    }


    private static String jsonify(Object message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (Exception e) {
            throw (RuntimeException) e;
        }
    }


    private String getPlatformMessage(Platform platform, String message) {
        switch (platform) {
            case APNS:
                return getAppleMessage(message);
            case APNS_SANDBOX:
                return getAppleMessage(message);
            case GCM:
                return getAndroidMessage(message);

            default:
                throw new IllegalArgumentException("Platform not supported : "
                        + platform.name());
        }
    }

    /*private String getPlatformMessage(Platform platform) {
        switch (platform) {
            case APNS:
                return getAppleMessage();
            case APNS_SANDBOX:
                return getAppleMessage();
            case GCM:
                return getAndroidMessage();

            default:
                throw new IllegalArgumentException("Platform not supported : "
                        + platform.name());
        }
    }*/
   /* private static Map<String, String> getData() {
        Map<String, String> payload = new HashMap<String, String>();
        payload.put("message", "Today: Topic tooLNew Message:Sent from SNS");
        return payload;
    }*/

    private static Map<String, String> getData(String message) {
        Map<String, String> payload = new HashMap<String, String>();
        payload.put("message", message);
        return payload;
    }

    /*public static String getAppleMessage() {
        Map<String, Object> appleMessageMap = new HashMap<String, Object>();
        Map<String, Object> appMessageMap = new HashMap<String, Object>();
        appMessageMap.put("alert", "You have got email.");
        appMessageMap.put("badge", 9);
        appMessageMap.put("sound", "default");
        appleMessageMap.put("aps", appMessageMap);
        return jsonify(appleMessageMap);
    }*/

    public static String getAppleMessage(String message) {
        Map<String, Object> appleMessageMap = new HashMap<String, Object>();
        Map<String, Object> appMessageMap = new HashMap<String, Object>();
        appMessageMap.put("alert", "You have got email.");
        appMessageMap.put("badge", 9);
        appMessageMap.put("sound", "default");
        appleMessageMap.put("aps", appMessageMap);
        return jsonify(appleMessageMap);
    }

    private String getAppleMessage(Map<String, Object> appMessageMap) {
        Map<String, Object> appleMessageMap = new HashMap<String, Object>();
        appMessageMap.put("alert", "You have got message.");
        appMessageMap.put("badge", 9);
        appMessageMap.put("sound", "default");
        appleMessageMap.put("aps", appMessageMap);
        return jsonify(appleMessageMap);
    }

    /*public static String getAndroidMessage() {
        Map<String, Object> androidMessageMap = new HashMap<String, Object>();
        androidMessageMap.put("collapse_key", "Welcome");
        androidMessageMap.put("data", getData());
        androidMessageMap.put("delay_while_idle", true);
        androidMessageMap.put("time_to_live", 125);
        androidMessageMap.put("dry_run", false);
        return jsonify(androidMessageMap);
    }*/

    public static String getAndroidMessage(String message) {
        Map<String, Object> androidMessageMap = new HashMap<String, Object>();

        JsonParser parser =  new JsonParser();
        JsonElement jsonElement = parser.parse(message);
        JsonObject jsonObj = jsonElement.getAsJsonObject();
        JsonObject json_array = jsonObj;
        Set keys = json_array.keySet();
        Iterator iterator = keys.iterator();
        while( iterator.hasNext() ) {
            String key = (String) iterator.next();
            logger.debug("Key: " + key);
            logger.debug("Value: " + json_array.get(key));
            if(key.equals("message")) {
                androidMessageMap.put("data", getData(json_array.get(key).getAsString()));
            }else {
                androidMessageMap.put(key, json_array.get(key).getAsString());
            }

        }

        androidMessageMap.put("collapse_key", "");
        //androidMessageMap.put("data", getData(message));
        androidMessageMap.put("delay_while_idle", true);
        androidMessageMap.put("time_to_live", 125);
        androidMessageMap.put("dry_run", false);
        return jsonify(androidMessageMap);
    }

    private String getAndroidMessage(Map<String, Object> androidMessageMap) {
        androidMessageMap.put("collapse_key", "");
        //androidMessageMap.put("data", getData(message));
        androidMessageMap.put("delay_while_idle", true);
        androidMessageMap.put("time_to_live", 125);
        androidMessageMap.put("dry_run", false);
        return jsonify(androidMessageMap);
    }

/*
    public void sendAndroidAppNotification() {
        // TODO: Please fill in following values for your application. You can
        // also change the notification payload as per your preferences using
        // the method

        //String serverAPIKey = "AIzaSyACPpC81veOSWYqHxs8cMyOVaJvWQ4sm1E";
        //String applicationName = "BusBeeApp";
        String serverAPIKey = "AIzaSyACPpC81veOSWYqHxs8cMyOVaJvWQ4sm1E"; // Principal
        String applicationName = "FCMANDSNS";
        String registrationId = this.token;
        sendNotification(Platform.GCM, "", serverAPIKey,
                registrationId, applicationName, attributesMap);
    }


    public void sendAppleAppNotification() {
        // TODO: Please fill in following values for your application. You can
        // also change the notification payload as per your preferences using
        // the method
        // com.amazonaws.sns.samples.tools.SampleMessageGenerator.getSampleAppleMessage()
        String certificate = ""; // This should be in pem format with \n at the
        // end of each line.
        String privateKey = ""; // This should be in pem format with \n at the
        // end of each line.
        String applicationName = "";
        String deviceToken = ""; // This is 64 hex characters.
        sendNotification(Platform.APNS, certificate,
                privateKey, deviceToken, applicationName, attributesMap);
    }

    public void sendNotification(Platform platform, String principal,
                                 String credential, String platformToken, String applicationName,
                                 Map<Platform, Map<String, MessageAttributeValue>> attrsMap) {
        // Create Platform Application. This corresponds to an app on a
        // platform.
        CreatePlatformApplicationResult platformApplicationResult = createPlatformApplication(
                applicationName, platform, principal, credential);
        logger.debug(platformApplicationResult);

        // The Platform Application Arn can be used to uniquely identify the
        // Platform Application.
        String platformApplicationArn = platformApplicationResult
                .getPlatformApplicationArn();

        // Create an Endpoint. This corresponds to an app on a device.
        CreatePlatformEndpointResult platformEndpointResult = createPlatformEndpoint(
                platform,
                "CustomData - Useful to store endpoint specific data",
                platformToken, platformApplicationArn);
        logger.debug(platformEndpointResult);

        // Publish a push notification to an Endpoint.
        PublishResult publishResult = publish(
                platformEndpointResult.getEndpointArn(), platform, attrsMap);
        logger.debug("Published! \n{MessageId="
                + publishResult.getMessageId() + "}");
        // Delete the Platform Application since we will no longer be using it.
        //deletePlatformApplication(platformApplicationArn); // Mathu if needed later uncomment this line.

        // Subscribe Endpoint to Topic also
        SubscribeResult subResult = subscribe(this.topicArn, platformEndpointResult.getEndpointArn(), "Application", platform);
        //SubscribeResult subResult = subscribe(this.topicArn, "mathumathi@hotmail.com", "Email", platform);
        //SubscribeResult subResult = subscribe(this.topicArn, "drmathumathi@gmail.com", "Email", platform);
        logger.debug("Subscribed!");
        // Publish to topic
        PublishResult publishTopicResult = publish(this.topicArn, platform, attrsMap);
        logger.debug("Published to Topic!");


    }
*/



    //  boolean subscribe(List<DeviceInfo> devices, List<String> topics);
    //  boolean unsubscribe(List<DeviceInfo> devices, List<String> topics);

    public void pushNotification(String topicName,String message){

        String topicArn = topicToArnMap.get(topicName);
        if(!StringUtils.hasText(topicArn)) {
            createTopic(topicName);
            topicArn = topicToArnMap.get(topicName);
        }

        if(topicArn == null) {
            logger.error("topicArn couldn't be created for topic name:{}.", topicName);
            return;
        }

        /*this.topicArn=null;
        List<Topic> topicArns = new ArrayList<>();
        ListTopicsResult result = snsClient.listTopics();
        topicArns.addAll(result.getTopics());

        while (result.getNextToken() != null) {
            result = snsClient.listTopics(result.getNextToken());
            topicArns.addAll(result.getTopics());
        }

        for (Topic resulttopicArn : topicArns) {
            String existTopicName = getTopicNameFromArn(resulttopicArn.getTopicArn());
            if (existTopicName.equals(topicName)) {
                this.topicArn = resulttopicArn.getTopicArn();
            }
        }

        //if the topic does not exist, create it.
        if(this.topicArn == null){
            createTopic(topicName); // this.topicArn gets initialized in createTopic
        }*/

        // Publish to topic
        PublishResult publishTopicResult = publish(topicArn, Platform.GCM, message);
        logger.debug("Published to Topic!");
    }

    @Override
    public void pushNotification(String topicName, Map<String, Object> dataMap) {
        String topicArn = topicToArnMap.get(topicName);
        if(!StringUtils.hasText(topicArn)) {
            createTopic(topicName);
            topicArn = topicToArnMap.get(topicName);
        }

        if(topicArn == null) {
            logger.error("topicArn couldn't be created for topic name:{}.", topicName);
            return;
        }

        if(logger.isDebugEnabled()) {
            logger.debug("Found TopicArn. Topic:{}, TopicArn:{}", topicName, topicArn);
        }

        Map<String, String> pushMessageMap = new HashMap<>();

        Map<String, Object> androidDataMap = new HashMap<>(dataMap);
        String andriodMessage = getAndroidMessage(androidDataMap);
        pushMessageMap.put(Platform.GCM.toString(), andriodMessage);
        pushMessageMap.put("default", andriodMessage);

        Map<String, Object> appleDataMap = new HashMap<>(dataMap);
        String appleMessage = getAppleMessage(appleDataMap);
        pushMessageMap.put(Platform.APNS.toString(), appleMessage);
        String message = jsonify(pushMessageMap);

        // Publish to topic
        PublishResult publishTopicResult = publish(topicArn, message);
        if(logger.isDebugEnabled()) {
            logger.debug("Published to Topic:{}, TopicArn:{}, Message:{}, publish-result:{}", topicName, topicArn, message, publishTopicResult);
        }
    }

    // GET the topic name from TopicARN
    private static String getTopicNameFromArn(String topicARN) {
        int index = topicARN.lastIndexOf(':');
        if (index > 0) {
            return topicARN.substring(index + 1);
        }
        return topicARN;
    }



    // Same user will have multiple devices. So Subscribe each device to one or multiple topics
    public boolean subscribe(List<DeviceInfo> devices, List<String> topics){

        for(DeviceInfo deviceInfo:devices){
            // Before subscribing this device, register with SNS by creating proper endpoint or updating existing endpoint's token/Enabled attributes for this device.
            String platformEndPointArn = registerWithSNS(deviceInfo);
            for(String topicName:topics){
                singleSubscribe(platformEndPointArn,topicName,"Application");
            }
        }

        return true;
    }

    // Same user will have multiple devices. So UnSubscribe each device to one or multiple topics
    public boolean unsubscribe(List<DeviceInfo> devices, List<String> topics) {
        for(DeviceInfo deviceInfo:devices){

            for(String topicName:topics){
                singleUnsubscribe(deviceInfo.getEndPointArn(), topicName);
            }
        }

        return true;

    }

    @Override
    public boolean pushBusArrivalNotificationMessage(String topicName, NotificationMessageDto notificationMessage) {
        Map<String, Object> data = new HashMap<>();
        data.put("bus_number", notificationMessage.getBusNumber());
        data.put("registration_number", notificationMessage.getBusRegistrationNumber());
        data.put("driver_name", notificationMessage.getDriverName());
        data.put("driver_thumbnail_url", notificationMessage.getDriverThumbNailUrl());
        data.put("driver_image_url", notificationMessage.getDriverFullImageUrl());
        data.put("short_message", notificationMessage.getShortMessage());
        data.put("expected_time", notificationMessage.getExpectedTimeInMins());
        data.put("message", notificationMessage.getMessage());
        if(notificationMessage.getEvent() != null) {
            data.put("event", notificationMessage.getEvent());
        }

        Map<String, Object> pushNotificationMap = new HashMap<>();
        pushNotificationMap.put("data", data);

        pushNotification(topicName, pushNotificationMap);
        return true;
    }

    /*
        Expected time is not sent with this message.
        Mobile app is coded to hide expected_time section of the UI when expected_time field is not present.
     */
    @Override
    public boolean pushDriverReminderNotificationMessage(String topicName, NotificationMessageDto notificationMessage) {
        Map<String, Object> data = new HashMap<>();
        data.put("bus_number", notificationMessage.getBusNumber());
        data.put("registration_number", notificationMessage.getBusRegistrationNumber());
        data.put("driver_name", notificationMessage.getDriverName());
        data.put("driver_thumbnail_url", notificationMessage.getDriverThumbNailUrl());
        data.put("driver_image_url", notificationMessage.getDriverFullImageUrl());
        data.put("short_message", notificationMessage.getShortMessage());
        data.put("message", notificationMessage.getMessage());
        if(notificationMessage.getEvent() != null) {
            data.put("event", notificationMessage.getEvent());
        }

        Map<String, Object> pushNotificationMap = new HashMap<>();
        pushNotificationMap.put("data", data);

        pushNotification(topicName, pushNotificationMap);
        return true;
    }

    @Override
    public void publishBusPosition(BusPosition busPosition, BusStop nextBusStopInTheRoute) {
        Map<String, String> tmpMap = new HashMap<>();
        tmpMap.put("trip_id", String.valueOf(busPosition.getTripId()));
        tmpMap.put("bus_id", String.valueOf(busPosition.getBusId()));
        tmpMap.put("latitude", String.valueOf(busPosition.getLocation().getLatitude()));
        tmpMap.put("longitude", String.valueOf(busPosition.getLocation().getLongitude()));

        tmpMap.put("next_bus_stop_id", nextBusStopInTheRoute == null ? "" : String.valueOf(nextBusStopInTheRoute.getBusStopDetailId()));
        tmpMap.put("next_bus_stop_latitude",
                (nextBusStopInTheRoute == null || nextBusStopInTheRoute.getLocation() == null)
                        ? ""
                        : String.valueOf(nextBusStopInTheRoute.getLocation().getLatitude()));
        tmpMap.put("next_bus_stop_longitude",
                (nextBusStopInTheRoute == null || nextBusStopInTheRoute.getLocation() == null)
                        ? ""
                        : String.valueOf(nextBusStopInTheRoute.getLocation().getLongitude()));

        /*tmpMap.put("message",
            String.format("Received bus position: lat-%f, long-%f; next bus stop: lat-%f, long-%f",
                busPosition.getLocation().getLatitude(), busPosition.getLocation().getLongitude(),
                nextBusStopInTheRoute.getLocation().getLatitude(), nextBusStopInTheRoute.getLocation().getLongitude()));*/

        String channel = "/subscribe/busposition/"+busPosition.getTripId()+"/"+busPosition.getBusId();
        if(logger.isDebugEnabled()) {
            logger.debug("sending busposition message to all parents for trip-id:{}, bus-id:{}, channel:{}",
                    busPosition.getTripId(), busPosition.getBusId(), channel);
        }

        sendWebSocketMessage(channel, tmpMap);
    }

    @Override
    public void publishStopSendingBusPositionMessage(int tripId, int busId) {
        Map<String, String> tmpMap = new HashMap<>();
        tmpMap.put("trip_id", String.valueOf(tripId));
        tmpMap.put("bus_id", String.valueOf(busId));

        String channel = "/subscribe/stop_busposition/"+tripId+"/"+busId;

        logger.info("sending message to driver app to stop sending bus positions. trip-id:{}, bus-id:{}, channel:{}", tripId, busId, channel);

        sendWebSocketMessage(channel, tmpMap);
    }

    @Override
    public void publishNextBusStopLocationMessage(int tripId, int busId, BusStop nextBusStop) {
        Map<String, String> tmpMap = new HashMap<>();
        tmpMap.put("trip_id", String.valueOf(tripId));
        tmpMap.put("bus_id", String.valueOf(busId));

        tmpMap.put("next_bus_stop_id", nextBusStop == null ? "" : String.valueOf(nextBusStop.getBusStopDetailId()));
        tmpMap.put("next_bus_stop_latitude",
                (nextBusStop == null || nextBusStop.getLocation() == null)
                        ? ""
                        : String.valueOf(nextBusStop.getLocation().getLatitude()));
        tmpMap.put("next_bus_stop_longitude",
                (nextBusStop == null || nextBusStop.getLocation() == null)
                        ? ""
                        : String.valueOf(nextBusStop.getLocation().getLongitude()));

        String channel = "/subscribe/next_busstop_location/"+tripId+"/"+busId;
        logger.info("sending next bus stop location info to driver for trip-id:{}, bus-id:{}, channel:{}", tripId, busId, channel);

        sendWebSocketMessage(channel, tmpMap);
    }

    private void sendWebSocketMessage(String channel, Map<String, String> messageMap) {
        try {
            String channelMsg = objectMapper.writeValueAsString(messageMap);
            if(logger.isDebugEnabled()) {
                logger.debug("Sending message to channel: {};   Message: {}", channel, channelMsg);
            }
            template.convertAndSend(channel, channelMsg);
        } catch (IOException e) {
            logger.error("{} occurred while creating json string to publish message on channel:{}." +
                    " Message is not sent.", e.getClass().getName(), channel, e);
        }
    }
}
