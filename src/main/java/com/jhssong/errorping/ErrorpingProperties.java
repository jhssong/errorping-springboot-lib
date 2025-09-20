package com.jhssong.errorping;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Setter
@Getter
@ConfigurationProperties(prefix = "errorping")
public class ErrorpingProperties {
    private String apiKey;
    private String channelId;
}
