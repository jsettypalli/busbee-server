package jstech.edu.transportmodel.dto;

import java.util.Objects;

public class BusPositionDto {
    private int busId;
    private double latitude;
    private double longitude;

    public int getBusId() {
        return busId;
    }

    public void setBusId(int busId) {
        this.busId = busId;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BusPositionDto that = (BusPositionDto) o;
        return busId == that.busId &&
                Double.compare(that.latitude, latitude) == 0 &&
                Double.compare(that.longitude, longitude) == 0;
    }

    @Override
    public int hashCode() {

        return Objects.hash(busId, latitude, longitude);
    }

    @Override
    public String toString() {
        return "BusPositionDto{" +
                "busId=" + busId +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                '}';
    }
}
