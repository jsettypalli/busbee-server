package jstech.edu.transportmodel.service.route;

import com.graphhopper.jsprit.core.util.FastVehicleRoutingTransportCostsMatrix;
import jstech.edu.transportmodel.common.BusStop;
import jstech.edu.transportmodel.dto.DistanceDurationDto;
import jstech.edu.transportmodel.service.GeographicalService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Created by jitendra1 on 30-Dec-17.
 */
@Service
public class TransportCostService {

    private static final Logger logger = LoggerFactory.getLogger(TransportCostService.class);

    @Autowired
    private GeographicalService geographicalService;

    public FastVehicleRoutingTransportCostsMatrix prepareCostMatrix(BusStop startingPoint, List<BusStop> busStops) {
        FastVehicleRoutingTransportCostsMatrix.Builder costMatrixBuilder = FastVehicleRoutingTransportCostsMatrix.Builder.newInstance(busStops.size()+1, false);

        Map<BusStop, Integer> locationIndexMap = getPoints(startingPoint, busStops);
        Set<BusStop> pointset = locationIndexMap.keySet();
        List<BusStop> points = new ArrayList<>(pointset);
        //points.addAll(pointset);

        // TODO -- It is more efficient fetch DistanceMatrix from all points to all points with one call.
        //          Explore the costing model. Are we charged for number of calls made to distance matrix api?
        //      or are we charged for number of points for which distance matrix is prepared.
        //      If so, explore if there is intelligent way to make less number of calls and reduce the cost (like caching etc)

        try {
            List<DistanceDurationDto> distanceDurationDtos = geographicalService.getDistanceMatrix(points, points);
            for(DistanceDurationDto distanceDurationDto: distanceDurationDtos) {

                logger.debug("from:" + distanceDurationDto.getFromBusStop().getName() + ", to:" + distanceDurationDto.getToBusStop().getName() +
                        ", distance:" +distanceDurationDto.getDistanceInMeters() + ", duration:" + distanceDurationDto.getTimeInSeconds());

                // TODO - remove below statement when logging to file is working
                System.out.println("from:" + distanceDurationDto.getFromBusStop().getName() + ", to:" + distanceDurationDto.getToBusStop().getName() +
                        ", distance:" +distanceDurationDto.getDistanceInMeters() + ", duration:" + distanceDurationDto.getTimeInSeconds());

                costMatrixBuilder.addTransportDistance(locationIndexMap.get(distanceDurationDto.getFromBusStop()),
                        locationIndexMap.get(distanceDurationDto.getToBusStop()), distanceDurationDto.getDistanceInMeters());
                costMatrixBuilder.addTransportTime(locationIndexMap.get(distanceDurationDto.getFromBusStop()),
                        locationIndexMap.get(distanceDurationDto.getToBusStop()), distanceDurationDto.getTimeInSeconds());
            }
        } catch (Exception e) {
            logger.error("{} occurred while building Cost Matrix between points.", e.getClass().getName(), e);
        }

        return costMatrixBuilder.build();
    }

    private static Map<BusStop, Integer> getPoints(BusStop startingPoint, List<BusStop> busStops) {
        Map<BusStop, Integer> locationIndexMap = new HashMap<>();

        locationIndexMap.put(startingPoint,  0);

        int i =1;
        for(BusStop point: busStops) {
            locationIndexMap.put(point, i++);
        }
        return locationIndexMap;
    }
}
