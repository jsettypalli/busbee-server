package jstech.edu.transportmodel;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.Map;

@ControllerAdvice
public class RestExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(value = {Exception.class})
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, WebRequest request) {
        String bodyOfResponse;
        if(ex instanceof BusBeeException || ex instanceof SQLException || ex instanceof GeoException) {
            bodyOfResponse = "Application Error occurred. Please try after some time.";
        } else if(ex.getClass().getName().contains("java.lang")) {
            // These are defects in the code. Proper validation of the objects should be performed in the code to prevent these exceptions.
            // Now that it has occurred, send graceful message to the client.
            bodyOfResponse = "We are sorry, an error occurred while processing the request. Our support staff is notified and will be addressed shortly. Thanks for your patience.";
        }
        else {
            bodyOfResponse = "We are sorry, an error occurred while processing the request. Message:" + ex.getLocalizedMessage();
        }
        String str = request.toString();
        logger.error(ex.getClass().getName()+" occurred. HTTP Request: " + makeRequestString(request), ex);
        return handleExceptionInternal(ex, bodyOfResponse,
                new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    protected ResponseEntity<Object> handleServletRequestBindingException(Exception ex, WebRequest request) {
        String message = ex.getLocalizedMessage();
        // logging at WARN level, because it is the problem with the way request is invoked and support staff don't need to be alerted.
        logger.warn(message, ex);

        String bodyOfResponse = "One or more of parameters given are not valid. Please resubmit with valid parameters.";
        return handleExceptionInternal(ex, bodyOfResponse,
                new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
    }

    private String makeRequestString(WebRequest request) {
        StringBuilder builder = new StringBuilder();
        builder.append(" { ");
        builder.append("\"context_path\": \"").append(request.getContextPath()).append("\", ");
        builder.append("\"description\": \"").append(request.getDescription(true)).append("\", ");
        builder.append("\"remote_user\": \"").append(request.getRemoteUser()).append("\", ");
        builder.append("\"user_principal\": \"").append(request.getUserPrincipal()).append("\", ");
        builder.append("\"is_secure\": \"").append(request.isSecure()).append("\", ");

        int headerCount = 0;
        builder.append("\"headers\": [{ ");
        for(Iterator<String> iter = request.getHeaderNames(); iter.hasNext();) {
            String header = iter.next();
            String[] values = request.getHeaderValues(header);
            if(headerCount > 0) {
                builder.append(", ");
            }
            builder.append("\"").append(header).append("\" : ");
            if(values != null) {
                builder.append("\"").append(String.join(", ", values)).append("\"");
            }
            headerCount++;
        }
        builder.append(" }], ");

        int paramsCount=0;
        builder.append("parameters: [{ ");
        for(Map.Entry<String, String[]> entry: request.getParameterMap().entrySet()) {
            String param = entry.getKey();
            String[] values = entry.getValue();
            if(paramsCount > 0) {
                builder.append(", ");
            }
            builder.append("\"").append(param).append("\": ").append("\"").append(String.join(", ", values)).append("\"");
            paramsCount++;
        }
        builder.append(" }] ");

        builder.append(" }");

        return builder.toString();
    }
}
