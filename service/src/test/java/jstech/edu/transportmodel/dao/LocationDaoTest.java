package jstech.edu.transportmodel.dao;

import jstech.edu.transportmodel.common.GeoLocation;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.test.context.ContextConfiguration;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class LocationDaoTest {

    private JdbcTemplate jdbcTemplate;

    //@Before
    public void setup() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();

        dataSource.setDriverClassName("org.postgresql.Driver");
        dataSource.setUrl("jdbc:postgresql://localhost:5432/school_transport");
        dataSource.setUsername("postgres");
        dataSource.setPassword("postgres");

        jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.setResultsMapCaseInsensitive(true);
    }

    //@Test
    public void testKeyGeneration() {
        String sql = "INSERT into geo_location (latitude, longitude) values (?, ?) " +
                " ON CONFLICT (latitude, longitude) DO update set latitude = ?, longitude = ?";

        //int out = jdbcTemplate.update(sql, 17.5, 74.5);
        PreparedStatementCreator preparedStatementCreator = new PreparedStatementCreator() {
            @Override
            public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
                PreparedStatement ps =
                        connection.prepareStatement(sql, new String[] {"id"});
                ps.setDouble(1, 17.5);
                ps.setDouble(2, 74.5);
                ps.setDouble(3, 17.5);
                ps.setDouble(4, 74.5);
                return ps;
            }
        };

//        SqlParameterSource fileParameters = new BeanPropertySqlParameterSource(new GeoLocation(17.5, 74.5));
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(preparedStatementCreator, keyHolder);
        int id = 0;
        if(keyHolder.getKey() != null) {
            id = keyHolder.getKey().intValue();
        }
        Assert.assertTrue(id > 0);
    }
}
