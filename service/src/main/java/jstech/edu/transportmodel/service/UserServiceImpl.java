package jstech.edu.transportmodel.service;

import jstech.edu.transportmodel.common.*;
import jstech.edu.transportmodel.dao.UserDao;
import jstech.edu.transportmodel.service.route.RouteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
public class UserServiceImpl implements UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private SchoolBusService schoolBusService;

    @Autowired
    private RouteService routeService;

    @Autowired
    private SettingsService settingsService;

    @Autowired
    private UserDao userDao;

    private int defaultNotificationMins = 10;

    @Override
    public List<DeviceInfo> getDevices(UserInfo userInfo) {
        return userDao.getDevices(userInfo);
    }

    @Override
    @Transactional
    public DeviceInfo addDevice(UserInfo userInfo, DeviceInfo deviceInfo) {
        DeviceInfo addedDeviceInfo =  userDao.addDevice(userInfo, deviceInfo);

        // set default notification time to 10min if logged in user is not driver and is not transport_in_charge.
        if(userInfo.getRole() != UserRole.DRIVER && userInfo.getRole() != UserRole.TRANSPORT_INCHARGE) {
            settingsService.addNotificationTime(userInfo, defaultNotificationMins);
        }
        boolean outcome = subscribeNotification(userInfo, addedDeviceInfo, defaultNotificationMins);
        return addedDeviceInfo;
    }

    @Override
    @Transactional
    public DeviceInfo updateDevice(UserInfo userInfo, DeviceInfo deviceInfo) {
        // TODO - check if we need to modify the subscription to push notification when device is updated.
        //      To be specific, only application_token would have changed when this method is called.
        //      EndpointARN created and subscribed to topic needs to be updated with the new apptoken.

        DeviceInfo updatedDeviceInfo =  userDao.updateDevice(userInfo, deviceInfo);

        boolean outcome = subscribeNotification(userInfo, updatedDeviceInfo, defaultNotificationMins);
        return updatedDeviceInfo;

    }

    @Override
    @Transactional
    public DeviceInfo removeDevice(UserInfo userInfo, DeviceInfo deviceInfo) {
        DeviceInfo removedDeviceInfo = userDao.removeDevice(userInfo, deviceInfo);
        List<ParentNotificationSetting> settings = settingsService.getNotificationTime(userInfo);
        if(settings == null || settings.isEmpty()) {
            boolean outcome = unsubscribeNotification(userInfo, removedDeviceInfo, defaultNotificationMins);
        } else {
            for (ParentNotificationSetting setting : settings) {
                boolean outcome = unsubscribeNotification(userInfo, removedDeviceInfo, setting.getNotificationMinutes());
            }
        }

        return removedDeviceInfo;
    }

    @Override
    public List<DeviceInfo> getOnCallUserDevices() {
        return userDao.getOnCallUserDevices();
    }

    @Override
    public Driver getDriverBySchoolBus(SchoolBus schoolBus) {
        return userDao.getDriverBySchoolBus(schoolBus);
    }

    @Override
    public List<DeviceInfo> getDriverDevicesBySchoolBus(SchoolBus schoolBus) {
        return userDao.getDriverDevicesBySchoolBus(schoolBus);
    }

    @Override
    public boolean subscribeNotification(UserInfo userInfo, List<DeviceInfo> devices, int notificationMins) {
        if(userInfo == null || devices == null || devices.isEmpty()) {
            logger.warn("subscribeNotification method(userInfo, devices, notificationMins) is called with incorrect parameters. " +
                    "Please check the places where it is called from and fix if this is an issue.");
            return false;
        }

        boolean finalResult = true;
        for(DeviceInfo deviceInfo: devices) {
            boolean notified = subscribeNotification(userInfo, deviceInfo, notificationMins);
            if(finalResult && !notified) {
                finalResult = notified;
            }
        }
        return finalResult;
    }

    @Override
    public boolean subscribeNotification(UserInfo userInfo, DeviceInfo deviceInfo, int notificationMins) {
        if(userInfo == null || deviceInfo == null) {
            logger.warn("subscribeNotification method(userInfo, deviceInfo, notificationMins) is called with incorrect parameters. " +
                    "Please check the places where it is called from and fix if this is an issue.");
            return false;
        }
        return updateNotification(userInfo, deviceInfo, notificationMins, true);
    }

    @Override
    public boolean unsubscribeNotification(UserInfo userInfo, List<DeviceInfo> devices, int notificationMins) {
        if(userInfo == null || devices == null || devices.isEmpty()) {
            logger.warn("unsubscribeNotification method(userInfo, devices, notificationMins) is called with incorrect parameters. " +
                    "Please check the places where it is called from and fix if this is an issue.");
            return false;
        }

        boolean finalResult = true;
        for(DeviceInfo deviceInfo: devices) {
            boolean notified = unsubscribeNotification(userInfo, deviceInfo, notificationMins);
            if(finalResult && !notified) {
                finalResult = notified;
            }
        }
        return finalResult;
    }

    @Override
    public boolean unsubscribeNotification(UserInfo userInfo, DeviceInfo deviceInfo, int notificationMins) {
        if(userInfo == null || deviceInfo == null) {
            logger.warn("unsubscribeNotification method(userInfo, deviceInfo, notificationMins) is called with incorrect parameters. " +
                    "Please check the places where it is called from and fix if this is an issue.");
            return false;
        }

        return updateNotification(userInfo, deviceInfo, notificationMins, false);
    }

    private boolean updateNotification(UserInfo userInfo, DeviceInfo deviceInfo, int notificationMins, boolean subscribe) {
        boolean notified = false;
        List<BusStop> busStops = null;
        if(userInfo.getRole() != UserRole.DRIVER && userInfo.getRole() != UserRole.TRANSPORT_INCHARGE) {
            busStops = schoolBusService.getBusStopsByParent(userInfo);
            if(busStops == null || busStops.isEmpty()) {
                logger.warn("Looks like no bus stop is associated with the user: {}. Please ensure this is accurate. Fix if this is an issue.");
                return false;
            }
        }

        List<SchoolBus> buses = schoolBusService.getSchoolBusesAssociatedWithUser(userInfo);
        if(buses != null && !buses.isEmpty()) {
            List<BusTrip> trips = routeService.getTrips(buses);
            if(trips != null && !trips.isEmpty()) {
                for(BusTrip trip: trips) {
                    for(Map.Entry<SchoolBus, SchoolBusRoute> entry: trip.getBusRoutes().entrySet()) {
                        SchoolBus schoolBus = entry.getKey();
                        if(buses.contains(schoolBus)) {
                            switch(userInfo.getRole()) {
                                case DRIVER:
                                    String driverTopicName = "-start_tracking-"+trip.getTripId()+"-"+schoolBus.getVehicleId();
                                    // TODO - call notification service to subscribe when the functaionlity is ready.
                                    if(subscribe) {
                                        notified = notificationService.subscribe(Arrays.asList(deviceInfo), Arrays.asList(driverTopicName));
                                        if(!notified) {
                                            logger.error("Couldn't subscribe to topic. Address ASAP. Topic-Name:{}, User: {}, Device: {}.",
                                                    driverTopicName, userInfo, deviceInfo);
                                        }

                                    } else {
                                        notified = notificationService.unsubscribe(Arrays.asList(deviceInfo), Arrays.asList(driverTopicName));
                                        if(!notified) {
                                            logger.error("Couldn't unsubscribe to topic. Address ASAP. Topic-Name:{}, User: {}, Device: {}.",
                                                    driverTopicName, userInfo, deviceInfo);
                                        }
                                    }
                                    break;
                                case PARENT:
                                    String parentTopicName = "-arrival_notification-"+ trip.getTripId()+"-"+schoolBus.getVehicleId()+"-"+
                                            busStops.get(0).getId()+"-"+notificationMins;
                                    // TODO - call notification service to subscribe when the functaionlity is ready.
                                    if(subscribe) {
                                        notified = notificationService.subscribe(Arrays.asList(deviceInfo), Arrays.asList(parentTopicName));
                                        if(!notified) {
                                            logger.error("Couldn't subscribe to topic. Address ASAP. Topic-Name:{}, User: {}, Device: {}.",
                                                    parentTopicName, userInfo, deviceInfo);
                                        }
                                    } else {
                                        notified = notificationService.unsubscribe(Arrays.asList(deviceInfo), Arrays.asList(parentTopicName));
                                        if(!notified) {
                                            logger.error("Couldn't unsubscribe to topic. Address ASAP. Topic-Name:{}, User: {}, Device: {}.",
                                                    parentTopicName, userInfo, deviceInfo);
                                        }
                                    }
                                    break;
                            }
                        }
                    }
                }
            }
        }

        return notified;
    }

    @Override
    public Picture getThumbnailPicture(String userName) {
        return getImage(userName, true);
    }

    @Override
    public Picture getRegularPicture(String userName) {
        return getImage(userName, false);
    }

    private Picture getImage(String userName, boolean thumbNailPicture) {
        String pictureFileName = thumbNailPicture ? userDao.getThumbnailPicture(userName) : userDao.getRegularPicture(userName);
        try {
            if (StringUtils.hasText(pictureFileName)) {
                byte[] data = Files.readAllBytes(Paths.get(pictureFileName));
                Picture picture = new Picture();
                picture.setData(data);
                picture.setFileName(pictureFileName);
                return picture;
            }
        } catch (IOException e) {
            logger.error("IOException occurred while reading {} picture data.", thumbNailPicture ? "thumbnail" : "regular", e);
        }
        return null;
    }
}
