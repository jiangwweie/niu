package com.xiaoniu.aftermarket.official.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.xiaoniu.aftermarket.common.persistence.entity.SoftDeleteEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("official_after_sales")
public class OfficialAfterSalesEntity extends SoftDeleteEntity {

    private Long storeId;
    private Long workOrderId;

    @TableField("is_official_after_sales")
    private Boolean officialAfterSales;

    private String officialOrderNo;
    private BigDecimal officialSettlementAmount;
    private String officialSettlementStatus;
    private LocalDateTime officialSettlementTime;
    private Long officialSettlementOperatorId;
    private String officialSettlementRemark;
    private String remark;
}
