package jstech.edu.transportmodel.common;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Created by jitendra1 on 17-Dec-17.
 */
public class GeoLocation {

    private int id;
    private double latitude;
    private double longitude;

    public GeoLocation(double latitude, double longitude) {
        this(0, latitude, longitude);
    }

    public GeoLocation(int id, double latitude, double longitude) {
        this.id = id;
        this.latitude = BigDecimal.valueOf(latitude).setScale(12, RoundingMode.HALF_UP).doubleValue();
        this.longitude = BigDecimal.valueOf(longitude).setScale(12, RoundingMode.HALF_UP).doubleValue();
    }

    public int getId() {
        return id;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GeoLocation that = (GeoLocation) o;

        /*if (Double.compare(that.latitude, latitude) != 0) return false;
        return Double.compare(that.longitude, longitude) == 0;*/
        return Double.compare(that.latitude, latitude)==0 &&
                Double.compare(that.longitude, longitude) == 0;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        temp = Double.doubleToLongBits(latitude);
        result = (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(longitude);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return "GeoLocation{" +
                "id=" + id +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                '}';
    }
}
