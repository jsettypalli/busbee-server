package jstech.edu.transportmodel.service;

import jstech.edu.transportmodel.common.DeviceInfo;
import jstech.edu.transportmodel.common.ParentNotificationSetting;
import jstech.edu.transportmodel.common.UserInfo;
import jstech.edu.transportmodel.dao.SettingsDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SettingsServiceImpl implements SettingsService{
    private static final Logger logger = LoggerFactory.getLogger(SettingsServiceImpl.class);

    @Autowired
    private SettingsDao settingsDao;

    @Autowired
    private UserService userService;

    @Override
    public List<ParentNotificationSetting> getNotificationTime(UserInfo userInfo) {
        return settingsDao.getNotificationTime(userInfo);
    }

    @Override
    @Transactional
    public int addNotificationTime(UserInfo userInfo, int notificationMinutes) {
        return settingsDao.addNotificationTime(userInfo, notificationMinutes);
    }

    // if the flag yesOrNo is 0, the row is deleted. If the flag is 1, the row is updated.
    @Override
    @Transactional
    public int updateNotificationTime(UserInfo userInfo, int notificationMinutes, boolean yesOrNo) {
        int result = settingsDao.updateNotificationTime(userInfo, notificationMinutes, yesOrNo);

        try {
            List<DeviceInfo> devices = userService.getDevices(userInfo);
            if (devices == null || devices.isEmpty()) {
                logger.warn("No devices are found for the user: {}. This usually happens when user never logged into the application from mobile device. " +
                        " If user complains that no push notifications are received, this is the place to check.", userInfo.getKeyProviderUserName());
            } else {
                if (yesOrNo) {
                    userService.subscribeNotification(userInfo, devices, notificationMinutes);
                } else {
                    userService.unsubscribeNotification(userInfo, devices, notificationMinutes);
                }
            }
        } catch (Exception ex) {
            logger.error("{} occurred while {} the user: {}", ex.getClass().getName(), yesOrNo ? "subscribing" : "unsubscribing", userInfo, ex);
        }

        return result;
    }

    @Override
    @Transactional
    public int deleteNotificationTime(UserInfo userInfo, int notificationMinutes) {
        return settingsDao.deleteNotificationTime(userInfo, notificationMinutes);
    }
}
