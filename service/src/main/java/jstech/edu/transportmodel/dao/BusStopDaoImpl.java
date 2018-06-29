package jstech.edu.transportmodel.dao;

import jstech.edu.transportmodel.common.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class BusStopDaoImpl implements BusStopDao {
    private static final Logger logger = LoggerFactory.getLogger(SchoolBusDao.class);

    private static final String BUS_STOP_SELECT_ID = "select id from bus_stop where name = ? and school_id = ?";

    private static final String BUS_STOP_SELECT_PART1 = "select bs.id as bp_id, bs.name as name, bs.address as address, bs.count as count, " +
                                                " bs.wait_time_mins as wait_time_mins, bs.school_id as school_id, bsd.id as bsd_id, " +
                                                " bsd.location_id as location_id, bsd.is_pickup as is_pickup " +
                                                " from bus_stop bs join bus_stop_details bsd on bsd.bus_stop_id = bs.id ";

    private static final String BUS_STOP_SELECT_PART2 = " where bs.school_id = ? and bsd.is_pickup = ?";

    private static final String BUS_STOP_SELECT_PART3 = " where bs.name = ? and bs.school_id = ?";

    private static final String BUS_STOP_SELECT_PART4 = " where bs.name = ? and bs.school_id = ? and bsd.is_pickup = ?";

    private static final String BUS_STOP_SELECT_PART5 = " where bs.id = ? and bsd.is_pickup = ?";

    private static final String BUS_STOP_SELECT_PART6 = " join student_bus_stop sbs on sbs.bus_stop_id = bs.id " +
                                                        " join student_parent sp on sp.student_id = sbs.student_id " +
                                                        " join person p on p.id = sp.person_id  " +
                                                        " where p.user_name = ?";

    private static final String BUS_STOP_INSERT = "insert into bus_stop (name, address, count, school_id, wait_time_mins) " +
                                                    " values (?, ?, ?, ?, ?) " +
                                                    " ON CONFLICT (name, school_id) " +
                                                    " DO update set address = ?, count = ?, wait_time_mins = ? ";

    // Do update set bus_stop_id is not necessary, conceptually. This is done so we can get the id of the row from spring jdbc
    private static final String BUS_STOP_DETAILS_INSERT = "insert into bus_stop_details (location_id, is_pickup, bus_stop_id)" +
                                                            " values(?, ?, ?) " +
                                                            " ON CONFLICT (location_id, is_pickup, bus_stop_id) " +
                                                            " DO update set location_id = ?, is_pickup = ?, bus_stop_id = ?";

    private static final String BUS_STOP_DELETE = "delete from bus_stop where id = ?";

    private static final String BUS_STOP_DETAILS_DELETE = "delete from bus_stop_details  where bus_stop_id = ?";


    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SchoolDao schoolDao;

    @Autowired
    private LocationDao locationDao;

    @Override
    public List<BusStop> getBusPoints(boolean pickup, School school) {
        String sql = BUS_STOP_SELECT_PART1 + BUS_STOP_SELECT_PART2;
        List<BusStop> busStops = jdbcTemplate.query(sql, new BusPointRowMapper(), school.getSchoolId(), pickup);
        return busStops == null || busStops.isEmpty() ? null : busStops;
    }

    @Override
    public List<BusStop> getBusPoints(boolean pickup, SchoolBus schoolBus) {
        return null;
    }

    public List<BusStop> getBusPoints(String name, School school) {
        String sql = BUS_STOP_SELECT_PART1 + BUS_STOP_SELECT_PART3;
        List<BusStop> busStops =  jdbcTemplate.query(sql, new BusPointRowMapper(), name, school.getSchoolId());
        return busStops == null || busStops.isEmpty() ? null : busStops;
    }

    @Override
    public BusStop getBusPoint(String name, boolean pickup, School school) {
        String sql = BUS_STOP_SELECT_PART1 + BUS_STOP_SELECT_PART4;
        List<BusStop> busStops = jdbcTemplate.query(sql, new BusPointRowMapper(), name, school.getSchoolId(), pickup);
        return busStops == null || busStops.isEmpty() ? null : busStops.get(0);
    }

    @Override
    public BusStop getBusPoint(int busPointId, boolean pickup) {
        String sql = BUS_STOP_SELECT_PART1 + BUS_STOP_SELECT_PART5;

        List<BusStop> busStops = jdbcTemplate.query(sql, new BusPointRowMapper(), busPointId, pickup);
        return busStops == null || busStops.isEmpty() ? null : busStops.get(0);
    }

    @Override
    public List<BusStop> getBusStopsByParent(UserInfo userInfo) {
        String sql = BUS_STOP_SELECT_PART1 + BUS_STOP_SELECT_PART6;

        List<BusStop> busStops = jdbcTemplate.query(sql, new BusPointRowMapper(), userInfo.getKeyProviderUserName());
        return busStops == null || busStops.isEmpty() ? null : busStops;
    }

    @Override
    public BusStop addBusPoint(BusStop busStop) {
        return upsertBusPoint(busStop);
    }

    @Override
    public BusStop updateBusPoint(BusStop busStop) {
        return upsertBusPoint(busStop);
    }

    @Override
    public BusStop deleteBusPoint(BusStop busStop) {
        int out = jdbcTemplate.update(BUS_STOP_DETAILS_DELETE, busStop.getId());
        if(out <= 0) {
            return null;
        }

        out = jdbcTemplate.update(BUS_STOP_DELETE, busStop.getId());

        return out> 0 ? busStop : null;
    }

    private BusStop upsertBusPoint(BusStop busStop) {
        // add school bus starting location, if it is not added yet. Get location-id if it is already added.
        GeoLocation location = locationDao.addLocation(busStop.getLocation().getLatitude(), busStop.getLocation().getLongitude());

        // add school if it is not added yet. Get school-id if it is already added.
        School school = schoolDao.addSchool(busStop.getSchool());

        // below statement is specific to mysql and hence commented out as we are using postgres now. Keep it here, in case we have to switch to mysql.
        /*sql = "insert into bus_point (location_id, address, count) values (?, ?, ?) " +
                    " on duplicate key update address = ?, count = ? ";*/

        // insert into bus_point table.
        /*jdbcTemplate.update(BUS_STOP_INSERT, busStop.getName(), busStop.getAddress(), busStop.getNumStudents(), school.getSchoolId(), busStop.getWaitTimeMins(),
                busStop.getAddress(), busStop.getNumStudents(), busStop.getWaitTimeMins());*/
        PreparedStatementCreator preparedStatementCreator = new PreparedStatementCreator() {
            @Override
            public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
                PreparedStatement ps =
                        connection.prepareStatement(BUS_STOP_INSERT, new String[] {"id"});
                ps.setString(1, busStop.getName());
                ps.setString(2, busStop.getAddress());
                ps.setInt(3, busStop.getNumStudents());
                ps.setInt(4, school.getSchoolId());
                ps.setInt(5, busStop.getWaitTimeMins());
                ps.setString(6, busStop.getAddress());
                ps.setInt(7, busStop.getNumStudents());
                ps.setInt(8, busStop.getWaitTimeMins());
                return ps;
            }
        };
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(preparedStatementCreator, keyHolder);
        int busStopId = 0;
        if(keyHolder.getKey() != null) {
            busStopId = keyHolder.getKey().intValue();
        }

        if(busStopId <= 0) {
            busStopId = jdbcTemplate.queryForObject(BUS_STOP_SELECT_ID, Integer.class, busStop.getName(), school.getSchoolId());
        }

        // insert into bus_stop_details
        //jdbcTemplate.update(BUS_STOP_DETAILS_INSERT, location.getId(), busStop.isPickupPoint(), busStopId);
        final int tmpBusStopId = busStopId;
        preparedStatementCreator = new PreparedStatementCreator() {
            @Override
            public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
                PreparedStatement ps =
                        connection.prepareStatement(BUS_STOP_DETAILS_INSERT, new String[] {"id"});
                ps.setInt(1, location.getId());
                ps.setBoolean(2, busStop.isPickupPoint());
                ps.setInt(3, tmpBusStopId);
                ps.setInt(4, location.getId());
                ps.setBoolean(5, busStop.isPickupPoint());
                ps.setInt(6, tmpBusStopId);
                return ps;
            }
        };
        keyHolder = new GeneratedKeyHolder();
        int busStopDetailsId = 0;
        jdbcTemplate.update(preparedStatementCreator, keyHolder);
        if(keyHolder.getKey() != null) {
            busStopDetailsId = keyHolder.getKey().intValue();
        }

        if(busStopDetailsId <= 0) {
            return getBusPoint(busStop.getName(), busStop.isPickupPoint(),school);
        } else {
            /*
             "select bs.id as bp_id, bs.name as name, bs.address as address, bs.count as count, " +
                                                " bs.wait_time_mins as wait_time_mins, bs.school_id as school_id, bsd.id as bsd_id, " +
                                                " bsd.location_id as location_id, bsd.is_pickup as is_pickup " +
                                                " from bus_stop bs join bus_stop_details bsd on bsd.bus_stop_id = bs.id "
             */
            BusStop addedBusStop = new BusStop.Builder().setId(busStopId).setName(busStop.getName()).setAddress(busStop.getAddress()).setWaitTimeMins(busStop.getWaitTimeMins())
                    .setSchool(school).setBusStopDetailId(busStopDetailsId).setLocation(location).setPickupPoint(busStop.isPickupPoint()).build();
            addedBusStop.setRelativeArrivalTimeSecs(busStop.getRelativeArrivalTimeSecs());
            addedBusStop.setRelativeDepartureTimeSecs(busStop.getRelativeDepartureTimeSecs());
            return addedBusStop;
        }
    }

    private class BusPointRowMapper implements RowMapper<BusStop> {
        @Override
        public BusStop mapRow(ResultSet resultSet, int i) throws SQLException {

            BusStop.Builder builder = new BusStop.Builder();
            builder.setId(resultSet.getInt("bp_id"));
            builder.setName(resultSet.getString("name"));
            builder.setAddress(resultSet.getString("address"));
            builder.setNumStudents(resultSet.getInt("count"));
            School school = schoolDao.getSchool(resultSet.getInt("school_id"));
            builder.setSchool(school);
            builder.setBusStopDetailId(resultSet.getInt("bsd_id"));
            GeoLocation location = locationDao.getLocation(resultSet.getInt("location_id"));
            builder.setLocation(location);
            builder.setPickupPoint(resultSet.getBoolean("is_pickup"));
            builder.setWaitTimeMins(resultSet.getInt("wait_time_mins"));

            return builder.build();
        }
    }
}
