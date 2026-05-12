package com.xiaoniu.aftermarket.inventory.application;

import com.xiaoniu.aftermarket.inventory.dto.InventoryAdjustCommand;
import com.xiaoniu.aftermarket.inventory.service.InventoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryAdjustApplicationService {

    private final InventoryService inventoryService;

    public InventoryAdjustApplicationService(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @Transactional
    public void execute(InventoryAdjustCommand command) {
        inventoryService.adjust(command);
    }
}
