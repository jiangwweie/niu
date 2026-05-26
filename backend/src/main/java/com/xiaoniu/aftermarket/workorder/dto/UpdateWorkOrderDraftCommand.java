package com.xiaoniu.aftermarket.workorder.dto;

import lombok.Data;

@Data
public class UpdateWorkOrderDraftCommand {

    private Long storeId;
    private Long customerId;
    private Long vehicleId;
    private String customerNameSnapshot;
    private String customerPhoneSnapshot;
    private String vehicleModelSnapshot;
    private String frameNoSnapshot;
    private String batteryNoSnapshot;
    private String repairItem;
    private String remark;
    private Long operatorId;
}
