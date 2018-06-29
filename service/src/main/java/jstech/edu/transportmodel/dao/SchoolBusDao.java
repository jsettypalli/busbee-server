package jstech.edu.transportmodel.dao;

import jstech.edu.transportmodel.common.*;
import java.util.List;


/**
 * Created by jitendra1 on 03-Dec-17.
 */

public interface SchoolBusDao {
    List<SchoolBus> getSchoolBuses(School school);
    SchoolBus getSchoolBus(int id);
    SchoolBus getSchoolBus(String registrationNumber);

    SchoolBus addSchoolBus(SchoolBus schoolBus);
    SchoolBus updateSchoolBus(SchoolBus schoolBus);
    SchoolBus deleteSchoolBus(SchoolBus schoolBus);

    SchoolBus getSchoolBusByDriver(String userName);
    List<SchoolBus> getSchoolBusesByParent(String userName);
    List<SchoolBus> getSchoolBusesByTransportIncharge(String userName);
}
