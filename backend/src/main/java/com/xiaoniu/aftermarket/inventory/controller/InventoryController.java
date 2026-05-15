package com.xiaoniu.aftermarket.inventory.controller;

import com.xiaoniu.aftermarket.common.api.ApiResponse;
import com.xiaoniu.aftermarket.common.context.CurrentUser;
import com.xiaoniu.aftermarket.common.context.CurrentUserContext;
import com.xiaoniu.aftermarket.common.exception.BusinessException;
import com.xiaoniu.aftermarket.common.api.ErrorCode;
import com.xiaoniu.aftermarket.common.pagination.PageResponse;
import com.xiaoniu.aftermarket.inventory.dto.InventoryAdjustCommand;
import com.xiaoniu.aftermarket.inventory.dto.InventoryFlowQueryRequest;
import com.xiaoniu.aftermarket.inventory.dto.InventoryFlowQueryResponse;
import com.xiaoniu.aftermarket.inventory.dto.InventoryInboundCommand;
import com.xiaoniu.aftermarket.inventory.dto.InventoryStockQueryResponse;
import com.xiaoniu.aftermarket.inventory.entity.InventoryStockEntity;
import com.xiaoniu.aftermarket.inventory.service.InventoryService;
import com.xiaoniu.aftermarket.part.entity.PartEntity;
import com.xiaoniu.aftermarket.part.mapper.PartMapper;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/inventory")
public class InventoryController {

    private final InventoryService inventoryService;
    private final PartMapper partMapper;

    public InventoryController(InventoryService inventoryService, PartMapper partMapper) {
        this.inventoryService = inventoryService;
        this.partMapper = partMapper;
    }

    @GetMapping("/stocks")
    public ApiResponse<PageResponse<InventoryStockQueryResponse>> listStocks(
            @RequestParam(required = false) String partCode,
            @RequestParam(required = false) String partName,
            @RequestParam(required = false) Integer pageNo,
            @RequestParam(required = false) Integer pageSize) {

        CurrentUser user = requireCurrentUser();
        PageResponse<InventoryStockQueryResponse> result =
                inventoryService.pageQuery(user.storeId(), partCode, partName, pageNo, pageSize);
        return ApiResponse.success(result);
    }

    @GetMapping("/stocks/{partId}")
    public ApiResponse<InventoryStockDetailResponse> getStockByPartId(@PathVariable Long partId) {
        CurrentUser user = requireCurrentUser();
        InventoryStockEntity entity = inventoryService.getByPartId(user.storeId(), partId);
        PartEntity part = partMapper.selectById(entity.getPartId());
        InventoryStockQueryResponse qr = new InventoryStockQueryResponse();
        qr.setId(entity.getId());
        qr.setStoreId(entity.getStoreId());
        qr.setPartId(entity.getPartId());
        qr.setPartCode(part != null ? part.getPartCode() : null);
        qr.setPartName(part != null ? part.getPartName() : null);
        qr.setPartSource(part != null ? part.getSource() : null);
        qr.setActualQty(entity.getActualQty());
        qr.setAvailableQty(entity.getAvailableQty());
        qr.setReservedQty(entity.getReservedQty());
        qr.setLastFlowId(entity.getLastFlowId());
        qr.setLastChangedAt(entity.getLastChangedAt());
        return ApiResponse.success(InventoryStockDetailResponse.fromQueryResponse(qr));
    }

    @PostMapping("/inbound")
    public ApiResponse<Void> inbound(@Valid @RequestBody InventoryInboundRequest request) {
        CurrentUser user = requireCurrentUser();
        InventoryInboundCommand command = new InventoryInboundCommand();
        command.setStoreId(user.storeId());
        command.setOperatorId(user.userId());
        command.setPartId(request.partId());
        command.setQuantity(request.quantity());
        command.setUnitCost(request.unitCost());
        command.setBarcode(request.barcode());
        command.setLocationRemark(request.locationRemark());
        command.setReason(request.reason());
        command.setRemark(request.remark());
        inventoryService.inbound(command);
        return ApiResponse.success(null);
    }

    @PostMapping("/adjust")
    @PreAuthorize("hasAuthority('INVENTORY_ADJUST')")
    public ApiResponse<Void> adjust(@Valid @RequestBody InventoryAdjustRequest request) {
        CurrentUser user = requireCurrentUser();
        InventoryAdjustCommand command = new InventoryAdjustCommand();
        command.setStoreId(user.storeId());
        command.setOperatorId(user.userId());
        command.setPartId(request.partId());
        command.setQuantityDelta(request.quantityDelta());
        command.setReason(request.reason());
        command.setRemark(request.remark());
        inventoryService.adjust(command);
        return ApiResponse.success(null);
    }

    @GetMapping("/flows")
    public ApiResponse<PageResponse<InventoryFlowQueryResponse>> listFlows(
            @RequestParam(required = false) Long partId,
            @RequestParam(required = false) String partCode,
            @RequestParam(required = false) String partName,
            @RequestParam(required = false) String flowType,
            @RequestParam(required = false) Integer pageNo,
            @RequestParam(required = false) Integer pageSize) {

        CurrentUser user = requireCurrentUser();
        InventoryFlowQueryRequest request = new InventoryFlowQueryRequest();
        request.setStoreId(user.storeId());
        request.setPartId(partId);
        request.setPartCode(partCode);
        request.setPartName(partName);
        request.setFlowType(flowType);
        request.setPageNo(pageNo);
        request.setPageSize(pageSize);
        return ApiResponse.success(inventoryService.pageFlowQuery(request));
    }

    private CurrentUser requireCurrentUser() {
        return CurrentUserContext.get()
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "缺少用户上下文"));
    }
}
