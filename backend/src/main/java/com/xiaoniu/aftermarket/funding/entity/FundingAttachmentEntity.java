package com.xiaoniu.aftermarket.funding.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xiaoniu.aftermarket.common.persistence.entity.SoftDeleteEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("funding_attachment")
public class FundingAttachmentEntity extends SoftDeleteEntity {

    private Long storeId;
    private String ownerType;
    private Long ownerId;
    private String attachmentType;
    private String originalFilename;
    private String storedFilename;
    private String storagePath;
    private String contentType;
    private Long fileSize;
    private String remark;
}
