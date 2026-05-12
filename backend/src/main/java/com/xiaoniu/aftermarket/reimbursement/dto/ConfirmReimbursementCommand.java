package com.xiaoniu.aftermarket.reimbursement.dto;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class ConfirmReimbursementCommand {

    private Long reimbursementId;
    private Long operatorId;
    private BigDecimal confirmedAmount;
    private String remark;
}
