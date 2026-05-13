package com.xiaoniu.aftermarket.workorder.application;

import com.xiaoniu.aftermarket.workorder.dto.SettleWorkOrderCommand;
import com.xiaoniu.aftermarket.workorder.service.WorkOrderService;
import org.springframework.stereotype.Service;

@Service
public class SettleWorkOrderApplicationService {

    private final WorkOrderService workOrderService;

    public SettleWorkOrderApplicationService(WorkOrderService workOrderService) {
        this.workOrderService = workOrderService;
    }

    public void execute(SettleWorkOrderCommand command) {
        workOrderService.settle(command);
    }
}
