package com.xiaoniu.aftermarket.common.pagination;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record PageRequest(
        @Min(1) Integer pageNo,
        @Min(1) @Max(200) Integer pageSize
) {

    private static final int DEFAULT_PAGE_NO = 1;
    private static final int DEFAULT_PAGE_SIZE = 20;

    public int normalizedPageNo() {
        return pageNo == null ? DEFAULT_PAGE_NO : pageNo;
    }

    public int normalizedPageSize() {
        return pageSize == null ? DEFAULT_PAGE_SIZE : pageSize;
    }
}
