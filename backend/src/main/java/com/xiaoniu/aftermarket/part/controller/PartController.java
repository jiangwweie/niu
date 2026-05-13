package com.xiaoniu.aftermarket.part.controller;

import com.xiaoniu.aftermarket.common.api.ApiResponse;
import com.xiaoniu.aftermarket.common.context.CurrentUser;
import com.xiaoniu.aftermarket.common.context.CurrentUserContext;
import com.xiaoniu.aftermarket.common.exception.BusinessException;
import com.xiaoniu.aftermarket.common.api.ErrorCode;
import com.xiaoniu.aftermarket.common.pagination.PageResponse;
import com.xiaoniu.aftermarket.part.dto.CreatePartCommand;
import com.xiaoniu.aftermarket.part.dto.PartQueryRequest;
import com.xiaoniu.aftermarket.part.dto.PartQueryResponse;
import com.xiaoniu.aftermarket.part.dto.UpdatePartCommand;
import com.xiaoniu.aftermarket.part.entity.PartEntity;
import com.xiaoniu.aftermarket.part.service.PartService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/parts")
public class PartController {

    private final PartService partService;

    public PartController(PartService partService) {
        this.partService = partService;
    }

    @GetMapping
    public ApiResponse<PageResponse<PartQueryResponse>> listParts(
            @RequestParam(required = false) String partCode,
            @RequestParam(required = false) String partName,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(required = false) Integer pageNo,
            @RequestParam(required = false) Integer pageSize) {

        CurrentUser user = requireCurrentUser();
        PartQueryRequest request = new PartQueryRequest();
        request.setStoreId(user.storeId());
        request.setPartCode(partCode);
        request.setPartName(partName);
        request.setSource(source);
        request.setStatus(enabled != null ? (enabled ? "ENABLED" : "DISABLED") : null);
        request.setPageNo(pageNo);
        request.setPageSize(pageSize);
        return ApiResponse.success(partService.pageQuery(request));
    }

    @GetMapping("/{partId}")
    public ApiResponse<PartDetailResponse> getPart(@PathVariable Long partId) {
        requireCurrentUser();
        PartEntity entity = partService.getById(partId);
        PartQueryResponse qr = new PartQueryResponse();
        qr.setId(entity.getId());
        qr.setStoreId(entity.getStoreId());
        qr.setPartCode(entity.getPartCode());
        qr.setOfficialPartNo(entity.getOfficialPartNo());
        qr.setPartName(entity.getPartName());
        qr.setModel(entity.getModel());
        qr.setSource(entity.getSource());
        qr.setCategoryCode(entity.getCategoryCode());
        qr.setReferenceCostPrice(entity.getReferenceCostPrice());
        qr.setDefaultBarcode(entity.getDefaultBarcode());
        qr.setLocationRemark(entity.getLocationRemark());
        qr.setCreateSource(entity.getCreateSource());
        qr.setStatus(entity.getStatus());
        qr.setRemark(entity.getRemark());
        return ApiResponse.success(PartDetailResponse.fromQueryResponse(qr));
    }

    @PostMapping("/official")
    public ApiResponse<Void> createOfficialPart(@Valid @RequestBody CreateOfficialPartRequest request) {
        CurrentUser user = requireCurrentUser();
        CreatePartCommand command = buildCreateCommand(user, request.partName(), request.model(),
                request.categoryCode(), request.referenceCostPrice(), request.locationRemark(), request.remark());
        command.setOfficialPartNo(request.officialPartNo());
        partService.createOfficialPart(command);
        return ApiResponse.success(null);
    }

    @PostMapping("/third-party")
    public ApiResponse<Void> createThirdPartyPart(@Valid @RequestBody CreateThirdPartyPartRequest request) {
        CurrentUser user = requireCurrentUser();
        CreatePartCommand command = buildCreateCommand(user, request.partName(), request.model(),
                request.categoryCode(), request.referenceCostPrice(), request.locationRemark(), request.remark());
        partService.createThirdPartyPart(command);
        return ApiResponse.success(null);
    }

    @PutMapping("/{partId}")
    public ApiResponse<Void> updatePart(@PathVariable Long partId,
                                        @Valid @RequestBody UpdatePartRequest request) {
        CurrentUser user = requireCurrentUser();
        UpdatePartCommand command = new UpdatePartCommand();
        command.setPartId(partId);
        command.setStoreId(user.storeId());
        command.setOperatorId(user.userId());
        command.setPartName(request.partName());
        command.setOfficialPartNo(request.officialPartNo());
        command.setModel(request.model());
        command.setCategoryCode(request.categoryCode());
        command.setReferenceCostPrice(request.referenceCostPrice());
        command.setDefaultBarcode(request.defaultBarcode());
        command.setLocationRemark(request.locationRemark());
        command.setRemark(request.remark());
        partService.updatePart(command);
        return ApiResponse.success(null);
    }

    @PostMapping("/{partId}/enable")
    public ApiResponse<Void> enablePart(@PathVariable Long partId) {
        requireCurrentUser();
        partService.enablePart(partId);
        return ApiResponse.success(null);
    }

    @PostMapping("/{partId}/disable")
    public ApiResponse<Void> disablePart(@PathVariable Long partId) {
        requireCurrentUser();
        partService.disablePart(partId);
        return ApiResponse.success(null);
    }

    private CurrentUser requireCurrentUser() {
        return CurrentUserContext.get()
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "缺少用户上下文"));
    }

    private CreatePartCommand buildCreateCommand(CurrentUser user, String partName, String model,
                                                 String categoryCode, java.math.BigDecimal referenceCostPrice,
                                                 String locationRemark, String remark) {
        CreatePartCommand command = new CreatePartCommand();
        command.setStoreId(user.storeId());
        command.setOperatorId(user.userId());
        command.setPartName(partName);
        command.setModel(model);
        command.setCategoryCode(categoryCode);
        command.setReferenceCostPrice(referenceCostPrice);
        command.setLocationRemark(locationRemark);
        command.setRemark(remark);
        return command;
    }
}
