package com.xiaoniu.aftermarket.customer.dto;

import lombok.Data;

@Data
public class CustomerPageQuery {
    private Long storeId;
    private String keyword; // searches name or phone
    private String phone;
    private String customerName;
    private Integer pageNo;
    private Integer pageSize;

    public int normalizedPageNo() {
        return pageNo == null || pageNo < 1 ? 1 : pageNo;
    }

    public int normalizedPageSize() {
        return pageSize == null || pageSize < 1 ? 20 : Math.min(pageSize, 100);
    }
}
