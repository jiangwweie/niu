package com.xiaoniu.aftermarket.dict.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateDictItemRequest(
        @NotBlank(message = "字典名称不能为空")
        @Size(max = 128, message = "字典名称不能超过128个字符")
        String itemName,

        Integer sortOrder,

        String status,

        @Size(max = 512, message = "备注不能超过512个字符")
        String remark
) {
}
