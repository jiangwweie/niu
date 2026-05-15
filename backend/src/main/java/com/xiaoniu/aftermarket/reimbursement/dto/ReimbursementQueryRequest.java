package com.xiaoniu.aftermarket.reimbursement.dto;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ReimbursementQueryRequest {

    private Long storeId;
    private String reimbursementNo;
    private Long applicantId;
    private String status;
    private LocalDateTime dateFrom;
    private LocalDateTime dateTo;
    private Integer pageNo;
    private Integer pageSize;

    public int normalizedPageNo() {
        return pageNo == null || pageNo < 1 ? 1 : pageNo;
    }

    public int normalizedPageSize() {
        if (pageSize == null || pageSize < 1) {
            return 20;
        }
        return Math.min(pageSize, 100);
    }
}
