package com.xiaoniu.aftermarket.workorder.dto;

import java.util.List;
import lombok.Data;

@Data
public class AdjustChargeItemsCommand {

    private Long workOrderId;
    private Long operatorId;
    private List<WorkOrderChargeItemInput> chargeItems;
    private String remark;
}
