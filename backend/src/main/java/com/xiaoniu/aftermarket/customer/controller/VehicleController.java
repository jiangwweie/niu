package com.xiaoniu.aftermarket.customer.controller;

import com.xiaoniu.aftermarket.common.api.ApiResponse;
import com.xiaoniu.aftermarket.common.api.ErrorCode;
import com.xiaoniu.aftermarket.common.context.CurrentUser;
import com.xiaoniu.aftermarket.common.context.CurrentUserContext;
import com.xiaoniu.aftermarket.common.exception.BusinessException;
import com.xiaoniu.aftermarket.common.pagination.PageResponse;
import com.xiaoniu.aftermarket.customer.dto.*;
import com.xiaoniu.aftermarket.customer.service.VehicleService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/vehicles")
public class VehicleController {

    private final VehicleService vehicleService;

    public VehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    @PreAuthorize("hasAuthority('CUSTOMER_VIEW')")
    @GetMapping
    public ApiResponse<PageResponse<VehicleResponse>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String vin,
            @RequestParam(required = false) String model,
            @RequestParam(required = false) String customerPhone,
            @RequestParam(required = false) Integer pageNo,
            @RequestParam(required = false) Integer pageSize) {
        CurrentUser user = requireCurrentUser();
        VehiclePageQuery query = new VehiclePageQuery();
        query.setStoreId(user.storeId());
        query.setKeyword(keyword);
        query.setVin(vin);
        query.setModel(model);
        query.setCustomerPhone(customerPhone);
        query.setPageNo(pageNo);
        query.setPageSize(pageSize);
        return ApiResponse.success(vehicleService.pageQuery(query));
    }

    @PreAuthorize("hasAuthority('CUSTOMER_VIEW')")
    @GetMapping("/{id}")
    public ApiResponse<VehicleDetailResponse> detail(@PathVariable Long id) {
        CurrentUser user = requireCurrentUser();
        return ApiResponse.success(vehicleService.getDetail(id, user.storeId()));
    }

    @PreAuthorize("hasAuthority('CUSTOMER_MANAGE')")
    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id,
                                     @Valid @RequestBody UpdateVehicleRequest request) {
        CurrentUser user = requireCurrentUser();
        vehicleService.update(id, user.storeId(), user.userId(), request);
        return ApiResponse.success(null);
    }

    private CurrentUser requireCurrentUser() {
        return CurrentUserContext.get()
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "缺少用户上下文"));
    }
}
