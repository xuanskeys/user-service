package com.xuan.userservice.controller;

import com.xuan.userservice.entity.dto.SendCodeDTO;
import com.xuan.userservice.entity.dto.UserAuthDTO;
import com.xuan.userservice.entity.dto.UserCreateDTO;
import com.xuan.userservice.entity.result.Result;
import com.xuan.userservice.service.IUserService;
import com.xuan.logging.OperationLog;
import com.xuan.logging.OperationType;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 用户表 前端控制器
 * </p>
 *
 * @author xuan
 * @since 2026-09-03
 */
@RestController
@RequestMapping("/user")
public class UserController {

    private final IUserService userService;

    public UserController(IUserService userService) {
        this.userService = userService;
    }

    /**
     * 新增用户（绑定租户、可选角色，密码 AES 解密后 BCrypt 落库）
     */
    @PostMapping("/create")
    @OperationLog(type = OperationType.CREATE, description = "创建用户")
    public Result<Long> createUser(@Valid @RequestBody UserCreateDTO dto) {
        return Result.success(userService.createUser(dto));
    }

    /**
     * 删除用户（删除用户本身、用户-角色绑定关系）
     */
    @DeleteMapping("/delete")
    @OperationLog(type = OperationType.DELETE, description = "删除用户")
    public Result<Void> deleteUser(@RequestParam("userId") Long userId) {
        userService.deleteUser(userId);
        return Result.success();
    }

    /**
     * 注册（默认租户1，邮箱/手机号双策略，需校验验证码，返回 token）
     */
    @PostMapping("/register")
    @OperationLog(type = OperationType.CREATE, description = "用户注册")
    public Result<Object> register(@Valid @RequestBody UserAuthDTO dto) {
        return Result.success(userService.register(dto));
    }

    /**
     * 登录（邮箱登录需验证码，返回 token）
     */
    @PostMapping("/login")
    @OperationLog(type = OperationType.READ, description = "用户登录")
    public Result<Object> login(@Valid @RequestBody UserAuthDTO dto) {
        return Result.success(userService.login(dto));
    }

    /**
     * 发送验证码（邮箱或手机号，存 Redis）
     */
    @PostMapping("/send-code")
    @OperationLog(type = OperationType.CREATE, description = "发送登录验证码")
    public Result<Void> sendCode(@Valid @RequestBody SendCodeDTO dto) {
        userService.sendCode(dto);
        return Result.success();
    }
}
