package jstech.edu.transportmodel.service;

import jstech.edu.transportmodel.common.ParentNotificationSetting;
import jstech.edu.transportmodel.common.UserInfo;

import java.util.List;

public interface SettingsService {

    //reads the info from the table settings_notification
    List<ParentNotificationSetting> getNotificationTime(UserInfo userInfo);

    // inserts into the table settings_notification
    int addNotificationTime(UserInfo userInfo, int notificationMinutes);

    //updates the info in the table settings_notification if onOff is 1, deletes the row from the table settings_notificationk if it is 0
    int updateNotificationTime(UserInfo userInfo, int notificationMinutes, boolean onOff);

    int deleteNotificationTime(UserInfo userInfo, int notificationMinutes);
}
