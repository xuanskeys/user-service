package com.xuan.userservice.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuan.userservice.entity.model.EncryptConfig;
import com.xuan.userservice.mapper.EncryptConfigMapper;
import com.xuan.userservice.utils.AesUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 密码加密处理服务
 *
 * <p>处理流程：前端用 AES 加密密码传输 -> 后端根据 encrypt_config 配置从 Nacos 拉取 AES 密钥 -> AES 解密 ->
 * 再用 BCrypt 加密后落库。密钥不落库，只存 Nacos 配置标识于 encrypt_config.encrypt_file。
 */
@Slf4j
@Service
public class EncryptionService {

    private final EncryptConfigMapper encryptConfigMapper;
    private final NacosConfigService nacosConfigService;
    private final PasswordEncoder passwordEncoder;
    private final String serviceName;

    public EncryptionService(EncryptConfigMapper encryptConfigMapper,
                             NacosConfigService nacosConfigService,
                             PasswordEncoder passwordEncoder,
                             @Value("${spring.application.name}") String serviceName) {
        this.encryptConfigMapper = encryptConfigMapper;
        this.nacosConfigService = nacosConfigService;
        this.passwordEncoder = passwordEncoder;
        this.serviceName = serviceName;
    }

    /**
     * 处理前端传入的密文密码：AES 解密后 BCrypt 加密，返回可落库的哈希值。
     *
     * @param encryptConfigId 加密配置ID（关联 encrypt_config 表），可为 null（使用默认配置）
     * @param cipherPassword  前端 AES 加密后的密文
     * @return BCrypt 哈希
     */
    public String resolveAndEncodePassword(Long encryptConfigId, String cipherPassword) {
        String aesKey = resolveAesKey(encryptConfigId);
        String rawPassword = AesUtils.decrypt(aesKey, cipherPassword);
        return passwordEncoder.encode(rawPassword);
    }

    /**
     * 校验明文（解密后的）密码与库中 BCrypt 哈希是否匹配
     */
    public boolean matches(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    /**
     * 根据加密配置ID，从 Nacos 拉取对应的 AES 密钥（public，供登录解密复用）
     */
    public String resolveAesKey(Long encryptConfigId) {
        EncryptConfig config = encryptConfigId == null
                ? encryptConfigMapper.selectOne(new LambdaQueryWrapper<EncryptConfig>()
                .eq(EncryptConfig::getEncryptService, serviceName)
                .last("LIMIT 1"))
                : encryptConfigMapper.selectById(encryptConfigId);
        if (config == null) {
            throw new IllegalStateException("未找到服务 " + serviceName + " 的加密配置");
        }
        if (!serviceName.equals(config.getEncryptService())) {
            throw new IllegalArgumentException("加密配置不属于当前服务: " + serviceName);
        }
        if (!"AES".equalsIgnoreCase(config.getEncryptType())) {
            throw new IllegalStateException("当前服务的传输加密配置不是 AES");
        }
        if (!StringUtils.hasText(config.getEncryptFile()) || !StringUtils.hasText(config.getEncryptConfigPrefix())) {
            throw new IllegalStateException("加密配置缺少 encrypt_file 或 encrypt_config_prefix");
        }

        String dataId = config.getEncryptFile();

        String content = nacosConfigService.getConfigContent(dataId);
        if (!StringUtils.hasText(content)) {
            throw new RuntimeException("无法从 Nacos 拉取加密配置: " + dataId);
        }

        String aesKey = extractValue(content, config.getEncryptConfigPrefix());
        if (!StringUtils.hasText(aesKey)) {
            throw new IllegalStateException("Nacos 配置 " + dataId + " 中缺失前缀 " + config.getEncryptConfigPrefix());
        }
        return aesKey;
    }

    static String extractValue(String content, String prefix) {
        Pattern pattern = Pattern.compile("(?m)^\\s*" + Pattern.quote(prefix)
                + "\\s*(?:=|:)\\s*([^#\\r\\n]+?)\\s*$");
        Matcher matcher = pattern.matcher(content);
        if (!matcher.find()) {
            return null;
        }
        String value = matcher.group(1).trim();
        if (value.length() >= 2 && ((value.startsWith("\"") && value.endsWith("\""))
                || (value.startsWith("'") && value.endsWith("'")))) {
            value = value.substring(1, value.length() - 1);
        }
        return value;
    }
}
