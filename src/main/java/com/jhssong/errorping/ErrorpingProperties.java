package com.jhssong.errorping;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Setter
@Getter
@ConfigurationProperties(prefix = "errorping")
public class ErrorpingProperties {
    private String apiKey;
    private StartupShutdownNotification startupShutdownNotification = new StartupShutdownNotification();


    @Getter
    @Setter
    public static class StartupShutdownNotification {
        private Boolean enabled = true;
        private String startupTitle = "서버 상태 알림";
        private String shutdownTitle = "서버 상태 알림";
        private String startupMessage = "서버가 실행되었습니다.";
        private String shutdownMessage = "서버가 종료되었습니다.";
    }
}
