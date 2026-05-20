package com.xiaoniu.aftermarket.user.controller;

import com.xiaoniu.aftermarket.auth.security.AuthenticatedUser;
import com.xiaoniu.aftermarket.auth.service.WechatService;
import com.xiaoniu.aftermarket.common.api.ApiResponse;
import com.xiaoniu.aftermarket.common.api.ErrorCode;
import com.xiaoniu.aftermarket.common.pagination.PageResponse;
import com.xiaoniu.aftermarket.user.controller.dto.AdminUserDtos.CreateUserRequest;
import com.xiaoniu.aftermarket.user.controller.dto.AdminUserDtos.PermissionResponse;
import com.xiaoniu.aftermarket.user.controller.dto.AdminUserDtos.ResetPasswordRequest;
import com.xiaoniu.aftermarket.user.controller.dto.AdminUserDtos.ResetPasswordResponse;
import com.xiaoniu.aftermarket.user.controller.dto.AdminUserDtos.RoleResponse;
import com.xiaoniu.aftermarket.user.controller.dto.AdminUserDtos.UpdateUserRequest;
import com.xiaoniu.aftermarket.user.controller.dto.AdminUserDtos.UserDetailResponse;
import com.xiaoniu.aftermarket.user.controller.dto.AdminUserDtos.UserSummaryResponse;
import com.xiaoniu.aftermarket.user.service.AdminUserService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminUserController {

    private final AdminUserService adminUserService;
    private final WechatService wechatService;

    public AdminUserController(AdminUserService adminUserService, WechatService wechatService) {
        this.adminUserService = adminUserService;
        this.wechatService = wechatService;
    }

    @PreAuthorize("hasAnyAuthority('USER_MANAGE', 'ROLE_MANAGE')")
    @GetMapping("/users")
    public ApiResponse<PageResponse<UserSummaryResponse>> listUsers(@RequestParam(required = false) String username,
                                                                    @RequestParam(required = false) String realName,
                                                                    @RequestParam(required = false) Boolean enabled,
                                                                    @RequestParam(required = false) String roleCode,
                                                                    @RequestParam(defaultValue = "1") int pageNo,
                                                                    @RequestParam(defaultValue = "10") int pageSize) {
        AuthenticatedUser user = requireCurrentUser();
        return ApiResponse.success(adminUserService.listUsers(user.storeId(), username, realName, enabled, roleCode, pageNo, pageSize));
    }

    @PreAuthorize("hasAnyAuthority('USER_MANAGE', 'ROLE_MANAGE')")
    @GetMapping("/users/{id}")
    public ApiResponse<UserDetailResponse> getUser(@PathVariable Long id) {
        AuthenticatedUser user = requireCurrentUser();
        return ApiResponse.success(adminUserService.getUser(user.storeId(), id));
    }

    @PreAuthorize("hasAuthority('USER_MANAGE')")
    @PostMapping("/users")
    public ApiResponse<UserDetailResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        AuthenticatedUser user = requireCurrentUser();
        return ApiResponse.success(adminUserService.createUser(user.userId(), user.storeId(), request));
    }

    @PreAuthorize("hasAuthority('USER_MANAGE')")
    @PutMapping("/users/{id}")
    public ApiResponse<UserDetailResponse> updateUser(@PathVariable Long id,
                                                      @Valid @RequestBody UpdateUserRequest request) {
        AuthenticatedUser user = requireCurrentUser();
        return ApiResponse.success(adminUserService.updateUser(user.userId(), user.storeId(), id, request));
    }

    @PreAuthorize("hasAuthority('USER_MANAGE')")
    @PostMapping("/users/{id}/enable")
    public ApiResponse<Void> enableUser(@PathVariable Long id) {
        AuthenticatedUser user = requireCurrentUser();
        adminUserService.enableUser(user.userId(), user.storeId(), id);
        return ApiResponse.success(null);
    }

    @PreAuthorize("hasAuthority('USER_MANAGE')")
    @PostMapping("/users/{id}/disable")
    public ApiResponse<Void> disableUser(@PathVariable Long id) {
        AuthenticatedUser user = requireCurrentUser();
        adminUserService.disableUser(user.userId(), user.storeId(), id);
        return ApiResponse.success(null);
    }

    @PreAuthorize("hasAuthority('USER_MANAGE')")
    @PostMapping("/users/{id}/reset-password")
    public ApiResponse<ResetPasswordResponse> resetPassword(@PathVariable Long id,
                                                            @RequestBody(required = false) ResetPasswordRequest request) {
        AuthenticatedUser user = requireCurrentUser();
        return ApiResponse.success(adminUserService.resetPassword(user.userId(), user.storeId(), id, request));
    }

    @PreAuthorize("hasAuthority('USER_MANAGE')")
    @PostMapping("/users/{id}/wechat/unbind")
    public ApiResponse<Void> unbindWechat(@PathVariable Long id) {
        AuthenticatedUser user = requireCurrentUser();
        wechatService.unbindWechat(user.storeId(), id);
        return ApiResponse.success(null);
    }

    @PreAuthorize("hasAnyAuthority('USER_MANAGE', 'ROLE_MANAGE')")
    @GetMapping("/roles")
    public ApiResponse<List<RoleResponse>> listRoles() {
        AuthenticatedUser user = requireCurrentUser();
        return ApiResponse.success(adminUserService.listRoles(user.storeId()));
    }

    @PreAuthorize("hasAnyAuthority('USER_MANAGE', 'ROLE_MANAGE')")
    @GetMapping("/permissions")
    public ApiResponse<List<PermissionResponse>> listPermissions() {
        return ApiResponse.success(adminUserService.listPermissions());
    }

    private AuthenticatedUser requireCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principal = authentication == null ? null : authentication.getPrincipal();
        if (principal instanceof AuthenticatedUser user) {
            return user;
        }
        throw new org.springframework.security.access.AccessDeniedException("Authenticated user required");
    }
}
