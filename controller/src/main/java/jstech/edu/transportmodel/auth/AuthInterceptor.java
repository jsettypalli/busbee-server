package jstech.edu.transportmodel.auth;

import jstech.edu.transportmodel.BusBeeException;
import jstech.edu.transportmodel.common.UserInfo;
import jstech.edu.transportmodel.common.UserRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

@Component
public class AuthInterceptor extends HandlerInterceptorAdapter {
    private static final Logger logger = LoggerFactory.getLogger(AuthInterceptor.class);

    @Autowired
    private JWTTokenValidator tokenValidator;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        boolean isValid = false;
        try {
            String bearerToken = request.getHeader("Authorization");

            if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
                String jwt = bearerToken.substring(7, bearerToken.length());
                Map<String, Object> userDetails = new HashMap<>();
                isValid = tokenValidator.validate(jwt, userDetails);

                // if request is valid and userDetails are filled by tokenValidator, then create UserInfo object and add to request
                if (isValid && !userDetails.isEmpty()) {
                    UserInfo.Builder builder = new UserInfo.Builder();
                    String phoneNumber = (String) userDetails.get("phone_number");
                    builder.setPhoneNumber(phoneNumber);
                    String email = (String) userDetails.get("email");
                    builder.setEmail(email);
                    builder.setKeyProviderUserName((String) userDetails.get("auth_provider_username"));

                    List<String> userRoles = (List<String>) userDetails.get("auth_provider_roles");
                    if(userRoles == null || userRoles.isEmpty()) {
                        throw new BusBeeException( String.format("No Role is defined for the user with email:%s and phone-number:%s. " +
                                        "Please contact System Administrator.", email, phoneNumber));
                    }
                    Set<String> roles = new HashSet<>(userRoles);

                    builder.setKeyProviderRoles(roles);
                    builder.setRole(getApplicationRole(roles));

                    request.setAttribute("user_info", builder.build());
                }
            }
        } catch(Exception ex) {
            isValid = false;
            logger.warn(ex.getClass().getName() +" occurred while authenticating the request. Message:" + ex.getLocalizedMessage(), ex);
        }

        if (!isValid) {
            response.setStatus(401);
            response.getWriter().write("User not authenticated. Please resubmit with proper authentication token.");
        }

        return isValid;
    }

    //TODO - handle the case when user plays multiple roles
    private UserRole getApplicationRole(Set<String> roles) {
        UserRole userRole = UserRole.PARENT;
        for(String role: roles) {
            if(role.toLowerCase().contains(UserRole.DRIVER.toString().toLowerCase())) {
                userRole = UserRole.DRIVER;
                break;
            } else if(role.toLowerCase().contains(UserRole.PARENT.toString().toLowerCase())) {
                userRole = UserRole.PARENT;
            } else if(role.toLowerCase().contains(UserRole.TRANSPORT_INCHARGE.toString().toLowerCase())) {
                userRole = UserRole.TRANSPORT_INCHARGE;
                break;
            }
        }
        return userRole;
    }
}
