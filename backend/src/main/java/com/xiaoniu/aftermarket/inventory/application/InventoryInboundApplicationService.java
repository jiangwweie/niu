package com.xiaoniu.aftermarket.inventory.application;

import com.xiaoniu.aftermarket.inventory.dto.InventoryInboundCommand;
import org.springframework.stereotype.Service;

@Service
public class InventoryInboundApplicationService {

    public void execute(InventoryInboundCommand command) {
        throw new UnsupportedOperationException("TODO: orchestrate inventory inbound in Phase 2");
    }
}
