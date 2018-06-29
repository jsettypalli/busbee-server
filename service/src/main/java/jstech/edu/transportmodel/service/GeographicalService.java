package jstech.edu.transportmodel.service;

import javafx.util.Pair;
import jstech.edu.transportmodel.GeoException;
import jstech.edu.transportmodel.common.BusStop;
import jstech.edu.transportmodel.common.GeoLocation;
import jstech.edu.transportmodel.dto.DistanceDurationDto;

import java.util.List;

/**
 * Created by jitendra1 on 25-Dec-17.
 */

public interface GeographicalService {

    GeoLocation getGeoLocationFromAddress(String address);
    Pair<Long, Long> getDistanceAndDuration(GeoLocation origin, GeoLocation destination) throws GeoException;

    DistanceDurationDto getDistanceAndDuration(BusStop origin, BusStop destination) throws GeoException;
    List<DistanceDurationDto> getDistanceMatrix(BusStop origin, List<BusStop> destinations) throws GeoException;
    List<DistanceDurationDto> getDistanceMatrix(List<BusStop> origins, List<BusStop> destinations) throws GeoException;
}
