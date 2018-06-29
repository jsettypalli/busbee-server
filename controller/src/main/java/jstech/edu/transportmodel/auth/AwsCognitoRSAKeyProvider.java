package jstech.edu.transportmodel.auth;

import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.JwkProviderBuilder;
import com.auth0.jwt.interfaces.RSAKeyProvider;
import jstech.edu.transportmodel.BusBeeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

@Component
public class AwsCognitoRSAKeyProvider implements RSAKeyProvider, AuthKeyLoader {

    private static final Logger logger = LoggerFactory.getLogger(AwsCognitoRSAKeyProvider.class);

    @Value("${aws.region:}")
    private String aws_cognito_region;

    @Value( "${aws.cognito.user_pools_id:}" )
    private String aws_user_pools_id;

    private URL aws_kid_store_url;
    private JwkProvider provider;

    @PostConstruct
    @Override
    public void loadKeys() throws BusBeeException{
        String url = String.format("https://cognito-idp.%s.amazonaws.com/%s/.well-known/jwks.json", aws_cognito_region, aws_user_pools_id);
        try {
            this.aws_kid_store_url = new URL(url);
            provider = new JwkProviderBuilder(aws_kid_store_url).build();
        } catch (MalformedURLException e) {
            String msg = String.format("MalFormedURLException occurred while loading keys from Cognito. The URL may be invalid. URL: %s", url);
            throw new BusBeeException(msg, e);
        }
    }

    @Override
    public RSAPublicKey getPublicKeyById(String kid) {
        try {
            Jwk jwk = provider.get(kid);
            return (RSAPublicKey) jwk.getPublicKey();
        } catch (Exception e) {
            throw new RuntimeException(String.format("Failed to get JWT kid=%s from aws_kid_store_url=%s", kid, aws_kid_store_url));
        }
    }

    @Override
    public RSAPrivateKey getPrivateKey() {
        return null;
    }

    @Override
    public String getPrivateKeyId() {
        return null;
    }
}