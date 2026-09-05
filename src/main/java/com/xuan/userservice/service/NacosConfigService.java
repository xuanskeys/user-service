package com.xuan.userservice.service;

import com.alibaba.cloud.nacos.NacosConfigManager;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.exception.NacosException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

/**
 * Nacos 配置中心访问封装
 *
 * <p>encrypt_config 表中只存配置的标识（dataId），真正的密钥/参数放在 Nacos 对应配置文件中，
 * 运行时按需拉取，避免密钥明文落库。
 */
@Slf4j
@Service
public class NacosConfigService {

    private final NacosConfigManager nacosConfigManager;
    private final String group;
    private final long timeoutMs;

    public NacosConfigService(NacosConfigManager nacosConfigManager,
                              @Value("${spring.cloud.nacos.config.group:DEFAULT_GROUP}") String group,
                              @Value("${spring.cloud.nacos.config.timeout:3000}") long timeoutMs) {
        this.nacosConfigManager = nacosConfigManager;
        this.group = group;
        this.timeoutMs = timeoutMs;
    }

    /**
     * 从 Nacos 拉取指定 dataId 的配置内容
     *
     * @param dataId 配置标识（与 encrypt_config.encrypt_file 对应）
     * @return 配置内容原文；拉取失败返回 null
     */
    public String getConfigContent(String dataId) {
        try {
            ConfigService configService = nacosConfigManager.getConfigService();
            String content = configService.getConfig(dataId, group, timeoutMs);
            log.info("从 Nacos 拉取配置成功 -> dataId={}, group={}", dataId, group);
            return content;
        } catch (NacosException e) {
            log.error("从 Nacos 拉取配置失败 -> dataId={}, group={}", dataId, group, e);
            return null;
        }
    }
}
