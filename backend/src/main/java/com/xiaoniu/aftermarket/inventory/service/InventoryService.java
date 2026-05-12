package com.xiaoniu.aftermarket.inventory.service;

import com.xiaoniu.aftermarket.inventory.dto.InventoryAdjustCommand;
import com.xiaoniu.aftermarket.inventory.dto.InventoryInboundCommand;
import com.xiaoniu.aftermarket.workorder.dto.AdjustChargeItemsCommand;

public interface InventoryService {

    void inbound(InventoryInboundCommand command);

    void adjust(InventoryAdjustCommand command);

    void reserveForWorkOrder(Long workOrderId, Long operatorId);

    void releaseForWorkOrder(Long workOrderId, Long operatorId);

    void consumeForWorkOrder(Long workOrderId, Long operatorId);

    void adjustReservationForChargeItems(AdjustChargeItemsCommand command);
}
