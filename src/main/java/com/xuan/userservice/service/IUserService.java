package com.xuan.userservice.service;

import com.xuan.userservice.entity.dto.SendCodeDTO;
import com.xuan.userservice.entity.dto.UserAuthDTO;
import com.xuan.userservice.entity.dto.UserCreateDTO;
import com.xuan.userservice.entity.model.User;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Map;

/**
 * <p>
 * 用户表 服务类
 * </p>
 *
 * @author xuan
 * @since 2026-09-03
 */
public interface IUserService extends IService<User> {

    /** 新增用户（绑定租户、可选角色，密码 AES 解密后 BCrypt 落库） */
    Long createUser(UserCreateDTO dto);

    /** 删除用户（删除用户本身、用户-角色绑定关系） */
    void deleteUser(Long userId);

    /** 注册（默认租户1，邮箱/手机号双策略，需校验验证码） */
    Map<String, Object> register(UserAuthDTO dto);

    /** 登录（邮箱/手机号，返回 token） */
    Map<String, Object> login(UserAuthDTO dto);

    /** 发送验证码（邮箱/手机号，存 Redis） */
    void sendCode(SendCodeDTO dto);
}
