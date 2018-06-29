package jstech.edu.transportmodel.service;

import com.google.maps.DistanceMatrixApi;
import com.google.maps.DistanceMatrixApiRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.errors.ApiException;
import com.google.maps.model.*;
import javafx.util.Pair;
import jstech.edu.transportmodel.GeoException;
import jstech.edu.transportmodel.common.BusStop;
import jstech.edu.transportmodel.common.GeoLocation;
import jstech.edu.transportmodel.dto.DistanceDurationDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

//@Service
public class GoogleGeoService implements GeographicalService {
    private static final String API_KEY = "AIzaSyBOa2lU1d3jHGjok5AxDSUG8ipzEcpXLlc";

    private static final Logger logger = LoggerFactory.getLogger(GeographicalService.class);
    private GeoApiContext context = new GeoApiContext.Builder().apiKey(API_KEY).build();

    public GeoLocation getGeoLocationFromAddress(String address) {
        int count = 0;
        while (count < 3) {
            try {
                GeocodingResult[] result = GeocodingApi.geocode(context, address).await();
                return new GeoLocation(result[0].geometry.location.lat, result[0].geometry.location.lng);
            } catch (Exception ex) {
                count++;
                logger.error("Exception occurred.", ex);
                if(count < 3) {
                    logger.error("Re-trying after 3secs...");
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        logger.error("InterruptedException occurred while wiating for geoCodingAPI.", e);
                    }
                }
            }
        }
        return null;
    }

    public Pair<Long, Long> getDistanceAndDuration(GeoLocation origin, GeoLocation destination) throws GeoException {
            return getDistanceAndDuration(origin, destination, TravelMode.DRIVING);
    }

    public Pair<Long, Long> getDistanceAndDuration(GeoLocation origin, GeoLocation destination, TravelMode travelMode) throws GeoException {
        // TODO - cache the distance/duration info for each origin-destination.
        //      This helps in case vehicle doesn't move between multiple requests and improves performance and reduces Google-Api-Cost

        try {
            DistanceMatrixApiRequest req = DistanceMatrixApi.newRequest(context);
            DistanceMatrix distanceMatrix = req.origins(origin.getLatitude() + "," + origin.getLongitude())
                    .destinations(destination.getLatitude() + "," + destination.getLongitude())
                    .mode(travelMode)
                    .await();

            long duration = distanceMatrix.rows[0].elements[0].duration.inSeconds;
            long distance = distanceMatrix.rows[0].elements[0].distance.inMeters;
            return new Pair<>(distance, duration);
        } catch(InterruptedException | ApiException | IOException e) {
            throw new GeoException("Exception occurred while getting Distance and Duration between points", e);
        }
    }

    @Override
    public DistanceDurationDto getDistanceAndDuration(BusStop origin, BusStop destination) throws GeoException {
        List<BusStop> origins = Collections.singletonList(origin);
        List<BusStop> destinations = Collections.singletonList(destination);
        List<DistanceDurationDto> out = getDistanceMatrix(origins, destinations);
        return out == null || out.isEmpty() ? null : out.get(0);
    }

    @Override
    public List<DistanceDurationDto> getDistanceMatrix(BusStop originLocation, List<BusStop> destLocations) throws GeoException  {
        List<BusStop> origLocations = Collections.singletonList(originLocation);
        return getDistanceMatrix(origLocations, destLocations);
    }

    @Override
    public List<DistanceDurationDto> getDistanceMatrix(List<BusStop> origLocations, List<BusStop> destLocations)
            throws GeoException {
        List<DistanceDurationDto> distanceDurationDtos = new ArrayList<>();
        try {
            LatLng[] origins = new LatLng[origLocations.size()];
            int i=0;
            for(BusStop busStop : origLocations) {
                origins[i++] = new LatLng(busStop.getLocation().getLatitude(), busStop.getLocation().getLongitude());
            }

            LatLng[] destinations = new LatLng[destLocations.size()];
            i=0;
            for(BusStop busStop : destLocations) {
                destinations[i++] = new LatLng(busStop.getLocation().getLatitude(), busStop.getLocation().getLongitude());
            }

            DistanceMatrix distanceMatrix = DistanceMatrixApi.newRequest(context)
                    .origins(origins)
                    .destinations(destinations)
                    .mode(TravelMode.DRIVING)
                    .units(Unit.METRIC)
                    .await();

            i=0;
            for(DistanceMatrixRow row: distanceMatrix.rows) {
                BusStop fromLocation = origLocations.get(i);
                LatLng fromPoint = origins[i++];
                int j=0;
                for(DistanceMatrixElement element: row.elements) {
                    BusStop toLocation = destLocations.get(j);
                    LatLng toPoint = destinations[j++];

                    if(!fromLocation.equals(toLocation)) {
                        DistanceDurationDto distanceDurationDto = new DistanceDurationDto(fromLocation, toLocation,
                                element.distance.inMeters, element.duration.inSeconds);
                        distanceDurationDtos.add(distanceDurationDto);
                    }
                }
            }
        } catch(InterruptedException | ApiException | IOException e) {
            throw new GeoException("Exception occurred while getting DistanceMatrix between points", e);
        }
        return distanceDurationDtos;
    }
}
