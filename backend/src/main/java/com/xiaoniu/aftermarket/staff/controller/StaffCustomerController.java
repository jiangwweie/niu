package com.xiaoniu.aftermarket.staff.controller;

import com.xiaoniu.aftermarket.common.api.ApiResponse;
import com.xiaoniu.aftermarket.common.api.ErrorCode;
import com.xiaoniu.aftermarket.common.context.CurrentUser;
import com.xiaoniu.aftermarket.common.context.CurrentUserContext;
import com.xiaoniu.aftermarket.common.exception.BusinessException;
import com.xiaoniu.aftermarket.common.pagination.PageResponse;
import com.xiaoniu.aftermarket.customer.dto.CustomerPageQuery;
import com.xiaoniu.aftermarket.customer.dto.CustomerResponse;
import com.xiaoniu.aftermarket.customer.service.CustomerService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/staff/customers")
public class StaffCustomerController {

    private final CustomerService customerService;

    public StaffCustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PreAuthorize("hasAuthority('CUSTOMER_VIEW')")
    @GetMapping("/search")
    public ApiResponse<List<CustomerSearchResult>> search(
            @RequestParam String keyword) {
        CurrentUser user = requireCurrentUser();
        CustomerPageQuery query = new CustomerPageQuery();
        query.setStoreId(user.storeId());
        query.setKeyword(keyword);
        query.setPageNo(1);
        query.setPageSize(20);
        PageResponse<CustomerResponse> page = customerService.pageQuery(query);
        List<CustomerSearchResult> results = page.records().stream()
                .map(r -> new CustomerSearchResult(r.getId(), r.getCustomerName(), r.getPhone()))
                .toList();
        return ApiResponse.success(results);
    }

    private CurrentUser requireCurrentUser() {
        return CurrentUserContext.get()
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "缺少用户上下文"));
    }

    public record CustomerSearchResult(Long id, String customerName, String phone) {}
}
