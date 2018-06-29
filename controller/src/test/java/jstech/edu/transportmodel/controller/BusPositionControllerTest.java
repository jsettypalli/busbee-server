package jstech.edu.transportmodel.controller;

import jstech.edu.transportmodel.common.BusPosition;
import jstech.edu.transportmodel.service.route.RouteService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.List;

//@RunWith(SpringRunner.class)
//@SpringBootTest
public class BusPositionControllerTest {

    @Autowired
    private RouteService routeService;

    private int tripId = 1;
    private int busId = 1;
    private List<BusPosition> busPositions;

    //@Before
    public void makeBusPositions() {
        busPositions = new ArrayList<>();
        busPositions.add(makeBusPosition(17.466696, 78.367361)); // siesta hotel
        busPositions.add(makeBusPosition(17.462847, 78.369934)); // Casa Rouge
        busPositions.add(makeBusPosition(17.459420, 78.366270)); // kothaguda signal
        busPositions.add(makeBusPosition(17.453597, 78.366742)); // bhavya's alluri meadows
        busPositions.add(makeBusPosition(17.454906, 78.370133)); // 3 cube towers
        busPositions.add(makeBusPosition(17.466546, 78.374916)); // Gem Ascent
        busPositions.add(makeBusPosition(17.468541, 78.376289)); // hitex minar entrance
        busPositions.add(makeBusPosition(17.466533, 78.376466)); // Alankrita Residency
        busPositions.add(makeBusPosition(17.464334, 78.375531)); // Meenakshi Towers
        busPositions.add(makeBusPosition(17.466342, 78.371881)); // Aditya Sunshine
        busPositions.add(makeBusPosition(17.463801, 78.369735)); // Reliance Fresh
        busPositions.add(makeBusPosition(17.464306, 78.368790)); // Aparna Towers
        busPositions.add(makeBusPosition(17.468709, 78.370074)); // school

        routeService.createRoutePlan();
    }

    //@Test
    public void testSendNotifications() {
        for(BusPosition busPosition: busPositions) {
            routeService.sendBusArrivalNotifications(busPosition);
        }

    }

    private BusPosition makeBusPosition(double latitude, double longitude) {
        return new BusPosition.Builder().setTripId(tripId).setBusId(busId).setLocation(latitude, longitude).build();
    }
}
