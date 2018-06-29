package jstech.edu.transportmodel.common;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BusTrip {
    private static final Logger logger = LoggerFactory.getLogger(BusTrip.class);

    private int tripId;
    private String name;
    private School school;

    // schedule string is expected to be in cron format
    private String schedule;
    private boolean approved;
    private boolean pickup;

    private ZonedDateTime startTime;
    private Map<SchoolBus, SchoolBusRoute> busRoutes = new HashMap<>();

    public int getTripId() {
        return tripId;
    }

    public void setTripId(int tripId) {
        this.tripId = tripId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public School getSchool() {
        return school;
    }

    public void setSchool(School school) {
        this.school = school;
    }

    public String getSchedule() {
        return schedule;
    }

    public void setSchedule(String schedule) {
        this.schedule = schedule;
    }

    public boolean isApproved() {
        return approved;
    }

    public void setApproved(boolean approved) {
        this.approved = approved;
    }

    public boolean isPickup() {
        return pickup;
    }

    public void setPickup(boolean pickup) {
        this.pickup = pickup;
    }

    public ZonedDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(ZonedDateTime startTime) {
        this.startTime = startTime;
    }

    public Map<SchoolBus, SchoolBusRoute> getBusRoutes() {
        return busRoutes;
    }

    public SchoolBusRoute getBusRoute(SchoolBus schoolBus) {
        return busRoutes.get(schoolBus);
    }

    public void setBusRoutes(Map<SchoolBus, SchoolBusRoute> busRoutes) {
        this.busRoutes = busRoutes;
    }

    public void addBusRoute(SchoolBus schoolBus, SchoolBusRoute busRoute) {
        busRoutes.put(schoolBus, busRoute);
    }

    public SchoolBusRoute removeBusRoute(SchoolBus schoolBus) {
        return  busRoutes.remove(schoolBus);
    }


    @Override
    public boolean equals(Object o) {
        logger.debug("checking if objects are equal. current trip:{}, object-trip:{}",this, o);
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BusTrip busTrip = (BusTrip) o;
        if(this.tripId == busTrip.tripId) {
            return true;
        }
        return  this.tripId == busTrip.tripId ||
                (Objects.equals(school, busTrip.school) &&
                Objects.equals(schedule, busTrip.schedule) &&
                Objects.equals(busRoutes, busTrip.busRoutes));
    }

    @Override
    public int hashCode() {

        return Objects.hash(tripId, school, schedule, busRoutes);
    }

    @Override
    public String toString() {
        return "BusTrip{" +
                "tripId=" + tripId +
                ", name='" + name + '\'' +
                ", school=" + school +
                ", schedule='" + schedule + '\'' +
                ", approved=" + approved +
                ", startTime=" + startTime +
                ", busRoutes=" + busRoutes +
                '}';
    }
}
