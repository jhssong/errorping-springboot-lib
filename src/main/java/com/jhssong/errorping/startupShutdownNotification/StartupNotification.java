package com.jhssong.errorping.startupShutdownNotification;

import com.jhssong.errorping.ErrorpingProperties;
import com.jhssong.errorping.ErrorpingProperties.StartupShutdownNotification;
import com.jhssong.errorping.ErrorpingService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StartupNotification implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StartupNotification.class);
    private final ErrorpingProperties props;
    private final ErrorpingService errorpingService;

    @Override
    public void run(ApplicationArguments args) {
        StartupShutdownNotification startupShutdownNotification = props.getStartupShutdownNotification();
        if (!startupShutdownNotification.getEnabled()) {
            return;
        }

        log.info("Server started!");
        errorpingService.sendInfo(startupShutdownNotification.getStartupTitle(),
                startupShutdownNotification.getStartupMessage());
    }
}
