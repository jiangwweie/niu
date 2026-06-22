package com.xiaoniu.aftermarket.funding.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xiaoniu.aftermarket.common.persistence.entity.SoftDeleteEntity;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("funding_contract")
public class FundingContractEntity extends SoftDeleteEntity {

    private Long storeId;
    private Long applicationId;
    private String contractNo;
    private String contractType;
    private LocalDate signedDate;
    private String status;
    private Long confirmedBy;
    private LocalDateTime confirmedAt;
    private String voidReason;
    private String remark;
}
