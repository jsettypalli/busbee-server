package jstech.edu.transportmodel.service.route;

import com.graphhopper.jsprit.core.algorithm.state.StateId;
import com.graphhopper.jsprit.core.algorithm.state.StateManager;
import com.graphhopper.jsprit.core.algorithm.state.StateUpdater;
import com.graphhopper.jsprit.core.problem.solution.route.VehicleRoute;
import com.graphhopper.jsprit.core.problem.solution.route.activity.ActivityVisitor;
import com.graphhopper.jsprit.core.problem.solution.route.activity.TourActivity;

/**
 * Created by jitendra1 on 30-Dec-17.
 */
public class MinOccupancyUpdater implements StateUpdater, ActivityVisitor {
    private final StateManager stateManager;
    private final StateId occupancyStateId;
    private VehicleRoute vehicleRoute;
    private int occupancy = 0;
    private TourActivity prevAct;

    public MinOccupancyUpdater(StateId occupancyStateId, StateManager stateManager){
        this.stateManager = stateManager;
        this.occupancyStateId = occupancyStateId;
    }

    public void begin(VehicleRoute vehicleRoute) {
        occupancy = 0;
        prevAct = vehicleRoute.getStart();
        this.vehicleRoute = vehicleRoute;
    }

    public void visit(TourActivity tourActivity) {
        occupancy += tourActivity.getSize().get(JSpritRouteService.CAPACITY_INDEX);
        prevAct = tourActivity;
    }


    public void finish() {
        occupancy += prevAct.getSize().get(JSpritRouteService.CAPACITY_INDEX);
        stateManager.putRouteState(vehicleRoute, occupancyStateId, occupancy);
    }
}
