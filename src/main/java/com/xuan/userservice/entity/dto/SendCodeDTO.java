package com.xuan.userservice.entity.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

/**
 * 发送验证码请求
 */
@Data
public class SendCodeDTO {

    /** 方式：email / phone */
    @NotBlank(message = "验证码类型不能为空")
    private String type;

    /** 邮箱或手机号 */
    @NotBlank(message = "验证码接收地址不能为空")
    private String target;
}
