package jstech.edu.transportmodel.common;

import java.time.ZonedDateTime;
import java.util.Objects;

/**
 * Created by jitendra1 on 07-Dec-17.
 */
public class BusStop {

    static final int WAIT_MINS_AT_EACH_POINT = 0;

    private int id;
    private String name;
    private String address;
    private int numStudents;
    private School school;
    private int waitTimeMins;

    private int busStopDetailId;
    private GeoLocation location;
    private boolean pickupPoint;

    private int relativeArrivalTimeSecs;
    private int relativeDepartureTimeSecs;
    private int relativeDistanceMtrs;

    private ZonedDateTime expectedArrivalTime;
    private ZonedDateTime expectedDepartureTime;

    private ZonedDateTime actualArrivalTime;
    private ZonedDateTime actualDepartureTime;

    private BusStop(int id, String name, String address, int numStudents, School school, int waitTimeMins,
                    int busStopDetailId, GeoLocation location, boolean pickupPoint) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.numStudents = numStudents;
        this.school = school;
        this.waitTimeMins = waitTimeMins;

        this.busStopDetailId = busStopDetailId;
        this.location = location;
        this.pickupPoint = pickupPoint;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public School getSchool() {
        return school;
    }

    public boolean isPickupPoint() {
        return pickupPoint;
    }

    public String getAddress() {
        return address;
    }

    public int getNumStudents() {
        return numStudents;
    }

    public int getBusStopDetailId() {
        return busStopDetailId;
    }

    public GeoLocation getLocation() {
        return location;
    }

    public int getWaitTimeMins() {
        return waitTimeMins;
    }

    public int getWaitTimeSecs() {
        return waitTimeMins * 60;
    }

    public int getRelativeArrivalTimeSecs() {
        return relativeArrivalTimeSecs;
    }

    public void setRelativeArrivalTimeSecs(int relativeArrivalTimeSecs) {
        this.relativeArrivalTimeSecs = relativeArrivalTimeSecs;
    }

    public int getRelativeDistanceMtrs() {
        return relativeDistanceMtrs;
    }

    public void setRelativeDistanceMtrs(int relativeDistanceMtrs) {
        this.relativeDistanceMtrs = relativeDistanceMtrs;
    }

    public int getRelativeDepartureTimeSecs() {
        return relativeDepartureTimeSecs;
    }

    public void setRelativeDepartureTimeSecs(int relativeDepartureTimeSecs) {
        this.relativeDepartureTimeSecs = relativeDepartureTimeSecs;
    }

    public ZonedDateTime getExpectedArrivalTime() {
        return expectedArrivalTime;
    }

    public void setExpectedArrivalTime(ZonedDateTime expectedArrivalTime) {
        this.expectedArrivalTime = expectedArrivalTime;
    }

    public ZonedDateTime getExpectedDepartureTime() {
        return expectedDepartureTime;
    }

    public void setExpectedDepartureTime(ZonedDateTime expectedDepartureTime) {
        this.expectedDepartureTime = expectedDepartureTime;
    }

    public ZonedDateTime getActualArrivalTime() {
        return actualArrivalTime;
    }

    public void setActualArrivalTime(ZonedDateTime actualArrivalTime) {
        this.actualArrivalTime = actualArrivalTime;
    }

    public ZonedDateTime getActualDepartureTime() {
        return actualDepartureTime;
    }

    public void setActualDepartureTime(ZonedDateTime actualDepartureTime) {
        this.actualDepartureTime = actualDepartureTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BusStop busStop = (BusStop) o;
        return pickupPoint == busStop.pickupPoint &&
                Objects.equals(name, busStop.name) &&
                Objects.equals(school, busStop.school) &&
                Objects.equals(location, busStop.location);
    }

    @Override
    public int hashCode() {

        return Objects.hash(name, school, location, pickupPoint);
    }

    @Override
    public String toString() {
        return "BusStop{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", address='" + address + '\'' +
                ", numStudents=" + numStudents +
                ", school=" + school +
                ", location=" + location +
                ", pickupPoint=" + pickupPoint +
                ", waitTimeMins=" + waitTimeMins +
                ", relativeArrivalTimeSecs=" + relativeArrivalTimeSecs +
                ", relativeDepartureTimeSecs=" + relativeDepartureTimeSecs +
                ", relativeDistanceMtrs=" + relativeDistanceMtrs +
                ", expectedArrivalTime=" + expectedArrivalTime +
                ", expectedDepartureTime=" + expectedDepartureTime +
                ", actualArrivalTime=" + actualArrivalTime +
                ", actualDepartureTime=" + actualDepartureTime +
                '}';
    }

    public static class Builder {
        private int id;
        private String name;
        private String address;
        private int numStudents;
        private School school;
        private int busStopDetailId;
        private GeoLocation location;
        private boolean pickupPoint = true;
        private int waitTimeMins = WAIT_MINS_AT_EACH_POINT;

        public BusStop build() {
            if(name == null || name.isEmpty() || school == null || location == null) {
                return null;
            }
            return new BusStop(id, name, address, numStudents, school, waitTimeMins, busStopDetailId, location, pickupPoint);
        }

        public BusStop build(BusStop busStop) {
            return new BusStop(busStop.getId(), busStop.getName(), busStop.getAddress(), busStop.getNumStudents(), busStop.getSchool(), busStop.getWaitTimeMins(),
                    busStop.getBusStopDetailId(), busStop.getLocation(), busStop.isPickupPoint());
        }

        public Builder setId(int id) {
            this.id = id;
            return this;
        }

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setAddress(String address) {
            this.address = address;
            return this;
        }

        public Builder setNumStudents(int numStudents) {
            this.numStudents = numStudents;
            return this;
        }

        public Builder setSchool(School school) {
            this.school = school;
            return this;
        }

        public Builder setBusStopDetailId(int busStopDetailId) {
            this.busStopDetailId = busStopDetailId;
            return this;
        }

        public Builder setLocation(GeoLocation location) {
            this.location = location;
            return this;
        }

        public Builder setLocation(double latitude, double longitude) {
            this.location = new GeoLocation(latitude, longitude);
            return this;
        }

        public Builder setPickupPoint(boolean pickupPoint) {
            this.pickupPoint = pickupPoint;
            return this;
        }

        public Builder setWaitTimeMins(int waitTimeMins) {
            this.waitTimeMins = waitTimeMins;
            return this;
        }
    }
}
