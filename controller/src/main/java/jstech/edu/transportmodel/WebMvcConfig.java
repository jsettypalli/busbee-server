package jstech.edu.transportmodel;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
// DON'T ADD BELOW ANNOTATION. THIS IS SPRING-BOOT APPLICATION AND ADDING THIS CAUSES WEIRD ISSUES. FIGURED OUT AFTER WASTING SOME TIME...
// IT IS EXPLICITLY ADDED BELOW & COMMENTED, SO NO ONE NEED TO SPEND TIME ON THIS.
// SPRING-BOOT INCLUDES THIS IN AUTO-CONFIGURATION. SO NOT REQUIRED TO ADD EXPLICITLY.
//@EnableWebMvc
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private HandlerInterceptor authInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor).addPathPatterns("/"+AppMain.REST_BASE_PATH+"/**");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**").allowedOrigins("*").allowedMethods("*");
    }
}
