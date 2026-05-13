package com.xiaoniu.aftermarket.payment.dto;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class RefundQueryRequest {

    private Long storeId;
    private String workOrderNo;
    private String customerName;
    private String refundMethod;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer pageNo;
    private Integer pageSize;

    public int normalizedPageNo() {
        return pageNo == null ? 1 : pageNo;
    }

    public int normalizedPageSize() {
        return pageSize == null ? 20 : pageSize;
    }
}
