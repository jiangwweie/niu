package com.xiaoniu.aftermarket.store.service;

import com.xiaoniu.aftermarket.store.controller.dto.StoreDtos.StoreResponse;
import com.xiaoniu.aftermarket.store.controller.dto.StoreDtos.UpdateStoreRequest;

public interface StoreConfigService {

    StoreResponse getCurrentStore(Long storeId);

    StoreResponse updateCurrentStore(Long storeId, Long operatorId, UpdateStoreRequest request);
}
