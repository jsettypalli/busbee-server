package jstech.edu.transportmodel.controller;

import jstech.edu.transportmodel.AppMain;
import jstech.edu.transportmodel.common.BusStop;
import jstech.edu.transportmodel.common.GeoLocation;
import jstech.edu.transportmodel.common.School;
import jstech.edu.transportmodel.common.SchoolBus;
import jstech.edu.transportmodel.service.GeographicalService;
import jstech.edu.transportmodel.service.SchoolBusService;
import jstech.edu.transportmodel.service.SchoolService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by jitendra1 on 23-Dec-17.
 */
@RestController
@RequestMapping(AppMain.REST_BASE_PATH)
public class UploadDataController {

    private static final Logger logger = LoggerFactory.getLogger(UploadDataController.class);

    @Autowired
    private SchoolService schoolService;
    @Autowired
    private SchoolBusService schoolBusService;

    @Autowired
    private GeographicalService geoService;

    // TODO - add better validation of input data and design proper details to return when error occurs.
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE, value="/upload/vehicle_data")
    public @ResponseBody
    List<VehicleUpdValueObj> uploadVehicleData(@RequestParam("file") MultipartFile file, RedirectAttributes redirectAttributes) throws Exception {
        List<VehicleUpdValueObj> updValueObjs = new ArrayList<>();
        if(file.isEmpty()) {
            logger.warn("Uploaded file {} is empty", file.getOriginalFilename());
            updValueObjs.add(new VehicleUpdValueObj(null, "Uploaded file is empty"));
            return updValueObjs;
        }

        Workbook workbook = new XSSFWorkbook(file.getInputStream());
        School school = uploadSchoolData(workbook);

        Sheet vehicleSheet = workbook.getSheet("Vehicles");
        Iterator<Row> iterator = vehicleSheet.iterator();

        int rowCount = 0;
        BusStop schoolStartBusStop = null;
        List<SchoolBus> vehicles = new ArrayList<>();

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
            busStop = schoolBusService.addBusPoint(busStop);
            schoolbusBuilder.setStartBusStop(busStop);

            SchoolBus vehicle = schoolbusBuilder.build();

            // TODO  - Change this to batch update
            try {
                SchoolBus schoolBus = schoolBusService.addSchoolBus(vehicle);
                updValueObjs.add(new VehicleUpdValueObj(vehicle, ""));
            } catch(Exception e) {
                logger.error("{} occurred while adding School Bus {}",e.getClass().getName(), vehicle, e);
                updValueObjs.add(new VehicleUpdValueObj(vehicle, "Error in uploading. Please contact Technical Support."));
            }
        }

        workbook.close();
        file.getInputStream().close();
        return updValueObjs;
    }

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE, value="/upload/pickup_points_data")
    public @ResponseBody
    List<BusPointValueObj> uploadPickupPointsData(@RequestParam("pickuppoints_file") MultipartFile file,
                                                  RedirectAttributes redirectAttributes)  throws Exception {
        List<BusPointValueObj> updValueObjs = new ArrayList<>();
        if(file.isEmpty()) {
            logger.warn("Uploaded file {} is empty", file.getOriginalFilename());
            updValueObjs.add(new BusPointValueObj(null, "Uploaded file is empty"));
            return updValueObjs;
        }

        Workbook workbook = new XSSFWorkbook(file.getInputStream());
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
                    updValueObjs.add(new BusPointValueObj(busStop, ""));
                } catch(Exception e) {
                    updValueObjs.add(new BusPointValueObj(busStop, "Error in uploading. Please contact Technical Support."));
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
                updValueObjs.add(new BusPointValueObj(busStop, ""));
            } catch(Exception e) {
                updValueObjs.add(new BusPointValueObj(busStop, "Error in uploading. Please contact Technical Support."));
            }
            rowCount++;
        }

        workbook.close();
        file.getInputStream().close();
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
}
