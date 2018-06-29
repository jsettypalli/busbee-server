package jstech.edu.transportmodel.service;

import javafx.util.Pair;
import jstech.edu.transportmodel.GeoException;
import jstech.edu.transportmodel.common.GeoLocation;
import org.junit.Test;

import static junit.framework.TestCase.assertTrue;

public class MapMyIndiaGeoServiceTest {

    //@Test
    public void testRouteApi() {
        GeoLocation startLocation = new GeoLocation(17.468709,78.3700740);
        GeoLocation destinationLocation = new GeoLocation(17.4535970,78.3667420);

        long distance = 0;
        long duration = 0;
        MapMyIndiaGeoService service = new MapMyIndiaGeoService();
        try {
            Pair<Long, Long> distDuration = service.getDistanceAndDuration(startLocation, destinationLocation);
            distance = distDuration.getKey();
            duration = distDuration.getValue();
        } catch (GeoException e) {
            e.printStackTrace();
        }

        assertTrue(distance > 0);
        assertTrue(duration > 0);
    }
}
