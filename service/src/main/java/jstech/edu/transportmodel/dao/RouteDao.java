package jstech.edu.transportmodel.dao;

import com.graphhopper.jsprit.core.problem.solution.route.VehicleRoute;
import jstech.edu.transportmodel.common.*;
import jstech.edu.transportmodel.dto.DistanceDurationDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;

public interface RouteDao {

    BusTrip getTripFromRoutePlan(int tripId);
    BusTrip getTripFromRouteMap(int tripId);
    List<BusTrip> getTrips(School school);
    List<BusTrip> fetchAllTripsFromDB();
    List<BusTrip> getTrips(List<SchoolBus> schoolBuses);

    int saveTrip(BusTrip busTrip);
    int saveTrip(String tripName, String schedule, School school, boolean pickup, Collection<VehicleRoute> routes);
    boolean approveTrip(int tripId);

    void createRoutePlan();
    void createRoutePlan(int tripId, String schedule);

    void setRouteStatus(BusTrip trip, SchoolBus vehicle, RouteStatus routeStatus);

    void setActualArrivalTime(BusTrip trip, SchoolBus vehicle, BusStop currentBusStop, ZonedDateTime actualArrivalTime);
    void setActualDepartureTime(BusTrip trip, SchoolBus vehicle, BusStop currentBusStop, ZonedDateTime actualDepartureTime);
    boolean updateDistanceAndDurationBetweenBusStops(BusTrip trip, SchoolBus schoolBus, List<DistanceDurationDto> distanceDurationDtos);

    String getDetailedViaPointsBetweenBusStopsFromGeoService(BusTrip trip, SchoolBus schoolBus, BusStop busStop);
    boolean updateDetailedViaPointsBetweenBusStopsFromGeoService(BusTrip trip, SchoolBus schoolBus, BusStop busStop, String viaPoints);
}
