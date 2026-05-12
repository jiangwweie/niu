package com.xiaoniu.aftermarket.part.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.xiaoniu.aftermarket.common.persistence.entity.SoftDeleteEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("part_barcode")
public class PartBarcodeEntity extends SoftDeleteEntity {

    private Long storeId;
    private Long partId;
    private String barcode;
    private String barcodeType;

    @TableField("is_primary")
    private Boolean primaryBarcode;

    private String status;
    private String remark;
}
