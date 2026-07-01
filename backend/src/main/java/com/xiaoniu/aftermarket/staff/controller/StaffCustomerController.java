package com.xiaoniu.aftermarket.staff.controller;

import com.xiaoniu.aftermarket.common.api.ApiResponse;
import com.xiaoniu.aftermarket.common.api.ErrorCode;
import com.xiaoniu.aftermarket.common.context.CurrentUser;
import com.xiaoniu.aftermarket.common.context.CurrentUserContext;
import com.xiaoniu.aftermarket.common.exception.BusinessException;
import com.xiaoniu.aftermarket.common.pagination.PageResponse;
import com.xiaoniu.aftermarket.common.util.SearchKeywordUtils;
import com.xiaoniu.aftermarket.customer.dto.CreateCustomerRequest;
import com.xiaoniu.aftermarket.customer.dto.CreateVehicleRequest;
import com.xiaoniu.aftermarket.customer.dto.CustomerPageQuery;
import com.xiaoniu.aftermarket.customer.dto.CustomerResponse;
import com.xiaoniu.aftermarket.customer.dto.VehiclePageQuery;
import com.xiaoniu.aftermarket.customer.service.CustomerService;
import com.xiaoniu.aftermarket.customer.service.VehicleService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/staff/customers")
public class StaffCustomerController {

    private final CustomerService customerService;
    private final VehicleService vehicleService;

    public StaffCustomerController(CustomerService customerService, VehicleService vehicleService) {
        this.customerService = customerService;
        this.vehicleService = vehicleService;
    }

    @PreAuthorize("hasAuthority('CUSTOMER_VIEW')")
    @GetMapping("/search")
    public ApiResponse<List<CustomerSearchResult>> search(
            @RequestParam String keyword) {
        CurrentUser user = requireCurrentUser();
        String normalizedKeyword = SearchKeywordUtils.normalize(keyword);
        if (normalizedKeyword == null) {
            return ApiResponse.success(List.of());
        }
        CustomerPageQuery query = new CustomerPageQuery();
        query.setStoreId(user.storeId());
        query.setKeyword(normalizedKeyword);
        query.setPageNo(1);
        query.setPageSize(20);
        PageResponse<CustomerResponse> page = customerService.pageQuery(query);
        List<CustomerSearchResult> results = page.records().stream()
                .map(r -> new CustomerSearchResult(r.getId(), r.getCustomerName(), r.getPhone()))
                .toList();
        return ApiResponse.success(results);
    }

    @PreAuthorize("hasAuthority('CUSTOMER_MANAGE')")
    @PostMapping
    public ApiResponse<Long> create(@Valid @RequestBody CreateCustomerRequest request) {
        CurrentUser user = requireCurrentUser();
        Long id = customerService.create(user.storeId(), user.userId(), request);
        return ApiResponse.success(id);
    }

    @PreAuthorize("hasAuthority('CUSTOMER_MANAGE')")
    @PostMapping("/{customerId}/vehicles")
    public ApiResponse<Long> createVehicle(@PathVariable Long customerId,
                                            @Valid @RequestBody CreateVehicleRequest request) {
        CurrentUser user = requireCurrentUser();
        Long id = vehicleService.create(customerId, user.storeId(), user.userId(), request);
        return ApiResponse.success(id);
    }

    @PreAuthorize("hasAuthority('CUSTOMER_VIEW')")
    @GetMapping("/{customerId}/vehicles")
    public ApiResponse<List<CustomerVehicleResult>> listVehicles(@PathVariable Long customerId) {
        CurrentUser user = requireCurrentUser();
        VehiclePageQuery query = new VehiclePageQuery();
        query.setStoreId(user.storeId());
        query.setCustomerId(customerId);
        query.setPageNo(1);
        query.setPageSize(50);
        List<CustomerVehicleResult> results = vehicleService.pageQuery(query).records().stream()
                .map(r -> new CustomerVehicleResult(r.getId(), r.getFrameNo(), r.getModel(),
                        r.getBatteryNo(), r.getCustomerId(), r.getCustomerName(), r.getCustomerPhone()))
                .toList();
        return ApiResponse.success(results);
    }

    private CurrentUser requireCurrentUser() {
        return CurrentUserContext.get()
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "缺少用户上下文"));
    }

    public record CustomerSearchResult(Long id, String customerName, String phone) {}

    public record CustomerVehicleResult(Long id, String frameNo, String model,
                                        String batteryNo, Long customerId,
                                        String customerName, String customerPhone) {}
}
