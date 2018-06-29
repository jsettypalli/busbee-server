package jstech.edu.transportmodel.service;

import jstech.edu.transportmodel.common.School;
import jstech.edu.transportmodel.dao.SchoolDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SchoolServiceImpl implements SchoolService {

    @Autowired
    private SchoolDao schoolDao;

    @Override
    public School getSchool(int schoolId) {
        return schoolDao.getSchool(schoolId);
    }

    @Override
    public School getSchool(String name) {
        return schoolDao.getSchool(name);
    }

    @Override
    @Transactional
    public School addSchool(String name, double latitude, double longitude) {
        return schoolDao.addSchool(name, latitude, longitude);
    }

    @Override
    @Transactional
    public School updateSchool(School school) {
        return schoolDao.updateSchool(school);
    }

    @Override
    @Transactional
    public School deleteSchool(String name) {
        School school = schoolDao.getSchool(name);
        return deleteSchool(school);
    }

    @Override
    @Transactional
    public School deleteSchool(School school) {
       return schoolDao.deleteSchool(school);
    }
}
