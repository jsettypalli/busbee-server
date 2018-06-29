package jstech.edu.transportmodel.dao;

import jstech.edu.transportmodel.common.BusStop;
import jstech.edu.transportmodel.common.School;
import jstech.edu.transportmodel.common.SchoolBus;
import jstech.edu.transportmodel.common.UserInfo;

import java.util.List;

public interface BusStopDao {
    List<BusStop> getBusPoints(boolean pickup, School school);
    List<BusStop> getBusPoints(boolean pickup, SchoolBus schoolBus);
    List<BusStop> getBusPoints(String name, School school);
    BusStop getBusPoint(String name, boolean pickup, School school);
    BusStop getBusPoint(int busPointId, boolean pickup);
    List<BusStop> getBusStopsByParent(UserInfo userInfo);

    BusStop addBusPoint(BusStop busStop);
    BusStop updateBusPoint(BusStop busStop);
    BusStop deleteBusPoint(BusStop busStop);
}
