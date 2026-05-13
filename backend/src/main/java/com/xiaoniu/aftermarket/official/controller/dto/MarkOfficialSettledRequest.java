package com.xiaoniu.aftermarket.official.controller.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class MarkOfficialSettledRequest {

    @NotNull(message = "官方结算金额不能为空")
    private BigDecimal settlementAmount;

    private LocalDateTime settlementTime;

    private String remark;
}
