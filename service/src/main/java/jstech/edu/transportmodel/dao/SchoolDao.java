package jstech.edu.transportmodel.dao;

import jstech.edu.transportmodel.common.School;

public interface SchoolDao {
    School getSchool(int schoolId);
    School getSchool(String name);

    School addSchool(String name, double latitude, double longitude);
    School addSchool(School school);
    School updateSchool(School school);
    School deleteSchool(School school);
}
