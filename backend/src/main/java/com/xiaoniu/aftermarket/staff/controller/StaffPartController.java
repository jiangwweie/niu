package com.xiaoniu.aftermarket.staff.controller;

import com.xiaoniu.aftermarket.common.api.ApiResponse;
import com.xiaoniu.aftermarket.common.context.CurrentUser;
import com.xiaoniu.aftermarket.common.context.CurrentUserContext;
import com.xiaoniu.aftermarket.common.exception.BusinessException;
import com.xiaoniu.aftermarket.common.api.ErrorCode;
import com.xiaoniu.aftermarket.common.pagination.PageResponse;
import com.xiaoniu.aftermarket.part.dto.PartQueryRequest;
import com.xiaoniu.aftermarket.part.dto.PartQueryResponse;
import com.xiaoniu.aftermarket.part.entity.PartEntity;
import com.xiaoniu.aftermarket.part.service.PartService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/staff/parts")
public class StaffPartController {

    private final PartService partService;

    public StaffPartController(PartService partService) {
        this.partService = partService;
    }

    @GetMapping
    public ApiResponse<PageResponse<StaffPartListItem>> listParts(
            @RequestParam(required = false) String partCode,
            @RequestParam(required = false) String partName,
            @RequestParam(required = false) String officialPartNo,
            @RequestParam(required = false) String model,
            @RequestParam(required = false) String categoryCode,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) Integer pageNo,
            @RequestParam(required = false) Integer pageSize) {

        CurrentUser user = requireCurrentUser();
        PartQueryRequest request = new PartQueryRequest();
        request.setStoreId(user.storeId());
        request.setPartCode(partCode);
        request.setPartName(partName);
        request.setOfficialPartNo(officialPartNo);
        request.setModel(model);
        request.setCategoryCode(categoryCode);
        request.setSource(source);
        request.setStatus("ENABLED");
        request.setPageNo(pageNo);
        request.setPageSize(pageSize);
        PageResponse<PartQueryResponse> result = partService.pageQuery(request);
        return ApiResponse.success(result.map(StaffPartListItem::from));
    }

    @GetMapping("/{partId}")
    public ApiResponse<StaffPartDetail> getPart(@PathVariable Long partId) {
        CurrentUser user = requireCurrentUser();
        PartEntity entity = partService.getById(partId);
        if (entity == null || !user.storeId().equals(entity.getStoreId())) {
            throw new BusinessException(ErrorCode.PART_NOT_FOUND, "配件不存在");
        }
        return ApiResponse.success(StaffPartDetail.from(entity));
    }

    private CurrentUser requireCurrentUser() {
        return CurrentUserContext.get()
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "缺少用户上下文"));
    }
}
