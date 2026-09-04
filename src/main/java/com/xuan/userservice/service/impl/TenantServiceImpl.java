package com.xuan.userservice.service.impl;

import com.xuan.userservice.entity.model.Tenant;
import com.xuan.userservice.mapper.TenantMapper;
import com.xuan.userservice.service.ITenantService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 租户表 服务实现类
 * </p>
 *
 * @author xuan
 * @since 2026-09-03
 */
@Service
public class TenantServiceImpl extends ServiceImpl<TenantMapper, Tenant> implements ITenantService {

}
