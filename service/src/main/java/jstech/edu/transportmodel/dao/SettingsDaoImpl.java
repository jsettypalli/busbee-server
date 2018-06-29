package jstech.edu.transportmodel.dao;

import jstech.edu.transportmodel.common.ParentNotificationSetting;
import jstech.edu.transportmodel.common.UserInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class SettingsDaoImpl implements SettingsDao {

    private static final String SETTINGS_NOTIFICATION_GET_TIME = "select sn.notification_time from settings_notification sn " +
            " join person p on p.id=sn.person_id where p.user_name = ?";

    private static final String SETTINGS_NOTIFICATION_UPDATE_TIME = "update settings_notification sn set sn.notification_time = ? " +
            " join person p on p.id=sn.person_id where p.user_name = ?";

    private static final String SETTINGS_NOTIFICATION_ADD = "insert into settings_notification(person_id, notification_time) " +
            " select id, ? from person where user_name = ?" +
            " ON CONFLICT (person_id, notification_time) DO NOTHING";

    private static final String SETTINGS_NOTIFICATION_DELETE = "delete from settings_notification using person where id = person_id  and user_name = ? and notification_time = ?";

    @Autowired
    private JdbcTemplate jdbcTemplate;


    @Override
    public List<ParentNotificationSetting> getNotificationTime(UserInfo userInfo) {
        List<Integer> notificationTimes = jdbcTemplate.queryForList(SETTINGS_NOTIFICATION_GET_TIME, new Object[] { userInfo.getKeyProviderUserName() }, Integer.class);
        List<ParentNotificationSetting> settings = new ArrayList<>();
        for(int notificationTime: notificationTimes) {
            ParentNotificationSetting setting = new ParentNotificationSetting.Builder()
                                                    .setUserInfo(userInfo)
                                                    .setNotificationMinutes(notificationTime)
                                                    .setEnabled(true)
                                                    .build();
            settings.add(setting);
        }
        return settings;
    }

    @Override
    public int addNotificationTime(UserInfo userInfo, int notificationMinutes) {
        return jdbcTemplate.update(SETTINGS_NOTIFICATION_ADD, notificationMinutes, userInfo.getKeyProviderUserName());
    }

    @Override
    public int updateNotificationTime(UserInfo userInfo, int notificationMinutes, boolean onOff){
        if(onOff) {
            return addNotificationTime(userInfo, notificationMinutes);
        }
        else {
            return deleteNotificationTime(userInfo, notificationMinutes);
        }
    }

    @Override
    public int deleteNotificationTime(UserInfo userInfo, int notificationMinutes){
        return jdbcTemplate.update(SETTINGS_NOTIFICATION_DELETE, userInfo.getKeyProviderUserName(), notificationMinutes);
    }
}
