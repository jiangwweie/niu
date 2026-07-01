package com.xiaoniu.aftermarket.dict.controller;

public record DictItemResponse(
        Long id,
        String itemCode,
        String itemName,
        Integer sortOrder,
        Boolean enabled,
        String scope,
        Long storeId,
        Boolean systemItem,
        Boolean editable
) {
}
