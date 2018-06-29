package jstech.edu.transportmodel;

import jstech.edu.transportmodel.controller.TEMPController;
import jstech.edu.transportmodel.service.route.RouteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Created by jitendra1 on 03-Dec-17.
 */
@SpringBootApplication
public class AppMain implements CommandLineRunner {
    private static final Logger logger = LoggerFactory.getLogger(AppMain.class);

    public static final String REST_BASE_PATH = "/app";

    @Autowired
    private AppDataInitializer appDataInitializer;

    /*@Autowired
    private RouteService routeService;*/

    /*@Autowired
    private TEMPController tempController;*/

    public static void main(String[] args) {
        SpringApplication.run(AppMain.class, args);
    }

    @Override
    public void run(String[] args) {
        try {
            appDataInitializer.loadData();
        } catch (BusBeeException e) {
            logger.error("Exception occurred.", e);
        }

        // schedule = 7am every day except saturday and sunday
        /*int tripId = routeService.generateOptimalRoute(1, "DAILY", "0 0 7 ? * MON,TUE,WED,THU,FRI", "0 0 4 ? * MON,TUE,WED,THU,FRI");
        //int tripId = 2;
        routeService.approveTrip(tripId);
        logger.debug("initialized...");*/

        //tempController.loadTestData();
    }
}
