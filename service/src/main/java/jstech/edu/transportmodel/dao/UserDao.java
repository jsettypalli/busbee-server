package jstech.edu.transportmodel.dao;

import jstech.edu.transportmodel.common.*;

import java.util.List;

public interface UserDao {
    List<DeviceInfo> getDevices(UserInfo userInfo);
    DeviceInfo addDevice(UserInfo userInfo, DeviceInfo deviceInfo);
    DeviceInfo updateDevice(UserInfo userInfo, DeviceInfo deviceInfo);
    DeviceInfo removeDevice(UserInfo userInfo, DeviceInfo deviceInfo);

    List<DeviceInfo> getOnCallUserDevices();

    Driver getDriverBySchoolBus(SchoolBus schoolBus);
    List<DeviceInfo> getDriverDevicesBySchoolBus(SchoolBus schoolBus);

    String getThumbnailPicture(String userName);
    String getRegularPicture(String userName);
}
