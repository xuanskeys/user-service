package com.xuan.userservice.client;

import com.xuan.userservice.entity.dto.RoleBindingRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 调用 role-service 处理跨库的用户-角色绑定关系
 *
 * <p>user_role 表位于 role_service 库，删除用户时需同步清理其角色绑定。
 */
@FeignClient(name = "role-service")
public interface RoleServiceClient {

    @PostMapping("/user-role/bind")
    void bindUserRole(@RequestBody RoleBindingRequest request);

    /**
     * 逻辑删除某用户的所有角色绑定
     *
     * @param userId 用户ID
     */
    @DeleteMapping("/user-role/user/{userId}")
    void deleteUserRoleBinding(@PathVariable("userId") Long userId);
}
