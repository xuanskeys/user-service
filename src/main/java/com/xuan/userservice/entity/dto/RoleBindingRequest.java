package com.xuan.userservice.entity.dto;

public record RoleBindingRequest(Long userId, Long roleId, String description) {
}
