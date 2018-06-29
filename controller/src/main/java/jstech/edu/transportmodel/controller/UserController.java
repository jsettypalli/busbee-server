package jstech.edu.transportmodel.controller;

import jstech.edu.transportmodel.AppMain;
import jstech.edu.transportmodel.common.DeviceInfo;
import jstech.edu.transportmodel.common.Picture;
import jstech.edu.transportmodel.common.Platform;
import jstech.edu.transportmodel.common.UserInfo;
import jstech.edu.transportmodel.service.UserService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(AppMain.REST_BASE_PATH)
public class UserController {
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;

    @GetMapping(value="/devices")
    public @ResponseBody void getDevices(@RequestAttribute(name="user_info") UserInfo userInfo) {

    }

    @PostMapping(value="/devices")
    public @ResponseBody void addDevice(@RequestParam("platform") String platform,
                          @RequestParam("device_id") String deviceId,
                          @RequestParam("application_token") String appToken,
                          @RequestAttribute(name="user_info") UserInfo userInfo) throws Exception {

        Platform devicePlatform = getPlatform(platform);
        DeviceInfo deviceInfo = new DeviceInfo.Builder().setDeviceId(deviceId).setAppToken(appToken).setPlatform(devicePlatform).build();
        userService.addDevice(userInfo, deviceInfo);
    }

    @PutMapping(value="/devices")
    public @ResponseBody void updateDevice(@RequestParam("platform") String platform,
                             @RequestParam("device_id") String deviceId,
                             @RequestParam(value="application_token", required=false) String appToken, @RequestParam(value="endpointarn", required=false) String endPointArn,
                             @RequestAttribute(name="user_info") UserInfo userInfo) throws Exception {
        Platform devicePlatform = getPlatform(platform);
        DeviceInfo deviceInfo = new DeviceInfo.Builder().setDeviceId(deviceId).setAppToken(appToken).setPlatform(devicePlatform).setEndPointArn(endPointArn).build();
        userService.updateDevice(userInfo, deviceInfo);
    }

    @DeleteMapping(value="/devices")
    public @ResponseBody void removeDevice(@RequestParam("platform") String platform,
                             @RequestParam("device_id") String deviceId,
                             @RequestParam("application_token") String appToken,
                             @RequestAttribute(name="user_info") UserInfo userInfo) throws Exception {
        Platform devicePlatform = getPlatform(platform);
        DeviceInfo deviceInfo = new DeviceInfo.Builder().setDeviceId(deviceId).setAppToken(appToken).setPlatform(devicePlatform).build();
        userService.removeDevice(userInfo, deviceInfo);
    }

    // or this could be the url -- "http://52.66.155.37/images/thumb_nail/<person_id>"
    @GetMapping(value="/users/{user_name}/thumbnail", produces = {MediaType.IMAGE_GIF_VALUE, MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_PNG_VALUE})
    public ResponseEntity<byte[]> getThumbnailImage(@PathVariable("user_name") String userName) {
        return getImage(userName, true);
    }

    // or this could be the url -- "http://52.66.155.37/images/regular/13"
    @GetMapping(value="/users/{user_name}/regular", produces = {MediaType.IMAGE_GIF_VALUE, MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_PNG_VALUE})
    public ResponseEntity<byte[]> getRegularImage(@PathVariable("user_name") String userName) {
        return getImage(userName, false);
    }

    private ResponseEntity<byte[]> getImage(String userName, boolean thumbNailPicture ) {
        Picture picture = thumbNailPicture ? userService.getThumbnailPicture(userName) : userService.getRegularPicture(userName);
        if(picture == null || picture.getData().length <= 0) {
            // return no data http response
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        String extension="";
        if(picture.getFileName().lastIndexOf(".") != -1 && picture.getFileName().lastIndexOf(".") != 0)
            extension = picture.getFileName().substring(picture.getFileName().lastIndexOf(".")+1);

        HttpHeaders httpHeaders = new HttpHeaders();

        switch(extension) {
            case "gif":
                httpHeaders.setContentType(MediaType.IMAGE_GIF);
                break;
            case "jpg":
            case "jpeg":
                httpHeaders.setContentType(MediaType.IMAGE_JPEG);
                break;
            case "png":
                httpHeaders.setContentType(MediaType.IMAGE_PNG);
                break;
        }

        return new ResponseEntity<>(picture.getData(), httpHeaders, HttpStatus.OK);
    }

    // TODO - ideally MethodArgumentNotValidException should be thrown. However, construction of this exception doesn't seem to be simple
    //      - Goal now is to throw an exception so it is caught in RestExceptionHandler and user friendly message is sent to client.
    //      - Change this to throw MethodArgumentNotValidException exception.
    private Platform getPlatform(String platform) throws ServletRequestBindingException {
        Platform devicePlatform = Platform.getInstance(platform);
        if(devicePlatform == null) {
            String msg = String.format("Platform given (%s) is not valid. Valid values are %s", platform, StringUtils.join(Platform.values()));
            throw new ServletRequestBindingException(msg);
        }
        return devicePlatform;
    }
}
