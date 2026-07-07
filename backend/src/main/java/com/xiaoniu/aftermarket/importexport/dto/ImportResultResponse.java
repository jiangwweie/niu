package com.xiaoniu.aftermarket.importexport.dto;

public record ImportResultResponse(
        int totalRows,
        int successRows,
        String message
) {
}
