package com.xiaoniu.aftermarket.staff.controller;

import com.xiaoniu.aftermarket.common.api.ApiResponse;
import com.xiaoniu.aftermarket.common.api.ErrorCode;
import com.xiaoniu.aftermarket.common.context.CurrentUser;
import com.xiaoniu.aftermarket.common.context.CurrentUserContext;
import com.xiaoniu.aftermarket.common.enums.CommonStatus;
import com.xiaoniu.aftermarket.common.exception.BusinessException;
import com.xiaoniu.aftermarket.common.pagination.PageResponse;
import com.xiaoniu.aftermarket.inventory.dto.InventoryInboundCommand;
import com.xiaoniu.aftermarket.inventory.dto.InventoryStockQueryResponse;
import com.xiaoniu.aftermarket.inventory.entity.InventoryFlowEntity;
import com.xiaoniu.aftermarket.inventory.entity.InventoryStockEntity;
import com.xiaoniu.aftermarket.inventory.mapper.InventoryFlowMapper;
import com.xiaoniu.aftermarket.inventory.service.InventoryService;
import com.xiaoniu.aftermarket.part.entity.PartEntity;
import com.xiaoniu.aftermarket.part.mapper.PartMapper;
import com.xiaoniu.aftermarket.staff.dto.StaffInventoryInboundRequest;
import com.xiaoniu.aftermarket.staff.dto.StaffInventoryInboundResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/staff/inventory")
public class StaffInventoryController {

    private final InventoryService inventoryService;
    private final PartMapper partMapper;
    private final InventoryFlowMapper inventoryFlowMapper;

    public StaffInventoryController(InventoryService inventoryService, PartMapper partMapper,
                                     InventoryFlowMapper inventoryFlowMapper) {
        this.inventoryService = inventoryService;
        this.partMapper = partMapper;
        this.inventoryFlowMapper = inventoryFlowMapper;
    }

    @GetMapping("/stocks")
    public ApiResponse<PageResponse<StaffInventoryStockItem>> listStocks(
            @RequestParam(required = false) String partCode,
            @RequestParam(required = false) String partName,
            @RequestParam(required = false) Integer pageNo,
            @RequestParam(required = false) Integer pageSize) {

        CurrentUser user = requireCurrentUser();
        PageResponse<InventoryStockQueryResponse> result =
                inventoryService.pageQuery(user.storeId(), partCode, partName, pageNo, pageSize);
        return ApiResponse.success(result.map(StaffInventoryStockItem::from));
    }

    @GetMapping("/stocks/{partId}")
    public ApiResponse<StaffInventoryStockDetail> getStockByPartId(@PathVariable Long partId) {
        CurrentUser user = requireCurrentUser();
        InventoryStockEntity entity = inventoryService.getByPartId(user.storeId(), partId);
        if (entity == null) {
            throw new BusinessException(ErrorCode.PART_STOCK_NOT_FOUND, "库存记录不存在");
        }
        PartEntity part = partMapper.selectById(entity.getPartId());
        if (part == null || !CommonStatus.ENABLED.getCode().equals(part.getStatus())) {
            throw new BusinessException(ErrorCode.PART_STOCK_NOT_FOUND, "库存记录不存在");
        }
        return ApiResponse.success(StaffInventoryStockDetail.from(entity, part));
    }

    @PostMapping("/inbound")
    public ApiResponse<StaffInventoryInboundResponse> inbound(
            @Valid @RequestBody StaffInventoryInboundRequest request) {
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

        InventoryStockEntity stock = inventoryService.getByPartId(user.storeId(), request.partId());
        PartEntity part = partMapper.selectById(request.partId());
        InventoryFlowEntity flow = stock != null && stock.getLastFlowId() != null
                ? inventoryFlowMapper.selectById(stock.getLastFlowId())
                : null;

        return ApiResponse.success(StaffInventoryInboundResponse.from(stock, part, flow));
    }

    private CurrentUser requireCurrentUser() {
        return CurrentUserContext.get()
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "缺少用户上下文"));
    }
}
