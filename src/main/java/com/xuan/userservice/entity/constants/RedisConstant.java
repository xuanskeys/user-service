package com.xuan.userservice.entity.constants;

/**
 * redis相关常量类
 */
public class RedisConstant {
    private static final String USER_TOKEN_PREFIX = "user:token:"; // 用户token前缀
    private static final Long USER_TOKEN_EXPIRE_TIME = 3600L; // 用户token有效期
}
