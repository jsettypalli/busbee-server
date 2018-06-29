package jstech.edu.transportmodel.common;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

/**
 * Created by jitendra1 on 03-Dec-17.
 */
public class SchoolBus {
    private static final Logger logger = LoggerFactory.getLogger(BusTrip.class);

    private int vehicleId;
    private String busNumber;
    private String registrationNumber;
    private int capacity;
    private String make;
    private String model;
    private School school;
    private BusStop startBusStop;

    private SchoolBus(int vehicleId, String busNumber, String registrationNumber, int capacity, School school, BusStop startBusStop,
                      String make, String model) {
        this.vehicleId = vehicleId;
        this.busNumber = busNumber;
        this.registrationNumber = registrationNumber;
        this.capacity = capacity;
        this.school = school;
        this.startBusStop = startBusStop;
        this.make = make;
        this.model = model;
    }

    public int getVehicleId() {
        return vehicleId;
    }

    public String getBusNumber() {
        return busNumber;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public int getCapacity() {
        return capacity;
    }

    public String getMake() {
        return make;
    }

    public String getModel() {
        return model;
    }

    public String getName() {
        return registrationNumber+"-"+make+"-"+model;
    }

    public School getSchool() {
        return school;
    }

    public BusStop getStartBusStop() {
        return startBusStop;
    }

    @Override
    public boolean equals(Object o) {
        logger.debug("checking if objects are equal. current school bus:{}, object-school bus:{}",this, o);
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SchoolBus schoolBus = (SchoolBus) o;
        /*if(this.vehicleId == schoolBus.vehicleId) {
            return true;
        }
        return Objects.equals(registrationNumber, schoolBus.registrationNumber);*/
        return this.vehicleId == schoolBus.vehicleId || Objects.equals(registrationNumber, schoolBus.registrationNumber);
    }

    @Override
    public int hashCode() {

        return Objects.hash(registrationNumber);
    }

    @Override
    public String toString() {
        return "SchoolBus{" +
                "vehicleId=" + vehicleId +
                ", busNumber=" + busNumber +
                ", registrationNumber='" + registrationNumber + '\'' +
                ", capacity=" + capacity +
                ", make='" + make + '\'' +
                ", model='" + model + '\'' +
                ", school=" + school +
                ", startBusStop=" + startBusStop +
                '}';
    }

    public static class Builder {
        private int vehicleId;
        private String busNumber;
        private String registrationNumber;
        private int capacity;
        private String make;
        private String model;
        private School school;
        private BusStop startBusStop;

        public Builder setVehicleId(int vehicleId) {
            this.vehicleId = vehicleId;
            return this;
        }

        public Builder setBusNumber(String busNumber) {
            this.busNumber = busNumber;
            return this;
        }

        public Builder setRegistrationNumber(String registrationNumber) {
            this.registrationNumber = registrationNumber;
            return this;
        }

        public Builder setCapacity(int capacity) {
            this.capacity = capacity;
            return this;
        }

        public Builder setMake(String make) {
            this.make = make;
            return this;
        }

        public Builder setModel(String model) {
            this.model = model;
            return this;
        }

        public Builder setSchool(School school) {
            this.school = school;
            return this;
        }

        public Builder setStartBusStop(BusStop startBusStop) {
            this.startBusStop = startBusStop;
            return this;
        }

        public SchoolBus build() {
            if(!StringUtils.hasText(busNumber) || registrationNumber == null || capacity <= 0 || school == null || startBusStop == null) {
                throw new RuntimeException("Not enough info to build School Bus.");
            }

            return new SchoolBus(vehicleId, busNumber, registrationNumber, capacity, school, startBusStop, make, model);
        }
    }
}
