package com.xiaoniu.aftermarket.dict.controller;

public record DictItemResponse(
        String itemCode,
        String itemName,
        Integer sortOrder,
        Boolean enabled
) {
}
