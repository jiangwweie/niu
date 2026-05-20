package com.xiaoniu.aftermarket.customer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateCustomerRequest(
    @NotBlank(message = "客户姓名不能为空") @Size(max = 64) String customerName,
    @Size(max = 32) String phone,
    @Size(max = 512) String remark
) {}
