package jstech.edu.transportmodel;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.core.AppenderBase;
import jstech.edu.transportmodel.common.DeviceInfo;
import jstech.edu.transportmodel.service.NotificationService;
import jstech.edu.transportmodel.service.UserService;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

@Component
public class CustomErrorAppender extends AppenderBase<ILoggingEvent> implements ApplicationContextAware {

    @Autowired
    private static UserService userService;

    @Autowired
    private static NotificationService notificationService;

    @Override
    protected void append(ILoggingEvent iLoggingEvent) {

        StringBuilder builder = new StringBuilder();
        try {
            String host = InetAddress.getLocalHost().getHostAddress();
            builder.append("Host:").append(host).append("; ");
        } catch (UnknownHostException e) {
            // nothing to handle here. Ignore the host info in the error message.
        }

        if(iLoggingEvent.getThrowableProxy() != null) {
            IThrowableProxy exceptionProxy = iLoggingEvent.getThrowableProxy();
            builder.append("Exception:").append(exceptionProxy.getClassName()).append("; ");
        }

        builder.append("Message:").append(iLoggingEvent.getFormattedMessage()).append("; ");
        String msg = builder.toString();

        List<DeviceInfo> devices = userService.getOnCallUserDevices();
        if(devices != null && !devices.isEmpty()) {
            //user Notification Service to send "Push Notifications" to these devices
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        AutowireCapableBeanFactory factory = applicationContext.getAutowireCapableBeanFactory();
        if (applicationContext.getAutowireCapableBeanFactory().getBean("userServiceImpl") != null) {
            userService = (UserService) applicationContext.getAutowireCapableBeanFactory().getBean("userServiceImpl");
        }

        if (applicationContext.getAutowireCapableBeanFactory().getBean("notificationServiceImpl") != null) {
            notificationService = (NotificationService) applicationContext.getAutowireCapableBeanFactory().getBean("notificationServiceImpl");
        }
    }
}
