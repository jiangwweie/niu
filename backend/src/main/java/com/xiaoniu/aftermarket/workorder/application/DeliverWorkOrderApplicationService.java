package com.xiaoniu.aftermarket.workorder.application;

import com.xiaoniu.aftermarket.workorder.dto.DeliverWorkOrderCommand;
import com.xiaoniu.aftermarket.workorder.service.WorkOrderService;
import org.springframework.stereotype.Service;

@Service
public class DeliverWorkOrderApplicationService {

    private final WorkOrderService workOrderService;

    public DeliverWorkOrderApplicationService(WorkOrderService workOrderService) {
        this.workOrderService = workOrderService;
    }

    public void execute(DeliverWorkOrderCommand command) {
        workOrderService.deliver(command);
    }
}
