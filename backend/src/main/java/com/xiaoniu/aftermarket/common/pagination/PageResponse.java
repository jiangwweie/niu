package com.xiaoniu.aftermarket.common.pagination;

import java.util.List;
import java.util.function.Function;

public record PageResponse<T>(
        List<T> records,
        int pageNo,
        int pageSize,
        long total
) {

    public <R> PageResponse<R> map(Function<T, R> mapper) {
        return new PageResponse<>(
                records.stream().map(mapper).toList(),
                pageNo,
                pageSize,
                total
        );
    }

    public static <T> PageResponse<T> empty(PageRequest request) {
        return new PageResponse<>(
                List.of(),
                request.normalizedPageNo(),
                request.normalizedPageSize(),
                0
        );
    }
}
