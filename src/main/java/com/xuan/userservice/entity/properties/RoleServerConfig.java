package com.xuan.userservice.entity.properties;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "role-service")
@Data
public class RoleServerConfig {
    private String host;
    private String port;
}
