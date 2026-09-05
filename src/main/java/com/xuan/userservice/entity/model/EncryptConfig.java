package com.xuan.userservice.entity.model;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 加密配置表
 * nacos中配置的秘钥地址格式:
 * encrypt_config.yml:  [file]
 * service: [service]
 *   prefix: xxxxxxxxxxxxxxxxxxx [prefix]
 * </p>
 *
 * @author xuan
 * @since 2026-09-05
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("encrypt_config")
public class EncryptConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 密钥所属服务
     */
    @TableField("encrypt_service")
    private String encryptService;

    /**
     * 加密方式，例如bcrypt、argon2、aes
     */
    private String encryptType;

    /**
     * 密钥在配置文件中的前缀
     */
    @TableField("encrypt_config_prefix")
    private String encryptConfigPrefix;

    /**
     * 密钥来源文件或配置标识；不建议存放密钥明文
     */
    private String encryptFile;


}
