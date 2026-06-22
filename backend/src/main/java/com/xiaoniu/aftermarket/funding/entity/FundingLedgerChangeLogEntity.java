package com.xiaoniu.aftermarket.funding.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xiaoniu.aftermarket.common.persistence.entity.CreatedEntity;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("funding_ledger_change_log")
public class FundingLedgerChangeLogEntity extends CreatedEntity {

    private Long storeId;
    private Long ledgerId;
    private String fieldName;
    private String oldValue;
    private String newValue;
    private Long operatorId;
    private LocalDateTime operatedAt;
    private String remark;
}
