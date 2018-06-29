package jstech.edu.transportmodel.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.RSAKeyProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class JWTTokenValidator {
    private static final Logger logger = LoggerFactory.getLogger(JWTTokenValidator.class);

    @Value("${aws.region:}")
    private String aws_cognito_region;

    @Value( "${aws.cognito.app_client_id:}" )
    private String app_client_id;

    @Value( "${aws.cognito.user_pools_id:}" )
    private String aws_user_pools_id = "ap-south-1_9V9g6m8sU";

    @Autowired
    private RSAKeyProvider keyProvider;

    public boolean validate(String token, Map<String, Object> userDetails){
        Algorithm algorithm = Algorithm.RSA256(keyProvider);

        // include verification of client-id that generated this token, matches with application client-id in below method
        JWTVerifier jwtVerifier = JWT.require(algorithm)
                .withAudience(app_client_id) // Validate your apps audience if needed
                .build();

        // this verifies signature of the token and returns decoded-jwt object
        DecodedJWT decodedJWT = jwtVerifier.verify(token);

        if(decodedJWT == null) {
            logger.warn("decodedJWT is null for region {} and user_pool id: {}.", aws_cognito_region, aws_user_pools_id);
            return false;
        }

        //Fail if token is not from your User Pool
        String tokenIssuer = String.format("https://cognito-idp.%s.amazonaws.com/%s", aws_cognito_region, aws_user_pools_id);
        if (!decodedJWT.getIssuer().equals(tokenIssuer)) {
            logger.warn("The issuer of the token for region: {} and user_pool id:{} is not from the expected issuer.",
                    aws_cognito_region, aws_user_pools_id);
            return false;
        }

        // check if the token_use is either "id" or "access"
        Claim claim =  decodedJWT.getClaims().get("token_use");
        if(claim == null || claim.isNull() || (!claim.asString().equals("id") && !claim.asString().equals("access"))) {
            logger.warn("The token-use has to be either id or access.");
            return false;
        }

        // check if the token has expired
        Date currentDate = Calendar.getInstance().getTime();
        Date expiryDate = decodedJWT.getExpiresAt();
        if(expiryDate.before(currentDate)) {
            logger.warn("Invalid token. Token has expired.");
            return false;
        }

        claim =  decodedJWT.getClaims().get("phone_number");
        if(claim != null && !claim.isNull() && userDetails != null) {
            String phoneNumber = claim.asString();
            userDetails.put("phone_number", phoneNumber);
        }

        claim =  decodedJWT.getClaims().get("email");
        if(claim != null && !claim.isNull() && userDetails != null) {
            String email = claim.asString();
            userDetails.put("email", email);
        }

        claim =  decodedJWT.getClaims().get("cognito:username");
        if(claim != null && !claim.isNull() && userDetails != null) {
            String userName = claim.asString();
            userDetails.put("auth_provider_username", userName);
        }

        claim = decodedJWT.getClaims().get("cognito:roles");
        if(claim != null && !claim.isNull() && userDetails != null) {
            List<String> roles = claim.asList(String.class);
            userDetails.put("auth_provider_roles", roles);
        }

        return true;
    }
}
