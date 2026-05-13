package com.xiaoniu.aftermarket.workorder.application;

import com.xiaoniu.aftermarket.workorder.dto.CancelWorkOrderCommand;
import com.xiaoniu.aftermarket.workorder.service.WorkOrderService;
import org.springframework.stereotype.Service;

@Service
public class CancelWorkOrderApplicationService {

    private final WorkOrderService workOrderService;

    public CancelWorkOrderApplicationService(WorkOrderService workOrderService) {
        this.workOrderService = workOrderService;
    }

    public void execute(CancelWorkOrderCommand command) {
        workOrderService.cancel(command);
    }
}
