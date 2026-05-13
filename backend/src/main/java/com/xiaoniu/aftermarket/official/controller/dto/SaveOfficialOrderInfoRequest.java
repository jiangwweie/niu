package com.xiaoniu.aftermarket.official.controller.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SaveOfficialOrderInfoRequest {

    @NotBlank(message = "官方售后订单号不能为空")
    private String officialOrderNo;

    private String remark;
}
