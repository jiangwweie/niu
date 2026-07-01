package com.xiaoniu.aftermarket.dict.controller;

import com.xiaoniu.aftermarket.common.api.ApiResponse;
import com.xiaoniu.aftermarket.common.enums.CommonStatus;
import com.xiaoniu.aftermarket.common.context.CurrentUser;
import com.xiaoniu.aftermarket.common.context.CurrentUserContext;
import com.xiaoniu.aftermarket.common.enums.AccountType;
import com.xiaoniu.aftermarket.common.api.ErrorCode;
import com.xiaoniu.aftermarket.common.exception.BusinessException;
import com.xiaoniu.aftermarket.dict.dto.CreateDictItemRequest;
import com.xiaoniu.aftermarket.dict.dto.UpdateDictItemRequest;
import com.xiaoniu.aftermarket.dict.entity.SysDictItemEntity;
import com.xiaoniu.aftermarket.dict.entity.SysDictTypeEntity;
import com.xiaoniu.aftermarket.dict.service.DictService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/dict")
public class DictController {

    private final DictService dictService;

    public DictController(DictService dictService) {
        this.dictService = dictService;
    }

    @GetMapping("/types")
    @PreAuthorize("hasAnyAuthority('DICT_MANAGE', 'PLATFORM_MANAGE')")
    public ApiResponse<List<DictTypeResponse>> listTypes() {
        List<DictTypeResponse> responses = dictService.listEnabledTypes().stream()
                .map(this::toTypeResponse)
                .toList();
        return ApiResponse.success(responses);
    }

    @GetMapping("/types/{typeCode}/items")
    @PreAuthorize("hasAnyAuthority('DICT_MANAGE', 'PLATFORM_MANAGE')")
    public ApiResponse<List<DictItemResponse>> listItems(@PathVariable String typeCode) {
        CurrentUser user = requireCurrentUser();
        Long storeId = AccountType.STORE_VALUE.equals(user.accountType()) ? user.storeId() : null;
        boolean canManageSystemItem = canManageSystemItem();
        List<DictItemResponse> responses = dictService.listItemsByTypeCode(typeCode, storeId).stream()
                .map(entity -> toResponse(entity, storeId, canManageSystemItem))
                .toList();
        return ApiResponse.success(responses);
    }

    @PostMapping("/types/{typeCode}/items")
    @PreAuthorize("hasAnyAuthority('DICT_MANAGE', 'PLATFORM_MANAGE')")
    public ApiResponse<DictItemResponse> createItem(@PathVariable String typeCode,
                                                    @Valid @RequestBody CreateDictItemRequest request) {
        CurrentUser user = requireCurrentUser();
        Long storeId = AccountType.STORE_VALUE.equals(user.accountType()) ? user.storeId() : null;
        boolean canManageSystemItem = canManageSystemItem();
        SysDictItemEntity entity = dictService.createItem(typeCode, storeId, user.userId(), canManageSystemItem, request);
        return ApiResponse.success(toResponse(entity, storeId, canManageSystemItem));
    }

    @PutMapping("/items/{itemId}")
    @PreAuthorize("hasAnyAuthority('DICT_MANAGE', 'PLATFORM_MANAGE')")
    public ApiResponse<DictItemResponse> updateItem(@PathVariable Long itemId,
                                                    @Valid @RequestBody UpdateDictItemRequest request) {
        CurrentUser user = requireCurrentUser();
        Long storeId = AccountType.STORE_VALUE.equals(user.accountType()) ? user.storeId() : null;
        boolean canManageSystemItem = canManageSystemItem();
        SysDictItemEntity entity = dictService.updateItem(itemId, storeId, user.userId(), canManageSystemItem, request);
        return ApiResponse.success(toResponse(entity, storeId, canManageSystemItem));
    }

    @DeleteMapping("/items/{itemId}")
    @PreAuthorize("hasAnyAuthority('DICT_MANAGE', 'PLATFORM_MANAGE')")
    public ApiResponse<Void> deleteItem(@PathVariable Long itemId) {
        CurrentUser user = requireCurrentUser();
        Long storeId = AccountType.STORE_VALUE.equals(user.accountType()) ? user.storeId() : null;
        dictService.deleteItem(itemId, storeId, user.userId(), canManageSystemItem());
        return ApiResponse.success(null);
    }

    private DictItemResponse toResponse(SysDictItemEntity entity, Long storeId, boolean canManageSystemItem) {
        boolean systemItem = !"STORE".equals(entity.getScope());
        boolean editable = systemItem
                ? canManageSystemItem
                : entity.getStoreId() != null && entity.getStoreId().equals(storeId);
        return new DictItemResponse(
                entity.getId(),
                entity.getItemCode(),
                entity.getItemName(),
                entity.getSortOrder(),
                CommonStatus.ENABLED.getCode().equals(entity.getStatus()),
                entity.getScope(),
                entity.getStoreId(),
                systemItem,
                editable
        );
    }

    private DictTypeResponse toTypeResponse(SysDictTypeEntity entity) {
        return new DictTypeResponse(
                entity.getTypeCode(),
                entity.getTypeName(),
                CommonStatus.ENABLED.getCode().equals(entity.getStatus()),
                entity.getEditMode()
        );
    }

    private boolean canManageSystemItem() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream().anyMatch(authority ->
                "ROLE_SUPER_ADMIN".equals(authority.getAuthority())
                        || "PLATFORM_MANAGE".equals(authority.getAuthority()));
    }

    private CurrentUser requireCurrentUser() {
        return CurrentUserContext.get()
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "缺少用户上下文"));
    }
}
