package jstech.edu.transportmodel.service;

import jstech.edu.transportmodel.common.*;
import jstech.edu.transportmodel.dto.NotificationMessageDto;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

//@RunWith(SpringJUnit4ClassRunner.class)
//@ContextConfiguration(locations = { "classpath:spring-test-config.xml" })
//@SpringBootTest
public class NotificationServiceTest {

    @Autowired
    private NotificationServiceImpl notificationService;

    //@Test
    public void testPublishNotification() {

        String pushNotificationTopic = "-arrival_notification-5-5-41-10";

        School school = new School();
        school.setSchoolId(21);
        school.setName("meridian");
        BusStop busStop = new BusStop.Builder().setBusStopDetailId(41).setId(41).setName("sairam towers").setNumStudents(1)
                .setSchool(school).setLocation(17.0, 78.0).build();
        SchoolBus bus = new SchoolBus.Builder().setVehicleId(5).setBusNumber("K7").setRegistrationNumber("TS 07 UA 0052")
                .setCapacity(30).setSchool(school).setStartBusStop(busStop).build();

        Driver.Builder driverBuilder = new Driver.Builder();
        driverBuilder.setId(5);
        driverBuilder.setKeyProviderUserName("942d1418-b166-41c3-83d9-4f41d70d10e5");
        driverBuilder.addKeyProviderRole("driver");
        driverBuilder.setRole(UserRole.DRIVER);
        Driver driver = driverBuilder.build();

        NotificationMessageDto notificationMessageDto = NotificationMessageDto.Builder.build(bus, driver,
                "Your Stop is next in the route. The bus is expected to reach in 8mins from now",
                "Your Stop is next in the route.", "bus_arrival", 8L);

        notificationService.pushBusArrivalNotificationMessage(pushNotificationTopic, notificationMessageDto);
        System.out.println("Test Done...");
    }
}
