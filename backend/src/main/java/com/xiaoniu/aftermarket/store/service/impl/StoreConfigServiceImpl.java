package com.xiaoniu.aftermarket.store.service.impl;

import com.xiaoniu.aftermarket.common.api.ErrorCode;
import com.xiaoniu.aftermarket.common.exception.BusinessException;
import com.xiaoniu.aftermarket.common.mapper.StoreMapper;
import com.xiaoniu.aftermarket.common.persistence.entity.StoreEntity;
import com.xiaoniu.aftermarket.store.controller.dto.StoreDtos.StoreResponse;
import com.xiaoniu.aftermarket.store.controller.dto.StoreDtos.UpdateStoreRequest;
import com.xiaoniu.aftermarket.store.service.StoreConfigService;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StoreConfigServiceImpl implements StoreConfigService {

    private final StoreMapper storeMapper;

    public StoreConfigServiceImpl(StoreMapper storeMapper) {
        this.storeMapper = storeMapper;
    }

    @Override
    public StoreResponse getCurrentStore(Long storeId) {
        return toResponse(requireStore(storeId));
    }

    @Override
    @Transactional
    public StoreResponse updateCurrentStore(Long storeId, Long operatorId, UpdateStoreRequest request) {
        if (request.storeName() == null || request.storeName().isBlank()) {
            throw new BusinessException(ErrorCode.STORE_NAME_REQUIRED);
        }
        StoreEntity store = requireStore(storeId);
        store.setStoreName(request.storeName().trim());
        store.setContactName(blankToNull(request.contactName()));
        store.setContactPhone(blankToNull(request.contactPhone()));
        store.setAddress(blankToNull(request.address()));
        store.setRemark(blankToNull(request.remark()));
        store.setUpdatedBy(operatorId);
        store.setUpdatedAt(LocalDateTime.now());
        storeMapper.updateById(store);
        return toResponse(store);
    }

    private StoreEntity requireStore(Long storeId) {
        StoreEntity store = storeMapper.selectById(storeId);
        if (store == null || store.getDeleted() != null && store.getDeleted() != 0) {
            throw new BusinessException(ErrorCode.STORE_NOT_FOUND);
        }
        return store;
    }

    private StoreResponse toResponse(StoreEntity store) {
        return new StoreResponse(
                store.getId(),
                store.getStoreCode(),
                store.getStoreName(),
                store.getContactName(),
                store.getContactPhone(),
                store.getAddress(),
                store.getRemark()
        );
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
