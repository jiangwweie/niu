package com.xiaoniu.aftermarket.inventory.service.impl;

import com.xiaoniu.aftermarket.inventory.dto.InventoryAdjustCommand;
import com.xiaoniu.aftermarket.inventory.dto.InventoryInboundCommand;
import com.xiaoniu.aftermarket.inventory.service.InventoryService;
import com.xiaoniu.aftermarket.workorder.dto.AdjustChargeItemsCommand;
import org.springframework.stereotype.Service;

@Service
public class InventoryServiceImpl implements InventoryService {

    @Override
    public void inbound(InventoryInboundCommand command) {
        throw new UnsupportedOperationException("TODO: implement inventory inbound in Phase 2");
    }

    @Override
    public void adjust(InventoryAdjustCommand command) {
        throw new UnsupportedOperationException("TODO: implement inventory adjustment in Phase 2");
    }

    @Override
    public void reserveForWorkOrder(Long workOrderId, Long operatorId) {
        throw new UnsupportedOperationException("TODO: implement inventory reservation in Phase 3");
    }

    @Override
    public void releaseForWorkOrder(Long workOrderId, Long operatorId) {
        throw new UnsupportedOperationException("TODO: implement inventory release in Phase 3");
    }

    @Override
    public void consumeForWorkOrder(Long workOrderId, Long operatorId) {
        throw new UnsupportedOperationException("TODO: implement inventory consumption in Phase 4");
    }

    @Override
    public void adjustReservationForChargeItems(AdjustChargeItemsCommand command) {
        throw new UnsupportedOperationException("TODO: implement reservation adjustment in Phase 3/4");
    }
}
