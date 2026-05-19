package com.xiaoniu.aftermarket.store.controller;

import com.xiaoniu.aftermarket.auth.security.AuthenticatedUser;
import com.xiaoniu.aftermarket.common.api.ApiResponse;
import com.xiaoniu.aftermarket.store.controller.dto.StoreDtos.StoreResponse;
import com.xiaoniu.aftermarket.store.controller.dto.StoreDtos.UpdateStoreRequest;
import com.xiaoniu.aftermarket.store.service.StoreConfigService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/store")
public class AdminStoreController {

    private final StoreConfigService storeConfigService;

    public AdminStoreController(StoreConfigService storeConfigService) {
        this.storeConfigService = storeConfigService;
    }

    @GetMapping("/current")
    public ApiResponse<StoreResponse> getCurrentStore() {
        AuthenticatedUser user = requireCurrentUser();
        return ApiResponse.success(storeConfigService.getCurrentStore(user.storeId()));
    }

    @PreAuthorize("hasAuthority('STORE_MANAGE')")
    @PutMapping("/current")
    public ApiResponse<StoreResponse> updateCurrentStore(@Valid @RequestBody UpdateStoreRequest request) {
        AuthenticatedUser user = requireCurrentUser();
        return ApiResponse.success(storeConfigService.updateCurrentStore(user.storeId(), user.userId(), request));
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
