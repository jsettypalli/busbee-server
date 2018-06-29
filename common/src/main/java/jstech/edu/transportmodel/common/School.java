package jstech.edu.transportmodel.common;

import java.util.Objects;

public class School {
    private int schoolId;
    private String name;
    private String address;
    private GeoLocation location;

    public int getSchoolId() {
        return schoolId;
    }

    public void setSchoolId(int schoolId) {
        this.schoolId = schoolId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public GeoLocation getLocation() {
        return location;
    }

    public void setLocation(double latitude, double longitude) {
        this.location = new GeoLocation(latitude, longitude);
    }

    public void setLocation(GeoLocation location) {
        this.location = location;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        School school = (School) o;
        return Objects.equals(name, school.name);
    }

    @Override
    public int hashCode() {

        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return "School{" +
                "schoolId=" + schoolId +
                ", name='" + name + '\'' +
                ", address='" + address + '\'' +
                '}';
    }
}
