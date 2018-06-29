package jstech.edu.transportmodel.common;

/**
 * Created by jitendra1 on 02-Dec-17.
 */
public class BusPosition {
    private int tripId;
    private int busId;
    private GeoLocation location;

    public int getTripId() {
        return tripId;
    }

    public int getBusId() {
        return busId;
    }

    public GeoLocation getLocation() {
        return location;
    }


    private BusPosition(int tripId, int busId, GeoLocation location) {
        this.tripId = tripId;
        this.busId = busId;
        this.location = location;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BusPosition that = (BusPosition) o;

        /*if (busId != that.busId) return false;
        return location.equals(that.location);*/
        return busId==that.busId && location.equals(that.location);
    }

    @Override
    public int hashCode() {
        int result = busId;
        result = 31 * result + location.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "BusPosition{" +
                "busId=" + busId +
                ", location=" + location +
                '}';
    }

    public static class Builder {
        private int tripId;
        private int busId;
        private GeoLocation location;

        public Builder setTripId(int tripId) {
            this.tripId = tripId;
            return this;
        }

        public Builder setBusId(int busId) {
            this.busId = busId;
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

        public BusPosition build() {
            return new BusPosition(tripId, busId,location);
        }
    }
}
