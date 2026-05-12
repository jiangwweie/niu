package com.xiaoniu.aftermarket.workorder.application;

import com.xiaoniu.aftermarket.workorder.dto.SubmitWorkOrderCommand;
import com.xiaoniu.aftermarket.workorder.service.WorkOrderService;
import org.springframework.stereotype.Service;

@Service
public class SubmitWorkOrderApplicationService {

    private final WorkOrderService workOrderService;

    public SubmitWorkOrderApplicationService(WorkOrderService workOrderService) {
        this.workOrderService = workOrderService;
    }

    public void execute(SubmitWorkOrderCommand command) {
        workOrderService.submit(command);
    }
}
