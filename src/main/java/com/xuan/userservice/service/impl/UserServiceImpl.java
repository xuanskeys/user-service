package com.xuan.userservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuan.userservice.client.RoleServiceClient;
import com.xuan.userservice.entity.dto.SendCodeDTO;
import com.xuan.userservice.entity.dto.RoleBindingRequest;
import com.xuan.userservice.entity.dto.UserAuthDTO;
import com.xuan.userservice.entity.dto.UserCreateDTO;
import com.xuan.userservice.entity.model.User;
import com.xuan.userservice.mapper.UserMapper;
import com.xuan.userservice.service.EncryptionService;
import com.xuan.userservice.service.IUserService;
import com.xuan.userservice.utils.JwtUtils;
import com.xuan.userservice.utils.MailSenderUtils;
import com.xuan.userservice.utils.RedisUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.List;

/**
 * <p>
 * 用户表 服务实现类
 * </p>
 *
 * @author xuan
 * @since 2026-09-03
 */
@Slf4j
@Service
public class UserServiceImpl extends com.baomidou.mybatisplus.extension.service.impl.ServiceImpl<UserMapper, User> implements IUserService {

    /** 默认租户ID */
    private static final Long DEFAULT_TENANT_ID = 1L;
    private static final Long INTERNAL_TENANT_ID = 0L;
    private static final Long SYSTEM_ADMIN_USER_ID = 0L;
    private static final Long DEFAULT_ROLE_ID = 1L;

    /** 验证码 Redis 前缀 */
    private static final String CODE_REDIS_PREFIX = "code:";

    /** 验证码有效期（秒） */
    @Value("${verify-code.expire-seconds:300}")
    private long codeExpireSeconds;

    private final EncryptionService encryptionService;
    private final JwtUtils jwtUtils;
    private final RedisUtils redisUtils;
    private final MailSenderUtils mailSenderUtils;
    private final RoleServiceClient roleServiceClient;

    public UserServiceImpl(EncryptionService encryptionService,
                           JwtUtils jwtUtils,
                           RedisUtils redisUtils,
                           MailSenderUtils mailSenderUtils,
                           RoleServiceClient roleServiceClient) {
        this.encryptionService = encryptionService;
        this.jwtUtils = jwtUtils;
        this.redisUtils = redisUtils;
        this.mailSenderUtils = mailSenderUtils;
        this.roleServiceClient = roleServiceClient;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createUser(UserCreateDTO dto) {
        // 邮箱/手机号至少传一个
        if (!StringUtils.hasText(dto.getEmail()) && !StringUtils.hasText(dto.getPhone())) {
            throw new RuntimeException("新增用户必须至少提供 email 或 phone 其一");
        }
        // 用户名必填
        if (!StringUtils.hasText(dto.getUsername())) {
            throw new RuntimeException("用户名不能为空");
        }

        // 唯一性校验（未删除用户）
        if (StringUtils.hasText(dto.getEmail()) && existsByEmail(dto.getEmail())) {
            throw new RuntimeException("该邮箱已存在，无法新增");
        }
        if (StringUtils.hasText(dto.getPhone()) && existsByPhone(dto.getPhone())) {
            throw new RuntimeException("该手机号已存在，无法新增");
        }

        Long tenantId = dto.getTenantId() != null ? dto.getTenantId() : DEFAULT_TENANT_ID;
        if (INTERNAL_TENANT_ID.equals(tenantId)) {
            throw new IllegalArgumentException("tenant_id=0 仅供内部系统使用");
        }
        User user = new User();
        user.setTenantId(tenantId);
        user.setUsername(dto.getUsername());
        user.setNickname(dto.getNickname());
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone());
        user.setEncryptConfigId(dto.getEncryptConfigId());

        // 密码：AES 解密 -> BCrypt 加密落库
        if (StringUtils.hasText(dto.getPassword())) {
            String encoded = encryptionService.resolveAndEncodePassword(dto.getEncryptConfigId(), dto.getPassword());
            user.setPassword(encoded);
        } else {
            throw new RuntimeException("密码不能为空");
        }

        save(user);
        List<Long> roleIds = dto.getRoleIds() == null ? List.of() : dto.getRoleIds().stream().distinct().toList();
        for (Long roleId : roleIds) {
            roleServiceClient.bindUserRole(new RoleBindingRequest(user.getId(), roleId, "创建用户时绑定"));
        }
        return user.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long userId) {
        if (SYSTEM_ADMIN_USER_ID.equals(userId)) {
            throw new IllegalArgumentException("系统最高权限管理员不允许删除");
        }
        User user = getById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        // 逻辑删除用户
        user.setIsDelete(1);
        user.setDeleteTime(java.time.LocalDateTime.now());
        updateById(user);

        // 删除用户-角色绑定（跨库，调 role-service）
        roleServiceClient.deleteUserRoleBinding(userId);
        // 用户-租户绑定关系即 user.tenant_id 字段，已随用户逻辑删除一并处理，无需额外操作
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> register(UserAuthDTO dto) {
        // 校验验证码
        verifyCode(dto.getType(), dto.getAccount(), dto.getCode());

        boolean isEmail = isEmailType(dto.getType());
        if (isEmail) {
            if (existsByEmail(dto.getAccount())) {
                throw new RuntimeException("该邮箱已注册");
            }
        } else {
            if (existsByPhone(dto.getAccount())) {
                throw new RuntimeException("该手机号已注册");
            }
        }

        User user = new User();
        user.setTenantId(DEFAULT_TENANT_ID);
        if (isEmail) {
            user.setEmail(dto.getAccount());
            user.setUsername(dto.getAccount());
        } else {
            user.setPhone(dto.getAccount());
            user.setUsername(dto.getAccount());
        }
        // 密码：AES 解密 -> BCrypt 加密
        String encoded = encryptionService.resolveAndEncodePassword(null, dto.getPassword());
        user.setPassword(encoded);

        save(user);

        // 注册用户固定绑定无租户默认角色 role_id=1；绑定失败则回滚本地用户事务。
        roleServiceClient.bindUserRole(new RoleBindingRequest(user.getId(), DEFAULT_ROLE_ID, "注册默认角色"));

        // 生成 token
        String token = generateUserToken(user);
        return buildAuthResult(user.getId(), token);
    }

    @Override
    public Map<String, Object> login(UserAuthDTO dto) {
        boolean isEmail = isEmailType(dto.getType());
        User user;
        if (isEmail) {
            user = getByEmail(dto.getAccount());
        } else {
            user = getByPhone(dto.getAccount());
        }
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        if (user.getIsDelete() != null && user.getIsDelete() == 1) {
            throw new RuntimeException("用户已被删除");
        }

        // 前端密码为 AES 密文，先解密再与库中用 BCrypt 比对
        String aesKey = encryptionService.resolveAesKey(user.getEncryptConfigId());
        String rawPassword = com.xuan.userservice.utils.AesUtils.decrypt(aesKey, dto.getPassword());
        if (!encryptionService.matches(rawPassword, user.getPassword())) {
            throw new RuntimeException("密码错误");
        }
        if (isEmail) {
            verifyCode(dto.getType(), dto.getAccount(), dto.getCode());
        }

        String token = generateUserToken(user);
        return buildAuthResult(user.getId(), token);
    }

    @Override
    public void sendCode(SendCodeDTO dto) {
        String code = generateCode();
        String type = normalizeType(dto.getType());
        String redisKey = CODE_REDIS_PREFIX + type + ":" + dto.getTarget();
        redisUtils.set(redisKey, code, codeExpireSeconds, TimeUnit.SECONDS);
        log.info("验证码已生成 -> key={}", redisKey);

        if (isEmailType(type)) {
            mailSenderUtils.sendText(dto.getTarget(), "您的验证码", "您的验证码为：" + code + "，有效期" + (codeExpireSeconds / 60) + "分钟");
        } else {
            // TODO: 短信网关未接入，先留空；后续接入短信服务发送 code 到 dto.getTarget()
            log.warn("短信网关未接入，验证码未实际发送 -> phone={}", dto.getTarget());
        }
    }

    /* ============================ 私有辅助 ============================ */

    private Map<String, Object> buildAuthResult(Long userId, String token) {
        Map<String, Object> result = new HashMap<>();
        result.put("userId", userId);
        result.put("token", token);
        return result;
    }

    private String generateUserToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("username", user.getUsername());
        claims.put("tenantId", user.getTenantId());
        return jwtUtils.generateToken(String.valueOf(user.getId()), claims);
    }

    private void verifyCode(String type, String account, String inputCode) {
        if (!StringUtils.hasText(inputCode)) {
            throw new RuntimeException("验证码不能为空");
        }
        String redisKey = CODE_REDIS_PREFIX + normalizeType(type) + ":" + account;
        String realCode = redisUtils.get(redisKey);
        if (!StringUtils.hasText(realCode)) {
            throw new RuntimeException("验证码已过期，请重新获取");
        }
        if (!realCode.equals(inputCode)) {
            throw new RuntimeException("验证码错误");
        }
        // 验证成功后删除，防止复用
        redisUtils.delete(redisKey);
    }

    private boolean isEmailType(String type) {
        return "email".equals(normalizeType(type));
    }

    private String normalizeType(String type) {
        if ("email".equalsIgnoreCase(type)) {
            return "email";
        }
        if ("phone".equalsIgnoreCase(type)) {
            return "phone";
        }
        throw new IllegalArgumentException("type 仅支持 email 或 phone");
    }

    private String generateCode() {
        SecureRandom random = new SecureRandom();
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }

    private boolean existsByEmail(String email) {
        return count(new LambdaQueryWrapper<User>().eq(User::getEmail, email).eq(User::getIsDelete, 0)) > 0;
    }

    private boolean existsByPhone(String phone) {
        return count(new LambdaQueryWrapper<User>().eq(User::getPhone, phone).eq(User::getIsDelete, 0)) > 0;
    }

    private User getByEmail(String email) {
        return getOne(new LambdaQueryWrapper<User>().eq(User::getEmail, email).eq(User::getIsDelete, 0));
    }

    private User getByPhone(String phone) {
        return getOne(new LambdaQueryWrapper<User>().eq(User::getPhone, phone).eq(User::getIsDelete, 0));
    }
}
