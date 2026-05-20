package com.xiaoniu.aftermarket.user.controller.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public final class AdminUserDtos {

    private AdminUserDtos() {
    }

    public record UserSummaryResponse(
            Long id,
            Long storeId,
            String username,
            String realName,
            String phone,
            Boolean enabled,
            Boolean passwordMustChange,
            Set<String> roleCodes,
            Boolean wechatBound,
            LocalDateTime wechatBoundAt,
            LocalDateTime lastLoginAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }

    public record UserDetailResponse(
            Long id,
            Long storeId,
            String username,
            String realName,
            String phone,
            Boolean enabled,
            Boolean passwordMustChange,
            LocalDateTime passwordChangedAt,
            LocalDateTime lastLoginAt,
            Set<String> roleCodes,
            Set<String> permissionCodes,
            Boolean wechatBound,
            LocalDateTime wechatBoundAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }

    public record CreateUserRequest(
            @NotBlank String username,
            @NotBlank String realName,
            String phone,
            Long storeId,
            List<String> roleCodes,
            String initialPassword,
            Boolean enabled
    ) {
    }

    public record UpdateUserRequest(
            String realName,
            String phone,
            Boolean enabled,
            List<String> roleCodes
    ) {
    }

    public record ResetPasswordRequest(String temporaryPassword) {
    }

    public record ResetPasswordResponse(String temporaryPassword) {
    }

    public record RoleResponse(
            String roleCode,
            String roleName,
            String description,
            List<String> permissionCodes
    ) {
    }

    public record PermissionResponse(
            String permissionCode,
            String permissionName,
            String module
    ) {
    }
}
