package com.xiaoniu.aftermarket.common.pagination;

import java.util.List;

public record PageResponse<T>(
        List<T> records,
        int pageNo,
        int pageSize,
        long total
) {

    public static <T> PageResponse<T> empty(PageRequest request) {
        return new PageResponse<>(
                List.of(),
                request.normalizedPageNo(),
                request.normalizedPageSize(),
                0
        );
    }
}
