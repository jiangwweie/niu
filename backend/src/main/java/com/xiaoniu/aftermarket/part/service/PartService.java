package com.xiaoniu.aftermarket.part.service;

import com.xiaoniu.aftermarket.common.pagination.PageResponse;
import com.xiaoniu.aftermarket.part.dto.CreatePartCommand;
import com.xiaoniu.aftermarket.part.dto.PartQueryRequest;
import com.xiaoniu.aftermarket.part.dto.PartQueryResponse;
import com.xiaoniu.aftermarket.part.dto.UpdatePartCommand;
import com.xiaoniu.aftermarket.part.entity.PartBarcodeEntity;
import com.xiaoniu.aftermarket.part.entity.PartEntity;

public interface PartService {

    PartEntity getById(Long id);

    PartEntity getByBarcode(Long storeId, String barcode);

    PartEntity getByPartCode(Long storeId, String partCode);

    PartEntity createOfficialPart(CreatePartCommand command);

    PartEntity createThirdPartyPart(CreatePartCommand command);

    void updatePart(UpdatePartCommand command);

    void enablePart(Long partId);

    void disablePart(Long partId);

    PartBarcodeEntity createBarcode(Long storeId, Long partId, String barcode, Long operatorId);

    void updateDefaultBarcode(Long partId, String newDefaultBarcode);

    PageResponse<PartQueryResponse> pageQuery(PartQueryRequest request);
}
