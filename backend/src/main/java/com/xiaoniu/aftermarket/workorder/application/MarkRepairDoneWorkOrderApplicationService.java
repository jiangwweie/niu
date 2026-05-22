package com.xiaoniu.aftermarket.workorder.application;

import com.xiaoniu.aftermarket.workorder.dto.MarkRepairDoneWorkOrderCommand;
import com.xiaoniu.aftermarket.workorder.service.WorkOrderService;
import org.springframework.stereotype.Service;

@Service
public class MarkRepairDoneWorkOrderApplicationService {

    private final WorkOrderService workOrderService;

    public MarkRepairDoneWorkOrderApplicationService(WorkOrderService workOrderService) {
        this.workOrderService = workOrderService;
    }

    public void execute(MarkRepairDoneWorkOrderCommand command) {
        workOrderService.markRepairDone(command);
    }
}
