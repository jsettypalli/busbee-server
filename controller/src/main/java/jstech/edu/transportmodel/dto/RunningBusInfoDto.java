package jstech.edu.transportmodel.dto;

import jstech.edu.transportmodel.common.GeoLocation;

import java.util.List;
import java.util.Objects;

public class RunningBusInfoDto {
    private String role;
    private int tripId;
    private int busId;
    private boolean inTransit;
    private String startDateTime;
    private GeoLocation currentLocation;
    //private List<BusStopDto> visitedBusStops;
    private BusStopDto nextBusStop;
    private BusStopDto prevBusStop;

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public int getTripId() {
        return tripId;
    }

    public void setTripId(int tripId) {
        this.tripId = tripId;
    }

    public int getBusId() {
        return busId;
    }

    public void setBusId(int busId) {
        this.busId = busId;
    }

    public boolean isInTransit() {
        return inTransit;
    }

    public void setInTransit(boolean inTransit) {
        this.inTransit = inTransit;
    }

    public String getStartDateTime() {
        return startDateTime;
    }

    public void setStartDateTime(String startDateTime) {
        this.startDateTime = startDateTime;
    }

    public GeoLocation getCurrentLocation() {
        return currentLocation;
    }

    public void setCurrentLocation(GeoLocation currentLocation) {
        this.currentLocation = currentLocation;
    }

    /*public List<BusStopDto> getVisitedBusStops() {
        return visitedBusStops;
    }

    public void setVisitedBusStops(List<BusStopDto> visitedBusStops) {
        this.visitedBusStops = visitedBusStops;
    }*/

    public BusStopDto getNextBusStop() {
        return nextBusStop;
    }

    public void setNextBusStop(BusStopDto nextBusStop) {
        this.nextBusStop = nextBusStop;
    }

    public BusStopDto getPrevBusStop() {
        return prevBusStop;
    }

    public void setPrevBusStop(BusStopDto prevBusStop) {
        this.prevBusStop = prevBusStop;
    }

    public BusStopDto createBusStopDto() {
        return new BusStopDto();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RunningBusInfoDto that = (RunningBusInfoDto) o;
        return tripId == that.tripId &&
                busId == that.busId &&
                inTransit == that.inTransit &&
                Objects.equals(role, that.role) &&
                Objects.equals(startDateTime, that.startDateTime) &&
                Objects.equals(currentLocation, that.currentLocation) &&
                Objects.equals(nextBusStop, that.nextBusStop) &&
                Objects.equals(prevBusStop, that.prevBusStop);
    }

    @Override
    public int hashCode() {

        return Objects.hash(role, tripId, busId, inTransit, startDateTime, currentLocation, nextBusStop, prevBusStop);
    }

    public class BusStopDto {
        private int busStopDetailsId;
        private String busStopName;
        private GeoLocation location;

        public int getBusStopDetailsId() {
            return busStopDetailsId;
        }

        public void setBusStopDetailsId(int busStopDetailsId) {
            this.busStopDetailsId = busStopDetailsId;
        }

        public String getBusStopName() {
            return busStopName;
        }

        public void setBusStopName(String busStopName) {
            this.busStopName = busStopName;
        }

        public GeoLocation getLocation() {
            return location;
        }

        public void setLocation(GeoLocation location) {
            this.location = location;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            BusStopDto that = (BusStopDto) o;
            return busStopDetailsId == that.busStopDetailsId &&
                    Objects.equals(busStopName, that.busStopName) &&
                    Objects.equals(location, that.location);
        }

        @Override
        public int hashCode() {

            return Objects.hash(busStopDetailsId, busStopName, location);
        }
    }
}
