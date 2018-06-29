package jstech.edu.transportmodel;

import jstech.edu.transportmodel.common.*;
import jstech.edu.transportmodel.dto.NotificationMessageDto;
import jstech.edu.transportmodel.service.NotificationService;
import jstech.edu.transportmodel.service.route.RouteService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.support.CronSequenceGenerator;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;


public class StartBusNotificationJob implements Job {

    private static final Logger LOG = LoggerFactory.getLogger(StartBusNotificationJob.class);

    @Autowired
    private RouteService routeService;

    @Autowired
    private NotificationService notificationService;

    @Value("${cron.busstart:}")
    private String cronBusStart;

    private static final int FIVE_MINUTES = 5;
    private static final int FIFTEEN_MINUTES = 15;
    private static final int JOB_INTERVAL = 1;

    @Override
    public void execute(JobExecutionContext jobExecutionContext) {
        LOG.info("StartBusNotificationJob...current time is:" + new Date());
        //This is to find the next run time
        CronSequenceGenerator cronTrigger = new CronSequenceGenerator(cronBusStart);
        Date next = cronTrigger.next(new Date());
        final ZoneId systemDefault = ZoneId.systemDefault();
        LOG.info("Next Cron Runtime is:"+ZonedDateTime.ofInstant(next.toInstant(), systemDefault));
        ZonedDateTime nextRun = ZonedDateTime.ofInstant(next.toInstant(), systemDefault);

        List<BusTrip> trips = routeService.getAllTrips();
        for(BusTrip trip: trips) {
            for (Map.Entry<SchoolBus, SchoolBusRoute> entry : trip.getBusRoutes().entrySet()) {
                SchoolBus bus = entry.getKey();
                SchoolBusRoute route = entry.getValue();

                // go to next iteration if the bus is IN_TRANSIT or COMPLETED (basically not YET_TO_START)
                if(RouteStatus.YET_TO_START != route.getRouteStatus()) {
                    continue;
                }

                // if start time of the route is after current time
                // and start time of the route is before or equal to the next run time of the job
                // send 1st notification to driver that trip should start shortly.
                if(route.getStartDateTime().isAfter(ZonedDateTime.now()) &&
                        (route.getStartDateTime().isBefore(nextRun) || route.getStartDateTime().isEqual(nextRun))) {

                    String message = String.format("Your trip is scheduled to start in %d %s. Please proceed to start the trip.",
                            JOB_INTERVAL, JOB_INTERVAL > 1 ? "mins" : "min");
                    String shortMessage = String.format("Your trip is scheduled to start in %d %s.",
                            JOB_INTERVAL, JOB_INTERVAL > 1 ? "mins" : "min");

                    String startTrackingTopicName="-start_tracking-"+trip.getTripId()+"-"+bus.getVehicleId();

                    Driver driver = routeService.getDriverBySchoolBus(bus);
                    NotificationMessageDto notificationMessageDto = NotificationMessageDto.Builder.build(bus, driver, message, shortMessage, "start_bus");
                    LOG.info("Sending message to driver to start the bus. Topic:{}, Message:{}", startTrackingTopicName, notificationMessageDto);
                    notificationService.pushDriverReminderNotificationMessage(startTrackingTopicName, notificationMessageDto);
                    //notificationService.pushNotification(startTrackingTopicName, message);
                }
                // if bus didn't start even after 5 mins of scheduled start time, then send 1st reminder to driver.
                else if(ZonedDateTime.now().isAfter(route.getStartDateTime().plusMinutes(FIVE_MINUTES)) &&
                        ZonedDateTime.now().isBefore(route.getStartDateTime().plusMinutes(FIVE_MINUTES+JOB_INTERVAL))) {
                    String message = "Your trip DIDN'T START as scheduled. Please start the trip.";
                    String shortMessage = "Your trip DIDN'T START as scheduled. Please start the trip.";
                    String startTrackingTopicName="-start_tracking-"+trip.getTripId()+"-"+bus.getVehicleId();

                    Driver driver = routeService.getDriverBySchoolBus(bus);
                    NotificationMessageDto notificationMessageDto = NotificationMessageDto.Builder.build(bus, driver, message, shortMessage, "start_bus");
                    LOG.info("Sending reminder message to driver to start the bus after 5mins from scheduled time. Topic:{}, Message:{}", startTrackingTopicName, notificationMessageDto);
                    notificationService.pushDriverReminderNotificationMessage(startTrackingTopicName, notificationMessageDto);
                    //notificationService.pushNotification(startTrackingTopicName, message);
                }
                // if bus didn't start even after 10 mins of scheduled start time, then escalate to transport-in-charge
                else if(ZonedDateTime.now().isAfter(route.getStartDateTime().plusMinutes(FIFTEEN_MINUTES)) &&
                        ZonedDateTime.now().isBefore(route.getStartDateTime().plusMinutes(FIFTEEN_MINUTES+JOB_INTERVAL))) {
                    String message = String.format("Bus number %s is scheduled to start 15min ago but didn't start yet. " +
                            "Request you to followup with the driver.", bus.getBusNumber());
                    String shortMessage = String.format("Bus number %s is scheduled to start 15min ago but didn't start yet.", bus.getBusNumber());
                    String escalateTopicName="-escalate_start_tracking_delay-"+trip.getTripId()+"-"+bus.getVehicleId();

                    Driver driver = routeService.getDriverBySchoolBus(bus);
                    NotificationMessageDto notificationMessageDto = NotificationMessageDto.Builder.build(bus, driver, message, shortMessage, "start_bus_delay");
                    LOG.info("Sending escalation message to transport-in-charge that bus is not started even after 15mins from scheduled time. Topic:{}, Message:{}", escalateTopicName, notificationMessageDto);
                    notificationService.pushDriverReminderNotificationMessage(escalateTopicName, notificationMessageDto);
                    //notificationService.pushNotification(escalateTopicName, message);
                }

                /* --- Below is supposed to be 2nd reminder to driver, if the bus didn't start even after 10mins.
                   --- But it was changed to escalate the delay to transport-in-charge, in this case.
                else if(route.getStartDateTime().isAfter(ZonedDateTime.now()) &&
                        ZonedDateTime.now().isAfter(route.getStartDateTime().plusMinutes(10))
                        && route.getRouteStatus().equals(RouteStatus.YET_TO_START)) {
                    String message = "Your trip is NOT STARTED as scheduled. This is the FINAL reminder. Please proceed to start the trip.";
                    String startTrackingTopicName="-start_tracking-"+trip.getTripId()+"-"+bus.getVehicleId();
                    notificationService.pushNotification(startTrackingTopicName, message);
                }
                 */
            }
        }

    }
}
