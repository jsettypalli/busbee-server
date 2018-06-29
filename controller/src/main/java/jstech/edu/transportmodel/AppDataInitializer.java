package jstech.edu.transportmodel;

import jstech.edu.transportmodel.auth.AuthKeyLoader;
import jstech.edu.transportmodel.service.route.RouteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AppDataInitializer {

    private static final Logger logger = LoggerFactory.getLogger(AppDataInitializer.class);

    @Autowired
    private RouteService routeService;

    @Autowired
    private AuthKeyLoader authKeyLoader;

    /**
     * Loads data into memory from database. If the data is already loaded into memory, it will be refreshed.
     */
    public void loadData() throws BusBeeException {
        routeService.createRoutePlan();
        authKeyLoader.loadKeys();
    }
}
