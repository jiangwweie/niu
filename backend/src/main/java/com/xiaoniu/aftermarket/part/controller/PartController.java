package com.xiaoniu.aftermarket.part.controller;

import com.xiaoniu.aftermarket.common.api.ApiResponse;
import com.xiaoniu.aftermarket.common.context.CurrentUser;
import com.xiaoniu.aftermarket.common.context.CurrentUserContext;
import com.xiaoniu.aftermarket.common.exception.BusinessException;
import com.xiaoniu.aftermarket.common.api.ErrorCode;
import com.xiaoniu.aftermarket.common.pagination.PageResponse;
import com.xiaoniu.aftermarket.part.dto.CreatePartCommand;
import com.xiaoniu.aftermarket.part.dto.PartDeleteCheckResponse;
import com.xiaoniu.aftermarket.part.dto.PartLookupResponse;
import com.xiaoniu.aftermarket.part.dto.PartQueryRequest;
import com.xiaoniu.aftermarket.part.dto.PartCreateResponse;
import com.xiaoniu.aftermarket.part.dto.PartQueryResponse;
import com.xiaoniu.aftermarket.part.dto.UpdatePartCommand;
import com.xiaoniu.aftermarket.part.entity.PartEntity;
import com.xiaoniu.aftermarket.part.service.PartService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
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
            @RequestParam(required = false) String officialPartNo,
            @RequestParam(required = false) String barcode,
            @RequestParam(required = false) String model,
            @RequestParam(required = false) String categoryCode,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(required = false) Integer pageNo,
            @RequestParam(required = false) Integer pageSize) {

        CurrentUser user = requireCurrentUser();
        PartQueryRequest request = new PartQueryRequest();
        request.setStoreId(user.storeId());
        request.setPartCode(partCode);
        request.setPartName(partName);
        request.setOfficialPartNo(officialPartNo);
        request.setBarcode(barcode);
        request.setModel(model);
        request.setCategoryCode(categoryCode);
        request.setSource(source);
        request.setStatus(enabled != null ? (enabled ? "ENABLED" : "DISABLED") : null);
        request.setPageNo(pageNo);
        request.setPageSize(pageSize);
        return ApiResponse.success(partService.pageQuery(request));
    }

    @PreAuthorize("hasAnyAuthority('PART_MANAGE', 'INVENTORY_VIEW')")
    @GetMapping("/lookup")
    public ApiResponse<PartLookupResponse> lookup(@RequestParam String code) {
        CurrentUser user = requireCurrentUser();
        return ApiResponse.success(partService.lookup(user.storeId(), code));
    }

    @GetMapping("/{partId}")
    public ApiResponse<PartDetailResponse> getPart(@PathVariable Long partId) {
        CurrentUser user = requireCurrentUser();
        PartEntity entity = partService.getById(partId);
        if (entity == null || (entity.getDeleted() != null && entity.getDeleted() == 1)
                || !user.storeId().equals(entity.getStoreId())) {
            throw new BusinessException(ErrorCode.PART_NOT_FOUND, "配件不存在");
        }
        entity = partService.getByPartCode(user.storeId(), entity.getPartCode());
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
        qr.setDefaultSalePrice(entity.getDefaultSalePrice());
        qr.setDefaultBarcode(entity.getDefaultBarcode());
        qr.setLocationRemark(entity.getLocationRemark());
        qr.setCreateSource(entity.getCreateSource());
        qr.setStatus(entity.getStatus());
        qr.setRemark(entity.getRemark());
        PartDeleteCheckResponse deleteCheck = partService.getDeleteCheck(user.storeId(), partId);
        qr.setCanDelete(deleteCheck.canDelete());
        qr.setDeleteReasons(deleteCheck.reasons());
        qr.setDeleteBlockReasonSummary(deleteCheck.reasons().isEmpty() ? null : String.join("；", deleteCheck.reasons()));
        qr.setActualQty(deleteCheck.stockSummary().actualQty());
        qr.setAvailableQty(deleteCheck.stockSummary().availableQty());
        qr.setReservedQty(deleteCheck.stockSummary().reservedQty());
        qr.setInventoryFlowCount(deleteCheck.referenceSummary().inventoryFlowCount());
        qr.setWorkOrderChargeItemCount(deleteCheck.referenceSummary().workOrderChargeItemCount());
        qr.setArchived(entity.getDeleted() == null || entity.getDeleted() == 0
                ? "DISABLED".equals(entity.getStatus())
                && deleteCheck.stockSummary().actualQty() == 0
                && deleteCheck.stockSummary().availableQty() == 0
                && deleteCheck.stockSummary().reservedQty() == 0
                && (deleteCheck.referenceSummary().inventoryFlowCount() > 0
                || deleteCheck.referenceSummary().workOrderChargeItemCount() > 0)
                : false);
        qr.setHasHistoryReference(deleteCheck.referenceSummary().inventoryFlowCount() > 0
                || deleteCheck.referenceSummary().workOrderChargeItemCount() > 0);
        return ApiResponse.success(PartDetailResponse.fromQueryResponse(qr));
    }

    @PreAuthorize("hasAuthority('PART_MANAGE')")
    @GetMapping("/{partId}/delete-check")
    public ApiResponse<PartDeleteCheckResponse> getDeleteCheck(@PathVariable Long partId) {
        CurrentUser user = requireCurrentUser();
        PartEntity entity = partService.getById(partId);
        if (entity == null || (entity.getDeleted() != null && entity.getDeleted() == 1)
                || !user.storeId().equals(entity.getStoreId())) {
            throw new BusinessException(ErrorCode.PART_NOT_FOUND, "配件不存在");
        }
        return ApiResponse.success(partService.getDeleteCheck(user.storeId(), partId));
    }

    @PreAuthorize("hasAuthority('PART_MANAGE')")
    @PostMapping("/official")
    public ApiResponse<PartCreateResponse> createOfficialPart(@Valid @RequestBody CreateOfficialPartRequest request) {
        CurrentUser user = requireCurrentUser();
        CreatePartCommand command = buildCreateCommand(user, request.partName(), request.model(),
                request.categoryCode(), request.referenceCostPrice(), request.defaultSalePrice(),
                request.defaultBarcode(), request.externalBarcode(),
                request.locationRemark(), request.remark());
        command.setOfficialPartNo(request.officialPartNo());
        PartEntity part = partService.createOfficialPart(command);
        return ApiResponse.success(PartCreateResponse.from(part));
    }

    @PreAuthorize("hasAuthority('PART_MANAGE')")
    @PostMapping("/third-party")
    public ApiResponse<PartCreateResponse> createThirdPartyPart(@Valid @RequestBody CreateThirdPartyPartRequest request) {
        CurrentUser user = requireCurrentUser();
        CreatePartCommand command = buildCreateCommand(user, request.partName(), request.model(),
                request.categoryCode(), request.referenceCostPrice(), request.defaultSalePrice(),
                request.defaultBarcode(), request.externalBarcode(),
                request.locationRemark(), request.remark());
        command.setOfficialPartNo(request.officialPartNo());
        PartEntity part = partService.createThirdPartyPart(command);
        return ApiResponse.success(PartCreateResponse.from(part));
    }

    @PreAuthorize("hasAuthority('PART_MANAGE')")
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
        command.setDefaultSalePrice(request.defaultSalePrice());
        command.setDefaultBarcode(request.defaultBarcode());
        command.setExternalBarcode(request.externalBarcode());
        command.setLocationRemark(request.locationRemark());
        command.setRemark(request.remark());
        partService.updatePart(command);
        return ApiResponse.success(null);
    }

    @PreAuthorize("hasAuthority('PART_MANAGE')")
    @PostMapping("/{partId}/enable")
    public ApiResponse<Void> enablePart(@PathVariable Long partId) {
        CurrentUser user = requireCurrentUser();
        partService.enablePart(user.storeId(), partId);
        return ApiResponse.success(null);
    }

    @PreAuthorize("hasAuthority('PART_MANAGE')")
    @PostMapping("/{partId}/disable")
    public ApiResponse<Void> disablePart(@PathVariable Long partId) {
        CurrentUser user = requireCurrentUser();
        partService.disablePart(user.storeId(), partId);
        return ApiResponse.success(null);
    }

    @PreAuthorize("hasAuthority('PART_MANAGE')")
    @DeleteMapping("/{partId}")
    public ApiResponse<Void> deletePart(@PathVariable Long partId) {
        CurrentUser user = requireCurrentUser();
        partService.deletePart(user.storeId(), partId, user.userId());
        return ApiResponse.success(null);
    }

    private CurrentUser requireCurrentUser() {
        return CurrentUserContext.get()
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "缺少用户上下文"));
    }

    private CreatePartCommand buildCreateCommand(CurrentUser user, String partName, String model,
                                                 String categoryCode, java.math.BigDecimal referenceCostPrice,
                                                 java.math.BigDecimal defaultSalePrice, String defaultBarcode,
                                                 String externalBarcode,
                                                 String locationRemark, String remark) {
        CreatePartCommand command = new CreatePartCommand();
        command.setStoreId(user.storeId());
        command.setOperatorId(user.userId());
        command.setPartName(partName);
        command.setModel(model);
        command.setCategoryCode(categoryCode);
        command.setReferenceCostPrice(referenceCostPrice);
        command.setDefaultSalePrice(defaultSalePrice);
        command.setDefaultBarcode(defaultBarcode);
        command.setExternalBarcode(externalBarcode);
        command.setLocationRemark(locationRemark);
        command.setRemark(remark);
        return command;
    }
}
