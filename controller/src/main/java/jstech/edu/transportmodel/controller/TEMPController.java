package jstech.edu.transportmodel.controller;

import jstech.edu.transportmodel.common.*;
import jstech.edu.transportmodel.service.GeographicalService;
import jstech.edu.transportmodel.service.SchoolBusService;
import jstech.edu.transportmodel.service.SchoolService;
import jstech.edu.transportmodel.service.route.RouteService;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.time.LocalTime;
import java.util.*;

// TODO - THIS CLASS SHOULD BE REMOVED. THIS IS PROVIDED FOR INTEGRATION TESTING TEMPORARILY...
@RestController
public class TEMPController {
    private static final Logger logger = LoggerFactory.getLogger(TEMPController.class);

    @Autowired
    private SchoolService schoolService;
    @Autowired
    private SchoolBusService schoolBusService;

    @Autowired
    private RouteService routeService;

    @Autowired
    private GeographicalService geoService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE, value="/temp_upload/vehicles")
    public @ResponseBody
    List<TEMPController.VehicleUpdValueObj> uploadVehicleData(@RequestParam(value="file_name", required = false) String fileName) throws Exception {
        logger.debug("Into TempUploadController.uploadVehicleData");
        List<TEMPController.VehicleUpdValueObj> updValueObjs = new ArrayList<>();
        File file = new File(StringUtils.hasText(fileName) ? fileName : "/home/ubuntu/busbee/school_data.xlsx");
        InputStream inputStream = new FileInputStream(file);
        Workbook workbook = new XSSFWorkbook(inputStream);
        School school = uploadSchoolData(workbook);

        Sheet vehicleSheet = workbook.getSheet("Vehicles");
        Iterator<Row> iterator = vehicleSheet.iterator();

        int rowCount = 0;
        BusStop schoolStartBusStop = null;
        List<SchoolBus> vehicles = new ArrayList<>();

        logger.debug("Iterating through the rows...");
        while (iterator.hasNext()) {
            Row nextRow = iterator.next();
            // skip first row which contains headers
            if(rowCount == 0) {
                rowCount++;
                continue;
            }

            int colCount = 0;
            double startLatitude = 0.0;
            double startLongitude = 0.0;
            SchoolBus.Builder schoolbusBuilder = new SchoolBus.Builder();
            schoolbusBuilder.setSchool(school);

            String registrationNumber = null;
            Iterator<Cell> cellIterator = nextRow.cellIterator();
            while (cellIterator.hasNext()) {
                Cell cell = cellIterator.next();
                logger.debug("rowcount: {}, cellcount: {}", rowCount, colCount);
                switch (colCount) {
                    case 0:
                        schoolbusBuilder.setBusNumber(cell.getStringCellValue());
                        break;
                    case 1:
                        registrationNumber = cell.getStringCellValue();
                        schoolbusBuilder.setRegistrationNumber(registrationNumber);
                        break;
                    case 2:
                        schoolbusBuilder.setCapacity(new Double(cell.getNumericCellValue()).intValue());
                        break;
                    case 3:
                        schoolbusBuilder.setMake(cell.getStringCellValue());
                        break;
                    case 4:
                        schoolbusBuilder.setModel(cell.getStringCellValue());
                        break;
                    case 5:
                        startLatitude = cell.getNumericCellValue();
                        break;
                    case 6:
                        startLongitude = cell.getNumericCellValue();
                        break;
                }
                colCount++;
            }

            boolean startingAtSchool = false;
            if(startLatitude <= 0.0 || startLongitude <= 0.0) {
                startLatitude = school.getLocation().getLatitude();
                startLongitude = school.getLocation().getLongitude();
                startingAtSchool = true;
            }

            BusStop busStop = null;
            if(startingAtSchool) {
                if(schoolStartBusStop == null) {
                    schoolStartBusStop = new BusStop.Builder()
                            .setLocation(new GeoLocation(school.getLocation().getLatitude(), school.getLocation().getLongitude()))
                            .setPickupPoint(true)
                            .setSchool(school)
                            .setName("School")
                            .build();
                }
                busStop = schoolStartBusStop;
            } else {
                busStop = new BusStop.Builder()
                        .setLocation(new GeoLocation(startLatitude, startLongitude))
                        .setPickupPoint(true)
                        .setSchool(school)
                        .setName(registrationNumber + "_StartingPoint")
                        .build();
            }
            logger.debug("rowcount:{}, bus-stop:{}", rowCount, busStop);
            busStop = schoolBusService.addBusPoint(busStop);
            schoolbusBuilder.setStartBusStop(busStop);

            SchoolBus vehicle = schoolbusBuilder.build();

            // TODO  - Change this to batch update
            try {
                SchoolBus schoolBus = schoolBusService.addSchoolBus(vehicle);
                logger.debug("rowcount:{}, schoolBus:{}", rowCount, schoolBus);
                updValueObjs.add(new TEMPController.VehicleUpdValueObj(vehicle, ""));
            } catch(Exception e) {
                logger.error("{} occurred while adding School Bus {}",e.getClass().getName(), vehicle, e);
                updValueObjs.add(new TEMPController.VehicleUpdValueObj(vehicle, "Error in uploading. Please contact Technical Support."));
            }
        }

        workbook.close();
        inputStream.close();
        logger.debug("getting out of UploadController.uploadVehicleData");
        return updValueObjs;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE, value="/temp_upload/busstops")
    public @ResponseBody
    List<TEMPController.BusPointValueObj> uploadPickupPointsData(@RequestParam(value="file_name", required = false) String fileName)  throws Exception {
        List<TEMPController.BusPointValueObj> updValueObjs = new ArrayList<>();
        File file = new File(StringUtils.hasText(fileName) ? fileName : "/home/ubuntu/busbee/school_data.xlsx");
        InputStream inputStream = new FileInputStream(file);

        Workbook workbook = new XSSFWorkbook(inputStream);
        School school = uploadSchoolData(workbook);

        List<BusStop> busStops = new ArrayList<>();

        Sheet busPointSheet = workbook.getSheet("Bus-Points");
        Iterator<Row> iterator = busPointSheet.iterator();
        int rowCount = 0;
        while (iterator.hasNext()) {
            Row nextRow = iterator.next();
            // first row contains headers - skip
            if(rowCount == 0) {
                rowCount++;
                continue;
            }

            int colCount = 0;
            double pickupLatitude = 0;
            double pickupLongitude = 0;
            double dropoffLatitude = 0;
            double dropoffLongitude = 0;

            BusStop.Builder builder = new BusStop.Builder();
            builder.setSchool(school);

            Iterator<Cell> cellIterator = nextRow.cellIterator();
            while (cellIterator.hasNext()) {
                Cell cell = cellIterator.next();
                switch (colCount) {
                    case 0:
                        builder.setName(cell.getStringCellValue());
                        break;
                    case 1:
                        builder.setAddress(cell.getStringCellValue());
                        break;
                    case 2:
                        builder.setNumStudents(new Double(cell.getNumericCellValue()).intValue());
                        break;
                    case 3:
                        pickupLatitude = cell.getNumericCellValue();
                        break;
                    case 4:
                        pickupLongitude = cell.getNumericCellValue();
                        break;
                    case 5:
                        dropoffLatitude = cell.getNumericCellValue();
                        break;
                    case 6:
                        dropoffLongitude = cell.getNumericCellValue();
                        break;
                    case 7:
                        builder.setWaitTimeMins(new Double(cell.getNumericCellValue()).intValue());
                        break;
                }
                colCount++;
            }

            // add pickup bus point
            if(pickupLatitude > 0 && pickupLongitude > 0) {
                builder.setLocation(pickupLatitude, pickupLongitude);
                builder.setPickupPoint(true);
                BusStop busStop = builder.build();

                // TODO  - Change this to batch update
                try {
                    BusStop busStop1 = schoolBusService.addBusPoint(busStop);
                    updValueObjs.add(new TEMPController.BusPointValueObj(busStop, ""));
                } catch(Exception e) {
                    updValueObjs.add(new TEMPController.BusPointValueObj(busStop, "Error in uploading. Please contact Technical Support."));
                }
            } else {
                logger.warn("pickup location is mandatory. Can't add buspoint with no pickup location for row number: {}", rowCount);
            }

            // now add drop off bus point
            if(dropoffLatitude > 0 && dropoffLongitude > 0) {
                builder.setLocation(dropoffLatitude, dropoffLongitude);
            } else {
                builder.setLocation(pickupLatitude, pickupLongitude);
            }
            builder.setPickupPoint(false);
            BusStop busStop = builder.build();

            // TODO  - Change this to batch update
            try {
                BusStop busStop1 = schoolBusService.addBusPoint(busStop);
                updValueObjs.add(new TEMPController.BusPointValueObj(busStop, ""));
            } catch(Exception e) {
                updValueObjs.add(new TEMPController.BusPointValueObj(busStop, "Error in uploading. Please contact Technical Support."));
            }
            rowCount++;
        }

        workbook.close();
       inputStream.close();
        return updValueObjs;
    }

    private School uploadSchoolData(Workbook workbook) {
        School school = null;
        Sheet worksheet = workbook.getSheet("School");
        Iterator<Row> iterator = worksheet.iterator();
        int rowCount = 0;
        while(iterator.hasNext()) {
            Row row = iterator.next();

            // skip first row which contains headers
            if(rowCount == 0) {
                rowCount++;
                continue;
            }

            int colCount = 0;
            String name = null;
            double latitude = 0.0;
            double longitude = 0.0;
            Iterator<Cell> cellIterator = row.cellIterator();
            while (cellIterator.hasNext()) {
                Cell cell = cellIterator.next();
                switch (colCount) {
                    case 0:
                        name = cell.getStringCellValue();
                        break;
                    case 1:
                        latitude = cell.getNumericCellValue();
                        break;
                    case 2:
                        longitude = cell.getNumericCellValue();
                        break;
                }
                colCount++;
            }

            school = schoolService.addSchool(name, latitude, longitude);
            break;
        }
        return school;
    }

    private static class VehicleUpdValueObj {
        private SchoolBus vehicle;
        private String message;

        VehicleUpdValueObj(SchoolBus vehicle, String message) {
            this.vehicle = vehicle;
            this.message = message;
        }

        public SchoolBus getVehicle() {
            return vehicle;
        }

        public void setVehicle(SchoolBus vehicle) {
            this.vehicle = vehicle;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    private static class BusPointValueObj {
        private BusStop busStop;
        private String message;

        BusPointValueObj(BusStop busStop, String message) {
            this.busStop = busStop;
            this.message = message;
        }

        public BusStop getBusStop() {
            return busStop;
        }

        public void setBusStop(BusStop busStop) {
            this.busStop = busStop;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE, value="/temp/gen_route")
    public @ResponseBody String generateRoute(@RequestParam("school_id") int schoolId) {
        if(schoolId <= 0) {
            schoolId = 1;
        }
        int tripId = routeService.generateOptimalRoute(schoolId, "DAILY", " 0 7 * * 1-5", " 0 3 * * 1-5");
        routeService.approveTrip(tripId);
        routeService.createRoutePlan();
        return "success";
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE, value="/temp/load_running_buses_data")
    public @ResponseBody String loadRunningBusesData() {
        int out = jdbcTemplate.update("delete from route_plan");
        routeService.createRoutePlan();
        return "success";
    }

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE, value="/temp/load_test_data")
    public @ResponseBody String loadTestData() {
        BusTrip busTrip = new BusTrip();
        busTrip.setName("Meridian-Kukatpally-Daily-Pickup");
        busTrip.setApproved(true);
        busTrip.setPickup(true);
        busTrip.setSchedule("0 0 0 1/1 * ?");

        School school = schoolService.addSchool("Meridian-Kukatpally", 17.487104, 78.387233);
        busTrip.setSchool(school);

        BusStop meridianKukatpallyStartingPoint = new BusStop.Builder().setName("Meridian-Kukatpally")
                .setAddress("Near Forum mall, Kukatpally").setSchool(school).setPickupPoint(true)
                .setLocation(17.487104, 78.387233).setNumStudents(0).build();
        meridianKukatpallyStartingPoint = schoolBusService.addBusPoint(meridianKukatpallyStartingPoint);

        SchoolBus k1Bus = new SchoolBus.Builder().setBusNumber("K1").setRegistrationNumber("AP 09 TA 9173")
                .setStartBusStop(meridianKukatpallyStartingPoint).setCapacity(31).setMake("TATA").setModel("JCBL").setSchool(school).build();
        k1Bus = schoolBusService.addSchoolBus(k1Bus);
        LocalTime k1StartTime = LocalTime.of(6, 50);
        int timeFromLastBusStopToDestinationSecs = 10 * 60;
        BusStop meridianKukatpallyDestinationK1 = new BusStop.Builder().build(meridianKukatpallyStartingPoint);
        SchoolBusRoute k1Route = createK1Route(school, meridianKukatpallyStartingPoint, meridianKukatpallyDestinationK1, k1StartTime, timeFromLastBusStopToDestinationSecs);
        busTrip.addBusRoute(k1Bus, k1Route);

        SchoolBus k6Bus = new SchoolBus.Builder().setBusNumber("K6").setRegistrationNumber("AP 09 TA 9434")
                .setStartBusStop(meridianKukatpallyStartingPoint).setCapacity(31).setMake("TATA").setModel("JCBL").setSchool(school).build();
        k6Bus = schoolBusService.addSchoolBus(k6Bus);
        LocalTime k6StartTime = LocalTime.of(7, 5);
        timeFromLastBusStopToDestinationSecs = 10 * 60;
        BusStop meridianKukatpallyDestinationK6 = new BusStop.Builder().build(meridianKukatpallyStartingPoint);
        SchoolBusRoute k6Route = createK6Route(school, meridianKukatpallyStartingPoint, meridianKukatpallyDestinationK6, k6StartTime, timeFromLastBusStopToDestinationSecs);
        busTrip.addBusRoute(k6Bus, k6Route);

        SchoolBus k7Bus = new SchoolBus.Builder().setBusNumber("K7").setRegistrationNumber("TS 07 UA 0052")
                .setStartBusStop(meridianKukatpallyStartingPoint).setCapacity(37).setMake("TATA").setModel("Marco Polo").setSchool(school).build();
        k7Bus = schoolBusService.addSchoolBus(k7Bus);
        LocalTime k7StartTime = LocalTime.of(6, 30);
        timeFromLastBusStopToDestinationSecs = 10 * 60;
        BusStop meridianKukatpallyDestinationK7 = new BusStop.Builder().build(meridianKukatpallyStartingPoint);
        SchoolBusRoute k7Route = createK7Route(school, meridianKukatpallyStartingPoint, meridianKukatpallyDestinationK7, k7StartTime, timeFromLastBusStopToDestinationSecs);
        busTrip.addBusRoute(k7Bus, k7Route);

        routeService.saveTrip(busTrip);

        return "{ \"status\" : \"success\" }";
    }

    private SchoolBusRoute createK6Route(School school, BusStop startingPoint, BusStop destination, LocalTime startTime, int timeFromLastBusStopToDestinationSecs) {
        return createRoute("K6", school, startingPoint, destination, startTime, timeFromLastBusStopToDestinationSecs,
                new BusStopInput[] {
                        new BusStopInput("K6 Stop 1", "K6 Stop 1", true, school,
                                17.522819, 78.394237,1, 5*60),
                        new BusStopInput("K6 Stop 2", "K6 Stop 2", true, school,
                                17.525336, 78.396618,1, 5*60),
                        new BusStopInput("K6 Stop 3", "K6 Stop 3", true, school,
                                17.521926, 78.397712,1, 5*60),
                        new BusStopInput("K6 Stop 4", "K6 Stop 4", true, school,
                                17.522006, 78.398153,1, 5*60),
                        new BusStopInput("K6 Stop 5", "K6 Stop 5", true, school,
                                17.517990, 78.400765,2, 5*60),
                        new BusStopInput("K6 Stop 6", "K6 Stop 6", true, school,
                                17.516457, 78.397870,1, 5*60),
                        new BusStopInput("K6 Stop 7", "K6 Stop 7", true, school,
                                17.516479, 78.396727,1, 5*60),
                        new BusStopInput("K6 Stop 8", "K6 Stop 8", true, school,
                                17.510833, 78.389773,9, 5*60),
                        new BusStopInput("K6 Stop 9", "K6 Stop 9", true, school,
                                17.506720, 78.387117,1, 5*60),
                        new BusStopInput("K6 Stop 10", "K6 Stop 10", true, school,
                                17.505127, 78.386479,5, 5*60),
                        new BusStopInput("K6 Stop 11", "K6 Stop 11", true, school,
                                17.510188, 78.384839,2, 5*60),
                        new BusStopInput("K6 Stop 12", "K6 Stop 12", true, school,
                                17.505316, 78.386986,3, 5*60)
                });
    }

    private SchoolBusRoute createK7Route(School school, BusStop startingPoint, BusStop destination, LocalTime startTime, int timeFromLastBusStopToDestinationSecs) {
        return createRoute("K7", school, startingPoint, destination, startTime, timeFromLastBusStopToDestinationSecs,
                new BusStopInput[] {
                        new BusStopInput("SP Tower 1, G.Ramaram", "SP Tower 1, G.Ramaram", true, school,
                                17.529329, 78.428724,2, 35*60),
                        new BusStopInput("SP Tower 2, G.Ramaram", "SP Tower 2, G.Ramaram", true, school,
                                17.523994, 78.427812,3, 5*60),
                        new BusStopInput("Tulasivanam", "Tulasivanam", true, school,
                                17.501408, 78.408749,9, 10*60),
                        new BusStopInput("Vivekananda Nagar 1", "Vivekananda Nagar 1", true, school,
                                17.497366, 78.414163,2, 10*60),
                        new BusStopInput("Vivekananda Nagar 2", "Vivekananda Nagar 2", true, school,
                                17.496999, 78.416281,1, 5*60),
                        new BusStopInput("VV Nagar", "VV Nagar", true, school,
                                17.496807, 78.419594,4, 5*60),
                        new BusStopInput("Bhagya Nagar 1", "Bhagya Nagar 1", true, school,
                                17.496637, 78.412961,1, 5*60),
                        new BusStopInput("Vivekananda Nagar 3", "Vivekananda Nagar 3", true, school,
                                17.492319, 78.409538,1, 0),
                        new BusStopInput("Vivekananda Nagar 4", "Vivekananda Nagar 4", true, school,
                                17.491073, 78.408519,2, 0),
                        new BusStopInput("Vivekananda Nagar 5", "Vivekananda Nagar 5", true, school,
                                17.496022, 78.408379,1, 5*60),
                        new BusStopInput("Bhagya Nagar 2", "Bhagya Nagar 2", true, school,
                                17.494895, 78.403979,3, 5*60),
                        new BusStopInput("Bhagya Nagar 3", "Bhagya Nagar 3", true, school,
                                17.494253, 78.403924,1, 0),
                        new BusStopInput("AS Raju Nagar", "AS Raju Nagar", true, school,
                                17.493661, 78.405080,1, 0)
                });
    }

    private SchoolBusRoute createRoute(String routeName, School school, BusStop startingPoint, BusStop destination, LocalTime startTime,
                                       int timeFromLastBusStopToDestinationSecs, BusStopInput[] busStopInputs) {
        SchoolBusRoute busRoute = new SchoolBusRoute();
        busRoute.setName(routeName);
        busRoute.setStartTime(startTime);
        busRoute.setStartingPoint(startingPoint);
        busRoute.setRouteStatus(RouteStatus.YET_TO_START);

        int relativeSecs = 0;
        List<BusStop> busStops = new ArrayList<>();
        for(BusStopInput input: busStopInputs) {
            relativeSecs += input.relativeSeconds;
            BusStop busStop = makeBusStop(input.name, input.address, input.pickupPoint, input.school, input.latitude, input.longitude,
                    input.numStudents, relativeSecs);
            busStops.add(busStop);
        }
        busRoute.setBusStops(busStops);

        destination.setRelativeArrivalTimeSecs(relativeSecs + timeFromLastBusStopToDestinationSecs);
        destination.setRelativeDepartureTimeSecs(0);
        busRoute.setDestination(destination);
        return busRoute;
    }

    private static class BusStopInput {
        String name;
        String address;
        boolean pickupPoint;
        School school;
        double latitude;
        double longitude;
        int numStudents;
        int relativeSeconds;

        public BusStopInput(String name, String address, boolean pickupPoint, School school, double latitude, double longitude,
                            int numStudents, int relativeSeconds) {
            this.name = name;
            this.address = address;
            this.pickupPoint = pickupPoint;
            this.school = school;
            this.latitude = latitude;
            this.longitude = longitude;
            this.numStudents = numStudents;
            this.relativeSeconds = relativeSeconds;
        }
    }

    private SchoolBusRoute createK1Route(School school, BusStop startingPoint, BusStop destination, LocalTime startTime, int timeFromLastBusStopToDestinationSecs) {
        SchoolBusRoute busRoute = new SchoolBusRoute();
        busRoute.setName("K1");
        busRoute.setStartTime(startTime);
        busRoute.setStartingPoint(startingPoint);
        busRoute.setRouteStatus(RouteStatus.YET_TO_START);

        List<BusStop> busStops = new ArrayList<>();
        int relativeSecs = 10*60;
        BusStop sriramTowers = makeBusStop("Sriram Towers Allwyn", "Sriram Towers Allwyn", true, school,
                17.486992, 78.352671, 2, relativeSecs);
        busStops.add(sriramTowers);

        relativeSecs += 20*60;
        BusStop smrVinayBackside = makeBusStop("SMR Vinay Backside", "SMR Vinay Backside", true, school,
                17.489876, 78.371238, 1, relativeSecs);
        busStops.add(smrVinayBackside);

        relativeSecs += 5*60;
        BusStop smrVinay = makeBusStop("SMR Vinay", "SMR Vinay", true, school,
                17.489669, 78.370632, 8, relativeSecs);
        busStops.add(smrVinay);

        relativeSecs += 5*60;
        BusStop srilaPark4 = makeBusStop("Srila Park 4", "Srila Park 4", true, school,
                17.488959, 78.372300, 2, relativeSecs);
        busStops.add(srilaPark4);

        relativeSecs += 5*60;
        BusStop srilaPark3 = makeBusStop("Srila Park 3", "Srila Park 3", true, school,
                17.488460, 78.372329, 2, relativeSecs);
        busStops.add(srilaPark3);

        relativeSecs += 30;
        BusStop srilaPark2 = makeBusStop("Srila Park 2", "Srila Park 2", true, school,
                17.489364, 78.372324, 1, relativeSecs);
        busStops.add(srilaPark2);

        relativeSecs += 5*60;
        BusStop srilaPark1 = makeBusStop("Srila Park 1", "Srila Park 1", true, school,
                17.488809, 78.372104, 1, relativeSecs);
        busStops.add(srilaPark1);

        relativeSecs += 10*60;
        BusStop mahindraAshvita1 = makeBusStop("Mahindra Ashvita 1", "Mahindra Ashvita 1", true, school,
                17.477529, 78.378635, 1, relativeSecs);
        busStops.add(mahindraAshvita1);

        relativeSecs += 5*60;
        BusStop prajayMegapolisMain = makeBusStop("Prajay Megapolis Main Gate", "Prajay Megapolis Main Gate", true, school,
                17.481141, 78.380017, 1, relativeSecs);
        busStops.add(prajayMegapolisMain);

        relativeSecs += 2*60;
        BusStop prajayMegapolisManjeera = makeBusStop("Prajay Megapolis Manjeera Mart", "Prajay Megapolis Manjeera Mart", true, school,
                17.482586, 78.380153, 3, relativeSecs);
        busStops.add(prajayMegapolisManjeera);

        relativeSecs += 3*60;
        BusStop modelChickenCenter = makeBusStop("Model Chicken Center", "Model Chicken Center", true, school,
                17.484325, 78.380805, 3, relativeSecs);
        busStops.add(modelChickenCenter);

        busRoute.setBusStops(busStops);

        destination.setRelativeArrivalTimeSecs(relativeSecs + timeFromLastBusStopToDestinationSecs);
        destination.setRelativeDepartureTimeSecs(0);
        busRoute.setDestination(destination);

        return busRoute;
    }

    private BusStop makeBusStop(String name, String address, boolean pickUpPoint, School school,
                                double latitude, double longitude, int numStudents, int relativeSecs) {
        BusStop busStop = new BusStop.Builder().setName(name).setAddress(address)
                .setPickupPoint(pickUpPoint).setSchool(school).setLocation(latitude, longitude)
                .setNumStudents(numStudents).build();
        busStop.setRelativeArrivalTimeSecs(relativeSecs);
        busStop.setRelativeDepartureTimeSecs(relativeSecs);
        busStop = schoolBusService.addBusPoint(busStop);
        return busStop;
    }
}
