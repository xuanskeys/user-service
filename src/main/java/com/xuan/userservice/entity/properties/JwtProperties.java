package com.xuan.userservice.entity.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "jwt")
@Data
public class JwtProperties {
    /** 签名密钥（建议至少 32 字节） */
    private String secret;

    /** 过期时间，单位：秒 */
    private Long expiration;
}
