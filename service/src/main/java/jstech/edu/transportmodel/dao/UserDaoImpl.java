package jstech.edu.transportmodel.dao;

import jstech.edu.transportmodel.common.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class UserDaoImpl implements UserDao {
    private static final String USER_DEVICE_SELECT_PART1 = "SELECT ud.id, ud.person_id, ud.platform, ud.device_id, ud.app_token, ud.endpointarn FROM user_device_details ud ";

    private static final String USER_DEVICE_SELECT_PART2  = " join person p on p.id = ud.person_id " +
            "where ud.platform = ? and ud.device_id = ? and ud.app_token = ?  and p.user_name = ?";

    private static final String USER_DEVICE_SELECT_PART3  = " join person p on p.id = ud.person_id where p.user_name = ?";

    private static final String USER_DEVICE_SELECT_PART4  =  " where ud.device_id = ?";


    private static final String USER_DEVICE_INSERT1 = "INSERT into user_device_details (person_id, platform, device_id, app_token) " +
            " values(?, ?, ?, ?) ON CONFLICT (person_id, platform, device_id, app_token) DO NOTHING";

    private static final String USER_DEVICE_INSERT2 = "INSERT into user_device_details (person_id, platform, device_id, app_token) " +
            " select id, ?, ?, ? from person where user_name = ? ON CONFLICT (person_id, platform, device_id, app_token) DO NOTHING";

    private static final String USER_DEVICE_UPDATE1 = "UPDATE user_device_details set app_token = ? where person_id = ? and platform = ? and device_id = ?";

    private static final String USER_DEVICE_UPDATE2 = "UPDATE user_device_details as ud set app_token = ? FROM person as p where p.id = ud.person_id " +
            " and ud.platform = ? and ud.device_id = ? and p.user_name = ?";

    private static final String USER_DEVICE_UPDATE3 = "UPDATE user_device_details as ud set endpointarn = ? where " +
            " ud.device_id = ?";

    private static final String USER_DEVICE_DELETE1  = "DELETE from user_device_details where person_id = ? and platform = ? and device_id = ? and app_token = ? ";

    private static final String USER_DEVICE_DELETE2  = "DELETE from user_device_details ud USING person p where p.id = ud.person_id " +
            " and ud.platform = ? and ud.device_id = ? and ud.app_token = ? and p.user_name = ?";

    private static final String ON_CALL_USER_DEVICES_SELECT_PART2 = " join on_call oc on oc.person_id = ud.person_id ";

    private static final String DRIVER_DEVICES_BY_SCHOOL_BUS_PART2 = "join driver_vehicle dv on dv.person_id = ud.person_id " +
            " where dv.vehicle_id = ?";

    private static final String USER_INFO_SELECT1 = " select p.id as id, p.first_name, p.last_name, p.phone_number, p.email, p.user_name, p.nick_name, " +
            "p.thumbnail_image, p.regular_image from person p join driver_vehicle dv on dv.person_id = p.id where dv.vehicle_id = ?";

    private static final String PERSON_THUMBNAIL_PICTURE = "select thumbnail_image from person where user_name = ?";

    private static final String PERSON_REGULAR_PICTURE = "select regular_image from person where user_name = ?";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public List<DeviceInfo> getDevices(UserInfo userInfo) {
        List<DeviceInfo> devices = jdbcTemplate.query(USER_DEVICE_SELECT_PART1 + USER_DEVICE_SELECT_PART3,
                new UserDeviceDetailsRowMapper(), userInfo.getKeyProviderUserName());
        return devices == null || devices.isEmpty() ? null : devices;
    }

    @Override
    public DeviceInfo addDevice(UserInfo userInfo, DeviceInfo deviceInfo) {
        int out = jdbcTemplate.update(USER_DEVICE_INSERT2, deviceInfo.getPlatform().toString(), deviceInfo.getDeviceId(), deviceInfo.getAppToken(),
                userInfo.getKeyProviderUserName());

        List<DeviceInfo> devices = jdbcTemplate.query(USER_DEVICE_SELECT_PART1 + USER_DEVICE_SELECT_PART2,
                new Object[] {deviceInfo.getPlatform().toString(), deviceInfo.getDeviceId(), deviceInfo.getAppToken(),
                        userInfo.getKeyProviderUserName()}, new UserDeviceDetailsRowMapper());
        return devices != null && !devices.isEmpty() ? devices.get(0) : null;
    }

    @Override
    public DeviceInfo updateDevice(UserInfo userInfo, DeviceInfo deviceInfo) {


        // Whenever EndpointArn is not null, update the endpointArn in the database.
        if(userInfo == null || deviceInfo.getEndPointArn()!=null) {
            updateDeviceEndPointArn(deviceInfo);
            return deviceInfo;
        }
        int out = jdbcTemplate.update(USER_DEVICE_UPDATE2, deviceInfo.getAppToken(), deviceInfo.getPlatform().toString(), deviceInfo.getDeviceId(), userInfo.getKeyProviderUserName());

        List<DeviceInfo> devices = jdbcTemplate.query(USER_DEVICE_SELECT_PART1 + USER_DEVICE_SELECT_PART2,
                new Object[] {deviceInfo.getPlatform().toString(), deviceInfo.getDeviceId(), deviceInfo.getAppToken(),
                        userInfo.getKeyProviderUserName()}, new UserDeviceDetailsRowMapper());
        return devices != null && !devices.isEmpty() ? devices.get(0) : null;
    }


    public DeviceInfo updateDeviceEndPointArn(DeviceInfo deviceInfo) {
        int out = jdbcTemplate.update(USER_DEVICE_UPDATE3, deviceInfo.getEndPointArn(), deviceInfo.getDeviceId());

        List<DeviceInfo> devices = jdbcTemplate.query(USER_DEVICE_SELECT_PART1 + USER_DEVICE_SELECT_PART4,
                new Object[] {deviceInfo.getDeviceId()}, new UserDeviceDetailsRowMapper());
        return devices != null && !devices.isEmpty() ? devices.get(0) : null;
    }

    @Override
    public DeviceInfo removeDevice(UserInfo userInfo, DeviceInfo deviceInfo) {
        List<DeviceInfo> devices = jdbcTemplate.query(USER_DEVICE_SELECT_PART1 + USER_DEVICE_SELECT_PART2,
                new Object[] {deviceInfo.getPlatform().toString(), deviceInfo.getDeviceId(), deviceInfo.getAppToken(),
                        userInfo.getKeyProviderUserName()}, new UserDeviceDetailsRowMapper());

        int out = jdbcTemplate.update(USER_DEVICE_DELETE2, deviceInfo.getPlatform().toString(), deviceInfo.getDeviceId(), deviceInfo.getAppToken(),
                userInfo.getKeyProviderUserName());

        return devices != null && !devices.isEmpty() ? devices.get(0) : null;
    }

    @Override
    public List<DeviceInfo> getOnCallUserDevices() {
        List<DeviceInfo> devices  = jdbcTemplate.query(USER_DEVICE_SELECT_PART1 + ON_CALL_USER_DEVICES_SELECT_PART2, new UserDeviceDetailsRowMapper());
        return devices == null || devices.isEmpty() ? null : devices;
    }

    @Override
    public Driver getDriverBySchoolBus(SchoolBus schoolBus) {
        List<Driver> drivers = jdbcTemplate.query(USER_INFO_SELECT1, new UserInfoRowMapper(), schoolBus.getVehicleId());
        return drivers == null || drivers.isEmpty() ? null : drivers.get(0);
    }

    @Override
    public List<DeviceInfo> getDriverDevicesBySchoolBus(SchoolBus schoolBus) {
        List<DeviceInfo> devices = jdbcTemplate.query(USER_DEVICE_SELECT_PART1 + DRIVER_DEVICES_BY_SCHOOL_BUS_PART2,
                new Object[] {schoolBus.getVehicleId()}, new UserDeviceDetailsRowMapper());
        return devices == null || devices.isEmpty() ? null : devices;
    }

    @Override
    public String getThumbnailPicture(String userName) {
        return jdbcTemplate.queryForObject(PERSON_THUMBNAIL_PICTURE, new Object[] {userName}, String.class);
    }

    @Override
    public String getRegularPicture(String userName) {
        return jdbcTemplate.queryForObject(PERSON_REGULAR_PICTURE, new Object[] {userName}, String.class);
    }

    public class UserDeviceDetailsRowMapper implements RowMapper<DeviceInfo> {
        @Override
        public DeviceInfo mapRow(ResultSet resultSet, int i) throws SQLException {
            int id = resultSet.getInt("id");
            int personId = resultSet.getInt("person_id");
            String platform = resultSet.getString("platform");
            String deviceId = resultSet.getString("device_id");
            String appToken = resultSet.getString("app_token");
            String endPointArn = resultSet.getString("endpointarn");

            return new DeviceInfo.Builder()
                    .setDeviceInfoId(id)
                    .setPersonId(personId)
                    .setPlatform(Platform.valueOf(platform))
                    .setDeviceId(deviceId)
                    .setAppToken(appToken)
                    .setEndPointArn(endPointArn)
                    .build();
        }
    }

    public class UserInfoRowMapper implements RowMapper<Driver> {
        @Override
        public Driver mapRow(ResultSet resultSet, int i) throws SQLException {
            int id = resultSet.getInt("id");
            String firstName = resultSet.getString("first_name");
            String lastName = resultSet.getString("last_name");
            String nickName = resultSet.getString("nick_name");
            String userName = resultSet.getString("user_name");
            String phoneNumber = resultSet.getString("phone_number");
            String email = resultSet.getString("email");
            String thumbnail_image = resultSet.getString("thumbnail_image");
            String regular_image = resultSet.getString("regular_image");

            Driver.Builder builder = new Driver.Builder();
            builder.setId(id)
                    .setFirstName(firstName)
                    .setLastName(lastName)
                    .setNickName(nickName)
                    .setThumbnailUrl(thumbnail_image)
                    .setRegularImageUrl(regular_image);
            builder.setKeyProviderUserName(userName);
            builder.setPhoneNumber(phoneNumber);
            builder.setEmail(email);

            return builder.build();
        }
    }
}
