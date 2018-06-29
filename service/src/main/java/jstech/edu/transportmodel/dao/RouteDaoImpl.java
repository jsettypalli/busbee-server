package jstech.edu.transportmodel.dao;

import com.graphhopper.jsprit.core.problem.solution.route.VehicleRoute;
import com.graphhopper.jsprit.core.problem.solution.route.activity.Start;
import com.graphhopper.jsprit.core.problem.solution.route.activity.TourActivity;
import com.graphhopper.jsprit.core.problem.vehicle.Vehicle;
import jstech.edu.transportmodel.common.*;
import jstech.edu.transportmodel.dto.DistanceDurationDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.scheduling.support.CronSequenceGenerator;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.sql.*;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Repository
public class RouteDaoImpl implements RouteDao {

    private static final Logger logger = LoggerFactory.getLogger(SchoolBusDao.class);

    private static final String BUS_TRIP_SELECT1 = "SELECT id from trip where name = ? and school_id = ? and is_pickup = ?";

    private static final String BUS_TRIP_SELECT2 = "SELECT id, schedule from trip where approved = true";

    private static final String BUS_TRIP_SELECT3 = "SELECT schedule from trip where trip_id = ?";

    private static final String BUS_TRIP_INSERT = "INSERT into trip (name, schedule, school_id, is_pickup) " +
                                                    " values (?, ?, ?, ?) " +
                                                    " ON CONFLICT (name, school_id, is_pickup) " +
                                                    " DO update set schedule = ? ";

    private static final String BUS_TRIP_UPDATE = "UPDATE trip set approved = true where id = ? ";

    private static final String ROUTE_PLAN_SELECT_PART1 =
                    "select rp.trip_id as trip_id, rp.starting_point, rp.destination_point, rp.expected_arrival_time, rp.expected_departure_time, " +
                    " rp.actual_arrival_time, rp.actual_departure_time, rp.vehicle_status, " +
                    " t.name as trip_name, t.school_id, t.schedule as trip_schedule, t.approved as trip_approved, " +
                    " v.id as vehicle_id, " +
                    " bp.id as bus_stop_id, bpl.is_pickup " +
                    " from route_plan rp " +
                    " join trip t on t.id = rp.trip_id " +
                    " join vehicle v on v.id = rp.vehicle_id " +
                    " join bus_stop_details bpl on bpl.id = rp.bus_stop_details_id and bpl.is_pickup = t.is_pickup " +
                    " join bus_stop bp on bp.id = bpl.bus_stop_id and bp.school_id = t.school_id ";

    private static final String ROUTE_PLAN_SELECT_PART2 = " where t.approved = true order by rp.trip_id, v.id, rp.bus_stop_order";

    private static final String ROUTE_PLAN_SELECT_PART3 = " where rp.trip_id = ?" +
                                                            " order by rp.trip_id, v.id, rp.bus_stop_order";

    private static final String ROUTE_PLAN_SELECT_PART4 = " where t.school_id = ? order by rp.trip_id, v.id, rp.bus_stop_order";

    private static final String ROUTE_PLAN_SELECT_PART5 = " where rp.vehicle_id in (?) order by rp.trip_id, v.id, rp.bus_stop_order";

    private static final String ROUTE_PLAN_INSERT = "INSERT into route_plan (trip_id, vehicle_id, bus_stop_details_id, bus_stop_order, " +
                                                    " starting_point, destination_point, expected_arrival_time, expected_departure_time) " +
                                                    " values (?, ?, ?, ?, ?, ?, ?, ?) " +
                                                    " ON CONFLICT (trip_id, vehicle_id, bus_stop_details_id, bus_stop_order) " +
                                                    " DO update set starting_point=?, destination_point = ?, " +
                                                    " expected_arrival_time=?, expected_departure_time=?";

    private static final String ROUTE_PLAN_UPDATE_ACTUAL_ARRIVAL_TIME = "UPDATE route_plan set actual_arrival_time = ? " +
            " where trip_id = ? and vehicle_id = ? and bus_stop_details_id = ?";

    private static final String ROUTE_PLAN_UPDATE_ACTUAL_DEPARTURE_TIME = "UPDATE route_plan set actual_departure_time = ? " +
            " where trip_id = ? and vehicle_id = ? and bus_stop_details_id = ?";

    private static final String ROUTE_PLAN_UPDATE_VEHICLE_STATUS = "UPDATE route_plan set vehicle_status = ? " +
            " where trip_id = ? and vehicle_id = ?";

    private static final String ROUTE_MAP_SELECT_PART1 =
                    "select rm.trip_id as trip_id, rm.starting_point, rm.start_time, rm.destination_point, " +
                    " t.name as trip_name, t.school_id, t.schedule as trip_schedule, t.approved as trip_approved, " +
                    " v.id as vehicle_id, " +
                    " bp.id as bus_stop_id, bpl.is_pickup " +
                    " from route_map rm " +
                    " join trip t on t.id = rm.trip_id " +
                    " join vehicle v on v.id = rm.vehicle_id " +
                    " join bus_stop_details bpl on bpl.id = rm.bus_stop_details_id and bpl.is_pickup = t.is_pickup " +
                    " join bus_stop bp on bp.id = bpl.bus_stop_id and bp.school_id = t.school_id ";

    private static final String ROUTE_MAP_SELECT_PART2 = " where rm.trip_id = ? order by rm.trip_id, v.id, rm.bus_stop_order";

    private static final String ROUTE_MAP_SELECT2 = "SELECT trip_id, vehicle_id, bus_stop_details_id, bus_stop_order, starting_point, " +
                                                    " start_time, destination_point, relative_arrival_time_secs, relative_departure_time_secs " +
                                                    " FROM route_map " +
                                                    " WHERE trip_id = ? order by bus_stop_order";

    private static final String ROUTE_MAP_SELECT3 = "WITH route_map_unique as " +
                                    " (SELECT trip_id, vehicle_id, bus_stop_details_id, max(bus_stop_order) as bus_stop_order FROM route_map " +
                                    "       WHERE trip_id = ? and vehicle_id = ? and bus_stop_details_id = ? " +
                                    "       GROUP BY trip_id, vehicle_id, bus_stop_details_id) " +
                                    " SELECT via_points FROM route_map rm JOIN route_map_unique " +
                                    "  on rm.trip_id = route_map_unique.trip_id " +
                                    " and rm.vehicle_id = route_map_unique.vehicle_id " +
                                    " and rm.bus_stop_details_id = route_map_unique.bus_stop_details_id " +
                                    " and rm.bus_stop_order = route_map_unique.bus_stop_order";


    private static final String ROUTE_MAP_INSERT = "INSERT INTO route_map (trip_id, vehicle_id, bus_stop_details_id, bus_stop_order, " +
                                                    " starting_point, destination_point, relative_arrival_time_secs, relative_departure_time_secs, start_time) " +
                                                    " values (?, ?, ?, ?, ?, ?, ?, ?, ? ) " +
                                                    " ON CONFLICT (trip_id, vehicle_id, bus_stop_details_id, bus_stop_order) " +
                                                    " DO UPDATE SET  starting_point = ? , destination_point = ?, " +
                                                    " relative_arrival_time_secs = ?, relative_departure_time_secs = ?, start_time = ? ";

    private static final String ROUTE_MAP_UPDATE1 = "UPDATE route_map set relative_distance_mtrs = ? " +
                                                        " where trip_id = ? and vehicle_id = ? and bus_stop_details_id = ? ";

    private static final String ROUTE_MAP_UPDATE2 = "WITH route_map_unique as " +
                                " (SELECT trip_id, vehicle_id, bus_stop_details_id, max(bus_stop_order) as bus_stop_order FROM route_map " +
                                "       WHERE trip_id = ? and vehicle_id = ? and bus_stop_details_id = ?" +
                                "       GROUP BY trip_id, vehicle_id, bus_stop_details_id) " +
                                " UPDATE route_map rm set via_points = ? FROM route_map_unique " +
                                " WHERE rm.trip_id = route_map_unique.trip_id " +
                                "   and rm.vehicle_id = route_map_unique.vehicle_id " +
                                "   and rm.bus_stop_details_id = route_map_unique.bus_stop_details_id " +
                                "   and rm.bus_stop_order = route_map_unique.bus_stop_order";


    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SchoolDao schoolDao;

    @Autowired
    private SchoolBusDao schoolBusDao;

    @Autowired
    private BusStopDao busStopDao;

    // SHOULD BE REMOVED AFTER DEV TESTING. SHOULD PROBABLY FIND A BETTER WAY TO SET DEV MODE.
    public static String appMode;
    @Value("${app_mode:}")
    public void setAppMode(String mode) {
        appMode = mode;
    }

    private ConcurrentMap<Integer, BusTrip> trips = new ConcurrentHashMap<>();

    @Override
    public List<BusTrip> fetchAllTripsFromDB() {
        String sql = ROUTE_PLAN_SELECT_PART1 + ROUTE_PLAN_SELECT_PART2;
        FetchTripRowCallBackHandler fetchTripRowCallBackHandler = new FetchTripRowCallBackHandler();
        jdbcTemplate.query(sql, fetchTripRowCallBackHandler);
        return fetchTripRowCallBackHandler.getTrips();
    }

    public BusTrip getTripFromRoutePlan(int tripId) {
        if(trips.containsKey(tripId)) {
            return trips.get(tripId);
        }

        BusTrip trip = fetchTripFromDb(tripId);
        if(trip == null) {
            return null;
        }

        BusTrip mappedTripObj = trips.putIfAbsent(trip.getTripId(), trip);

        // This is done to handle concurrent condition. If two threads call this method with same tripId
        //  and both threads fetch trip from database, the first one that gets added to "trips" map will take precedence.
        //  The trip object from next thread will be ignored.
        //  "putIfAbsent" method returns null if no object is mapped to given tripId
        //  and returns existing value if object is already mapped to given tripId.
        return mappedTripObj == null ? trip : mappedTripObj;
    }

    private BusTrip fetchTripFromDb(int tripId) {
        String sql = ROUTE_PLAN_SELECT_PART1 + ROUTE_PLAN_SELECT_PART3;
        FetchTripRowCallBackHandler fetchTripRowCallBackHandler = new FetchTripRowCallBackHandler();
        jdbcTemplate.query(sql, fetchTripRowCallBackHandler, tripId);
        List<BusTrip> trips = fetchTripRowCallBackHandler.getTrips();
        return trips == null || trips.isEmpty() ? null : trips.get(0);
    }

    @Override
    public List<BusTrip> getTrips(School school) {
        String sql = ROUTE_PLAN_SELECT_PART1 + ROUTE_PLAN_SELECT_PART4;
        FetchTripRowCallBackHandler fetchTripRowCallBackHandler = new FetchTripRowCallBackHandler();
        jdbcTemplate.query(sql, fetchTripRowCallBackHandler, school.getSchoolId());
        return fetchTripRowCallBackHandler.getTrips();
    }

    @Override
    public List<BusTrip> getTrips(List<SchoolBus> schoolBuses) {
        if(schoolBuses == null || schoolBuses.isEmpty()) {
            return new ArrayList<>();
        }
        StringBuilder builder = new StringBuilder();
        for(SchoolBus bus: schoolBuses) {
            if(builder.length() > 0) {
                builder.append(", ");
            }
            builder.append(bus.getVehicleId());
        }
        String sql = ROUTE_PLAN_SELECT_PART1 + ROUTE_PLAN_SELECT_PART5;
        FetchTripRowCallBackHandler fetchTripRowCallBackHandler = new FetchTripRowCallBackHandler();
        sql = sql.replace("?", builder.toString());
        jdbcTemplate.query(sql, fetchTripRowCallBackHandler);
        return fetchTripRowCallBackHandler.getTrips();
    }

    @Override
    public BusTrip getTripFromRouteMap(int tripId) {
        String sql = ROUTE_MAP_SELECT_PART1 + ROUTE_MAP_SELECT_PART2;
        FetchTripRowCallBackHandler fetchTripRowCallBackHandler = new FetchTripRowCallBackHandler();
        fetchTripRowCallBackHandler.setExpectedTimes(false);
        jdbcTemplate.query(sql, fetchTripRowCallBackHandler, tripId);
        List<BusTrip> trips = fetchTripRowCallBackHandler.getTrips();
        return trips.isEmpty() ? null : trips.get(0);
    }


    public int saveTrip(String tripName, String schedule, School school, boolean pickup, Collection<VehicleRoute> routes) {
        /* BELOW SQL IS MYSQL SPECIFIC.
        String sql = "INSERT IGNORE INTO vehicle_route (vehicle_id, bus_point_id, bus_point_order) values (?, ?, ?)" +
                " on duplicate key update pickup_order = ? ";
        */

        int tripId = createTrip(tripName, schedule, school, pickup);

        int count = 1;
        for(VehicleRoute route: routes) {
            logger.debug("Persisting Route - {}", count++);

            int order = 1;
            Vehicle vehicle = route.getVehicle();
            int vehicleId = Integer.parseInt(vehicle.getId());

            Start start = route.getStart();
            int start_id = Integer.parseInt(start.getLocation().getId());

            // TODO - set start time of the route (i.e, start time of the bus). It should be input to the route generation tool
            LocalTime startTime = null;
            int out = jdbcTemplate.update(ROUTE_MAP_INSERT, tripId, vehicleId, start_id, order, true, false, 0, start.getEndTime(), startTime,
                    true, false, 0, start.getEndTime(), startTime);
            order++;
            logger.debug("Route-Id: {}, Start-Id: {}", tripId, start_id);

            for(TourActivity activity: route.getActivities()) {
                int pickPointId = Integer.parseInt(activity.getLocation().getId());
                out = jdbcTemplate.update(ROUTE_MAP_INSERT, tripId, vehicleId, pickPointId, order, false, false, activity.getArrTime(), activity.getEndTime(), null,
                        false, false, activity.getArrTime(), activity.getEndTime(),  null);
                order++;
                logger.debug("Route-Id: {}, Pickup-Point-Id: {}", tripId, pickPointId);
            }

            int end_id = Integer.parseInt(route.getEnd().getLocation().getId());
            out = jdbcTemplate.update(ROUTE_MAP_INSERT, tripId, vehicleId, end_id, order, false, true, route.getEnd().getArrTime(), route.getEnd().getEndTime(), null,
                    false, true, route.getEnd().getArrTime(), route.getEnd().getEndTime(), null);
            //order++;
            logger.debug("Route-Id: {}, End-Id: {}", tripId, end_id);
        }

        return tripId;
    }

    private int createTrip(String tripName, String schedule, School school, boolean pickup) {
        //int out = jdbcTemplate.update(BUS_TRIP_INSERT, tripName, schedule, school.getSchoolId(), pickup, schedule);
        PreparedStatementCreator preparedStatementCreator = new PreparedStatementCreator() {
            @Override
            public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
                PreparedStatement ps =
                        connection.prepareStatement(BUS_TRIP_INSERT, new String[] {"id"});
                ps.setString(1, tripName);
                ps.setString(2, schedule);
                ps.setInt(3, school.getSchoolId());
                ps.setBoolean(4, pickup);
                ps.setString(5, schedule);
                return ps;
            }
        };
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(preparedStatementCreator, keyHolder);
        int tripId = 0;
        if(keyHolder.getKey() != null) {
            tripId = keyHolder.getKey().intValue();
        }

        if(tripId <= 0) {
            tripId = jdbcTemplate.queryForObject(BUS_TRIP_SELECT1, new Object[] {tripName, school.getSchoolId(), pickup}, Integer.class);
        }

        return tripId;
    }

    @Override
    public int saveTrip(BusTrip trip) {
        School school = schoolDao.addSchool(trip.getSchool());
        int tripId = createTrip(trip.getName(), trip.getSchedule(), school, trip.isPickup());
        trip.setTripId(tripId);

        for(Map.Entry<SchoolBus, SchoolBusRoute> entry: trip.getBusRoutes().entrySet()) {
            SchoolBus schoolBus = entry.getKey();
            SchoolBusRoute busRoute = entry.getValue();

            logger.debug("Adding Route: {} for SchoolBus: {}", busRoute.getName(), schoolBus);

            schoolBus = schoolBusDao.addSchoolBus(schoolBus);

            int order = 1;

            BusStop addedStartingPoint = busStopDao.addBusPoint(busRoute.getStartingPoint());
            busRoute.setStartingPoint(addedStartingPoint);
            int out = jdbcTemplate.update(ROUTE_MAP_INSERT, tripId, schoolBus.getVehicleId(), addedStartingPoint.getBusStopDetailId(),
                    order, true, false, 0, addedStartingPoint.getRelativeDepartureTimeSecs(), busRoute.getStartTime(),
                    true, false, 0, addedStartingPoint.getRelativeDepartureTimeSecs(), busRoute.getStartTime());
            order++;

            List<BusStop> addedBusStops = new ArrayList<>();
            for(BusStop busStop: busRoute.getBusStops()) {
                BusStop addedStop = busStopDao.addBusPoint(busStop);
                addedBusStops.add(addedStop);

                out = jdbcTemplate.update(ROUTE_MAP_INSERT, tripId, schoolBus.getVehicleId(), addedStop.getBusStopDetailId(),
                        order, false, false, addedStop.getRelativeArrivalTimeSecs(), addedStop.getRelativeDepartureTimeSecs(), null,
                        false, false, addedStop.getRelativeArrivalTimeSecs(), addedStop.getRelativeDepartureTimeSecs(), null);
                order++;
            }

            BusStop addedDestinationPoint = busStopDao.addBusPoint(busRoute.getDestination());
            busRoute.setDestination(addedDestinationPoint);
            out = jdbcTemplate.update(ROUTE_MAP_INSERT, tripId, schoolBus.getVehicleId(), addedDestinationPoint.getBusStopDetailId(),
                    order, false, true, addedDestinationPoint.getRelativeArrivalTimeSecs(), addedDestinationPoint.getRelativeDepartureTimeSecs(), null,
                    false, true, addedDestinationPoint.getRelativeArrivalTimeSecs(), addedDestinationPoint.getRelativeDepartureTimeSecs(), null);

            busRoute.setBusStops(addedBusStops);
        }
        //approveTrip(tripId);
        return tripId;
    }

    @Override
    public boolean approveTrip(int tripId) {
        int out = jdbcTemplate.update(BUS_TRIP_UPDATE, tripId);

        return out == 1;

        //TODO - Generate route plan of the approved trip. This covers the case when trip starts on the same day of creation/approval
        //  route-plan is otherwise generated every morning @2/3am. This covers intra-day changes to the trip.
    }

    @Override
    public void createRoutePlan() {
        List<Map<String, Object>> trips = jdbcTemplate.queryForList(BUS_TRIP_SELECT2);

        for(Map<String, Object> trip: trips) {
            int tripId = Integer.parseInt(trip.get("id").toString());
            String schedule = trip.get("schedule").toString();
            createRoutePlan(tripId, schedule);
        }
    }

    @Override
    public void createRoutePlan(int tripId, String schedule) {
        if(tripId <= 0) {
            logger.warn("Invalid tripId:{}. route plan can't be created.", tripId);
            return;
        }

        if(!StringUtils.hasText(schedule)) {
            schedule = jdbcTemplate.queryForObject(BUS_TRIP_SELECT3, String.class, tripId);
        }

        if(!StringUtils.hasText(schedule)) {
            logger.warn("Invalid trip schedule:{}. route plan can't be created.", schedule);
            return;
        }

        ZonedDateTime startDateTime = getTripStartTime(schedule);

        // if startTime is null, that means this service is not active for that day
        if(startDateTime == null) {
            return;
        }

        List<Map<String, Object>> results = jdbcTemplate.queryForList(ROUTE_MAP_SELECT2, tripId);
        for(Map<String, Object> result: results) {
            // TODO - Should this be changed to batchUpdate???
            Object obj = result.get("start_time");
            if(obj != null) {
                LocalTime startTime =   LocalTime.parse(obj.toString());
                startDateTime = ZonedDateTime.of(startDateTime.toLocalDate(), startTime, startDateTime.getZone());
            }
            if(StringUtils.hasText(appMode) && appMode.equalsIgnoreCase("dev")) {
                startDateTime = ZonedDateTime.now().plusMinutes(3);
            }

            int relArrivalTimeSecs = Integer.parseInt(result.get("relative_arrival_time_secs").toString());
            int relDepartureTimeSecs = Integer.parseInt(result.get("relative_departure_time_secs").toString());

            ZonedDateTime pickupPointArrivalTime = startDateTime.plusSeconds(relArrivalTimeSecs);
            ZonedDateTime pickupPointDepartureTime = startDateTime.plusSeconds(relDepartureTimeSecs);

            // TODO - Should we have published_arrival_time column too? This would hold time that will be published to parents.

            int out = jdbcTemplate.update(ROUTE_PLAN_INSERT,
                    new Object[] {
                            Integer.parseInt(result.get("trip_id").toString()),
                            Integer.parseInt(result.get("vehicle_id").toString()),
                            Integer.parseInt(result.get("bus_stop_details_id").toString()),
                            Integer.parseInt(result.get("bus_stop_order").toString()),
                            Boolean.parseBoolean(result.get("starting_point").toString()),
                            Boolean.parseBoolean(result.get("destination_point").toString()),
                            pickupPointArrivalTime.toOffsetDateTime(),
                            pickupPointDepartureTime.toOffsetDateTime(),
                            Boolean.parseBoolean(result.get("starting_point").toString()),
                            Boolean.parseBoolean(result.get("destination_point").toString()),
                            pickupPointArrivalTime.toOffsetDateTime(),
                            pickupPointDepartureTime.toOffsetDateTime()
                    },
                    new int[] {
                            Types.INTEGER, Types.INTEGER, Types.INTEGER, Types.INTEGER,
                            Types.BOOLEAN, Types.BOOLEAN, Types.TIMESTAMP_WITH_TIMEZONE, Types.TIMESTAMP_WITH_TIMEZONE,
                            Types.BOOLEAN, Types.BOOLEAN, Types.TIMESTAMP_WITH_TIMEZONE,
                            Types.TIMESTAMP_WITH_TIMEZONE
                    });
        }
    }

    private ZonedDateTime getTripStartTime(String schedule) {
        CronSequenceGenerator cronTrigger = new CronSequenceGenerator(schedule);
        Date next = cronTrigger.next(new Date());
        final ZoneId systemDefault = ZoneId.systemDefault();
        logger.info("Next Runtime is:"+ZonedDateTime.ofInstant(next.toInstant(), systemDefault));
        return ZonedDateTime.ofInstant(next.toInstant(), systemDefault);
    }

    private class FetchTripRowCallBackHandler implements RowCallbackHandler {
        List<BusTrip> trips = new ArrayList<>();

        /*  expected arrival & expected departure times are not known when data is fetched from route_map.
         *  set this param to true, if above values are expected (basically when fetched from route_plan table).
         */
        boolean expectedTimes = true;

        int prevTripId = -1;
        BusTrip currentTrip;

        int prevVehicleId = -1;
        SchoolBus currentBus;

        List<BusTrip> getTrips() {
            if(! trips.isEmpty()) {
                setTripStartTime();
            }
            return trips;
        }

        void setExpectedTimes(boolean expectedTimes) {
            this.expectedTimes = expectedTimes;
        }

        private void setTripStartTime() {
            // set the time at which the first bus starts as starting time of the trip
            for(BusTrip trip: trips) {
                ZonedDateTime tripStartTime = null;
                for(Map.Entry<SchoolBus, SchoolBusRoute> entry: trip.getBusRoutes().entrySet()) {
                    SchoolBusRoute busRoute = entry.getValue();
                    if(tripStartTime == null || busRoute.getStartDateTime().isBefore(tripStartTime)) {
                        tripStartTime = busRoute.getStartDateTime();
                    }
                }
                trip.setStartTime(tripStartTime);
            }
        }

        @Override
        public void processRow(ResultSet resultSet) throws SQLException {
            int tripId = resultSet.getInt("trip_id");
            if(tripId != prevTripId) {
                prevTripId = tripId;
                currentTrip = new BusTrip();
                currentTrip.setTripId(tripId);
                currentTrip.setName(resultSet.getString("trip_name"));
                currentTrip.setSchedule(resultSet.getString("trip_schedule"));
                currentTrip.setApproved(resultSet.getBoolean("trip_approved"));
                int schoolId = resultSet.getInt("school_id");
                School school = schoolDao.getSchool(schoolId);
                currentTrip.setSchool(school);

                trips.add(currentTrip);
            }

            int vehicleId = resultSet.getInt("vehicle_id");
            if(vehicleId != prevVehicleId) {
                prevVehicleId = vehicleId;
                currentBus = schoolBusDao.getSchoolBus(vehicleId);
                SchoolBusRoute busRoute = new SchoolBusRoute();
                // TODO - check if vehicleId should be set here or route_plan.id?
                busRoute.setId(vehicleId);

                busRoute.setName(currentBus.getBusNumber());
                currentTrip.addBusRoute(currentBus, busRoute);
            }

            BusStop busStop = busStopDao.getBusPoint(resultSet.getInt("bus_stop_id"),
                                                        resultSet.getBoolean("is_pickup"));

            LocalTime routeStartTime = null;
            ZonedDateTime expectedArrivalTime = null;
            ZonedDateTime expectedDepartureTime = null;
            ZonedDateTime actualArrivalTime = null;
            ZonedDateTime actualDepartureTime = null;
            if(this.expectedTimes) {
                expectedArrivalTime = ZonedDateTime.ofInstant(
                        resultSet.getTimestamp("expected_arrival_time").toInstant(), ZoneId.systemDefault());
                expectedDepartureTime = ZonedDateTime.ofInstant(
                        resultSet.getTimestamp("expected_departure_time").toInstant(), ZoneId.systemDefault());

                Timestamp actualArrivalTimeStamp = resultSet.getTimestamp("actual_arrival_time");
                if(actualArrivalTimeStamp != null) {
                    actualArrivalTime = ZonedDateTime.ofInstant(actualArrivalTimeStamp.toInstant(), ZoneId.systemDefault());
                }

                Timestamp actualDepartureTimeStamp = resultSet.getTimestamp("actual_departure_time");
                if(actualDepartureTimeStamp != null) {
                    actualDepartureTime = ZonedDateTime.ofInstant(actualDepartureTimeStamp.toInstant(), ZoneId.systemDefault());
                }

                busStop.setExpectedArrivalTime(expectedArrivalTime);
                busStop.setExpectedDepartureTime(expectedDepartureTime);
                busStop.setActualArrivalTime(actualArrivalTime);
                busStop.setActualDepartureTime(actualDepartureTime);

                String status = resultSet.getString("vehicle_status");
                if(StringUtils.hasText(status)) {
                    currentTrip.getBusRoute(currentBus).setRouteStatus(RouteStatus.getRouteStatus(status));
                }
            } else {
                routeStartTime = resultSet.getTime("start_time").toLocalTime();
            }

            boolean boolStartingPoint = resultSet.getBoolean("starting_point");
            boolean boolDestinationPoint = resultSet.getBoolean("destination_point");
            if(boolStartingPoint) {
                currentTrip.getBusRoute(currentBus).setStartingPoint(busStop);
                currentTrip.getBusRoute(currentBus).setStartTime(routeStartTime);
                currentTrip.getBusRoute(currentBus).setStartDateTime(expectedDepartureTime);
                if(expectedTimes) {
                    currentTrip.setStartTime(expectedDepartureTime);
                }
            } else if(boolDestinationPoint) {
                currentTrip.getBusRoute(currentBus).setDestination(busStop);
                currentTrip.getBusRoute(currentBus).setEndTime(expectedArrivalTime);
            } else {
                currentTrip.getBusRoute(currentBus).addPickupPoint(busStop);
            }
        }
    }

    @Override
    public boolean updateDistanceAndDurationBetweenBusStops(BusTrip trip, SchoolBus schoolBus, List<DistanceDurationDto> distanceDurationDtos) {

        int[] updateCounts = jdbcTemplate.batchUpdate(ROUTE_MAP_UPDATE1, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                ps.setInt(1, distanceDurationDtos.get(i).getDistanceInMeters().intValue());
                ps.setInt(2, trip.getTripId());
                ps.setInt(3, schoolBus.getVehicleId());
                ps.setInt(4, distanceDurationDtos.get(i).getToBusStop().getBusStopDetailId());
            }

            @Override
            public int getBatchSize() {
                return distanceDurationDtos.size();
            }
        });

        return true;
    }

    @Override
    public void setActualArrivalTime(BusTrip trip, SchoolBus vehicle, BusStop currentBusStop, ZonedDateTime actualArrivalTime) {
        jdbcTemplate.update(ROUTE_PLAN_UPDATE_ACTUAL_ARRIVAL_TIME,
                        new Object[] {
                                actualArrivalTime.toOffsetDateTime(),
                                trip.getTripId(),
                                vehicle.getVehicleId(),
                                currentBusStop.getBusStopDetailId()
                        },
                        new int[] {
                                Types.TIMESTAMP_WITH_TIMEZONE,
                                Types.INTEGER,
                                Types.INTEGER,
                                Types.INTEGER
                        });
    }

    @Override
    public void setActualDepartureTime(BusTrip trip, SchoolBus vehicle, BusStop currentBusStop, ZonedDateTime actualDepartureTime) {
        jdbcTemplate.update(ROUTE_PLAN_UPDATE_ACTUAL_DEPARTURE_TIME,
                new Object[] {
                        actualDepartureTime.toOffsetDateTime(),
                        trip.getTripId(),
                        vehicle.getVehicleId(),
                        currentBusStop.getBusStopDetailId()
                },
                new int[] {
                        Types.TIMESTAMP_WITH_TIMEZONE,
                        Types.INTEGER,
                        Types.INTEGER,
                        Types.INTEGER
                });
    }

    @Override
    public void setRouteStatus(BusTrip trip, SchoolBus vehicle, RouteStatus routeStatus) {
        jdbcTemplate.update(ROUTE_PLAN_UPDATE_VEHICLE_STATUS, routeStatus.toString(), trip.getTripId(), vehicle.getVehicleId());
    }

    @Override
    public String getDetailedViaPointsBetweenBusStopsFromGeoService(BusTrip trip, SchoolBus schoolBus, BusStop busStop) {
        String dbViapoints = jdbcTemplate.queryForObject(ROUTE_MAP_SELECT3, String.class, trip.getTripId(),
                schoolBus.getVehicleId(), busStop.getBusStopDetailId());
        return dbViapoints;
    }

    @Override
    public boolean updateDetailedViaPointsBetweenBusStopsFromGeoService(BusTrip trip, SchoolBus schoolBus, BusStop busStop, String viaPoints) {
        String dbViapoints = getDetailedViaPointsBetweenBusStopsFromGeoService(trip, schoolBus, busStop);
        if(!StringUtils.hasText(dbViapoints)) {
            int out = jdbcTemplate.update(ROUTE_MAP_UPDATE2, trip.getTripId(), schoolBus.getVehicleId(), busStop.getBusStopDetailId(), viaPoints);
            return out > 0;
        }
        return true;
    }
}
