package jstech.edu.transportmodel.service;

import jstech.edu.transportmodel.common.*;

import java.util.List;

public interface UserService {
    List<DeviceInfo> getDevices(UserInfo userInfo);
    DeviceInfo addDevice(UserInfo userInfo, DeviceInfo deviceInfo);
    DeviceInfo updateDevice(UserInfo userInfo, DeviceInfo deviceInfo);
    DeviceInfo removeDevice(UserInfo userInfo, DeviceInfo deviceInfo);

    List<DeviceInfo> getOnCallUserDevices();

    Driver getDriverBySchoolBus(SchoolBus schoolBus);
    List<DeviceInfo> getDriverDevicesBySchoolBus(SchoolBus schoolBus);
    boolean subscribeNotification(UserInfo userInfo, DeviceInfo deviceInfo, int notificationMins);
    boolean subscribeNotification(UserInfo userInfo, List<DeviceInfo> deviceInfo, int notificationMins);

    boolean unsubscribeNotification(UserInfo userInfo, DeviceInfo deviceInfo, int notificationMins);
    boolean unsubscribeNotification(UserInfo userInfo, List<DeviceInfo> deviceInfo, int notificationMins);

    Picture getThumbnailPicture(String userName);
    Picture getRegularPicture(String userName);
}
