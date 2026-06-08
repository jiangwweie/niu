package com.xiaoniu.aftermarket.inventory.service;

import com.xiaoniu.aftermarket.common.pagination.PageResponse;
import com.xiaoniu.aftermarket.inventory.dto.InventoryAdjustCommand;
import com.xiaoniu.aftermarket.inventory.dto.InventoryFlowQueryRequest;
import com.xiaoniu.aftermarket.inventory.dto.InventoryFlowQueryResponse;
import com.xiaoniu.aftermarket.inventory.dto.InventoryInboundCommand;
import com.xiaoniu.aftermarket.inventory.dto.InventoryStockQueryResponse;
import com.xiaoniu.aftermarket.inventory.entity.InventoryStockEntity;

public interface InventoryService {

    void inbound(InventoryInboundCommand command);

    void adjust(InventoryAdjustCommand command);

    InventoryStockEntity getByPartId(Long storeId, Long partId);

    PageResponse<InventoryStockQueryResponse> pageQuery(Long storeId, String partCode,
                                                        String partName, String source,
                                                        String view, Integer pageNo,
                                                        Integer pageSize);

    PageResponse<InventoryStockQueryResponse> pageQuery(Long storeId, String keyword,
                                                        String partCode, String partName,
                                                        String source, String view,
                                                        Integer pageNo, Integer pageSize);

    PageResponse<InventoryFlowQueryResponse> pageFlowQuery(InventoryFlowQueryRequest request);
}
