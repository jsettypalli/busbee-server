package jstech.edu.transportmodel.service.route;

import com.graphhopper.jsprit.core.algorithm.state.StateId;
import com.graphhopper.jsprit.core.algorithm.state.StateManager;
import com.graphhopper.jsprit.core.problem.Location;
import com.graphhopper.jsprit.core.problem.constraint.HardActivityConstraint;
import com.graphhopper.jsprit.core.problem.misc.JobInsertionContext;
import com.graphhopper.jsprit.core.problem.solution.route.activity.End;
import com.graphhopper.jsprit.core.problem.solution.route.activity.TourActivity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by jitendra1 on 30-Dec-17.
 */
public class OccupancyConstraint implements HardActivityConstraint {
    private static final Logger logger = LoggerFactory.getLogger(OccupancyConstraint.class);

    private static final int DIMENSION_INDEX = 0;
    private final StateManager stateManager;
    private final double minOccupancyPercent;
    private final StateId occupancyStateId;

    public OccupancyConstraint(double minOccupancyPercent, StateId distanceStateId, StateManager stateManager)
    {
        this.minOccupancyPercent = minOccupancyPercent;
        this.stateManager = stateManager;
        this.occupancyStateId = distanceStateId;
    }

    public ConstraintsStatus fulfilled(JobInsertionContext context, TourActivity prevAct, TourActivity newAct, TourActivity nextAct, double v)
    {
        Location currentLocation = newAct.getLocation();
        Location endLocation = context.getRoute().getEnd().getLocation();
        if(endLocation == null) {
            return ConstraintsStatus.FULFILLED;
        }

        Location nextLocation = nextAct.getLocation();
        //if(nextAct instanceof End) {
        if(currentLocation.getId().equals(endLocation.getId())) {
            Integer  currentLoad = stateManager.getRouteState(context.getRoute(), occupancyStateId, Integer.class);
            if(currentLoad != null) {
                System.out.println("Current Capacity is   :   "+ currentLoad);
            }
            if (currentLoad == null) {
                currentLoad = 0;
            }

            currentLoad += newAct.getSize().get(DIMENSION_INDEX);
            double vehicleCapacity = context.getRoute().getVehicle().getType().getCapacityDimensions().get(DIMENSION_INDEX);
            double loadPercent = 0;
            if(vehicleCapacity > 0) {
                loadPercent = (currentLoad/vehicleCapacity) * 100;
            }

            logger.debug("currentLocationId:{}, currentLoad:{}, vehicleType:{}, " +
                            "vehicleCapacity:{}, loadPercent:{}",
                    currentLocation.getId(), currentLoad, context.getRoute().getVehicle().getType().getTypeId(),
                    vehicleCapacity, loadPercent);

            return (loadPercent >= minOccupancyPercent) ? ConstraintsStatus.FULFILLED
                    : ConstraintsStatus.NOT_FULFILLED;


            /*
            int newCapacity=capacity;
            if (newCapacity < minOccupancyPercent*JSpritRouteService.WEIGHT_INDEX)
                return ConstraintsStatus.NOT_FULFILLED;
            else return ConstraintsStatus.FULFILLED;
            */
        }

        return ConstraintsStatus.FULFILLED;
    }
}
