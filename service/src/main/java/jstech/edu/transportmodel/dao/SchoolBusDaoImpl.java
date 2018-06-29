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
import java.util.ArrayList;

import java.util.List;

@Repository
public class SchoolBusDaoImpl implements SchoolBusDao {
    private static final Logger logger = LoggerFactory.getLogger(SchoolBusDaoImpl.class);

    private static final String SCHOOL_BUS_SELECT_PART1 = "select v.id, v.bus_number, v.registration_number, v.capacity, v.school_id, " +
                                                            " v.start_bus_stop_id, v.make, v.model from vehicle v";

    private static final String SCHOOL_BUS_SELECT_PART2 = " where v.school_id = ?";

    private static final String SCHOOL_BUS_SELECT_PART3 = " where v.id = ?";

    private static final String SCHOOL_BUS_SELECT_PART4 = " where v.registration_number = ?";

    private static final String SCHOOL_BUS_SELECT_PART5 =   " join driver_vehicle dv on dv.vehicle_id = v.id " +
                                                            " join person p on p.id = dv.person_id " +
                                                            " where p.user_name = ?";

    private static final String SCHOOL_BUS_SELECT_PART6 =   " join route_map rm on rm.vehicle_id = v.id " +
                                                            " join bus_stop_details bsd on bsd.id = rm.bus_stop_details_id " +
                                                            " join bus_stop bs on bs.id = bsd.bus_stop_id " +
                                                            " join student_bus_stop sbs on sbs.bus_stop_id = bs.id " +
                                                            " join student_parent sp on sp.student_id = sbs.student_id " +
                                                            " join person p on p.id = sp.person_id  " +
                                                            " where p.user_name = ?";

    private static final String SCHOOL_BUS_SELECT_PART7 =   " join transport_incharge ti on ti.school_id = v.school_id " +
                                                            " join person p on p.id = ti.person_id " +
                                                            " where p.user_name = ?";

    private static final String SCHOOL_BUS_INSERT = "INSERT into vehicle (bus_number, registration_number, capacity, school_id, " +
                                                    " start_bus_stop_id, make, model) " +
                                                    " values (?, ?, ?, ?, ?, ?, ?) " +
                                                    " ON CONFLICT (registration_number) " +
                                                    " DO UPDATE set bus_number=?, capacity=?, school_id=?, " +
                                                    " start_bus_stop_id=?, make=?, model=?";

    private static final String SCHOOL_BUS_DELETE = "delete from SchoolBus where registration_number = ?";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SchoolDao schoolDao;

    @Autowired
    private LocationDao locationDao;

    @Autowired
    private BusStopDao busStopDao;

    /**
     *
     * @return all schoolbuses
     */
    @Override
    public List<SchoolBus> getSchoolBuses(School school) {
        String sql = SCHOOL_BUS_SELECT_PART1 + SCHOOL_BUS_SELECT_PART2;
        List<SchoolBus> schoolBuses = jdbcTemplate.query(sql, new SchoolBusRowMapper(), school.getSchoolId());
        return schoolBuses == null || schoolBuses.isEmpty() ? null : schoolBuses;
    }

    /**
     *
     * @param id school-id
     * @return SchoolBus object if exists, null otherwise.
     */
    @Override
    public SchoolBus getSchoolBus(int id) {
        String sql = SCHOOL_BUS_SELECT_PART1 + SCHOOL_BUS_SELECT_PART3;
        List<SchoolBus> schoolBuses = jdbcTemplate.query(sql, new SchoolBusRowMapper(), id);
        return schoolBuses == null || schoolBuses.isEmpty() ? null : schoolBuses.get(0);
    }

    /**
     *
     * @param registrationNumber of the vehicle
     * @return SchoolBus object if exists, null otherwise.
     */
    @Override
    public SchoolBus getSchoolBus(String registrationNumber) {
        String sql = SCHOOL_BUS_SELECT_PART1 + SCHOOL_BUS_SELECT_PART4;
        List<SchoolBus> schoolBuses = jdbcTemplate.query(sql, new SchoolBusRowMapper(), registrationNumber);
        return schoolBuses == null || schoolBuses.isEmpty() ? null : schoolBuses.get(0);
    }

    /**
     *
     * @param schoolBus object
     * @return returns SchoolBus object if added, null otherwise.
     */
    @Override
    public SchoolBus addSchoolBus(SchoolBus schoolBus) {
        return upsertSchoolBus(schoolBus);
    }

    /**
     *
     * @param schoolBus object
     * @return returns SchoolBus object if updated, null otherwise.
     */
    @Override
    public SchoolBus updateSchoolBus(SchoolBus schoolBus) {
        return upsertSchoolBus(schoolBus);
    }

    /**
     *
     * @param schoolBus object
     * @return schoolBus object if deleted, null otherwise.
     */
    @Override
    public SchoolBus deleteSchoolBus(SchoolBus schoolBus) {
        int out = jdbcTemplate.update(SCHOOL_BUS_DELETE, schoolBus.getRegistrationNumber());

        // return the schoolbus is it is deleted
        return out > 0 ? schoolBus : null;
    }

    @Override
    public SchoolBus getSchoolBusByDriver(String userName) {
        String sql = SCHOOL_BUS_SELECT_PART1 + SCHOOL_BUS_SELECT_PART5;
        List<SchoolBus> schoolBuses = jdbcTemplate.query(sql, new SchoolBusRowMapper(), userName);
        return schoolBuses == null || schoolBuses.isEmpty() ? null : schoolBuses.get(0);
    }

    @Override
    public List<SchoolBus> getSchoolBusesByParent(String userName) {
        String sql = SCHOOL_BUS_SELECT_PART1 + SCHOOL_BUS_SELECT_PART6;
        List<SchoolBus> schoolBuses = jdbcTemplate.query(sql, new SchoolBusRowMapper(), userName);
        return schoolBuses == null || schoolBuses.isEmpty() ? null : schoolBuses;
    }

    @Override
    public List<SchoolBus> getSchoolBusesByTransportIncharge(String userName) {
        String sql = SCHOOL_BUS_SELECT_PART1 + SCHOOL_BUS_SELECT_PART7;
        List<SchoolBus> schoolBuses = jdbcTemplate.query(sql, new SchoolBusRowMapper(), userName);
        return schoolBuses == null || schoolBuses.isEmpty() ? null : schoolBuses;
    }

    private SchoolBus upsertSchoolBus(SchoolBus schoolBus) {
        // add school bus starting location, if it is not added yet. Get location-id if it is already added.
        BusStop startBusStop = schoolBus.getStartBusStop();

        // add school if it is not added yet. Get school-id if it is already added.
        School school = schoolDao.addSchool(schoolBus.getSchool());

        logger.debug("addSchoolBus sql: {}, registration_number: {}, capacity: {}, make: {}, model: {}",
                SCHOOL_BUS_INSERT, schoolBus.getRegistrationNumber(), schoolBus.getCapacity(), schoolBus.getMake(), schoolBus.getModel());
        /*int out = jdbcTemplate.update(SCHOOL_BUS_INSERT, schoolBus.getBusNumber(), schoolBus.getRegistrationNumber(), schoolBus.getCapacity(),
                schoolBus.getSchool().getSchoolId(), schoolBus.getStartBusStop().getId(), schoolBus.getMake(), schoolBus.getModel(),
                schoolBus.getBusNumber(), schoolBus.getCapacity(),  schoolBus.getSchool().getSchoolId(),
                schoolBus.getStartBusStop().getId(), schoolBus.getMake(), schoolBus.getModel());*/

        PreparedStatementCreator preparedStatementCreator = new PreparedStatementCreator() {
            @Override
            public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
                PreparedStatement ps =
                        connection.prepareStatement(SCHOOL_BUS_INSERT, new String[] {"id"});
                ps.setString(1, schoolBus.getBusNumber());
                ps.setString(2, schoolBus.getRegistrationNumber());
                ps.setInt(3, schoolBus.getCapacity());
                ps.setInt(4, schoolBus.getSchool().getSchoolId());
                ps.setInt(5,  schoolBus.getStartBusStop().getId());
                ps.setString(6, schoolBus.getMake());
                ps.setString(7,schoolBus.getModel());
                ps.setString(8, schoolBus.getBusNumber());
                ps.setInt(9, schoolBus.getCapacity());
                ps.setInt(10, schoolBus.getSchool().getSchoolId());
                ps.setInt(11,  schoolBus.getStartBusStop().getId());
                ps.setString(12, schoolBus.getMake());
                ps.setString(13,schoolBus.getModel());
                return ps;
            }
        };
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(preparedStatementCreator, keyHolder);
        int schoolBusId = 0;
        if(keyHolder.getKey() != null) {
            schoolBusId = keyHolder.getKey().intValue();
        }

        if(schoolBusId <= 0) {
            return getSchoolBus(schoolBus.getRegistrationNumber());
        } else {
            return new SchoolBus.Builder().setVehicleId(schoolBusId).setBusNumber(schoolBus.getBusNumber()).setRegistrationNumber(schoolBus.getRegistrationNumber())
                    .setCapacity(schoolBus.getCapacity()).setMake(schoolBus.getMake()).setModel(schoolBus.getModel()).setSchool(schoolBus.getSchool())
                    .setStartBusStop(schoolBus.getStartBusStop()).build();
        }
    }

    public class SchoolBusRowMapper implements RowMapper<SchoolBus> {
        @Override
        public SchoolBus mapRow(ResultSet resultSet, int i) throws SQLException {
            School school = schoolDao.getSchool(resultSet.getInt("school_id"));
            BusStop startBusStop = busStopDao.getBusPoint(resultSet.getInt("start_bus_stop_id"), true);

            SchoolBus.Builder builder = new SchoolBus.Builder();
            builder.setVehicleId(resultSet.getInt("id"));
            builder.setBusNumber(resultSet.getString("bus_number"));
            builder.setRegistrationNumber(resultSet.getString("registration_number"));
            builder.setCapacity(resultSet.getInt("capacity"));
            builder.setSchool(school);
            builder.setStartBusStop(startBusStop);
            builder.setMake(resultSet.getString("make"));
            builder.setModel(resultSet.getString("model"));

            return builder.build();
        }
    }
}
