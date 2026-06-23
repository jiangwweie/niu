package com.xiaoniu.aftermarket.funding.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xiaoniu.aftermarket.common.persistence.entity.SoftDeleteEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("funding_import_batch")
public class FundingImportBatchEntity extends SoftDeleteEntity {

    private Long storeId;
    private String batchNo;
    private String originalFilename;
    private String status;
    private Integer totalRows;
    private Integer successRows;
    private Integer failedRows;
    private String errorSummary;
}
