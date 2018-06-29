package jstech.edu.transportmodel.common;

import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Created by jitendra1 on 07-Dec-17.
 */
public class SchoolBusRoute {

    private int id;
    private String name;
    private BusStop startingPoint;
    private LocalTime startTime;
    private ZonedDateTime startDateTime;
    private List<BusStop> busStops = new ArrayList<>();
    private BusStop destination;
    private ZonedDateTime endTime;
    private RouteStatus routeStatus;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BusStop getStartingPoint() {
        return startingPoint;
    }

    public void setStartingPoint(BusStop startingPoint) {
        this.startingPoint = startingPoint;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public ZonedDateTime getStartDateTime() {
        return startDateTime;
    }

    public void setStartDateTime(ZonedDateTime startDateTime) {
        this.startDateTime = startDateTime;
    }

    public List<BusStop> getBusStops() {
        return busStops;
    }

    public void setBusStops(List<BusStop> busStops) {
        this.busStops = busStops;
    }

    public void addPickupPoint(BusStop busStop) {
        busStops.add(busStop);
    }

    public BusStop getDestination() {
        return destination;
    }

    public void setDestination(BusStop destination) {
        this.destination = destination;
    }

    public RouteStatus getRouteStatus() {
        return routeStatus;
    }

    public void setRouteStatus(RouteStatus routeStatus) {
        this.routeStatus = routeStatus;
    }

    public ZonedDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(ZonedDateTime endTime) {
        this.endTime = endTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SchoolBusRoute busRoute = (SchoolBusRoute) o;
        /*if(this.id == busRoute.id) {
            return true;
        }*/
        return this.id == busRoute.id ||
                (Objects.equals(name, busRoute.name) &&
                Objects.equals(startingPoint, busRoute.startingPoint) &&
                Objects.equals(startDateTime, busRoute.startDateTime) &&
                Objects.equals(busStops, busRoute.busStops) &&
                Objects.equals(destination, busRoute.destination) &&
                Objects.equals(endTime, busRoute.endTime) &&
                routeStatus == busRoute.routeStatus);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, startingPoint, startDateTime, busStops, destination, endTime, routeStatus);
    }

    @Override
    public String toString() {
        return "SchoolBusRoute{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", startingPoint=" + startingPoint +
                ", startDateTime=" + startDateTime +
                ", busStops=" + busStops +
                ", destination=" + destination +
                ", endTime=" + endTime +
                ", routeStatus=" + routeStatus +
                '}';
    }
}
