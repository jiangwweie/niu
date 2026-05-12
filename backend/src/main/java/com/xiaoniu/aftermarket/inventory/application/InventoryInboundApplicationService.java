package com.xiaoniu.aftermarket.inventory.application;

import com.xiaoniu.aftermarket.inventory.dto.InventoryInboundCommand;
import com.xiaoniu.aftermarket.inventory.service.InventoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryInboundApplicationService {

    private final InventoryService inventoryService;

    public InventoryInboundApplicationService(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @Transactional
    public void execute(InventoryInboundCommand command) {
        inventoryService.inbound(command);
    }
}
