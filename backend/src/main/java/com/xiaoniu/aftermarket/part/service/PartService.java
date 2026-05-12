package com.xiaoniu.aftermarket.part.service;

import com.xiaoniu.aftermarket.part.entity.PartEntity;

public interface PartService {

    PartEntity getById(Long id);

    PartEntity getByBarcode(Long storeId, String barcode);

    PartEntity createPart(PartEntity part);

    void disablePart(Long partId, Long operatorId);
}
