package jstech.edu.transportmodel.dao;

import jstech.edu.transportmodel.common.ParentNotificationSetting;
import jstech.edu.transportmodel.common.UserInfo;

import java.util.List;

public interface SettingsDao {

    List<ParentNotificationSetting> getNotificationTime(UserInfo userInfo);

    int addNotificationTime(UserInfo userInfo, int notificationMinutes);
    int updateNotificationTime(UserInfo userInfo, int notificationMinutes, boolean onOff);
    int deleteNotificationTime(UserInfo userInfo, int notificationMinutes);
}
