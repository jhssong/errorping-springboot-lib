package com.jhssong.errorping;

import com.jhssong.errorping.exception.GlobalExceptionHandler;
import com.jhssong.errorping.startupShutdownNotification.ShutdownNotification;
import com.jhssong.errorping.startupShutdownNotification.StartupNotification;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@EnableConfigurationProperties(ErrorpingProperties.class)
public class ErrorpingAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ErrorpingService errorpingService(ErrorpingProperties props) {
        return new ErrorpingService(props);
    }

    @Bean
    @ConditionalOnMissingBean
    public GlobalExceptionHandler globalExceptionHandler(ErrorpingService errorpingService) {
        return new GlobalExceptionHandler(errorpingService);
    }

    @Bean
    @ConditionalOnMissingBean
    public StartupNotification startupNotification(ErrorpingProperties props, ErrorpingService errorpingService) {
        return new StartupNotification(props, errorpingService);
    }

    // 4. 서버 종료 알림 컴포넌트 빈 등록
    @Bean
    @ConditionalOnMissingBean
    public ShutdownNotification shutdownNotification(ErrorpingProperties props, ErrorpingService errorpingService) {
        return new ShutdownNotification(props, errorpingService);
    }

}
