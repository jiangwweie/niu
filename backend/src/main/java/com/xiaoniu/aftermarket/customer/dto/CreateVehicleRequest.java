package com.xiaoniu.aftermarket.customer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateVehicleRequest(
    @NotBlank(message = "车架号不能为空") @Size(max = 128) String frameNo,
    @Size(max = 128) String model,
    @Size(max = 128) String batteryNo,
    @Size(max = 512) String remark
) {}
