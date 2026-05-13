package com.xiaoniu.aftermarket.dict.controller;

public record DictTypeResponse(
        String typeCode,
        String typeName,
        Boolean enabled
) {
}
