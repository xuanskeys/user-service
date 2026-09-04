package com.xuan.userservice.service.impl;

import com.xuan.userservice.entity.model.TenantType;
import com.xuan.userservice.mapper.TenantTypeMapper;
import com.xuan.userservice.service.ITenantTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 租户类型表 服务实现类
 * </p>
 *
 * @author xuan
 * @since 2026-09-03
 */
@Service
public class TenantTypeServiceImpl extends ServiceImpl<TenantTypeMapper, TenantType> implements ITenantTypeService {

}
