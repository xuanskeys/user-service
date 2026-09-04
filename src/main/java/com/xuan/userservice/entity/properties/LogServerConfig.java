package com.xuan.userservice.entity.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "log-service")
public class LogServerConfig {
    private String host;
    private String port;
}
