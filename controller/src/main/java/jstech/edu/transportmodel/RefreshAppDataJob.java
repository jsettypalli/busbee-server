package jstech.edu.transportmodel;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class RefreshAppDataJob implements Job {

    private static final Logger LOG = LoggerFactory.getLogger(RefreshAppDataJob.class);

    @Autowired
    private AppDataInitializer appDataInitializer;


    @Override
    public void execute(JobExecutionContext jobExecutionContext) {
        LOG.info("RefreshAppDataJob...");
        try {
            appDataInitializer.loadData();
        } catch(Exception ex) {
            LOG.error(ex.getClass().getName() + " occurred while loading data into memory from AppInitializer. Message:" + ex.getLocalizedMessage());
        }
    }
}
