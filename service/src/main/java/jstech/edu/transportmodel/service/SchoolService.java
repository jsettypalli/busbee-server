package jstech.edu.transportmodel.service;

import jstech.edu.transportmodel.common.School;

public interface SchoolService {
    School getSchool(int schoolId);
    School getSchool(String name);

    School addSchool(String name, double latitude, double longitude);
    School updateSchool(School school);
    School deleteSchool(String name);
    School deleteSchool(School school);
}
