package com.xiaoniu.aftermarket.staff.controller;

import com.xiaoniu.aftermarket.common.api.ApiResponse;
import com.xiaoniu.aftermarket.common.api.ErrorCode;
import com.xiaoniu.aftermarket.common.context.CurrentUser;
import com.xiaoniu.aftermarket.common.context.CurrentUserContext;
import com.xiaoniu.aftermarket.common.exception.BusinessException;
import com.xiaoniu.aftermarket.common.pagination.PageResponse;
import com.xiaoniu.aftermarket.inventory.dto.InventoryStockQueryResponse;
import com.xiaoniu.aftermarket.inventory.entity.InventoryStockEntity;
import com.xiaoniu.aftermarket.inventory.service.InventoryService;
import com.xiaoniu.aftermarket.part.entity.PartEntity;
import com.xiaoniu.aftermarket.part.mapper.PartMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/staff/inventory")
public class StaffInventoryController {

    private final InventoryService inventoryService;
    private final PartMapper partMapper;

    public StaffInventoryController(InventoryService inventoryService, PartMapper partMapper) {
        this.inventoryService = inventoryService;
        this.partMapper = partMapper;
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
        return ApiResponse.success(StaffInventoryStockDetail.from(entity, part));
    }

    private CurrentUser requireCurrentUser() {
        return CurrentUserContext.get()
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "缺少用户上下文"));
    }
}
