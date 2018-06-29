package jstech.edu.transportmodel;

import java.io.IOException;
import java.util.Date;
import java.util.Properties;

import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.quartz.spi.JobFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.quartz.CronTriggerFactoryBean;
import org.springframework.scheduling.quartz.JobDetailFactoryBean;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.scheduling.quartz.SimpleTriggerFactoryBean;
import org.springframework.scheduling.support.CronSequenceGenerator;

@Configuration
public class SchedulerConfig {

    private static final Logger LOG = LoggerFactory.getLogger(SchedulerConfig.class);

    @Value( "${cron.refreshappdata:}" )
    private String cronRefreshAppData;

    @Value( "${cron.busstart:}" )
    private String cronBusStart;

    @Bean
    public JobFactory jobFactory(ApplicationContext applicationContext) {
        AutowiringSpringBeanJobFactory jobFactory = new AutowiringSpringBeanJobFactory();
        jobFactory.setApplicationContext(applicationContext);
        LOG.info("jobFactory...initialized..");
        return jobFactory;
    }

    @Bean
    public Scheduler refreshAppDataSchedulerFactoryBean(JobFactory jobFactory,
                                          @Qualifier("cronRefreshAppDataJobTrigger") Trigger cronRefreshAppDataJobTrigger) throws Exception {
        SchedulerFactoryBean factory = new SchedulerFactoryBean();
        factory.setQuartzProperties(getQuartzProperties());
        factory.afterPropertiesSet();

        Scheduler scheduler = factory.getScheduler();
        scheduler.setJobFactory(jobFactory);
        scheduler.scheduleJob((JobDetail) cronRefreshAppDataJobTrigger.getJobDataMap().get("jobDetail"), cronRefreshAppDataJobTrigger);
        LOG.info("starting scheduler for refresh app data...");
        scheduler.start();
        return scheduler;
    }

    @Bean
    public Scheduler startBusNotificationSchedulerFactoryBean(JobFactory jobFactory,
                                                   @Qualifier("cronStartBusNotificationJobTrigger") Trigger cronStartBusNotificationJobTrigger) throws Exception {
        SchedulerFactoryBean factory = new SchedulerFactoryBean();
        factory.setQuartzProperties(getQuartzProperties());
        factory.afterPropertiesSet();

        Scheduler scheduler = factory.getScheduler();
        scheduler.setJobFactory(jobFactory);
        scheduler.scheduleJob((JobDetail) cronStartBusNotificationJobTrigger.getJobDataMap().get("jobDetail"), cronStartBusNotificationJobTrigger);
        LOG.info("starting scheduler for startBusNotification....");
        scheduler.start();
        return scheduler;
    }

    @Bean
    public CronTriggerFactoryBean cronRefreshAppDataJobTrigger(@Qualifier("refreshAppDataJobDetail") JobDetail jobDetail) {
        LOG.info("cronRefreshAppDataJobTrigger......"+cronRefreshAppData);

        CronTriggerFactoryBean factoryBean = new CronTriggerFactoryBean();
        factoryBean.setJobDetail(jobDetail);
        factoryBean.setStartDelay(0L);
        factoryBean.setCronExpression(cronRefreshAppData);

        //This is to find the next run time
//        CronSequenceGenerator cronTrigger = new CronSequenceGenerator("0 0/1 * 1/1 * ?");
//        Date next = cronTrigger.next(new Date());
//        LOG.info("Next Execution Time: " + next);
        return factoryBean;
    }

    @Bean
    public CronTriggerFactoryBean cronStartBusNotificationJobTrigger(@Qualifier("startBusNotificationJobDetail") JobDetail jobDetail) {
        LOG.info("cronStartBusNotificationJobTrigger......"+cronBusStart);

        CronTriggerFactoryBean factoryBean = new CronTriggerFactoryBean();
        factoryBean.setJobDetail(jobDetail);
        factoryBean.setStartDelay(0L);
        factoryBean.setCronExpression(cronBusStart);
        return factoryBean;
    }
    @Bean
    public JobDetailFactoryBean refreshAppDataJobDetail() {
        JobDetailFactoryBean factoryBean = new JobDetailFactoryBean();
        factoryBean.setJobClass(RefreshAppDataJob.class);
        factoryBean.setDurability(true);
        LOG.info("refreshAppDataJobDetail....");
        return factoryBean;
    }

    @Bean
    public JobDetailFactoryBean startBusNotificationJobDetail() {
        JobDetailFactoryBean factoryBean = new JobDetailFactoryBean();
        factoryBean.setJobClass(StartBusNotificationJob.class);
        factoryBean.setDurability(true);
        LOG.info("startBusNotificationJobDetail....");
        return factoryBean;
    }


    public Properties getQuartzProperties() {
        Properties p = new Properties();
        p.setProperty("org.quartz.scheduler.instanceName", "Busbee_SchedulerJob");
        p.setProperty("org.quartz.scheduler.instanceId", "AUTO");
        p.setProperty("org.quartz.threadPool.threadCount", "5");
        p.setProperty("org.quartz.jobStore.class", "org.quartz.simpl.RAMJobStore");
        p.setProperty("org.quartz.threadPool.class", "org.quartz.simpl.SimpleThreadPool");
        p.setProperty("org.quartz.threadPool.makeThreadsDaemons", "true");
        p.setProperty("org.quartz.threadPool.threadPriority", "5");
        return p;
    }
}
