package com.xuan.userservice.service.impl;

import com.xuan.userservice.entity.model.EncryptConfig;
import com.xuan.userservice.mapper.EncryptConfigMapper;
import com.xuan.userservice.service.IEncryptConfigService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 加密配置表 服务实现类
 * </p>
 *
 * @author xuan
 * @since 2026-09-05
 */
@Service
public class EncryptConfigServiceImpl extends ServiceImpl<EncryptConfigMapper, EncryptConfig> implements IEncryptConfigService {

}
