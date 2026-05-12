package com.xiaoniu.aftermarket.workorder.application;

import com.xiaoniu.aftermarket.workorder.dto.SubmitWorkOrderCommand;
import com.xiaoniu.aftermarket.workorder.service.WorkOrderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubmitWorkOrderApplicationService {

    private final WorkOrderService workOrderService;

    public SubmitWorkOrderApplicationService(WorkOrderService workOrderService) {
        this.workOrderService = workOrderService;
    }

    @Transactional
    public void execute(SubmitWorkOrderCommand command) {
        workOrderService.submit(command);
    }
}
