package jstech.edu.transportmodel.service;

import com.google.api.client.http.*;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.GenericJson;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.ArrayMap;

import javafx.util.Pair;
import jstech.edu.transportmodel.GeoException;
import jstech.edu.transportmodel.common.BusStop;
import jstech.edu.transportmodel.common.GeoLocation;
import jstech.edu.transportmodel.dto.DistanceDurationDto;
import jstech.edu.transportmodel.service.route.RouteService;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class MapMyIndiaGeoService implements GeographicalService {

    private static final Logger logger = LoggerFactory.getLogger(MapMyIndiaGeoService.class);

    private static final int WITH_TRAFFIC = 1;
    private static HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
    private static JsonFactory JSON_FACTORY = new JacksonFactory();
    private static final String MapMyindiaUrlFormat =
            "https://apis.mapmyindia.com/advancedmaps/v1/%s/route?start=%s&destination=%s&rtype=%d&vtype=%d&with_traffic=%d&viapoints=%s";

    @Autowired
    private RouteService routeService;

    @Value("${MapMyIndia.LicenseKey:}")
    private String licenseKey;

    // max via_points supported by MapMyIndia are 16.  Whereas Google only supports maximum of 10 via_points
    private static final int MAX_VIA_POINTS = 16;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public GeoLocation getGeoLocationFromAddress(String address) {
        return null;
    }

    @Override
    public Pair<Long, Long> getDistanceAndDuration(GeoLocation origin, GeoLocation destination) {
        RouteApiResult result = getRouteBetweenTwoLocations(origin, destination, null);
        return (result == null) ? null : new Pair<>(result.distanceInMeters, result.timeInSeconds);
    }

    @Override
    public DistanceDurationDto getDistanceAndDuration(BusStop origin, BusStop destination) {
        List<GeoLocation> viaPointLocations = routeService.getViaPointLocationsBetweenBusStopsForRouteDetermination(
                origin.getBusStopDetailId(), destination.getBusStopDetailId());
        if(viaPointLocations != null && viaPointLocations.size() > MAX_VIA_POINTS) {
            viaPointLocations = viaPointLocations.subList(0, MAX_VIA_POINTS);
        }

        RouteApiResult result = getRouteBetweenTwoLocations(origin.getLocation(), destination.getLocation(), viaPointLocations);
        return (result == null) ? null : new DistanceDurationDto(origin, destination, result.distanceInMeters, result.timeInSeconds, result.viaPoints);
    }

    @Override
    public List<DistanceDurationDto> getDistanceMatrix(BusStop origin, List<BusStop> destinations) throws GeoException {
        List<DistanceDurationDto> distanceDurationDtos = new ArrayList<>();
        for(BusStop destination: destinations) {
            distanceDurationDtos.add(getDistanceAndDuration(origin, destination));
        }
        return distanceDurationDtos;
    }

    @Override
    public List<DistanceDurationDto> getDistanceMatrix(List<BusStop> origins, List<BusStop> destinations) throws GeoException {
        List<DistanceDurationDto> distanceDurationDtos = new ArrayList<>();
        for(BusStop origin: origins) {
            for (BusStop destination : destinations) {
                distanceDurationDtos.add(getDistanceAndDuration(origin, destination));
            }
        }
        return distanceDurationDtos;
    }

    private RouteApiResult getRouteBetweenTwoLocations(GeoLocation start, GeoLocation destination, List<GeoLocation> viaPointLocations) {
        return getRouteBetweenTwoLocations(start, destination, viaPointLocations, RouteType.Quickest, VehicleType.Taxi, WITH_TRAFFIC);
    }

    private RouteApiResult getRouteBetweenTwoLocations(GeoLocation start, GeoLocation destination, List<GeoLocation> viaPointLocations,
                                                       RouteType routeType, VehicleType vehicleType, int withTraffic) {

        String urlStr = makeRouteApiUrl(start, destination, viaPointLocations, routeType, vehicleType, withTraffic);

        int maxCount = 3;
        int count = 0;
        RouteApiResult routeApiResult = null;
        while(count < maxCount) {
            try {
                HttpRequestFactory requestFactory
                        = HTTP_TRANSPORT.createRequestFactory(
                        (HttpRequest request) -> {
                            request.setParser(new JsonObjectParser(JSON_FACTORY));
                        });
                GenericUrl url = new GenericUrl(urlStr);
                HttpRequest request = requestFactory.buildGetRequest(url).setParser(new JsonObjectParser(JSON_FACTORY));

                HttpResponse response = request.execute();
                if(response.isSuccessStatusCode()) {
                    GenericJson routeApiTripResponse = response.parseAs(GenericJson.class);
                    ArrayMap<String, Object> resultTrips = (ArrayMap<String, Object>) routeApiTripResponse.get("results");
                    ArrayList<Object> resultTripsValue = (ArrayList<Object>) resultTrips.get("trips");

                    long duration = 0;
                    long distance = 0;
                    for(Object tripValue: resultTripsValue) {
                        ArrayMap<String, Object> dataMap = (ArrayMap<String, Object>) tripValue;

                        duration += ((BigDecimal) dataMap.get("duration")).longValue();
                        distance += ((BigDecimal) dataMap.get("length")).longValue();
                    }

                    String viaPoints = objectMapper.writeValueAsString(resultTripsValue);

                    routeApiResult = new RouteApiResult();
                    routeApiResult.start = start;
                    routeApiResult.destination = destination;
                    routeApiResult.distanceInMeters = distance;
                    routeApiResult.timeInSeconds = duration;
                    routeApiResult.viaPoints = viaPoints;
                    routeApiResult.routeType = routeType;
                    routeApiResult.vehicleType = vehicleType;
                    routeApiResult.withTraffic = withTraffic;

                    return routeApiResult;
                }
            } catch (IOException e) {
                count++;
                if(count == 2) {
                    logger.error("IOException occurred when MapMyIndia RouteAPI is submitted for route " +
                            "between start: {} and destination: {}. Gave up after 3 trials.", start, destination);
                    return null;
                } else {
                    logger.warn("IOException occurred when MapMyIndia RouteAPI is submitted for route " +
                            "between start: {} and destination: {}. Trying once more...", start, destination);
                }
            }
        }

        return null;
    }

    private String makeRouteApiUrl(GeoLocation start, GeoLocation destination, List<GeoLocation> viaPointLocations,
                                   RouteType routeType, VehicleType vehicleType, int withTraffic) {
        String startStr = String.format("%10.8f,%10.8f", start.getLatitude(), start.getLongitude());
        String destinationStr = String.format("%10.8f,%10.8f", destination.getLatitude(), destination.getLongitude());
        String viaPointsStr = "";
        if(viaPointLocations != null && !viaPointLocations.isEmpty()) {
            boolean firstViaPoint = true;
            StringBuilder builder = new StringBuilder();
            for(GeoLocation location: viaPointLocations) {
                if(! firstViaPoint){
                    builder.append("|");
                }
                builder.append(location.getLatitude()).append(",").append(location.getLongitude());
                firstViaPoint = false;
            }
            viaPointsStr = builder.toString();
        }
        String urlStr = String.format(MapMyindiaUrlFormat, licenseKey, startStr, destinationStr,
                routeType.getValue(), vehicleType.getValue(), withTraffic, viaPointsStr);

        return urlStr;
    }

    private enum RouteType {
        Quickest(0), Shortest(1);

        private final int value;

        RouteType(final int value) {
            this.value = value;
        }

        public int getValue() { return value; }
    }

    private enum VehicleType {
        Passenger(0), Taxi(1);

        private final int value;

        VehicleType(final int value) {
            this.value = value;
        }

        public int getValue() { return value; }
    }

    private static class RouteApiResult {
        private GeoLocation start;
        private GeoLocation destination;
        private RouteType routeType;
        private VehicleType vehicleType;
        private int withTraffic;
        private Long distanceInMeters;
        private Long timeInSeconds;
        private String viaPoints;
    }
}
