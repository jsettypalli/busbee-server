package jstech.edu.transportmodel.dto;

import jstech.edu.transportmodel.common.BusStop;

public class DistanceDurationDto {
    private BusStop fromBusStop;
    private BusStop toBusStop;
    private Long distanceInMeters;
    private Long timeInSeconds;
    private String viaPoints;

    public DistanceDurationDto(BusStop fromBusStop, BusStop toBusStop, Long distanceInMeters, Long timeInSeconds) {
        this(fromBusStop, toBusStop, distanceInMeters, timeInSeconds, null);
    }

    public DistanceDurationDto(BusStop fromBusStop, BusStop toBusStop, Long distanceInMeters, Long timeInSeconds, String viaPoints) {
        this.fromBusStop = fromBusStop;
        this.toBusStop = toBusStop;
        this.distanceInMeters = distanceInMeters;
        this.timeInSeconds = timeInSeconds;
        this.viaPoints = viaPoints;
    }

    public BusStop getFromBusStop() {
        return fromBusStop;
    }

    public BusStop getToBusStop() {
        return toBusStop;
    }

    public Long getDistanceInMeters() {
        return distanceInMeters;
    }

    public Long getTimeInSeconds() {
        return timeInSeconds;
    }

    public String getViaPoints() {
        return viaPoints;
    }

    @Override
    public String toString() {
        return "DistanceDurationDto{" +
                "fromBusStop=" + fromBusStop +
                ", toBusStop=" + toBusStop +
                ", distanceInMeters=" + distanceInMeters +
                ", timeInSeconds=" + timeInSeconds +
                ", viaPoints=" + viaPoints +
                '}';
    }
}
