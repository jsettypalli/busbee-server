package jstech.edu.transportmodel.service.route;

import javafx.util.Pair;
import jstech.edu.transportmodel.GeoException;
import jstech.edu.transportmodel.common.*;
import jstech.edu.transportmodel.dto.DistanceDurationDto;

import java.util.List;

/**
 * Created by jitendra1 on 30-Dec-17.
 */
public interface RouteService {

    BusTrip getTrip(int tripId);
    BusTrip getTrip(int tripId, boolean approved);
    List<BusTrip> getTrips(School school);
    List<BusTrip> getTrips(List<SchoolBus> schoolBuses);
    List<BusTrip> getAllTrips();

    int saveTrip(BusTrip trip);
    int generateOptimalRoute(int schoolId, String routeName, String pickupSchedule, String dropoffSchedule);
    boolean approveTrip(int tripId);
    boolean approveTrip(int tripId, String schedule);
    // commented as there are no use cases at this point. Should be implemented if necessary.
    //boolean approveTrip(List<Integer> tripIds);

    /**
     * This method gets called by a scheduled job that runs @2am/@3am and generates route-plan for that day.
     * These route plan info is loaded into memory of this service object.
     */
    void createRoutePlan();

    void setRouteStatus(BusTrip trip, SchoolBus schoolBus, RouteStatus status);
    RouteStatus getRouteStatus(BusTrip trip, SchoolBus schoolBus);
    RouteStatus getRouteStatus(int tripId, int busId);

    Driver getDriverBySchoolBus(SchoolBus schoolBus);
    void sendBusArrivalNotifications(BusPosition position);
    GeoLocation getCurrentBusLocation(BusTrip trip, SchoolBus bus);

    BusStop getNextBusStopInTheRoute(int tripId, int busId);
    List<BusStop> getToBeVisitedBusStops(BusTrip trip, SchoolBus bus);
    List<BusStop> getVisitedBusStops(BusTrip trip, SchoolBus bus);

    List<BusTrip> getTripsAssociatedWithUser(UserInfo userInfo);

    DistanceDurationDto getDistanceAndDurationBetweenBusStops(BusStop stop1, BusStop stop2);
    void updateDistanceAndDurationBetweenBusStops() throws GeoException;

    String getDetailedViaPointsBetweenBusStopsFromGeoService(int tripId, int busId, int busStopDetailsId);

    List<GeoLocation> getViaPointLocationsBetweenBusStopsForRouteDetermination(int fromBusStopDetailsId, int toBusStopDetailsid);
}
