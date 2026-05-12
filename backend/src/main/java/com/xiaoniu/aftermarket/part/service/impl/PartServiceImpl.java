package com.xiaoniu.aftermarket.part.service.impl;

import com.xiaoniu.aftermarket.part.entity.PartBarcodeEntity;
import com.xiaoniu.aftermarket.part.entity.PartEntity;
import com.xiaoniu.aftermarket.part.mapper.PartBarcodeMapper;
import com.xiaoniu.aftermarket.part.mapper.PartMapper;
import com.xiaoniu.aftermarket.part.service.PartService;
import org.springframework.stereotype.Service;

@Service
public class PartServiceImpl implements PartService {

    private final PartMapper partMapper;
    private final PartBarcodeMapper partBarcodeMapper;

    public PartServiceImpl(PartMapper partMapper, PartBarcodeMapper partBarcodeMapper) {
        this.partMapper = partMapper;
        this.partBarcodeMapper = partBarcodeMapper;
    }

    @Override
    public PartEntity getById(Long id) {
        return partMapper.selectById(id);
    }

    @Override
    public PartEntity getByBarcode(Long storeId, String barcode) {
        PartBarcodeEntity barcodeEntity = partBarcodeMapper.selectByStoreIdAndBarcode(storeId, barcode);
        return barcodeEntity == null ? null : partMapper.selectById(barcodeEntity.getPartId());
    }

    @Override
    public PartEntity createPart(PartEntity part) {
        throw new UnsupportedOperationException("TODO: implement part creation in a later phase");
    }

    @Override
    public void disablePart(Long partId, Long operatorId) {
        throw new UnsupportedOperationException("TODO: implement part disable flow in a later phase");
    }
}
