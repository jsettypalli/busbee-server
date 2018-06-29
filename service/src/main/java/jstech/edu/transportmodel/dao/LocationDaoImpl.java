package jstech.edu.transportmodel.dao;

import jstech.edu.transportmodel.common.GeoLocation;
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
public class LocationDaoImpl implements LocationDao {
    private static final Logger logger = LoggerFactory.getLogger(LocationDao.class);

    private static final String GEO_LOCATION_SELECT1 = "select id, latitude, longitude from geo_location where id = ?";

    private static final String GEO_LOCATION_SELECT2 = "select id, latitude, longitude from geo_location " +
                                                        " where latitude = ? and longitude = ?";

    // "Do update set latitude, longitude" is not necessary, conceptually. This is done so we can get the id of the row from spring jdbc
    private static final String GEO_LOCATION_INSERT = "INSERT into geo_location (latitude, longitude) values (?, ?) " +
                                                        " ON CONFLICT (latitude, longitude) DO update set latitude = ?, longitude = ?";

    private static final String GEO_LOCATION_DELETE = "DELETE from geo_location where latitude = ? and longitude = ?";

    private static final String GEO_LOCATION_SELECT3 = "SELECT gl.id, gl.latitude, gl.longitude FROM geo_location gl " +
            " JOIN input_via_points ivp on ivp.via_point = gl.id " +
            " WHERE ivp.from_bus_stop_details_id = ? and to_bus_stop_details_id = ? " +
            " ORDER BY ivp.via_point_order";


    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public GeoLocation getLocation(int locationId) {

        List<GeoLocation> locations = jdbcTemplate.query(GEO_LOCATION_SELECT1, new GeoLocationRowMapper(), locationId);
        return locations.get(0);
    }

    @Override
    public GeoLocation getLocation(double latitude, double longitude) {
        List<GeoLocation> locations = jdbcTemplate.query(GEO_LOCATION_SELECT2, new GeoLocationRowMapper(), latitude, longitude);
        return locations == null || locations.isEmpty() ? null : locations.get(0);
    }

    @Override
    public GeoLocation addLocation(double latitude, double longitude) {

        /*below statement is specific to mysql and hence commented out as we are using postgres now
        String sql = "INSERT IGNORE into geo_location (latitude, longitude) values (?, ?) ";*/

        /*
        INSERT into geo_location (latitude, longitude) values (?, ?) " +
                                                        " ON CONFLICT (latitude, longitude) DO update set latitude = ?, longitude = ?"
         */
        //int out = jdbcTemplate.update(GEO_LOCATION_INSERT, latitude, longitude);
        PreparedStatementCreator preparedStatementCreator = new PreparedStatementCreator() {
            @Override
            public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
                PreparedStatement ps =
                        connection.prepareStatement(GEO_LOCATION_INSERT, new String[] {"id"});
                ps.setDouble(1, latitude);
                ps.setDouble(2, longitude);
                ps.setDouble(3, latitude);
                ps.setDouble(4, longitude);
                return ps;
            }
        };
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(preparedStatementCreator, keyHolder);
        int locationId = 0;
        if(keyHolder.getKey() != null) {
            locationId = keyHolder.getKey().intValue();
        }

        if(locationId <= 0) {
            return getLocation(latitude, longitude);
        } else {
            return new GeoLocation(locationId, latitude, longitude);
        }
    }

    @Override
    public GeoLocation deleteLocation(double latitude, double longitude) {
        GeoLocation location = getLocation(latitude, longitude);

        jdbcTemplate.update(GEO_LOCATION_DELETE, latitude, longitude);
        return location;
    }

    @Override
    public List<GeoLocation> getViaPointLocationsBetweenBusStopsForRouteDetermination(int fromBusStopDetailsId, int toBusStopDetailsid) {
        List<GeoLocation> locations = jdbcTemplate.query(GEO_LOCATION_SELECT3, new GeoLocationRowMapper(), fromBusStopDetailsId, toBusStopDetailsid);
        return locations;
    }

    public class GeoLocationRowMapper implements RowMapper<GeoLocation> {

        @Override
        public GeoLocation mapRow(ResultSet resultSet, int i) throws SQLException {
            int id = resultSet.getInt("id");
            double latitude = resultSet.getDouble("latitude");
            double longitude = resultSet.getDouble("longitude");
            return new GeoLocation(id, latitude, longitude);
        }
    }
}
