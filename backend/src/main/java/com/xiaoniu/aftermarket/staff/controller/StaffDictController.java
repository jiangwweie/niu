package com.xiaoniu.aftermarket.staff.controller;

import com.xiaoniu.aftermarket.common.api.ApiResponse;
import com.xiaoniu.aftermarket.common.api.ErrorCode;
import com.xiaoniu.aftermarket.common.context.CurrentUser;
import com.xiaoniu.aftermarket.common.context.CurrentUserContext;
import com.xiaoniu.aftermarket.common.exception.BusinessException;
import com.xiaoniu.aftermarket.common.enums.CommonStatus;
import com.xiaoniu.aftermarket.dict.controller.DictItemResponse;
import com.xiaoniu.aftermarket.dict.entity.SysDictItemEntity;
import com.xiaoniu.aftermarket.dict.service.DictService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/staff/dict")
public class StaffDictController {

    private final DictService dictService;

    public StaffDictController(DictService dictService) {
        this.dictService = dictService;
    }

    @GetMapping("/types/{typeCode}/items")
    public ApiResponse<List<DictItemResponse>> listItems(@PathVariable String typeCode) {
        CurrentUser user = requireCurrentUser();
        List<DictItemResponse> responses = dictService.listItemsByTypeCode(typeCode, user.storeId()).stream()
                .map(this::toResponse)
                .toList();
        return ApiResponse.success(responses);
    }

    private DictItemResponse toResponse(SysDictItemEntity entity) {
        return new DictItemResponse(
                entity.getId(),
                entity.getItemCode(),
                entity.getItemName(),
                entity.getSortOrder(),
                CommonStatus.ENABLED.getCode().equals(entity.getStatus()),
                entity.getScope(),
                entity.getStoreId(),
                !"STORE".equals(entity.getScope()),
                false
        );
    }

    private CurrentUser requireCurrentUser() {
        return CurrentUserContext.get()
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "缺少用户上下文"));
    }
}
