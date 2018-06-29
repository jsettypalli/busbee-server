package jstech.edu.transportmodel.dao;

import jstech.edu.transportmodel.common.GeoLocation;
import jstech.edu.transportmodel.common.School;
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
public class SchoolDaoImpl implements SchoolDao {
    private static final Logger logger = LoggerFactory.getLogger(SchoolDaoImpl.class);

    private static final String SCHOOL_SELECT1 = "select id, name, location_id from school where id = ?";

    private static final String SCHOOL_SELECT2 = "select id, name, location_id from school where name = ?";

    private static final String SCHOOL_INSERT = "INSERT into school (name, location_id) values (?, ?)  " +
                                                " ON CONFLICT (name) DO UPDATE set location_id = ?";

    private static final String SCHOOL_UPDATE = "UPDATE school set name = ? where id = ?";

    private static final String SCHOOL_DELETE = "delete from school where id = ?";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private LocationDao locationDao;

    @Override
    public School getSchool(int schoolId) {
        List<School> schools = jdbcTemplate.query(SCHOOL_SELECT1, new SchoolRowMapper(), schoolId);
        return schools == null || schools.isEmpty() ? null : schools.get(0);
    }

    @Override
    public School getSchool(String name) {
        List<School> schools = jdbcTemplate.query(SCHOOL_SELECT2, new SchoolRowMapper(), name);
        return schools == null || schools.isEmpty() ? null : schools.get(0);
    }

    @Override
    public School addSchool(String name, double latitude, double longitude) {
        GeoLocation location = locationDao.addLocation(latitude, longitude);
        return addSchool(name, location, null);
    }

    @Override
    public School addSchool(School school) {
        GeoLocation location = school.getLocation();
        int locationId = school.getLocation().getId();
        if(locationId <= 0) {
            location = locationDao.addLocation(school.getLocation().getLatitude(), school.getLocation().getLongitude());
            //locationId = location.getId();
        }

        return addSchool(school.getName(), location, school.getAddress());
    }

    private School addSchool(String name, GeoLocation location, String address) {
        //int out = jdbcTemplate.update(SCHOOL_INSERT, name, locationId, locationId);
        PreparedStatementCreator preparedStatementCreator = new PreparedStatementCreator() {
            @Override
            public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
                PreparedStatement ps =
                        connection.prepareStatement(SCHOOL_INSERT, new String[] {"id"});
                ps.setString(1, name);
                ps.setInt(2, location.getId());
                ps.setInt(3, location.getId());
                return ps;
            }
        };
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(preparedStatementCreator, keyHolder);
        int schoolId = 0;
        if(keyHolder.getKey() != null) {
            schoolId = keyHolder.getKey().intValue();
        }

        if(schoolId <= 0) {
            return getSchool(name);
        } else {
            School school = new School();
            school.setSchoolId(schoolId);
            school.setName(name);
            school.setAddress(address);
            school.setLocation(location);
            return school;
        }
    }

    @Override
    public School updateSchool(School school) {
        int out = jdbcTemplate.update(SCHOOL_UPDATE, school.getName(), school.getSchoolId());
        return getSchool(school.getName());
    }

    @Override
    public School deleteSchool(School school) {
        int out = jdbcTemplate.update(SCHOOL_DELETE, school.getSchoolId());
        return school;
    }

    public class SchoolRowMapper implements RowMapper<School> {
        @Override
        public School mapRow(ResultSet resultSet, int i) throws SQLException {
            int id = resultSet.getInt("id");
            String name = resultSet.getString("name");
            int locationId = resultSet.getInt("location_id");
            GeoLocation location = locationDao.getLocation(locationId);

            School school = new School();
            school.setSchoolId(id);
            school.setName(name);
            school.setLocation(location);
            return school;
        }
    }
}
