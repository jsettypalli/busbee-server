package jstech.edu.transportmodel.service.route;

import com.graphhopper.jsprit.core.algorithm.VehicleRoutingAlgorithm;
import com.graphhopper.jsprit.core.algorithm.box.Jsprit;
import com.graphhopper.jsprit.core.algorithm.state.StateId;
import com.graphhopper.jsprit.core.algorithm.state.StateManager;
import com.graphhopper.jsprit.core.problem.Location;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.constraint.ConstraintManager;
import com.graphhopper.jsprit.core.problem.job.Service;
import com.graphhopper.jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import com.graphhopper.jsprit.core.problem.solution.route.VehicleRoute;
import com.graphhopper.jsprit.core.problem.vehicle.Vehicle;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleImpl;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleType;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleTypeImpl;
import com.graphhopper.jsprit.core.reporting.SolutionPrinter;
import com.graphhopper.jsprit.core.util.Coordinate;
import com.graphhopper.jsprit.core.util.FastVehicleRoutingTransportCostsMatrix;
import com.graphhopper.jsprit.core.util.Solutions;
import jstech.edu.transportmodel.common.*;
import jstech.edu.transportmodel.dao.BusStopDao;
import jstech.edu.transportmodel.dao.LocationDao;
import jstech.edu.transportmodel.dao.RouteDao;
import jstech.edu.transportmodel.dao.SchoolBusDao;
import jstech.edu.transportmodel.service.SchoolService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Created by jitendra1 on 27-Dec-17.
 */
@org.springframework.stereotype.Service
public class JSpritRouteService extends AbstractRouteService {

    private static final Logger logger = LoggerFactory.getLogger(JSpritRouteService.class);
    public static final int CAPACITY_INDEX = 0;

    @Autowired
    private LocationDao locationDao;

    @Autowired
    private SchoolBusDao schoolBusDao;

    @Autowired
    private BusStopDao busStopDao;

    @Autowired
    private TransportCostService transportCostService;

    @Autowired
    private RouteDao routeDao;

    @Autowired
    private SchoolService schoolService;

    @Transactional
    public int generateOptimalRoute(int schoolId, String routeName, String pickupSchedule, String dropoffSchedule) {
        School school = schoolService.getSchool(schoolId);
        List<SchoolBus> vehicles = schoolBusDao.getSchoolBuses(school);

        boolean isPickup = false;
        List<BusStop> busStops = null;
        if(!StringUtils.isEmpty(pickupSchedule)) {
            isPickup = true;
            busStops = busStopDao.getBusPoints(true, school);
        } else if(!StringUtils.isEmpty(dropoffSchedule)) {
            busStops = busStopDao.getBusPoints(false, school);
        }

        if(busStops == null) {
            logger.error("No Bus Points found for school: {}. Can't generate optimal route.", school.getName());
            return 0;
        }

        // remove school bus starting point/destination points from busStops object.
        for(Iterator<BusStop> iter = busStops.iterator(); iter.hasNext();) {
            BusStop busStop = iter.next();
            for(SchoolBus schoolBus: vehicles) {
                if(schoolBus.getStartBusStop().equals(busStop)) {
                    iter.remove();
                    break;
                }
            }
        }

        logger.debug("Starting....");
        Collection<VehicleRoute> routes = createRoutes(schoolId, routeName, pickupSchedule, dropoffSchedule, vehicles, busStops);

        return routeDao.saveTrip(routeName, pickupSchedule, school, isPickup, routes);
    }

    private Collection<VehicleRoute> createRoutes(int schoolId, String routeName, String pickupSchedule, String dropoffSchedule,
                             List<SchoolBus> schoolBuses, List<BusStop> busStops) {
        // TODO - it is assumed that all school buses will start from same location. This could be utterly wrong.
        //      Change this so vehicles can start from diff locations to pickup kids.
        BusStop startingPoint = schoolBuses.get(0).getStartBusStop();
        List<Vehicle> vehicles = getSchoolBuses(schoolBuses, startingPoint);
        VehicleRoutingProblem routingProblem = prepareVehicleRoutingProblem(startingPoint, busStops, vehicles);

        StateManager stateManager = new StateManager(routingProblem);
        StateId occupancyStateId = stateManager.createStateId("occupancy");
        stateManager.addStateUpdater(new MinOccupancyUpdater(occupancyStateId, stateManager));
        stateManager.updateLoadStates();

        // TODO - minimum occupancy percent is hardcoded here. Drive this by parameter in properties file
        ConstraintManager constraintManager = new ConstraintManager(routingProblem, stateManager);
        constraintManager.addConstraint(new OccupancyConstraint(60, occupancyStateId, stateManager),
                ConstraintManager.Priority.CRITICAL);
        constraintManager.addLoadConstraint();

        VehicleRoutingAlgorithm algorithm = Jsprit.Builder.newInstance(routingProblem).setStateAndConstraintManager(stateManager,constraintManager).buildAlgorithm();

        //VehicleRoutingAlgorithm algorithm = Jsprit.createAlgorithm(routingProblem);

        Collection<VehicleRoutingProblemSolution> solutions = algorithm.searchSolutions();

        VehicleRoutingProblemSolution bestSolution1 = Solutions.bestOf(solutions);

        SolutionPrinter.print(routingProblem, bestSolution1, SolutionPrinter.Print.VERBOSE);
        return bestSolution1.getRoutes();
    }

    private List<Vehicle> getSchoolBuses(List<SchoolBus> schoolBuses, BusStop startingPoint) {
        List<Vehicle> buses=new ArrayList<>();
        Location startLocation = Location.Builder.newInstance()
                //.setId(Integer.toString(0)).setIndex(0)
                .setId(Integer.toString(startingPoint.getId()))
                .setIndex(0)
                .setName(startingPoint.getName())
                .setCoordinate(Coordinate.newInstance(startingPoint.getLocation().getLatitude(),
                        startingPoint.getLocation().getLongitude()))
                .build();

        for(SchoolBus schoolBus: schoolBuses) {
            VehicleType vehicleType= VehicleTypeImpl.Builder.newInstance(schoolBus.getName())
                    .addCapacityDimension(CAPACITY_INDEX, schoolBus.getCapacity())
                    .build();

            //Vehicle_Remove vehicle= VehicleImpl.Builder.newInstance(schoolBus.getRegistrationNumber())
            Vehicle vehicle= VehicleImpl.Builder.newInstance(Integer.toString(schoolBus.getVehicleId()))
                    .setStartLocation(startLocation)
                    .setType(vehicleType)
                    .build();

            buses.add(vehicle);
        }
        return(buses);
    }

    private VehicleRoutingProblem prepareVehicleRoutingProblem(BusStop startingPoint, List<BusStop> busStops, List<Vehicle> buses) {
        FastVehicleRoutingTransportCostsMatrix costMatrix = transportCostService.prepareCostMatrix(startingPoint, busStops);
        VehicleRoutingProblem.Builder vrpBuilder= VehicleRoutingProblem.Builder.newInstance();

        for(int i = 0; i< busStops.size(); i++){
            Location pickupLocation=Location.Builder.newInstance()
                    .setId(Integer.toString(busStops.get(i).getBusStopDetailId()))
                    //.setId(Integer.toString(i+1))
                    .setIndex(i+1)
                    .setName(busStops.get(i).getName())
                    .setCoordinate(Coordinate.newInstance(busStops.get(i).getLocation().getLatitude(),
                                                            busStops.get(i).getLocation().getLongitude()))
                    .build();

            //Service service=Service.Builder.newInstance(Integer.toString(i))
            Service service=Service.Builder.newInstance(busStops.get(i).getName())
                    .addSizeDimension(CAPACITY_INDEX, busStops.get(i).getNumStudents())
                    .setLocation(pickupLocation)
                    .setServiceTime(busStops.get(i).getWaitTimeSecs())
                    .build();
            vrpBuilder.addJob(service);
        }
        vrpBuilder.addAllVehicles(buses);
        vrpBuilder.setRoutingCost(costMatrix).setFleetSize(VehicleRoutingProblem.FleetSize.FINITE);
        VehicleRoutingProblem problem = vrpBuilder.build();

        return(problem);
    }
}
