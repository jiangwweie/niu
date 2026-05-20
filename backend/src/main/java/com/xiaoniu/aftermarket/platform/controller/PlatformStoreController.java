package com.xiaoniu.aftermarket.platform.controller;

import com.xiaoniu.aftermarket.auth.security.AuthenticatedUser;
import com.xiaoniu.aftermarket.common.api.ApiResponse;
import com.xiaoniu.aftermarket.common.enums.AccountType;
import com.xiaoniu.aftermarket.platform.dto.PlatformStoreDtos.*;
import com.xiaoniu.aftermarket.platform.service.PlatformStoreService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/platform")
public class PlatformStoreController {

    private final PlatformStoreService platformStoreService;

    public PlatformStoreController(PlatformStoreService platformStoreService) {
        this.platformStoreService = platformStoreService;
    }

    @GetMapping("/stores")
    @PreAuthorize("hasAuthority('PLATFORM_MANAGE')")
    public ApiResponse<List<StoreListResponse>> listStores() {
        requirePlatformAccount();
        return ApiResponse.success(platformStoreService.listStores());
    }

    @PostMapping("/stores")
    @PreAuthorize("hasAuthority('PLATFORM_MANAGE')")
    public ApiResponse<StoreListResponse> createStore(@Valid @RequestBody CreateStoreRequest request) {
        AuthenticatedUser user = requirePlatformAccount();
        return ApiResponse.success(platformStoreService.createStore(user.userId(), request));
    }

    @PutMapping("/stores/{id}")
    @PreAuthorize("hasAuthority('PLATFORM_MANAGE')")
    public ApiResponse<StoreListResponse> updateStore(@PathVariable Long id,
                                                      @Valid @RequestBody UpdateStoreRequest request) {
        AuthenticatedUser user = requirePlatformAccount();
        return ApiResponse.success(platformStoreService.updateStore(user.userId(), id, request));
    }

    @PostMapping("/stores/{id}/admin-users")
    @PreAuthorize("hasAuthority('PLATFORM_MANAGE')")
    public ApiResponse<CreateStoreAdminResponse> createStoreAdmin(@PathVariable Long id,
                                                                   @Valid @RequestBody CreateStoreAdminRequest request) {
        AuthenticatedUser user = requirePlatformAccount();
        return ApiResponse.success(platformStoreService.createStoreAdmin(user.userId(), id, request));
    }

    private AuthenticatedUser requirePlatformAccount() {
        AuthenticatedUser user = (AuthenticatedUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!AccountType.PLATFORM_VALUE.equals(user.accountType())) {
            throw new AccessDeniedException("平台管理员账号才能执行此操作");
        }
        return user;
    }
}
