package com.xiaoniu.aftermarket.dict.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateDictItemRequest(
        @Size(max = 64, message = "字典编码不能超过64个字符")
        String itemCode,

        @NotBlank(message = "字典名称不能为空")
        @Size(max = 128, message = "字典名称不能超过128个字符")
        String itemName,

        Integer sortOrder,

        String scope,

        @Size(max = 512, message = "备注不能超过512个字符")
        String remark
) {
}
