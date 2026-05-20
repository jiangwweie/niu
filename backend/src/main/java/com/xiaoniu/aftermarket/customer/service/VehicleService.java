package com.xiaoniu.aftermarket.customer.service;

import com.xiaoniu.aftermarket.common.pagination.PageResponse;
import com.xiaoniu.aftermarket.customer.dto.*;

public interface VehicleService {
    PageResponse<VehicleResponse> pageQuery(VehiclePageQuery query);
    VehicleDetailResponse getDetail(Long vehicleId, Long storeId);
    Long create(Long customerId, Long storeId, Long operatorId, CreateVehicleRequest request);
    void update(Long vehicleId, Long storeId, Long operatorId, UpdateVehicleRequest request);
}
