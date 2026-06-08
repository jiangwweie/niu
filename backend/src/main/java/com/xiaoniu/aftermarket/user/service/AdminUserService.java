package com.xiaoniu.aftermarket.user.service;

import com.xiaoniu.aftermarket.auth.security.AuthenticatedUser;
import com.xiaoniu.aftermarket.common.pagination.PageResponse;
import com.xiaoniu.aftermarket.user.controller.dto.AdminUserDtos.CreateUserResponse;
import com.xiaoniu.aftermarket.user.controller.dto.AdminUserDtos.CreateUserRequest;
import com.xiaoniu.aftermarket.user.controller.dto.AdminUserDtos.PermissionResponse;
import com.xiaoniu.aftermarket.user.controller.dto.AdminUserDtos.ResetPasswordResponse;
import com.xiaoniu.aftermarket.user.controller.dto.AdminUserDtos.RoleResponse;
import com.xiaoniu.aftermarket.user.controller.dto.AdminUserDtos.UpdateUserRequest;
import com.xiaoniu.aftermarket.user.controller.dto.AdminUserDtos.UserDetailResponse;
import com.xiaoniu.aftermarket.user.controller.dto.AdminUserDtos.UserSummaryResponse;
import java.util.List;

public interface AdminUserService {

    PageResponse<UserSummaryResponse> listUsers(AuthenticatedUser currentUser, Long storeId, String username,
                                                String realName, String phone, Boolean enabled, String roleCode,
                                                int pageNo, int pageSize);

    UserDetailResponse getUser(AuthenticatedUser currentUser, Long id);

    CreateUserResponse createUser(AuthenticatedUser currentUser, CreateUserRequest request);

    UserDetailResponse updateUser(AuthenticatedUser currentUser, Long id, UpdateUserRequest request);

    void enableUser(AuthenticatedUser currentUser, Long id);

    void disableUser(AuthenticatedUser currentUser, Long id);

    ResetPasswordResponse resetPassword(AuthenticatedUser currentUser, Long id);

    void unbindWechat(AuthenticatedUser currentUser, Long id);

    List<RoleResponse> listRoles(AuthenticatedUser currentUser);

    List<PermissionResponse> listPermissions();
}
