package jstech.edu.transportmodel.service;


import jstech.edu.transportmodel.common.*;
import jstech.edu.transportmodel.dao.BusStopDao;
import jstech.edu.transportmodel.dao.SchoolBusDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class SchoolBusServiceImpl implements SchoolBusService {
    private static final Logger logger = LoggerFactory.getLogger(SchoolBusService.class);

    @Autowired
    private SchoolBusDao schoolBusDao;

    @Autowired
    private BusStopDao busStopDao;

    public List<SchoolBus> getSchoolBuses(School school) {
        return schoolBusDao.getSchoolBuses(school);
    }

    @Override
    public SchoolBus getSchoolBus(int id) {
        return schoolBusDao.getSchoolBus(id);
    }

    @Override
    public SchoolBus getSchoolBus(String registrationNumber) {
        return schoolBusDao.getSchoolBus(registrationNumber);
    }

    @Override
    @Transactional
    public SchoolBus addSchoolBus(SchoolBus schoolBus) {
        return schoolBusDao.addSchoolBus(schoolBus);
    }

    @Override
    @Transactional
    public SchoolBus updateSchoolBus(SchoolBus schoolBus) {
        return schoolBusDao.updateSchoolBus(schoolBus);
    }

    @Override
    @Transactional
    public SchoolBus deleteSchoolBus(SchoolBus schoolBus) {
        return schoolBusDao.deleteSchoolBus(schoolBus);
    }


    @Override
    public List<BusStop> getBusPoints(boolean pickup, School school) {
        return busStopDao.getBusPoints(pickup, school);
    }

    @Override
    public List<BusStop> getBusPoints(boolean pickup, SchoolBus schoolBus) {
        return busStopDao.getBusPoints(pickup, schoolBus);
    }

    @Override
    public List<BusStop> getBusPoints(String name, School school) {
        return busStopDao.getBusPoints(name, school);
    }

    @Override
    public BusStop getBusPoint(String name, boolean pickup, School school) {
        return busStopDao.getBusPoint(name, pickup, school);
    }

    @Override
    @Transactional
    public BusStop addBusPoint(BusStop busStop) {
        return busStopDao.addBusPoint(busStop);
    }

    @Override
    @Transactional
    public BusStop updateBusPoint(BusStop busStop) {
        return busStopDao.updateBusPoint(busStop);
    }

    @Override
    @Transactional
    public BusStop deleteBusPoint(BusStop busStop) {
        return busStopDao.deleteBusPoint(busStop);
    }

    @Override
    public List<SchoolBus> getSchoolBusesAssociatedWithUser(UserInfo userInfo) {
        List<SchoolBus> buses = new ArrayList<>();
        switch(userInfo.getRole()) {
            case DRIVER:
                SchoolBus bus = getSchoolBusByDriver(userInfo);
                if(bus != null) {
                    buses.add(bus);
                }
                break;
            case PARENT:
                buses = getSchoolBusesByParent(userInfo);
                break;
            case TRANSPORT_INCHARGE:
                buses = getSchoolBusesByTransportIncharge(userInfo);
                break;
        }

        return buses;
    }

    @Override
    public SchoolBus getSchoolBusByDriver(UserInfo userInfo) {
        return schoolBusDao.getSchoolBusByDriver(userInfo.getKeyProviderUserName());
    }

    @Override
    public List<SchoolBus> getSchoolBusesByParent(UserInfo userInfo) {
        return schoolBusDao.getSchoolBusesByParent(userInfo.getKeyProviderUserName());
    }

    @Override
    public List<SchoolBus> getSchoolBusesByTransportIncharge(UserInfo userInfo) {
        return schoolBusDao.getSchoolBusesByTransportIncharge(userInfo.getKeyProviderUserName());
    }

    @Override
    public List<BusStop> getBusStopsByParent(UserInfo userInfo) {
        return busStopDao.getBusStopsByParent(userInfo);
    }
}
