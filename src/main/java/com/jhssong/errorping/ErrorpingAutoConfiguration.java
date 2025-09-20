package com.jhssong.errorping;

import com.jhssong.errorping.exception.GlobalExceptionHandler;
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

}
