package jstech.edu.transportmodel.dao;

import jstech.edu.transportmodel.common.GeoLocation;

import java.util.List;

/**
 * Created by jitendra1 on 27-Dec-17.
 */

public interface LocationDao {
    GeoLocation getLocation(int locationId);
    GeoLocation getLocation(double latitude, double longitude);

    GeoLocation addLocation(double latitude, double longitude);
    GeoLocation deleteLocation(double latitude, double longitude);

    List<GeoLocation> getViaPointLocationsBetweenBusStopsForRouteDetermination(int fromBusStopDetailsId, int toBusStopDetailsid);
}
