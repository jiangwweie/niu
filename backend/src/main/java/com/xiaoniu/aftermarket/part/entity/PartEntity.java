package com.xiaoniu.aftermarket.part.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xiaoniu.aftermarket.common.persistence.entity.SoftDeleteEntity;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("part")
public class PartEntity extends SoftDeleteEntity {

    private Long storeId;
    private String partCode;
    private String officialPartNo;
    private String partName;
    private String model;
    private String source;
    private String categoryCode;
    private BigDecimal referenceCostPrice;
    private String defaultBarcode;
    private String locationRemark;
    private String createSource;
    private String status;
    private String remark;
}
