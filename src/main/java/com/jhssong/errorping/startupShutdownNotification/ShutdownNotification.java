package com.jhssong.errorping.startupShutdownNotification;

import com.jhssong.errorping.ErrorpingProperties;
import com.jhssong.errorping.ErrorpingProperties.StartupShutdownNotification;
import com.jhssong.errorping.ErrorpingService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ShutdownNotification {

    private final ErrorpingProperties props;
    private final ErrorpingService errorpingService;

    @EventListener
    public void onShutdown(ContextClosedEvent event) {
        StartupShutdownNotification startupShutdownNotification = props.getStartupShutdownNotification();
        if (!startupShutdownNotification.getEnabled()) {
            return;
        }

        errorpingService.sendInfo(startupShutdownNotification.getShutdownTitle(),
                startupShutdownNotification.getShutdownMessage());
    }
}
