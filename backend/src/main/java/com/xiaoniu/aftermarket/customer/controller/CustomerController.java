package com.xiaoniu.aftermarket.customer.controller;

import com.xiaoniu.aftermarket.common.api.ApiResponse;
import com.xiaoniu.aftermarket.common.api.ErrorCode;
import com.xiaoniu.aftermarket.common.context.CurrentUser;
import com.xiaoniu.aftermarket.common.context.CurrentUserContext;
import com.xiaoniu.aftermarket.common.exception.BusinessException;
import com.xiaoniu.aftermarket.common.pagination.PageResponse;
import com.xiaoniu.aftermarket.customer.dto.*;
import com.xiaoniu.aftermarket.customer.service.CustomerService;
import com.xiaoniu.aftermarket.customer.service.VehicleService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/customers")
public class CustomerController {

    private final CustomerService customerService;
    private final VehicleService vehicleService;

    public CustomerController(CustomerService customerService, VehicleService vehicleService) {
        this.customerService = customerService;
        this.vehicleService = vehicleService;
    }

    @PreAuthorize("hasAuthority('CUSTOMER_VIEW')")
    @GetMapping
    public ApiResponse<PageResponse<CustomerResponse>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String customerName,
            @RequestParam(required = false) Integer pageNo,
            @RequestParam(required = false) Integer pageSize) {
        CurrentUser user = requireCurrentUser();
        CustomerPageQuery query = new CustomerPageQuery();
        query.setStoreId(user.storeId());
        query.setKeyword(keyword);
        query.setPhone(phone);
        query.setCustomerName(customerName);
        query.setPageNo(pageNo);
        query.setPageSize(pageSize);
        return ApiResponse.success(customerService.pageQuery(query));
    }

    @PreAuthorize("hasAuthority('CUSTOMER_VIEW')")
    @GetMapping("/{id}")
    public ApiResponse<CustomerDetailResponse> detail(@PathVariable Long id) {
        CurrentUser user = requireCurrentUser();
        return ApiResponse.success(customerService.getDetail(id, user.storeId()));
    }

    @PreAuthorize("hasAuthority('CUSTOMER_MANAGE')")
    @PostMapping
    public ApiResponse<Long> create(@Valid @RequestBody CreateCustomerRequest request) {
        CurrentUser user = requireCurrentUser();
        Long id = customerService.create(user.storeId(), user.userId(), request);
        return ApiResponse.success(id);
    }

    @PreAuthorize("hasAuthority('CUSTOMER_MANAGE')")
    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id,
                                     @Valid @RequestBody UpdateCustomerRequest request) {
        CurrentUser user = requireCurrentUser();
        customerService.update(id, user.storeId(), user.userId(), request);
        return ApiResponse.success(null);
    }

    @PreAuthorize("hasAuthority('CUSTOMER_MANAGE')")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        CurrentUser user = requireCurrentUser();
        customerService.delete(id, user.storeId(), user.userId());
        return ApiResponse.success(null);
    }

    @PreAuthorize("hasAuthority('CUSTOMER_MANAGE')")
    @PostMapping("/{customerId}/vehicles")
    public ApiResponse<Long> createVehicle(@PathVariable Long customerId,
                                            @Valid @RequestBody CreateVehicleRequest request) {
        CurrentUser user = requireCurrentUser();
        Long id = vehicleService.create(customerId, user.storeId(), user.userId(), request);
        return ApiResponse.success(id);
    }

    private CurrentUser requireCurrentUser() {
        return CurrentUserContext.get()
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "缺少用户上下文"));
    }
}
