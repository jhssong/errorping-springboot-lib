package com.jhssong.errorping;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StartupShutdownNotification {
    private final ErrorpingProperties props;
    private final ErrorpingService errorpingService;

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        ErrorpingProperties.StartupShutdownNotification startupShutdownNotification = props.getStartupShutdownNotification();
        if (!startupShutdownNotification.getEnabled()) {
            return;
        }

        errorpingService.sendInfo(startupShutdownNotification.getStartupTitle(),
                startupShutdownNotification.getStartupMessage());
    }

    @EventListener(ContextClosedEvent.class)
    public void onShutdown() {
        ErrorpingProperties.StartupShutdownNotification startupShutdownNotification = props.getStartupShutdownNotification();
        if (!startupShutdownNotification.getEnabled()) {
            return;
        }

        errorpingService.sendInfo(startupShutdownNotification.getShutdownTitle(),
                startupShutdownNotification.getShutdownMessage());
    }
}