package com.xiaoniu.aftermarket.official.dto;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OfficialAfterSalesQueryRequest {

    private Long storeId;
    private String officialOrderNo;
    private String workOrderNo;
    private String settlementStatus;
    private LocalDateTime settlementStartTime;
    private LocalDateTime settlementEndTime;
    private Integer pageNo;
    private Integer pageSize;

    public int normalizedPageNo() {
        return pageNo == null ? 1 : pageNo;
    }

    public int normalizedPageSize() {
        return pageSize == null ? 20 : pageSize;
    }
}
