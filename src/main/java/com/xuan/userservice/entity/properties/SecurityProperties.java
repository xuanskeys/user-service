package com.xuan.userservice.entity.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "security")
public class SecurityProperties {
    /** Paths that do not require an access token. Values support Ant-style patterns. */
    private List<String> loginWhitelist = new ArrayList<>(List.of(
            "/user/register",
            "/user/login",
            "/user/send-code"
    ));
}
