package com.xiaoniu.aftermarket.workorder.dto;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class WorkOrderQueryRequest {

    private Long storeId;
    private String status;
    private String keyword;
    private String workOrderNo;
    private String customerName;
    private String customerPhone;
    private String vehicleFrameNo;
    private Long partId;
    private Boolean officialOnly;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer pageNo;
    private Integer pageSize;
}
