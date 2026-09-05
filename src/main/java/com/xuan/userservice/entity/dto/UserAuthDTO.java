package com.xuan.userservice.entity.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

/**
 * 注册 / 登录 请求（邮箱或手机号两种方式）
 * type = email 表示邮箱方式，type = phone 表示手机号方式
 */
@Data
public class UserAuthDTO {

    /** 方式：email / phone */
    @NotBlank(message = "认证方式不能为空")
    private String type;

    /** 邮箱或手机号（根据 type） */
    @NotBlank(message = "账号不能为空")
    private String account;

    /** 前端 AES 加密后的密码密文 */
    @NotBlank(message = "密码不能为空")
    private String password;

    /** 验证码（注册及邮箱登录时必填） */
    private String code;
}
