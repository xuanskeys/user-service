package com.xuan.userservice.config;

import com.xuan.userservice.entity.properties.LogServerConfig;
import com.xuan.userservice.entity.properties.RoleServerConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@RequiredArgsConstructor
public class RestClientConfig {

    private final RoleServerConfig roleServerConfig;
    private final LogServerConfig logServerConfig;

    @Bean
    public RestClient roleClient(RestClient.Builder builder) {
        return builder
                .baseUrl(roleServerConfig.getHost() + roleServerConfig.getPort())
                .build();
    }
    @Bean
    public RestClient logClient(RestClient.Builder builder) {
        return builder
                .baseUrl(logServerConfig.getHost() + logServerConfig.getPort())
                .build();
    }
}
