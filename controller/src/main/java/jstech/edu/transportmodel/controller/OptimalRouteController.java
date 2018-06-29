package jstech.edu.transportmodel.controller;

import jstech.edu.transportmodel.AppMain;
import jstech.edu.transportmodel.GeoException;
import jstech.edu.transportmodel.common.*;
import jstech.edu.transportmodel.dto.RunningBusInfoDto;
import jstech.edu.transportmodel.service.SchoolBusService;
import jstech.edu.transportmodel.service.route.RouteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static java.time.format.DateTimeFormatter.ISO_INSTANT;

/**
 * Created by jitendra1 on 22-Jan-18.
 */
@RestController
@RequestMapping(AppMain.REST_BASE_PATH)
public class OptimalRouteController {

    private static final Logger logger = LoggerFactory.getLogger(OptimalRouteController.class);

    @Autowired
    private RouteService routeService;

    @Autowired
    private SchoolBusService schoolBusService;

    // TODO - Add ability to specify groups of students who will be travelling, so route can be generated accordingly.
    @PostMapping(value="/routes/generate")
    public @ResponseBody
    BusTrip generateOptimalRoute(@RequestParam("school_id") int schoolId,
                                 @RequestParam("route_name") String routeName,
                                 @RequestParam("pickup_schedule") String pickupSchedule,
                                 @RequestParam("dropoff_schedule") String dropoffSchedule) {
        // sample schedule   0 0 7 * * 1-5  (for 7am every day except saturday & sunday)
        int tripId = routeService.generateOptimalRoute(schoolId, routeName, pickupSchedule, dropoffSchedule);

        /* Now that we have pickup trip details, dropoff is just reverse of pickup.
            But the reverse route may not be optimal because of various reasons ex., one-way, section of road is closed during dropoff time etc
            So use Djonstrup algorithm (comes under graph structure) to determine fastest route from school to last dropoff point.
         */
        return routeService.getTrip(tripId, false);
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE, value="/routes")
    public @ResponseBody
    BusTrip getOptimalRoutes(@RequestParam("trip_id") int tripId) {
        return routeService.getTrip(tripId, true);
    }

    @PostMapping(value="/routes/authorize")
    public @ResponseBody boolean authorizeOptimalRoute(@RequestParam("trip_id") int tripId) {
        return routeService.approveTrip(tripId);
    }

    // Convenience method to update distance between bus stops and update route_map table with this info.
    //  The corresponding service method will also update corresponding info in memory.
    //  TODO - Enable this method only for SYSADMIN ROLE in the long term
    @PostMapping(value="/routes/update_distance")
    public @ResponseBody
    void updateDistanceBetweenBusStops() throws GeoException{
        routeService.updateDistanceAndDurationBetweenBusStops();
    }

    /*
    Request to know if the bus for the user is running or is scheduled for later time.
    - get roles played by the user from UserInfo object (determined by jwt)
    - if user plays driver role
        - get corresponding bus assigned to driver
    - if user plays parent role
        - get corresponding bus assigned to parent
    - if user plays transport_incharge role
        - get all buses.

    - for the buses received from previous step
        - consider only the first bus in the list for now. will have to be enhanced to handle multiple buses.
        - check if the bus is running at this time.
        - If so, trip_id, bus_id, start_time, send current location of the bus, bus stops covered (this includes starting point as well) and upcoming bus stop
        - if bus is not running, send trip_id, bus_id, start_time of the bus to client.
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE, value="/get_running_buses")
    public ResponseEntity<List<RunningBusInfoDto>>
     getRunningBusInfo(@RequestAttribute(name="user_info") UserInfo userInfo) {
        List<SchoolBus> buses = schoolBusService.getSchoolBusesAssociatedWithUser(userInfo);
        if(buses == null || buses.isEmpty()) {
            logger.warn("No School Bus is associated with logged in user: {}", userInfo);
            return new ResponseEntity<>(Arrays.asList(new RunningBusInfoDto()), HttpStatus.NOT_FOUND);
        }

        if(logger.isDebugEnabled()) {
            logger.debug("Got Running Info of {} buses", buses.size());
        }

        List<BusTrip> trips = routeService.getTrips(buses);
        //return trips;

        //List<BusTrip> trips = routeService.getTripsAssociatedWithUser(userInfo);
        if(trips == null || trips.isEmpty()) {
            logger.warn("No Trip is scheduled for logged in user: {}", userInfo);
            return new ResponseEntity<>(Arrays.asList(new RunningBusInfoDto()), HttpStatus.NOT_FOUND);
            //return Arrays.asList(new RunningBusInfoDto());
        }

        // above object contains all trips associated with buses that are scheduled to run today.
        // find if any of them are already running, if so, return info of only running buses.
        // if no bus is running, then return RunningBusInfoDto object for all buses.
        List<RunningBusInfoDto> runningBuses = findRunningBuses(trips, buses, userInfo.getRole().toString());
        if(logger.isDebugEnabled()) {
            logger.debug(runningBuses.isEmpty() ? "no buses are running at this point...."
                    : runningBuses.size()+" buses are running now...");
        }
        if(runningBuses.isEmpty()) {
            List<RunningBusInfoDto> yetToStartBuses = findYetToStartBuses(trips, buses, userInfo.getRole().toString());
            return new ResponseEntity<>(yetToStartBuses, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(runningBuses, HttpStatus.OK);
        }
    }

    @GetMapping(value="/routes/via_points")
    public @ResponseBody
    String getViaPoints(@RequestAttribute(name="user_info") UserInfo userInfo,
                        @RequestParam("trip_id") int tripId,
                        @RequestParam("vehicle_id") int busId,
                        @RequestParam("next_bus_stop_id") int busStopId) {
        return routeService.getDetailedViaPointsBetweenBusStopsFromGeoService(tripId, busId, busStopId);
    }

    private List<RunningBusInfoDto> findRunningBuses(List<BusTrip> trips, List<SchoolBus> buses, String role) {
        List<RunningBusInfoDto> runningBusInfoDtos = new ArrayList<>();
        for(BusTrip trip: trips) {
            for(Map.Entry<SchoolBus, SchoolBusRoute> entry: trip.getBusRoutes().entrySet()) {
                SchoolBus bus = entry.getKey();
                SchoolBusRoute route = entry.getValue();

                // skip, if this bus is not associated with the user
                if(!buses.contains(bus)) {
                    continue;
                }

                // if route already started and is not completed yet
                if(route.getStartDateTime().isBefore(ZonedDateTime.now()) && RouteStatus.COMPLETED != route.getRouteStatus()) {
                    RunningBusInfoDto runningBusInfoDto = new RunningBusInfoDto();
                    runningBusInfoDto.setBusId(bus.getVehicleId());
                    runningBusInfoDto.setTripId(trip.getTripId());
                    runningBusInfoDto.setRole(role);
                    runningBusInfoDto.setInTransit(route.getRouteStatus() == RouteStatus.IN_TRANSIT);
                    runningBusInfoDto.setStartDateTime(ISO_INSTANT.format(route.getStartDateTime()));

                    GeoLocation location = routeService.getCurrentBusLocation(trip, bus);
                    runningBusInfoDto.setCurrentLocation(location);

                    List<BusStop> tobeVisitedStops = routeService.getToBeVisitedBusStops(trip, bus);
                    if(!tobeVisitedStops.isEmpty()) {
                        BusStop nextBusStop = tobeVisitedStops.get(0);
                        RunningBusInfoDto.BusStopDto busStopDto = runningBusInfoDto.createBusStopDto();
                        busStopDto.setBusStopDetailsId(nextBusStop.getBusStopDetailId());
                        busStopDto.setBusStopName(nextBusStop.getName());
                        busStopDto.setLocation(nextBusStop.getLocation());
                        runningBusInfoDto.setNextBusStop(busStopDto);
                    }

                    List<BusStop> visitedBusStops = routeService.getVisitedBusStops(trip, bus);
                    BusStop tmpBusStop;
                    if(visitedBusStops.isEmpty()) {
                        tmpBusStop = route.getStartingPoint();
                    } else {
                        tmpBusStop = visitedBusStops.get(visitedBusStops.size()-1);
                    }
                    if(tmpBusStop != null) {
                        RunningBusInfoDto.BusStopDto busStopDto = runningBusInfoDto.createBusStopDto();
                        busStopDto.setBusStopDetailsId(tmpBusStop.getBusStopDetailId());
                        busStopDto.setBusStopName(tmpBusStop.getName());
                        busStopDto.setLocation(tmpBusStop.getLocation());
                        runningBusInfoDto.setPrevBusStop(busStopDto);
                    }
                    /*List<RunningBusInfoDto.BusStopDto> busStopDtos = new ArrayList<>();
                    for(BusStop visitedBusStop: visitedBusStops) {
                        RunningBusInfoDto.BusStopDto busStopDto = runningBusInfoDto.createBusStopDto();
                        busStopDto.setBusStopName(visitedBusStop.getName());
                        busStopDto.setLocation(visitedBusStop.getLocation());
                        busStopDtos.add(busStopDto);
                    }
                    if(!busStopDtos.isEmpty()) {
                        runningBusInfoDto.setVisitedBusStops(busStopDtos);
                    }*/

                    runningBusInfoDtos.add(runningBusInfoDto);
                }
            }
        }
        return runningBusInfoDtos;
    }


    private List<RunningBusInfoDto> findYetToStartBuses(List<BusTrip> trips, List<SchoolBus> buses, String role) {
        List<RunningBusInfoDto> runningBusInfoDtos = new ArrayList<>();
        for(BusTrip trip: trips) {
            for (Map.Entry<SchoolBus, SchoolBusRoute> entry : trip.getBusRoutes().entrySet()) {
                SchoolBus bus = entry.getKey();
                SchoolBusRoute route = entry.getValue();

                // skip, if this bus is not associated with the user
                if(!buses.contains(bus)) {
                    continue;
                }

                // if start time of the route is after current time and status is yet to start
                if(route.getStartDateTime().isAfter(ZonedDateTime.now()) && RouteStatus.YET_TO_START == route.getRouteStatus()) {
                    RunningBusInfoDto runningBusInfoDto = new RunningBusInfoDto();
                    runningBusInfoDto.setBusId(bus.getVehicleId());
                    runningBusInfoDto.setTripId(trip.getTripId());
                    runningBusInfoDto.setRole(role);
                    runningBusInfoDto.setInTransit(false);
                    runningBusInfoDto.setStartDateTime(ISO_INSTANT.format(route.getStartDateTime()));
                    runningBusInfoDtos.add(runningBusInfoDto);
                }
            }
        }
        return runningBusInfoDtos;
    }
}
