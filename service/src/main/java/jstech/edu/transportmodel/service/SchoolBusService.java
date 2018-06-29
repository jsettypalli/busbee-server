package jstech.edu.transportmodel.service;

import jstech.edu.transportmodel.common.*;

import java.util.*;

/**
 * Created by jitendra1 on 03-Dec-17.
 */

public interface SchoolBusService {

    List<SchoolBus> getSchoolBuses(School school);
    SchoolBus getSchoolBus(int id);
    SchoolBus getSchoolBus(String registrationNumber);

    SchoolBus addSchoolBus(SchoolBus vehicle);
    SchoolBus updateSchoolBus(SchoolBus vehicle);
    SchoolBus deleteSchoolBus(SchoolBus vehicle);

    List<BusStop> getBusPoints(boolean pickup, School school);
    List<BusStop> getBusPoints(boolean pickup, SchoolBus schoolBus);
    List<BusStop> getBusPoints(String name, School school);
    BusStop getBusPoint(String name, boolean pickup, School school);

    BusStop addBusPoint(BusStop busStop);
    BusStop updateBusPoint(BusStop busStop);
    BusStop deleteBusPoint(BusStop busStop);

    List<SchoolBus> getSchoolBusesAssociatedWithUser(UserInfo userInfo);
    SchoolBus getSchoolBusByDriver(UserInfo userInfo);
    List<SchoolBus> getSchoolBusesByParent(UserInfo userInfo);
    List<SchoolBus> getSchoolBusesByTransportIncharge(UserInfo userInfo);

    List<BusStop> getBusStopsByParent(UserInfo userInfo);
}
