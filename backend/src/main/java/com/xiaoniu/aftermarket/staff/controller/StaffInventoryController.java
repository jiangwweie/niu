package com.xiaoniu.aftermarket.staff.controller;

import com.xiaoniu.aftermarket.common.api.ApiResponse;
import com.xiaoniu.aftermarket.common.api.ErrorCode;
import com.xiaoniu.aftermarket.common.context.CurrentUser;
import com.xiaoniu.aftermarket.common.context.CurrentUserContext;
import com.xiaoniu.aftermarket.common.enums.CommonStatus;
import com.xiaoniu.aftermarket.common.exception.BusinessException;
import com.xiaoniu.aftermarket.common.pagination.PageResponse;
import com.xiaoniu.aftermarket.inventory.application.CreatePartAndInboundApplicationService;
import com.xiaoniu.aftermarket.inventory.dto.InventoryInboundCommand;
import com.xiaoniu.aftermarket.inventory.dto.InventoryStockQueryResponse;
import com.xiaoniu.aftermarket.inventory.entity.InventoryFlowEntity;
import com.xiaoniu.aftermarket.inventory.entity.InventoryStockEntity;
import com.xiaoniu.aftermarket.inventory.mapper.InventoryFlowMapper;
import com.xiaoniu.aftermarket.inventory.service.InventoryService;
import com.xiaoniu.aftermarket.part.entity.PartEntity;
import com.xiaoniu.aftermarket.part.mapper.PartMapper;
import com.xiaoniu.aftermarket.staff.dto.StaffCreatePartAndInboundRequest;
import com.xiaoniu.aftermarket.staff.dto.StaffCreatePartAndInboundResponse;
import com.xiaoniu.aftermarket.staff.dto.StaffInventoryInboundRequest;
import com.xiaoniu.aftermarket.staff.dto.StaffInventoryInboundResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/staff/inventory")
public class StaffInventoryController {

    private final InventoryService inventoryService;
    private final PartMapper partMapper;
    private final InventoryFlowMapper inventoryFlowMapper;
    private final CreatePartAndInboundApplicationService createPartAndInboundService;

    public StaffInventoryController(InventoryService inventoryService, PartMapper partMapper,
                                     InventoryFlowMapper inventoryFlowMapper,
                                     CreatePartAndInboundApplicationService createPartAndInboundService) {
        this.inventoryService = inventoryService;
        this.partMapper = partMapper;
        this.inventoryFlowMapper = inventoryFlowMapper;
        this.createPartAndInboundService = createPartAndInboundService;
    }

    @PreAuthorize("hasAuthority('INVENTORY_VIEW')")
    @GetMapping("/stocks")
    public ApiResponse<PageResponse<StaffInventoryStockItem>> listStocks(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String partCode,
            @RequestParam(required = false) String partName,
            @RequestParam(required = false) Integer pageNo,
            @RequestParam(required = false) Integer pageSize) {

        CurrentUser user = requireCurrentUser();
        PageResponse<InventoryStockQueryResponse> result =
                inventoryService.pageQuery(user.storeId(), keyword, partCode, partName, null, "ALL", pageNo, pageSize);
        return ApiResponse.success(result.map(StaffInventoryStockItem::from));
    }

    @PreAuthorize("hasAuthority('INVENTORY_VIEW')")
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
    @PreAuthorize("hasAuthority('INVENTORY_INBOUND')")
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
        command.setCode(request.code());
        command.setLocationRemark(request.locationRemark());
        command.setReason(request.reason());
        command.setRemark(request.remark());

        inventoryService.inbound(command);

        InventoryStockEntity stock = inventoryService.getByPartId(user.storeId(), command.getPartId());
        PartEntity part = partMapper.selectById(command.getPartId());
        InventoryFlowEntity flow = stock != null && stock.getLastFlowId() != null
                ? inventoryFlowMapper.selectById(stock.getLastFlowId())
                : null;

        return ApiResponse.success(StaffInventoryInboundResponse.from(stock, part, flow));
    }

    @PostMapping("/inbound/create-part-and-inbound")
    @PreAuthorize("hasAuthority('INVENTORY_INBOUND')")
    public ApiResponse<StaffCreatePartAndInboundResponse> createPartAndInbound(
            @Valid @RequestBody StaffCreatePartAndInboundRequest request) {
        CurrentUser user = requireCurrentUser();
        requirePartCreatePermission(user);

        CreatePartAndInboundApplicationService.Command command =
                new CreatePartAndInboundApplicationService.Command(
                        user.storeId(),
                        user.userId(),
                        request.source(),
                        request.partName(),
                        request.officialPartNo(),
                        request.externalBarcode(),
                        request.model(),
                        request.categoryCode(),
                        request.costPrice(),
                        request.salePrice(),
                        request.inboundQuantity(),
                        request.unitCost(),
                        request.locationRemark(),
                        request.reason(),
                        request.remark()
                );
        CreatePartAndInboundApplicationService.Result result = createPartAndInboundService.execute(command);
        return ApiResponse.success(new StaffCreatePartAndInboundResponse(
                result.partId(),
                result.partCode(),
                result.partName(),
                result.defaultBarcode(),
                result.externalBarcode(),
                result.actualQty(),
                result.availableQty(),
                result.reservedQty(),
                result.flowId(),
                result.operatedAt()
        ));
    }

    private CurrentUser requireCurrentUser() {
        return CurrentUserContext.get()
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "缺少用户上下文"));
    }

    /**
     * create-part-and-inbound 是原子操作：创建配件 + 入库。
     * 仅 INVENTORY_INBOUND 不足以创建配件，还必须具备 PART_CREATE 或 PART_MANAGE。
     */
    private void requirePartCreatePermission(CurrentUser user) {
        boolean canCreatePart = user.permissions().contains("PART_MANAGE")
                || user.permissions().contains("PART_CREATE");
        if (!canCreatePart) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST,
                    "创建配件需要 PART_CREATE 或 PART_MANAGE 权限");
        }
    }
}
