package com.xiaoniu.aftermarket.reimbursement.dto;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class SubmitReimbursementCommand {

    private Long storeId;
    private Long applicantId;
    private Long operatorId;
    private String purpose;
    private BigDecimal amount;
    private String remark;
}
