package jstech.edu.transportmodel.controller;


import jstech.edu.transportmodel.AppMain;
import jstech.edu.transportmodel.common.ParentNotificationSetting;
import jstech.edu.transportmodel.common.UserInfo;
import jstech.edu.transportmodel.service.SettingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(AppMain.REST_BASE_PATH)
public class SettingsController {

    @Autowired
    private SettingsService settingsService;

    @GetMapping(value="/settings_notification")
    public @ResponseBody
    List<ParentNotificationSetting> getSettings(@RequestAttribute(name="user_info") UserInfo userInfo) {
        return settingsService.getNotificationTime(userInfo);
    }

    @PostMapping(value="/settings_notification")
    public @ResponseBody
    String updateSettings(@RequestAttribute(name="user_info") UserInfo userInfo,
                          @RequestParam("minutes") int minutes,
                          @RequestParam("enabled") boolean enabled) {
        settingsService.updateNotificationTime(userInfo, minutes, enabled);
        return "{ \"status\" : \"success\" }";
    }
}
