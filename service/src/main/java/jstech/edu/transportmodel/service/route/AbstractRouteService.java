package jstech.edu.transportmodel.service.route;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table;
import javafx.util.Pair;
import jstech.edu.transportmodel.common.*;
import jstech.edu.transportmodel.dao.LocationDao;
import jstech.edu.transportmodel.dao.RouteDao;
import jstech.edu.transportmodel.GeoException;
import jstech.edu.transportmodel.dao.RouteDaoImpl;
import jstech.edu.transportmodel.dto.DistanceDurationDto;
import jstech.edu.transportmodel.dto.NotificationMessageDto;
import jstech.edu.transportmodel.service.GeographicalService;
import jstech.edu.transportmodel.service.SchoolBusService;
import jstech.edu.transportmodel.service.NotificationService;
import jstech.edu.transportmodel.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractRouteService implements RouteService {

    private static final Logger logger = LoggerFactory.getLogger(AbstractRouteService.class);

    private static final int TEN_SECONDS = 10;
    private static final int THIRTY_SECONDS = 30;
    private static final int ONE_MINUTE = 60;
    private static final int FIVE_MINUTES = 300;
    private static final int TEN_MINUTES = 600;
    private static final int FIFTEEN_MINUTES = 900;

    private static final int PICKUP_DELAY_SECS = 3 * 60;

    private static final int THIRTY_METERS = 30;

    @Autowired
    private GeographicalService geoService;

    @Autowired
    private SchoolBusService schoolBusService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserService userService;

    @Autowired
    private RouteDao  routeDao;

    @Autowired
    private LocationDao locationDao;

    private static final int PICKUP_WAIT_TIME_SECS = 0;

    // All below variables are unmodifiable/immutable. They will throw exception if an operation to modify them is called.
    private List<BusTrip> trips = new ArrayList<>();
    private Map<Integer, BusTrip> tripMap = new ConcurrentHashMap<>();
    private Map<Integer, SchoolBus> idToVehicleMap = new ConcurrentHashMap<>();

    private Map<String, List<BusStop>> tobeVisitedBusStops = new ConcurrentHashMap<>();
    private Map<String, List<BusStop>> visitedBusStops = new ConcurrentHashMap<>();
    //private Table<BusTrip, SchoolBus, List<BusStop>> tobeVisitedBusStops = HashBasedTable.create();
    //private Table<BusTrip, SchoolBus, List<BusStop>> visitedBusStops = HashBasedTable.create();

    // These are mutable objects and thread safe
    private Map<SchoolBus, Driver> schoolBusDriverMap = new ConcurrentHashMap<>();
    private Set<String> startTrackingTopics = ConcurrentHashMap.newKeySet();
    private Set<String> arrivalNotificationTopics = ConcurrentHashMap.newKeySet();
    private Set<String> escalateStartTrackingDelayTopics = ConcurrentHashMap.newKeySet();
    private Set<String> delayNotificationTopics = ConcurrentHashMap.newKeySet();

    // This is mutable object and should NEVER be returned as is.
    // getCurrentBusLocation() method returns location for given BusTrip & SchoolBus objects.
    private Map<String, GeoLocation> currentBusLocation = new ConcurrentHashMap<>();
    private Map<String, DistanceDurationDto> distanceDurationBetweenBusStops = new ConcurrentHashMap<>();
    //private Table<BusTrip, SchoolBus, GeoLocation> currentBusLocation = HashBasedTable.create();
    //private Table<BusStop, BusStop, DistanceDurationDto> distanceDurationBetweenBusStops = HashBasedTable.create();

    private Set<String> delayedBuses = ConcurrentHashMap.newKeySet();
    private Set<String> pushNotificationsSentTopics = ConcurrentHashMap.newKeySet();
    private Map<String, UpcomingBusStopDistanceDuration> upcomingBusStopInfo = new ConcurrentHashMap<>();
    //private Table<BusTrip, SchoolBus, UpcomingBusStopDistanceDuration> upcomingBusStopInfo = HashBasedTable.create();

    private ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    private void refreshData() {
        Map<Integer, BusTrip> locTripMap = new ConcurrentHashMap<>();
        Map<Integer, SchoolBus> locIdToVehicleMap = new ConcurrentHashMap<>();
        Map<SchoolBus, Driver> locSchoolBusDriverMap = new ConcurrentHashMap<>();
        Map<String, List<BusStop>> locToBeVisitedBusStops = new ConcurrentHashMap<>();
        Map<String, List<BusStop>> locVisitedBusStops = new ConcurrentHashMap<>();
        //Table<BusTrip, SchoolBus, List<BusStop>> locToBeVisitedBusStops = HashBasedTable.create();
        //Table<BusTrip, SchoolBus, List<BusStop>> locVisitedBusStops = HashBasedTable.create();

        List<BusTrip> locBusTrips = fetchAllTripsFromDB();
        if(! CollectionUtils.isEmpty(locBusTrips)) {
            for(BusTrip trip: locBusTrips) {
                locTripMap.put(trip.getTripId(), trip);
                loadTripDetails(trip, locIdToVehicleMap, locSchoolBusDriverMap, locToBeVisitedBusStops, locVisitedBusStops);
            }

            try {
                distanceDurationBetweenBusStops = new ConcurrentHashMap<>();
                //TODO - call this method just before trip starts. This is done in StartBusNotificationJob.java
                updateDistanceAndDurationBetweenBusStops(locBusTrips);
            } catch (GeoException ex) {
                logger.error("GeoException occurred while updating distance and duration between bus stops. Message:{}", ex.getLocalizedMessage(), ex);
            }

            // remove previous day's entries from delayedBuses
            String today = LocalDate.now().toString();
            delayedBuses.removeIf((String delayedBus) -> !delayedBus.startsWith(today));
            /*for(Iterator<String> iter = delayedBuses.iterator(); iter.hasNext();) {
                if(!iter.next().startsWith(today)) {
                    iter.remove();
                }
            }*/

            synchronized (this) {
                trips = Collections.unmodifiableList(locBusTrips);
                tripMap = Collections.unmodifiableMap(locTripMap);
                idToVehicleMap = Collections.unmodifiableMap(locIdToVehicleMap);
                schoolBusDriverMap = locSchoolBusDriverMap;
                tobeVisitedBusStops = Collections.unmodifiableMap(locToBeVisitedBusStops);
                visitedBusStops = Collections.unmodifiableMap(locVisitedBusStops);

                currentBusLocation = new ConcurrentHashMap<>();
                upcomingBusStopInfo = new ConcurrentHashMap<>();
                pushNotificationsSentTopics = ConcurrentHashMap.newKeySet();
            }
        }
    }

    private List<BusTrip> fetchAllTripsFromDB() {
        return routeDao.fetchAllTripsFromDB();
    }

    private void loadTripDetails(BusTrip trip, Map<Integer, SchoolBus> locIdToVehicleMap, Map<SchoolBus, Driver> locSchoolBusDriverMap,
                                 Map<String, List<BusStop>> locToBeVisitedBusStops,
                                 Map<String, List<BusStop>> locVisitedBusStops) {

        Map<SchoolBus, SchoolBusRoute> busRoutes = trip.getBusRoutes();
        for(Map.Entry<SchoolBus, SchoolBusRoute> entries: busRoutes.entrySet()) {
            SchoolBus bus = entries.getKey();
            SchoolBusRoute busRoute = entries.getValue();
            locIdToVehicleMap.putIfAbsent(bus.getVehicleId(), bus);

            Driver driver = userService.getDriverBySchoolBus(bus);
            if(driver != null) {
                locSchoolBusDriverMap.put(bus, driver);
            }

            // add all bus stops and destination point to "to-be-visited-bus-stops" list.
            List<BusStop> tmpBusStops = new ArrayList<>(busRoute.getBusStops().size()+1);
            tmpBusStops.addAll(busRoute.getBusStops());
            tmpBusStops.add(busRoute.getDestination());
            locToBeVisitedBusStops.put(String.format("%d_%d",trip.getTripId(), bus.getVehicleId()), tmpBusStops);
            locVisitedBusStops.put(String.format("%d_%d",trip.getTripId(), bus.getVehicleId()), new ArrayList<>());

            // add topic to send push notification to driver couple of mins before the bus starts
            String startTrackingTopicName="-start_tracking-"+trip.getTripId()+"-"+bus.getVehicleId();
            if(!startTrackingTopics.contains(startTrackingTopicName)){
                startTrackingTopics.add(startTrackingTopicName);
                notificationService.createTopic(startTrackingTopicName);
            }

            // add topic to send push notification to transport-in-charge, if the bus didn't start even 10mins after scheduled start time.
            String escalateStartTrackingDelayTopicName = "-escalate_start_tracking_delay-"+trip.getTripId()+"-"+bus.getVehicleId();
            if(!escalateStartTrackingDelayTopics.contains(escalateStartTrackingDelayTopicName)){
                escalateStartTrackingDelayTopics.add(startTrackingTopicName);
                notificationService.createTopic(escalateStartTrackingDelayTopicName);
            }

            for (BusStop busStop : busRoute.getBusStops()){

                // create topics, if don't exist, to send push notifications to parents about bus arrival
                // the topics should be created for every trip_id, bus_id, bus_stop_id and frequency combination.
                List<String> notificationFrequencies = Arrays.asList("15", "10", "5", "0");
                for(String frequency : notificationFrequencies){
                    String arrivalNotificationTopicName= "-arrival_notification-"+ trip.getTripId()+"-"+bus.getVehicleId()+"-"+
                            busStop.getId()+"-"+frequency;
                    if(!arrivalNotificationTopics.contains(arrivalNotificationTopicName)){
                        arrivalNotificationTopics.add(arrivalNotificationTopicName);
                        notificationService.createTopic(arrivalNotificationTopicName);
                    }
                }

                // create topic to send (delay) notifications to parents and transport-in-charge, when bus is running late
                String delayNotificationTopicName = "-delay_notification-" + trip.getTripId() + "-" + bus.getVehicleId() + "-" + busStop.getId();
                if(!delayNotificationTopics.contains(delayNotificationTopicName)){
                    delayNotificationTopics.add(delayNotificationTopicName);
                    notificationService.createTopic(delayNotificationTopicName);
                }
            }
        }
    }

    /**
     * This method gets called by a scheduled job that runs @2am/@3am and generates route-plan for that day.
     * These route plan info is loaded into memory of this service object.
     */
    @Override
    @Transactional
    public void createRoutePlan() {
        routeDao.createRoutePlan();
        refreshData();
    }

    private void createRoutePlan(int tripId, String schedule) {
        routeDao.createRoutePlan(tripId, schedule);
        refreshData();
    }

    @Override
    public List<BusTrip> getAllTrips() {
        return trips;
    }

    @Override
    public List<BusTrip> getTrips(School school) {
        //return routeDao.getTrips(school);
        List<BusTrip> locTrips = new ArrayList<>();
        for(BusTrip trip: this.trips) {
           if(trip.getSchool().equals(school)) {
               locTrips.add(trip);
           }
        }
        return locTrips;
    }

    @Override
    public List<BusTrip> getTrips(List<SchoolBus> schoolBuses) {
        //return routeDao.getTrips(schoolBuses);
        List<BusTrip> locTrips = new ArrayList<>();
        for(BusTrip trip: this.trips) {
            for(Map.Entry<SchoolBus, SchoolBusRoute> entry: trip.getBusRoutes().entrySet()) {
                SchoolBus bus = entry.getKey();
                if(schoolBuses.contains(bus)) {
                    locTrips.add(trip);
                    break;
                }
            }
        }
        return locTrips;
    }

    @Override
    public BusTrip getTrip(int tripId) {
        return getTrip(tripId, true);
    }

    @Override
    public BusTrip getTrip(int tripId, boolean approved) {
        BusTrip trip;
        if(approved) {
            return tripMap.get(tripId);

            // Didn't find the use case where tripId doesn't exist in memory already.
            // trip details are loaded in memory every morning @2am.
            // If a trip is create & approved during the day, then approve method refreshes the memory,
            // so latest trip info should exist in memory all the time.
            // Hence commented below code that tries to retrieve trip from database.
            /*trip = routeDao.getTripFromRoutePlan(tripId);
            if (trip != null && trip.isApproved() && !trips.contains(trip)) {
                trips.add(trip);
                loadTripDetails(trip, idToVehicleMap, tobeVisitedBusStops, visitedBusStops);
            }*/
        } else {
            trip = routeDao.getTripFromRouteMap(tripId);
        }
        return trip;
    }

    @Override
    @Transactional
    public void setRouteStatus(BusTrip trip, SchoolBus schoolBus, RouteStatus status) {
        SchoolBusRoute busRoute = trip.getBusRoute(schoolBus);
        if(busRoute != null) {
            busRoute.setRouteStatus(status);
        }
    }

    @Override
    public RouteStatus getRouteStatus(int tripId, int busId) {
        BusTrip trip = tripMap.get(tripId);
        SchoolBus vehicle = idToVehicleMap.get(busId);
        return getRouteStatus(trip, vehicle);
    }

    @Override
    public RouteStatus getRouteStatus(BusTrip trip, SchoolBus schoolBus) {
        SchoolBusRoute busRoute = trip.getBusRoute(schoolBus);
        if(busRoute != null) {
            return busRoute.getRouteStatus();
        }
        return null;
    }

    @Override
    public Driver getDriverBySchoolBus(SchoolBus schoolBus) {
        Driver driver = schoolBusDriverMap.get(schoolBus);
        if(driver == null) {
            driver = userService.getDriverBySchoolBus(schoolBus);
            if(driver != null) {
                schoolBusDriverMap.put(schoolBus, driver);
            }
        }
        return driver;
    }

    @Override
    public List<BusTrip> getTripsAssociatedWithUser(UserInfo userInfo) {
        List<SchoolBus> buses = schoolBusService.getSchoolBusesAssociatedWithUser(userInfo);
        if(buses == null || buses.isEmpty()) {
            logger.warn("No School Bus is associated with logged in user: {}", userInfo);
            return null;
        }
        if(logger.isDebugEnabled()) {
            logger.debug("Got Running Info of {} buses", buses.size());
        }

        List<BusTrip> trips = getTrips(buses);
        return trips;
    }

    /*
    NOTE:   THIS METHOD IS CALLED LOT OF TIMES WHEN BUSUES ARE RUNNING. ALL STEPS ARE TAKEN TO MAKE IT AS EFFICIENT AS POSSIBLE.
            All accessors on data structures are ensured to have "constant" run time complexity.
            If loops are added to ensure only the required logic is processed and nothing else.
            The readability of the code is probably little compromised in that process. There is definite scope to make this more readable.
     */
    @Override
    @Transactional
    public void sendBusArrivalNotifications(BusPosition busPosition) {
        logger.debug(" sending notifications, if necessary, for bus: {} with position: {}", busPosition.getBusId(), busPosition);

        boolean devMode = StringUtils.hasText(RouteDaoImpl.appMode) && RouteDaoImpl.appMode.equalsIgnoreCase("dev");
        BusTrip trip = tripMap.get(busPosition.getTripId());
        SchoolBus vehicle = idToVehicleMap.get(busPosition.getBusId());
        if(vehicle == null) {
            logger.error("No vehicle found with id: {}", busPosition.getBusId());
            return;
        }

        SchoolBusRoute busRoute = trip.getBusRoute(vehicle);
        if(busRoute == null) {
            logger.error("No Bus Route is found for trip:{} and School-Bus:{}", trip, vehicle);
            return;
        }

        if(RouteStatus.YET_TO_START == busRoute.getRouteStatus()) {
            logger.debug("Marking the route status to IN_TRANSIT....Updated Actual Departure time from starting point...");
            busRoute.setRouteStatus(RouteStatus.IN_TRANSIT);
            routeDao.setActualDepartureTime(trip, vehicle, busRoute.getStartingPoint(), ZonedDateTime.now());
            routeDao.setRouteStatus(trip, vehicle, RouteStatus.IN_TRANSIT);
        }

        // store current position of the bus
        currentBusLocation.put(String.format("%d_%d",trip.getTripId(), vehicle.getVehicleId()), busPosition.getLocation());

        List<BusStop> tobeVisitedPoints = tobeVisitedBusStops.get(String.format("%d_%d",trip.getTripId(), vehicle.getVehicleId()));
        List<BusStop> visitedPoints = visitedBusStops.get(String.format("%d_%d",trip.getTripId(), vehicle.getVehicleId()));


        BusStop prevBusStop = null;
        long distanceFromBusPositionToPrevBusStop = 0;
        long durationFromBusPositionToPrevBusStop = 0;

        UpcomingBusStopDistanceDuration upcomingBusStopDistanceDuration =
                upcomingBusStopInfo.get(String.format("%d_%d",trip.getTripId(), vehicle.getVehicleId()));
        long distanceDurationUpdateIntervalSecs = (upcomingBusStopDistanceDuration == null)
                ? FIVE_MINUTES
                : upcomingBusStopDistanceDuration.distanceDurationUpdateIntervalSecs;

        int pickupPointCount = 0;
        BusStop secondBusStopToBeVisited = tobeVisitedPoints.size() > 1 ? tobeVisitedPoints.get(1) : null;
        Iterator<BusStop> iter = tobeVisitedPoints.iterator();
        while(iter.hasNext()) {
            BusStop currentBusStop = iter.next();
            ZonedDateTime currentDateTime = ZonedDateTime.now();

            boolean isDestinationPoint = !iter.hasNext();
            logger.debug("isDestinationPoint: {}", isDestinationPoint);

            logger.debug("Iteration --- count: {}, BusStop: {}, BusPosition:{}, currentDateTime:{}", pickupPointCount, currentBusStop, busPosition, currentDateTime);

            long distanceFromBusPositionToCurrentBusStop = 0;
            long durationFromBusPositionToCurrentBusStop = 0;

            // fire geoService request if
            // (a) busStop is the next bus stop to be visited (identified by pickupPointCount == 0) and
            // (b) previous to-be-visited bus stop(prevToBeVisitedBusStop) is not same as upcoming bus stop (busStop object) or
            // (c) if the distance and duration were measured <distanceDurationUpdateIntervalSecs - 5min/1min/30sec/10sec> ago.
            // Essentially geoService request is fired only for next upcoming bus-stop and if the last request was fired 5min ago.

            if(pickupPointCount == 0) {
                // this is the upcoming busStop
                BusStop prevToBeVisitedBusStop = null;
                if(upcomingBusStopDistanceDuration != null) {
                    prevToBeVisitedBusStop = upcomingBusStopDistanceDuration.busStop;
                }

                // check if bus moved away from the bus stop. record departure time from the bus stop then.
                if(prevToBeVisitedBusStop != null && !prevToBeVisitedBusStop.equals(currentBusStop)) {
                    // record actual departure time from the last bus stop.
                    logger.debug("Moved to next bus stop. So setting actual departure time of previous bus stop: {}", prevToBeVisitedBusStop);
                    routeDao.setActualDepartureTime(trip, vehicle, prevToBeVisitedBusStop, ZonedDateTime.now());
                    upcomingBusStopDistanceDuration = null;
                }

                long durationFromLastFetchTimeToNow = (upcomingBusStopDistanceDuration == null) ? 0 :
                        Duration.between(upcomingBusStopDistanceDuration.lastDistanceDurationFetchTime, currentDateTime).getSeconds();
                if(devMode || prevToBeVisitedBusStop == null || upcomingBusStopDistanceDuration == null || !prevToBeVisitedBusStop.equals(currentBusStop) ||
                        durationFromLastFetchTimeToNow > distanceDurationUpdateIntervalSecs) {
                    try {
                        logger.debug("getting distance and duration from GeoService from current bus position to upcoming bus stop:{}", currentBusStop);
                        Pair<Long, Long> distanceDurationPair = geoService.getDistanceAndDuration(busPosition.getLocation(), currentBusStop.getLocation());

                        upcomingBusStopDistanceDuration = new UpcomingBusStopDistanceDuration();
                        upcomingBusStopDistanceDuration.busStop = currentBusStop;
                        upcomingBusStopDistanceDuration.lastDistanceDurationFetchTime = ZonedDateTime.now();
                        upcomingBusStopDistanceDuration.distanceMtrs = distanceDurationPair.getKey();
                        upcomingBusStopDistanceDuration.durationSecs = distanceDurationPair.getValue();
                        upcomingBusStopInfo.put(String.format("%d_%d",trip.getTripId(), vehicle.getVehicleId()), upcomingBusStopDistanceDuration);

                        distanceFromBusPositionToCurrentBusStop = upcomingBusStopDistanceDuration.distanceMtrs;
                        durationFromBusPositionToCurrentBusStop = upcomingBusStopDistanceDuration.durationSecs;

                        // geoService will be fired every 5mins to get distance and duration, by default.
                        // if the duration to upcoming bus stop is less than 5min, set the interval to 1min.
                        // if the duration to upcoming bus stop is less than 1min, set the interval to 30secs.
                        // if the duration to upcoming bus stop is less than 30secs, set the interval to 10secs.
                        if(durationFromBusPositionToCurrentBusStop < THIRTY_SECONDS) {
                            upcomingBusStopDistanceDuration.distanceDurationUpdateIntervalSecs = TEN_SECONDS;
                        } else if(durationFromBusPositionToCurrentBusStop < ONE_MINUTE) {
                            upcomingBusStopDistanceDuration.distanceDurationUpdateIntervalSecs = THIRTY_SECONDS;
                        }else if(durationFromBusPositionToCurrentBusStop < FIVE_MINUTES) {
                            upcomingBusStopDistanceDuration.distanceDurationUpdateIntervalSecs = ONE_MINUTE;
                        }
                        logger.debug("Distance:{}, Duration:{}, updateIntervalSecs:{}", distanceFromBusPositionToCurrentBusStop,
                                durationFromBusPositionToCurrentBusStop, upcomingBusStopDistanceDuration.distanceDurationUpdateIntervalSecs);
                    } catch(GeoException e) {
                        logger.error("GeoException occurred while getting distance and duration " +
                                "between current bus-position {} and upcoming bus-point {}.", busPosition, currentBusStop, e);
                        continue;
                    }
                } else {
                    // previous geoService request was fired within distanceDurationUpdateIntervalSecs (5min/1min/30sec/10sec).
                    logger.debug("getting distance and duration from MEMORY from current bus position to upcoming bus stop:{} ", currentBusStop);

                    // subtract number of seconds travelled from the last fetched duration. This gives revised duration from current bus position to upcoming bus stop.
                    long numSecsTravelled = Math.abs(Duration.between(upcomingBusStopDistanceDuration.lastDistanceDurationFetchTime, ZonedDateTime.now()).getSeconds());
                    durationFromBusPositionToCurrentBusStop = upcomingBusStopDistanceDuration.durationSecs - numSecsTravelled;
                    logger.debug("upcomingBusStopDistanceDuration.durationSecs:{}, numSecsTravelled:{}, durationFromBusPositionToCurrentBusStop:{}",
                            upcomingBusStopDistanceDuration.durationSecs, numSecsTravelled, durationFromBusPositionToCurrentBusStop);

                    // calculate approx distance travelled, assumning vehicle is travelling at constant speed.
                    // Distance is used to send notification only when bus reaches the bus stop. Approx value is ok in all other cases.
                    long proportionalDistanceTravelled = Math.abs((numSecsTravelled * upcomingBusStopDistanceDuration.distanceMtrs) / upcomingBusStopDistanceDuration.durationSecs);
                    distanceFromBusPositionToCurrentBusStop = upcomingBusStopDistanceDuration.distanceMtrs - proportionalDistanceTravelled;
                    logger.debug("upcomingBusStopDistanceDuration.distanceMtrs:{}, proportionalDistanceTravelled:{}, durationFromBusPositionToCurrentBusStop:{}",
                            upcomingBusStopDistanceDuration.distanceMtrs, proportionalDistanceTravelled, distanceFromBusPositionToCurrentBusStop);

                    logger.debug("Distance:{}, Duration:{}, updateIntervalSecs:{}", distanceFromBusPositionToCurrentBusStop,
                            durationFromBusPositionToCurrentBusStop, distanceDurationUpdateIntervalSecs);
                }
            } else {
                // this is more than one stop away from current bus position.
                DistanceDurationDto distanceDurationDto = distanceDurationBetweenBusStops
                        .get(String.format("%d_%d", prevBusStop.getBusStopDetailId(), currentBusStop.getBusStopDetailId()));
                if(distanceDurationDto == null) {
                    logger.error("Distance and Duration between BusStop:{} and BusStop:{} couldn't be found. Something is wrong. " +
                            "The info should have been loaded into the memory. Check this ASAP", prevBusStop, currentBusStop);
                    try {
                        distanceDurationDto = geoService.getDistanceAndDuration(prevBusStop, currentBusStop);
                        if(distanceDurationDto != null) {
                            distanceDurationBetweenBusStops
                                    .put(String.format("%d_%d", prevBusStop.getBusStopDetailId(), currentBusStop.getBusStopDetailId()),
                                            distanceDurationDto);
                        }
                    } catch (GeoException ex) {
                        logger.error("GeoException occurred while getting distance and duration between BusStop:{} and BusStop:{} " +
                                "while determining if the notification should be sent to parents.", prevBusStop, currentBusStop, ex);
                    }
                }
                if(distanceDurationDto != null) {
                    durationFromBusPositionToCurrentBusStop = durationFromBusPositionToPrevBusStop + distanceDurationDto.getTimeInSeconds();
                    distanceFromBusPositionToCurrentBusStop = distanceFromBusPositionToPrevBusStop + distanceDurationDto.getDistanceInMeters();
                }

                logger.debug("Cumulated Distance n Duration......Distance:{}, Duration:{}", distanceFromBusPositionToCurrentBusStop,
                        durationFromBusPositionToCurrentBusStop);
            }

            // TODO - The duration and distance thresholds are hardcoded now.
            //      Change them to properties. They can be different for different schools.
            //      We may even let parents set their own preferences.
            String numStopsMsg = pickupPointCount == 0 ? " Your stop is next in the route"
                    : " The Bus is " + pickupPointCount + " " + (pickupPointCount > 1 ? "stops" : "stop") + " away";

            long numOfMinsToReachBusStop = durationFromBusPositionToCurrentBusStop/ONE_MINUTE;

            // if the bus position is with in 30mtrs from current bus stop, then we can assume bus has reached the bus stop.
            if(distanceFromBusPositionToCurrentBusStop <= THIRTY_METERS) {
                pickupPointCount = 0;
                iter.remove();

                // add to visited bus stops list
                visitedPoints.add(currentBusStop);
                String pushNotificationTopic = "-arrival_notification-" + trip.getTripId()+"-" + vehicle.getVehicleId() + "-" + currentBusStop.getId()+"-"+"0";
                if(isDestinationPoint) {
                    logger.debug("isDestinationPoint:true, marking the route status COMPLETED.... setting actual arrival time of destination point....");
                    busRoute.setRouteStatus(RouteStatus.COMPLETED);
                    routeDao.setActualArrivalTime(trip, vehicle, currentBusStop, ZonedDateTime.now());
                    routeDao.setRouteStatus(trip, vehicle, RouteStatus.COMPLETED);

                    // notify driver app (via websocket) to stop sending bus positions, now that destination is reached.
                    notificationService.publishStopSendingBusPositionMessage(trip.getTripId(), vehicle.getVehicleId());

                } else if(! pushNotificationsSentTopics.contains(pushNotificationTopic)) {
                    Driver driver = schoolBusDriverMap.get(vehicle);
                    NotificationMessageDto notificationMessageDto = NotificationMessageDto.Builder.build(vehicle, driver,"Your pickup is here.",
                                                                                "Your pickup is here.", "bus_arrival", 0L);

                    // notify parents (via push notification) that vehicle has reached the bus stop
                    notificationService.pushBusArrivalNotificationMessage(pushNotificationTopic, notificationMessageDto);
                    pushNotificationsSentTopics.add(pushNotificationTopic);

                    // record actual arrival time
                    routeDao.setActualArrivalTime(trip, vehicle, currentBusStop, ZonedDateTime.now());
                    logger.debug("sent push notification to topic:{} and set actual arrival time of bus stop:{}", pushNotificationTopic, currentBusStop);

                    // send websocket message to driver app with next bus stop location info
                    if(secondBusStopToBeVisited != null) {
                        notificationService.publishNextBusStopLocationMessage(trip.getTripId(), vehicle.getVehicleId(), secondBusStopToBeVisited);
                    }
                }
                prevBusStop = currentBusStop;
                logger.debug("Reached the bus stop:{}, continue the while loop...", currentBusStop);
                continue;
            } else if(!isDestinationPoint) {
                // Process the request only if currentBusStop is not the destination point.
                // If it is destination point, no notification needs to be sent. So there is no need to process below conditions/instructions.
                // Since this method is expected to be called LOT OF TIMES, it makes sense to optimize this as much as possible.

                if (durationFromBusPositionToCurrentBusStop > FIFTEEN_MINUTES) {
                    // if duration from current bus position to next bus stop is more than 15mins
                    //      (1) check if the expected arrival time as per running status is after expected arrival time informed to parents
                    //      (2) and if current time is just about 15min before expected arrival time informed to parents (+/- 10secs).
                    //              This is achieved by checking if diff between "current time" and "expected arrival time informed to parents" is less than 15mins.
                    //      (3) then it means bus is running late. Send "Delay Notification" if (a) delay is more than 5mins and (b) if it is not sent already
                    ZonedDateTime expArrTimePerBusRunningStatus = ZonedDateTime.now().plusSeconds(durationFromBusPositionToCurrentBusStop);
                    ZonedDateTime expArrTimeInformedToParents = currentBusStop.getExpectedArrivalTime();
                    long diffDuration = Duration.between(ZonedDateTime.now(), expArrTimeInformedToParents).getSeconds();
                    logger.debug("expArrTimePerBusRunningStatus:{}, expArrTimeInformedToParents:{}, diffDuration:{}",
                            expArrTimePerBusRunningStatus, expArrTimeInformedToParents, diffDuration);
                    if (expArrTimePerBusRunningStatus.isAfter(expArrTimeInformedToParents) && diffDuration < FIFTEEN_MINUTES) {
                        logger.debug("bus is running late and diff of duration between current date time and time informed to parents is less than 15min....");
                        //Math.abs(diffDuration - 15*60) <= 10 ) {
                        long pickupDelaySecs = Duration.between(currentBusStop.getExpectedArrivalTime(), expArrTimePerBusRunningStatus).getSeconds();

                        // If the delay is more than PICKUP_DELAY_SECS (set to 3min now)
                        if (pickupDelaySecs > PICKUP_DELAY_SECS) {
                            logger.debug("pickupDelayMins:{} is more than {}. Notify parents about delay...", pickupDelaySecs, PICKUP_DELAY_SECS);
                            delayedBuses.add(LocalDate.now().toString() + "_" + vehicle.getVehicleId());
                            long pickupDelayMins = pickupDelaySecs/60;
                            String message = String.format("The bus is running late by %d mins and is expected to reach in %d mins from now. " +
                                            " You will again be notified before actual arrival, per your preference.",
                                    pickupDelayMins, numOfMinsToReachBusStop);
                            String shortMessage = String.format("The bus is running late by %d mins.", pickupDelayMins);
                            String pushNotificationTopic = "-delay_notification-" + trip.getTripId() + "-" + vehicle.getVehicleId() + "-" + currentBusStop.getId();
                            if (!pushNotificationsSentTopics.contains(pushNotificationTopic)) {
                                Driver driver = schoolBusDriverMap.get(vehicle);
                                NotificationMessageDto notificationMessageDto = NotificationMessageDto.Builder.build(vehicle, driver, message, shortMessage, "bus_arrival", numOfMinsToReachBusStop);

                                notificationService.pushBusArrivalNotificationMessage(pushNotificationTopic, notificationMessageDto);
                                pushNotificationsSentTopics.add(pushNotificationTopic);
                                logger.debug("Sent push notification to parents about delay on topic:{}.", pushNotificationTopic);
                            }
                        }
                    }

                    // If this bus stop is at a place, which is more than 15min away when checked against "arrival time informed to parents",
                    //  then obviously other bus stops in the list are even farther. Hence, no messages would be sent anyway. So break the while loop.
                    // Ex., if "arrival time informed to parents is 7:45am" for this bus stop and current time is 7:25am, the bus is more than 15mins away as per the time informed to parents.
                    //      Then all other bus stops would be farther than this and no need to verify the distance/duration to send message. So break the loop.
                    if (diffDuration > FIFTEEN_MINUTES) {
                        logger.debug("diffDuration:{} is more than 15min... break while loop...", diffDuration);
                        break;
                    }
                } else {
                    String pushNotificationTopic;
                    if (durationFromBusPositionToCurrentBusStop > TEN_MINUTES) {
                        // bus will reach between 10min to 15mins.
                        pushNotificationTopic = "-arrival_notification-" + trip.getTripId() + "-" + vehicle.getVehicleId() + "-" + currentBusStop.getId() + "-" + "15";
                    } else if (durationFromBusPositionToCurrentBusStop > FIVE_MINUTES) {
                        // bus will reach between 5min to 10mins
                        pushNotificationTopic = "-arrival_notification-" + trip.getTripId() + "-" + vehicle.getVehicleId() + "-" + currentBusStop.getId() + "-" + "10";
                    } else {
                        // bus will reach with in 5mins.
                        pushNotificationTopic = "-arrival_notification-" + trip.getTripId() + "-" + vehicle.getVehicleId() + "-" + currentBusStop.getId() + "-" + "5";
                    }

                    if (!pushNotificationsSentTopics.contains(pushNotificationTopic)) {
                        String delaymessage = "";
                        /*if (delayedBuses.contains(LocalDate.now().toString() + "_" + vehicle.getVehicleId())) {
                            message = "Bus is running late.";
                        }*/
                        String message = String.format("%s %s and is expected to reach in %d mins from now. ", delaymessage, numStopsMsg, numOfMinsToReachBusStop);
                        String shortMessage = String.format("%s %s", delaymessage, numStopsMsg);

                        Driver driver = schoolBusDriverMap.get(vehicle);
                        NotificationMessageDto notificationMessageDto = NotificationMessageDto.Builder.build(vehicle, driver, message, shortMessage, "bus_arrival", numOfMinsToReachBusStop);
                        notificationService.pushBusArrivalNotificationMessage(pushNotificationTopic, notificationMessageDto);

                        // add to pushNotificationsSentTopics list if 'true' is returned by above pushNotification method.
                        pushNotificationsSentTopics.add(pushNotificationTopic);
                        logger.debug("Sent push notification to parents on topic: {}, message:{}", pushNotificationTopic, message);
                    }
                }
            }
            logger.debug("resetting the variables to prepare for next iteration of while loop.....");
            pickupPointCount++;
            prevBusStop = currentBusStop;
            durationFromBusPositionToPrevBusStop = durationFromBusPositionToCurrentBusStop;
            distanceFromBusPositionToPrevBusStop = distanceFromBusPositionToCurrentBusStop;
        }
    }

    @Override
    public GeoLocation getCurrentBusLocation(BusTrip trip, SchoolBus bus) {
        return currentBusLocation.get(String.format("%d_%d",trip.getTripId(), bus.getVehicleId()));
    }

    @Override
    public BusStop getNextBusStopInTheRoute(int tripId, int busId) {
        BusTrip trip = tripMap.get(tripId);
        SchoolBus schoolBus = idToVehicleMap.get(busId);
        if(trip == null || schoolBus == null) {
            return null;
        }

        if(RouteStatus.COMPLETED == getRouteStatus(trip, schoolBus)) {
            return null;
        }

        List<BusStop> busStops = tobeVisitedBusStops.get(String.format("%d_%d",trip.getTripId(), schoolBus.getVehicleId()));
        return (busStops == null || busStops.isEmpty()) ? null : busStops.get(0);
    }

    @Override
    public List<BusStop> getToBeVisitedBusStops(BusTrip trip, SchoolBus bus) {
        List<BusStop> busStops = tobeVisitedBusStops.get(String.format("%d_%d",trip.getTripId(), bus.getVehicleId()));
        return busStops == null? new ArrayList<>() : ImmutableList.copyOf(busStops);
    }

    @Override
    public List<BusStop> getVisitedBusStops(BusTrip trip, SchoolBus bus) {
        List<BusStop> busStops = visitedBusStops.get(String.format("%d_%d",trip.getTripId(), bus.getVehicleId()));
        return ImmutableList.copyOf(busStops);
    }

    @Override
    public int saveTrip(BusTrip trip) {
        int tripId = routeDao.saveTrip(trip);
        approveTrip(tripId, trip.getSchedule());
        return tripId;
    }

    @Override
    public boolean approveTrip(int tripId) {
        return approveTrip(tripId, null);
    }

    @Override
    @Transactional
    public boolean approveTrip(int tripId, String schedule) {
        boolean approved = routeDao.approveTrip(tripId);
        /*
            Below code is commented as createRoutePlan calls refreshData method, which initiates all memory variables.
            This erases all session information (delayedBuses, currentBusLocation, tobeVisitedBusStops etc).
            Erasing of session info should happen only during nightly refresh.

            Keep the code commented until we enhanced the app to refresh only the data related to given tripId,
            without disturbing other data in memory.
         */
        // check route-plan if trip starts on the same day. If so, load trip details in to memory.
        /*if(approved) {
            createRoutePlan(tripId, schedule);
            getTrip(tripId, true);
        }*/
        return approved;
    }

    /*@Override
    @Transactional
    public boolean approveTrip(List<Integer> tripIds) {
        boolean overallStatus = true;
        for(int tripId: tripIds) {
            boolean approveStatus = approveTrip(tripId);
            if(!approveStatus) {
                overallStatus = approveStatus;
            }
        }
        return overallStatus;
    }*/

    @Override
    public DistanceDurationDto getDistanceAndDurationBetweenBusStops(BusStop stop1, BusStop stop2) {
        return distanceDurationBetweenBusStops.get(String.format("%d_%d", stop1.getBusStopDetailId(), stop2.getBusStopDetailId()));
    }

    @Override
    public void updateDistanceAndDurationBetweenBusStops() throws GeoException {
        updateDistanceAndDurationBetweenBusStops(trips);
    }

    @Override
    public String getDetailedViaPointsBetweenBusStopsFromGeoService(int tripId, int busId, int busStopDetailsId) {
        BusTrip trip = tripMap.get(tripId);
        SchoolBus schoolBus = idToVehicleMap.get(busId);
        if(trip == null || schoolBus == null) {
            return null;
        }

        SchoolBusRoute busRoute = trip.getBusRoute(schoolBus);
        // Shouldn't need to check if busStopDetailsId is equal to that of starting_point.
        // via_points are stored in route_map table against the "to" bus_stop and not "from" bus_stop.
        // So, starting_point can never be "to" bus_stop in the route.
        if(busRoute.getDestination().getBusStopDetailId() == busStopDetailsId) {
            String viaPoints = routeDao.getDetailedViaPointsBetweenBusStopsFromGeoService(trip, schoolBus, busRoute.getDestination());
            return viaPoints;
        } else {
            for (BusStop busStop : busRoute.getBusStops()) {
                if (busStop.getBusStopDetailId() == busStopDetailsId) {
                    String viaPoints = routeDao.getDetailedViaPointsBetweenBusStopsFromGeoService(trip, schoolBus, busStop);
                    return viaPoints;
                }
            }
        }
        return null;
    }

    @Override
    public List<GeoLocation> getViaPointLocationsBetweenBusStopsForRouteDetermination(int fromBusStopDetailsId, int toBusStopDetailsid) {
        List<GeoLocation> locations = locationDao.getViaPointLocationsBetweenBusStopsForRouteDetermination(fromBusStopDetailsId, toBusStopDetailsid);
        return locations;

    }

    private void updateDistanceAndDurationBetweenBusStops(List<BusTrip> locTrips) throws GeoException {

        for(BusTrip trip: locTrips) {
            for(Map.Entry<SchoolBus, SchoolBusRoute> entry: trip.getBusRoutes().entrySet()) {
                SchoolBus schoolBus = entry.getKey();
                SchoolBusRoute busRoute = entry.getValue();

                int i=0;
                BusStop prevBusStop = null;
                List<DistanceDurationDto> distanceDurationDtos = new ArrayList<>();
                List<BusStop> allStops = new ArrayList<>();
                allStops.add(busRoute.getStartingPoint());
                allStops.addAll(busRoute.getBusStops());
                allStops.add(busRoute.getDestination());
                for(BusStop busStop: allStops) {
                    // Note the starting bus stop (usually school?) and move to next bus stop.
                    // Get distance and duration between the points from 2nd iteration onwards.
                    if(i > 0 && prevBusStop != null) {
                        DistanceDurationDto distanceDurationDto = geoService.getDistanceAndDuration(prevBusStop, busStop);
                        if(distanceDurationDto != null) {
                            distanceDurationBetweenBusStops
                                    .put(String.format("%d_%d", prevBusStop.getBusStopDetailId(), busStop.getBusStopDetailId()), distanceDurationDto);
                            distanceDurationDtos.add(distanceDurationDto);
                            routeDao.updateDetailedViaPointsBetweenBusStopsFromGeoService(trip, schoolBus, distanceDurationDto.getToBusStop(),
                                                                                            distanceDurationDto.getViaPoints());
                        }
                    }

                    i++;
                    prevBusStop = busStop;
                }

                routeDao.updateDistanceAndDurationBetweenBusStops(trip, schoolBus, distanceDurationDtos);
            }
        }
    }

    private static class UpcomingBusStopDistanceDuration {
        private BusStop busStop;
        private ZonedDateTime lastDistanceDurationFetchTime;
        private long distanceMtrs;
        private long durationSecs;
        private long distanceDurationUpdateIntervalSecs = FIVE_MINUTES;
    }

    /*private static class NotificationMessage {
        private String driverName;
        private String driverPictureUrl;
        private String busNumber;
        private String busRegistrationNumber;
        private String message;
    }*/

    /*
        private Pair<Long, Long> getDistanceDurationFromBusPositionToCurrentBusStop(int pickupPointCount,
                                                                                BusPosition busPosition,
                                                                                BusStop currentBusStop,
                                                                                BusStop prevBusStop,
                                                                                UpcomingBusStopDistanceDuration upcomingBusStopDistanceDuration,
                                                                                long distanceFromBusPositionToPrevBusStop,
                                                                                long durationFromBusPositionToPrevBusStop
                                                                                ) {
        long distanceFromBusPositionToCurrentBusStop = 0;
        long durationFromBusPositionToCurrentBusStop = 0;
        ZonedDateTime currentDateTime = ZonedDateTime.now();

        // fire geoService request if
        // (a) busStop is the next bus stop to be visited (identified by pickupPointCount == 0) and
        // (b) previous to-be-visited bus stop(prevToBeVisitedBusStop) is not same as upcoming bus stop (busStop object) or
        // (c) if the distance and duration were measured <distanceDurationUpdateIntervalSecs - 5min/1min/30sec/10sec> ago.
        // Essentially geoService request is fired only for next upcoming bus-stop and if the last request was fired 5min ago.

        if(pickupPointCount == 0) {
            // this is the upcoming busStop
            BusStop prevToBeVisitedBusStop = null;
            if(upcomingBusStopDistanceDuration != null) {
                prevToBeVisitedBusStop = upcomingBusStopDistanceDuration.busStop;
            }

            // check if bus moved away from the bus stop. record departure time from the bus stop then.
            if(prevToBeVisitedBusStop != null && !prevToBeVisitedBusStop.equals(currentBusStop)) {
                // record actual departure time from the bus stop.
            }

            if(prevToBeVisitedBusStop == null || upcomingBusStopDistanceDuration == null || !prevToBeVisitedBusStop.equals(currentBusStop) ||
                    Duration.between(upcomingBusStopDistanceDuration.lastDistanceDurationFetchTime, currentDateTime).getSeconds() > distanceDurationUpdateIntervalSecs) {
                try {
                    Pair<Long, Long> distanceDurationPair = geoService.getDistanceAndDuration(busPosition.getLocation(), currentBusStop.getLocation());

                    upcomingBusStopDistanceDuration = new UpcomingBusStopDistanceDuration();
                    upcomingBusStopDistanceDuration.busStop = currentBusStop;
                    upcomingBusStopDistanceDuration.lastDistanceDurationFetchTime = ZonedDateTime.now();
                    upcomingBusStopDistanceDuration.distanceMtrs = distanceDurationPair.getKey();
                    upcomingBusStopDistanceDuration.durationSecs = distanceDurationPair.getValue();
                    upcomingBusStopInfo.put(trip, vehicle, upcomingBusStopDistanceDuration);

                    distanceFromBusPositionToCurrentBusStop = upcomingBusStopDistanceDuration.distanceMtrs;
                    durationFromBusPositionToCurrentBusStop = upcomingBusStopDistanceDuration.durationSecs;

                    // geoService will be fired every 5mins to get distance and duration, by default.
                    // if the duration to upcoming bus stop is less than 5min, set the interval to 1min.
                    // if the duration to upcoming bus stop is less than 1min, set the interval to 30secs.
                    // if the duration to upcoming bus stop is less than 30secs, set the interval to 10secs.
                    if(durationFromBusPositionToCurrentBusStop < 30) {
                        distanceDurationUpdateIntervalSecs = 10;
                    } else if(durationFromBusPositionToCurrentBusStop < ONE_MINUTE) {
                        distanceDurationUpdateIntervalSecs = 30;
                    }else if(durationFromBusPositionToCurrentBusStop < FIVE_MINUTES) {
                        distanceDurationUpdateIntervalSecs = ONE_MINUTE;
                    }
                } catch(GeoException e) {
                    logger.error("GeoException occurred while getting distance and duration " +
                            "between current bus-position {} and upcoming bus-point {}.", busPosition, currentBusStop, e);
                    continue;
                }
            } else {
                // previous geoService request was fired within distanceDurationUpdateIntervalSecs (5min/1min/30sec/10sec).

                // subtract number of seconds travelled from the last fetched duration. This gives revised duration from current bus position to upcoming bus stop.
                long numSecsTravelled = Duration.between(ZonedDateTime.now(), upcomingBusStopDistanceDuration.lastDistanceDurationFetchTime).getSeconds();
                durationFromBusPositionToCurrentBusStop = upcomingBusStopDistanceDuration.durationSecs - numSecsTravelled;

                // calculate approx distance travelled, assumning vehicle is travelling at constant speed.
                // Distance is used to send notification only when bus reaches the bus stop. Approx value is ok in all other cases.
                long proportionalDistanceTravelled = (numSecsTravelled * upcomingBusStopDistanceDuration.distanceMtrs) / upcomingBusStopDistanceDuration.durationSecs;
                distanceFromBusPositionToCurrentBusStop = upcomingBusStopDistanceDuration.distanceMtrs - proportionalDistanceTravelled;
            }
        } else {
            // this is more than one stop away from current bus position.
            DistanceDurationDto distanceDurationDto = distanceDurationBetweenBusStops.get(prevBusStop, currentBusStop);
            if(distanceDurationDto == null) {
                logger.error("Distance and Duration between BusStop:{} and BusStop:{} couldn't be found. Something is wrong. " +
                        "The info should have been loaded into the memory. Check this ASAP", prevBusStop, currentBusStop);
                try {
                    distanceDurationDto = geoService.getDistanceAndDuration(prevBusStop, currentBusStop);
                    if(distanceDurationDto != null) {
                        distanceDurationBetweenBusStops.put(prevBusStop, currentBusStop, distanceDurationDto);
                    }
                } catch (GeoException ex) {
                    logger.error("GeoException occurred while getting distance and duration between BusStop:{} and BusStop:{} " +
                            "while determining if the notification should be sent to parents.", prevBusStop, currentBusStop, ex);
                }
            }
            if(distanceDurationDto != null) {
                durationFromBusPositionToCurrentBusStop = durationFromBusPositionToPrevBusStop + distanceDurationDto.getTimeInSeconds();
                distanceFromBusPositionToCurrentBusStop = distanceFromBusPositionToPrevBusStop + distanceDurationDto.getDistanceInMeters();
            }
        }

        return new Pair<Long, Long>(distanceFromBusPositionToPrevBusStop, durationFromBusPositionToPrevBusStop);
    }
     */
}
