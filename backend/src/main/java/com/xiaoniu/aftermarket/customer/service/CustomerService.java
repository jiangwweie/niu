package com.xiaoniu.aftermarket.customer.service;

import com.xiaoniu.aftermarket.common.pagination.PageResponse;
import com.xiaoniu.aftermarket.customer.dto.*;

public interface CustomerService {
    PageResponse<CustomerResponse> pageQuery(CustomerPageQuery query);
    CustomerDetailResponse getDetail(Long customerId, Long storeId);
    Long create(Long storeId, Long operatorId, CreateCustomerRequest request);
    void update(Long customerId, Long storeId, Long operatorId, UpdateCustomerRequest request);
    void delete(Long customerId, Long storeId, Long operatorId);
}
