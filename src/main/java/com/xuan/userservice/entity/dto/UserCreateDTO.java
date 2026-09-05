package com.xuan.userservice.entity.dto;

import lombok.Data;

import java.util.List;

/**
 * 新增用户请求
 */
@Data
public class UserCreateDTO {

    /** 租户ID，不传默认绑定租户 1 */
    private Long tenantId;

    /** 用户名 */
    private String username;

    private String nickname;

    /** 邮箱（与 phone 至少传一个） */
    private String email;

    /** 手机号（与 email 至少传一个） */
    private String phone;

    /** 前端 AES 加密后的密码密文 */
    private String password;

    /** 加密配置ID，关联 encrypt_config.id（不传使用默认配置） */
    private Long encryptConfigId;

    /** 绑定角色ID列表（可选） */
    private List<Long> roleIds;
}
