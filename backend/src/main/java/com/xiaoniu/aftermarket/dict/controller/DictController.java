package com.xiaoniu.aftermarket.dict.controller;

import com.xiaoniu.aftermarket.common.api.ApiResponse;
import com.xiaoniu.aftermarket.common.enums.CommonStatus;
import com.xiaoniu.aftermarket.dict.entity.SysDictItemEntity;
import com.xiaoniu.aftermarket.dict.entity.SysDictTypeEntity;
import com.xiaoniu.aftermarket.dict.service.DictService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
    public ApiResponse<List<DictTypeResponse>> listTypes() {
        List<DictTypeResponse> responses = dictService.listEnabledTypes().stream()
                .map(this::toTypeResponse)
                .toList();
        return ApiResponse.success(responses);
    }

    @GetMapping("/types/{typeCode}/items")
    public ApiResponse<List<DictItemResponse>> listItems(@PathVariable String typeCode) {
        List<DictItemResponse> responses = dictService.listItemsByTypeCode(typeCode).stream()
                .map(this::toResponse)
                .toList();
        return ApiResponse.success(responses);
    }

    private DictItemResponse toResponse(SysDictItemEntity entity) {
        return new DictItemResponse(
                entity.getItemCode(),
                entity.getItemName(),
                entity.getSortOrder(),
                CommonStatus.ENABLED.getCode().equals(entity.getStatus())
        );
    }

    private DictTypeResponse toTypeResponse(SysDictTypeEntity entity) {
        return new DictTypeResponse(
                entity.getTypeCode(),
                entity.getTypeName(),
                CommonStatus.ENABLED.getCode().equals(entity.getStatus())
        );
    }
}
