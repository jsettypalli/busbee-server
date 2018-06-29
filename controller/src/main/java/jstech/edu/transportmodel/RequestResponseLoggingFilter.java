package jstech.edu.transportmodel;

import jstech.edu.transportmodel.common.UserInfo;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonMethod;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.*;

public class RequestResponseLoggingFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(RequestResponseLoggingFilter.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final List<MediaType> VISIBLE_TYPES = Arrays.asList(
            MediaType.valueOf("text/*"),
            MediaType.APPLICATION_FORM_URLENCODED,
            MediaType.APPLICATION_JSON,
            MediaType.APPLICATION_XML,
            MediaType.valueOf("application/*+json"),
            MediaType.valueOf("application/*+xml"),
            MediaType.MULTIPART_FORM_DATA
    );

    {
        objectMapper.setVisibility(JsonMethod.FIELD, JsonAutoDetect.Visibility.ANY);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, FilterChain filterChain) throws ServletException, IOException {
        if (isAsyncDispatch(httpServletRequest)) {
            filterChain.doFilter(httpServletRequest, httpServletResponse);
        } else {
            doFilterWrapped(wrapRequest(httpServletRequest), wrapResponse(httpServletResponse), filterChain);
        }
    }

    protected void doFilterWrapped(ContentCachingRequestWrapper request, ContentCachingResponseWrapper response, FilterChain filterChain) throws ServletException, IOException {
        RequestResponseObject requestResponseObject = new RequestResponseObject();
        try {
            beforeRequest(request, response, requestResponseObject);
            filterChain.doFilter(request, response);
        }
        finally {
            afterRequest(request, response, requestResponseObject);
            response.copyBodyToResponse();

            try {
                String requestResponseStr = objectMapper.writeValueAsString(requestResponseObject);
                logger.info(requestResponseStr);
            } catch (JsonGenerationException e) {
                logger.error("JsonGenerationException occurred while deserializing RequestResponseObject: {}", requestResponseObject, e);
            } catch (JsonMappingException e) {
                logger.error("JsonMappingException occurred while deserializing RequestResponseObject: {}", requestResponseObject, e);
            } catch (IOException e) {
                logger.error("IOException occurred while deserializing RequestResponseObject: {}", requestResponseObject, e);
            }
        }
    }

    protected void beforeRequest(ContentCachingRequestWrapper request, ContentCachingResponseWrapper response, RequestResponseObject requestResponseObject) {
        if (logger.isInfoEnabled()) {
            logRequestHeader(request, requestResponseObject,request.getRemoteAddr() + "|>");
        }
    }

    protected void afterRequest(ContentCachingRequestWrapper request, ContentCachingResponseWrapper response, RequestResponseObject requestResponseObject) {
        if (logger.isInfoEnabled()) {
            logRequestBody(request, requestResponseObject, request.getRemoteAddr() + "|>");
            logResponse(response, requestResponseObject, request.getRemoteAddr() + "|<");
        }
    }

    private void logRequestHeader(ContentCachingRequestWrapper request, RequestResponseObject requestResponseObject, String prefix) {
        requestResponseObject.request.method = request.getMethod();
        requestResponseObject.request.uri = request.getRequestURI();
        requestResponseObject.request.remoteAddress = request.getRemoteAddr();

        if (request.getQueryString() != null) {
            //logger.info("{} {} {}?{}", prefix, request.getMethod(), request.getRequestURI(), queryString);
            requestResponseObject.request.queryString = request.getQueryString();
        }
        /*Collections.list(request.getHeaderNames()).forEach(headerName ->
                Collections.list(request.getHeaders(headerName)).forEach(headerValue ->
                        logger.info("{} {}: {}", prefix, headerName, headerValue)));*/

        Collections.list(request.getHeaderNames()).forEach(headerName ->
                Collections.list(request.getHeaders(headerName)).forEach(headerValue ->
                        requestResponseObject.request.headers.put(headerName, headerName.equalsIgnoreCase("authorization") ? "" : headerValue)));

        //logger.info("{}", prefix);
    }

    private void logRequestBody(ContentCachingRequestWrapper request, RequestResponseObject requestResponseObject, String prefix) {
        if(request.getAttribute("user_info") != null) {
            UserInfo userInfo = (UserInfo) request.getAttribute("user_info");
            requestResponseObject.request.userName = userInfo.getKeyProviderUserName();
            requestResponseObject.request.userRole = userInfo.getRole().toString();
        }

        byte[] content = request.getContentAsByteArray();
        if (content.length > 0) {
            requestResponseObject.request.payload = logContent(content, request.getContentType(), request.getCharacterEncoding(), prefix, true).toString();
        }
    }

    private void logResponse(ContentCachingResponseWrapper response, RequestResponseObject requestResponseObject, String prefix) {
        int status = response.getStatus();
        //logger.info("{} {} {}", prefix, status, HttpStatus.valueOf(status).getReasonPhrase());
        requestResponseObject.response.status = String.valueOf(status);
        requestResponseObject.response.reason = HttpStatus.valueOf(status).getReasonPhrase();

        /*response.getHeaderNames().forEach(headerName ->
                response.getHeaders(headerName).forEach(headerValue ->
                        logger.info("{} {}: {}", prefix, headerName, headerValue)));*/

        response.getHeaderNames().forEach(headerName ->
                response.getHeaders(headerName).forEach(headerValue ->
                        requestResponseObject.response.headers.put(headerName, headerValue)));

        //logger.info("{}", prefix);
        byte[] content = response.getContentAsByteArray();
        if (content.length > 0) {
            requestResponseObject.response.body = logContent(content, response.getContentType(), response.getCharacterEncoding(), prefix, false);
        }
    }

    private Object logContent(byte[] content, String contentType, String contentEncoding, String prefix, boolean requestPayload) {
        boolean visible = false;
        if(contentType != null) {
            MediaType mediaType = MediaType.valueOf(contentType);
            visible = VISIBLE_TYPES.stream().anyMatch(visibleType -> visibleType.includes(mediaType));
        }
        if(content == null) {
            return "";
        }

        if (visible) {
            try {
                String contentString = new String(content, contentEncoding);
                if(requestPayload) {
                    return contentString;
                }
                try {
                    if(contentString.startsWith("[") && contentString.endsWith("]")) {
                        List<Map<String, Object>> myObjects =
                                objectMapper.readValue(contentString, new TypeReference<List<Map<String, Object>>>(){});
                        return myObjects;
                    } else {
                        Map<String, Object> jsonMap = objectMapper.readValue(contentString,
                                new TypeReference<Map<String, Object>>() {
                                });
                        return jsonMap;
                    }
                } catch(IOException e) {
                    return contentString;
                }

                //Stream.of(contentString.split("\r\n|\r|\n")).forEach(line -> logger.info("{} {}", prefix, line));
                //return contentString;
            } catch (UnsupportedEncodingException e) {
                //logger.info("{} [{} bytes content]", prefix, content.length);
                return String.format("[%d bytes content]", content.length);
            }
        } else {
            try {
                String str = new String(content);
                //logger.info("{} [{}]", prefix, str);
                return str;
            } catch(Exception ex) {
                //logger.info("{} [{} bytes content]", prefix, content.length);
                return String.format("[%d bytes content]", content.length);
            }

        }
    }

    private static ContentCachingRequestWrapper wrapRequest(HttpServletRequest request) {
        if (request instanceof ContentCachingRequestWrapper) {
            return (ContentCachingRequestWrapper) request;
        } else {
            return new ContentCachingRequestWrapper(request);
        }
    }

    private static ContentCachingResponseWrapper wrapResponse(HttpServletResponse response) {
        if (response instanceof ContentCachingResponseWrapper) {
            return (ContentCachingResponseWrapper) response;
        } else {
            return new ContentCachingResponseWrapper(response);
        }
    }

    private static class RequestResponseObject {
        RequestObject request = new RequestObject();
        ResponseObject response = new ResponseObject();

        @Override
        public String toString() {
            return "RequestResponseObject{" +
                    "request=" + request +
                    ", responseObject=" + response +
                    '}';
        }
    }

    private static class RequestObject {
        String method;
        String uri;
        String queryString;
        String remoteAddress;
        Map<String, String> headers = new HashMap<>();
        String payload;
        String userName;
        String userRole;

        @Override
        public String toString() {
            return "RequestObject{" +
                    "method='" + method + '\'' +
                    ", uri='" + uri + '\'' +
                    ", queryString='" + queryString + '\'' +
                    ", remoteAddress='" + remoteAddress + '\'' +
                    ", headers=" + headers +
                    ", payload='" + payload + '\'' +
                    ", userName='" + userName + '\'' +
                    ", userRole='" + userRole + '\'' +
                    '}';
        }
    }

    private static class ResponseObject {
        String status;
        String reason;
        Map<String, String> headers = new HashMap<>();
        Object body;

        @Override
        public String toString() {
            return "ResponseObject{" +
                    "status='" + status + '\'' +
                    ", headers=" + headers +
                    ", body='" + body + '\'' +
                    '}';
        }
    }
}
