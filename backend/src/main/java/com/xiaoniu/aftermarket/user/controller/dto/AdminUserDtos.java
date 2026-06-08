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
            String storeName,
            String accountType,
            String username,
            String realName,
            String phone,
            Boolean enabled,
            Boolean passwordMustChange,
            Set<String> roleCodes,
            List<UserRoleResponse> roles,
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
            String storeName,
            String accountType,
            String username,
            String realName,
            String phone,
            Boolean enabled,
            Boolean passwordMustChange,
            LocalDateTime passwordChangedAt,
            LocalDateTime lastLoginAt,
            Set<String> roleCodes,
            List<UserRoleResponse> roles,
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
            String remark,
            List<Long> roleIds,
            List<String> roleCodes,
            Boolean enabled
    ) {
    }

    public record UpdateUserRequest(
            String realName,
            String phone,
            String remark,
            List<Long> roleIds,
            Boolean enabled,
            List<String> roleCodes
    ) {
    }

    public record CreateUserResponse(
            UserDetailResponse user,
            String temporaryPassword
    ) {
    }

    public record ResetPasswordResponse(String temporaryPassword) {
    }

    public record RoleResponse(
            Long roleId,
            Long storeId,
            String storeName,
            String roleCode,
            String roleName,
            String description,
            List<String> permissionCodes
    ) {
    }

    public record UserRoleResponse(
            Long roleId,
            Long storeId,
            String storeName,
            String roleCode,
            String roleName
    ) {
    }

    public record PermissionResponse(
            String permissionCode,
            String permissionName,
            String module
    ) {
    }
}
