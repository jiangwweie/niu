package com.xiaoniu.aftermarket.user.service;

import com.xiaoniu.aftermarket.common.pagination.PageResponse;
import com.xiaoniu.aftermarket.user.controller.dto.AdminUserDtos.CreateUserRequest;
import com.xiaoniu.aftermarket.user.controller.dto.AdminUserDtos.PermissionResponse;
import com.xiaoniu.aftermarket.user.controller.dto.AdminUserDtos.ResetPasswordRequest;
import com.xiaoniu.aftermarket.user.controller.dto.AdminUserDtos.ResetPasswordResponse;
import com.xiaoniu.aftermarket.user.controller.dto.AdminUserDtos.RoleResponse;
import com.xiaoniu.aftermarket.user.controller.dto.AdminUserDtos.UpdateUserRequest;
import com.xiaoniu.aftermarket.user.controller.dto.AdminUserDtos.UserDetailResponse;
import com.xiaoniu.aftermarket.user.controller.dto.AdminUserDtos.UserSummaryResponse;
import java.util.List;

public interface AdminUserService {

    PageResponse<UserSummaryResponse> listUsers(Long storeId, String username, String realName,
                                                String phone, Boolean enabled, String roleCode, int pageNo, int pageSize);

    UserDetailResponse getUser(Long storeId, Long id);

    UserDetailResponse createUser(Long currentUserId, Long currentStoreId, CreateUserRequest request);

    UserDetailResponse updateUser(Long currentUserId, Long currentStoreId, Long id, UpdateUserRequest request);

    void enableUser(Long currentUserId, Long currentStoreId, Long id);

    void disableUser(Long currentUserId, Long currentStoreId, Long id);

    ResetPasswordResponse resetPassword(Long currentUserId, Long currentStoreId, Long id, ResetPasswordRequest request);

    List<RoleResponse> listRoles(Long storeId, Long currentUserId);

    List<PermissionResponse> listPermissions();
}
