package jstech.edu.transportmodel.controller;

import jstech.edu.transportmodel.AppMain;
import jstech.edu.transportmodel.common.BusPosition;
import jstech.edu.transportmodel.common.BusStop;
import jstech.edu.transportmodel.common.RouteStatus;
import jstech.edu.transportmodel.dto.BusPositionDto;
import jstech.edu.transportmodel.service.NotificationService;
import jstech.edu.transportmodel.service.route.RouteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by jitendra1 on 02-Dec-17.
 */
@RestController
@RequestMapping(AppMain.REST_BASE_PATH)
public class BusPositionController {

    private static final Logger logger = LoggerFactory.getLogger(BusPositionController.class);

    @Autowired
    private RouteService routeService;

    @Autowired
    private NotificationService notificationService;


    @MessageMapping("/busposition/{tripId}/{busId}")
    public void busPosition(@DestinationVariable int tripId, @DestinationVariable int busId, BusPositionDto position) {
        logger.debug(" in BusPositionController...tripId:{}, busId:{}", tripId, busId);

        if(RouteStatus.COMPLETED == routeService.getRouteStatus(tripId, busId)) {
            logger.warn("Bus position is sent for the route that is already completed. trip_id:{}, bus_id:{}", tripId, busId);
            return;
        }

        BusPosition busPosition = new BusPosition.Builder()
                                    .setTripId(tripId)
                                    .setBusId(busId)
                                    .setLocation(position.getLatitude(), position.getLongitude())
                                    .build();

        //send bus position to all subscribers. next bus stop in the route should be included in the message so line can be drawn on the map.
        BusStop nextBusStopInTheRoute = routeService.getNextBusStopInTheRoute(tripId, busId);
        notificationService.publishBusPosition(busPosition, nextBusStopInTheRoute);

        // TODO - Send Bus Arrival Notifications in another thread.
        //      This will send the response back to client (driver app) immediately.
        //      Regular producer/consumer solution is to add position to a BlockingQueue and let a thread pickup from this queue
        //      However, evaluate tools/solutions based on LMAX Disruptor approach.
        //      log4j2 uses this type of solution is exponentially faster than normal BlockingQueue based implementation.
        routeService.sendBusArrivalNotifications(busPosition);
    }

    @MessageMapping("/parentposition/{busId}")
    // Should ParentPosition object be created?
    public void parentPosition(@DestinationVariable int busId, BusPosition position) {

    }
}
