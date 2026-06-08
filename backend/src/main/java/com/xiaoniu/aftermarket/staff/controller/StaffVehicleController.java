package com.xiaoniu.aftermarket.staff.controller;

import com.xiaoniu.aftermarket.common.api.ApiResponse;
import com.xiaoniu.aftermarket.common.api.ErrorCode;
import com.xiaoniu.aftermarket.common.context.CurrentUser;
import com.xiaoniu.aftermarket.common.context.CurrentUserContext;
import com.xiaoniu.aftermarket.common.exception.BusinessException;
import com.xiaoniu.aftermarket.common.pagination.PageResponse;
import com.xiaoniu.aftermarket.common.util.SearchKeywordUtils;
import com.xiaoniu.aftermarket.customer.dto.VehiclePageQuery;
import com.xiaoniu.aftermarket.customer.dto.VehicleResponse;
import com.xiaoniu.aftermarket.customer.service.VehicleService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/staff/vehicles")
public class StaffVehicleController {

    private final VehicleService vehicleService;

    public StaffVehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    @PreAuthorize("hasAuthority('CUSTOMER_VIEW')")
    @GetMapping("/search")
    public ApiResponse<List<VehicleSearchResult>> search(
            @RequestParam String keyword,
            @RequestParam(required = false) Long customerId) {
        CurrentUser user = requireCurrentUser();
        String normalizedKeyword = SearchKeywordUtils.normalize(keyword);
        if (normalizedKeyword == null) {
            return ApiResponse.success(List.of());
        }
        VehiclePageQuery query = new VehiclePageQuery();
        query.setStoreId(user.storeId());
        query.setKeyword(normalizedKeyword);
        query.setCustomerId(customerId);
        query.setPageNo(1);
        query.setPageSize(20);
        PageResponse<VehicleResponse> page = vehicleService.pageQuery(query);
        Map<Long, VehicleSearchResult> resultMap = new LinkedHashMap<>();
        page.records().forEach(r -> resultMap.put(r.getId(), toSearchResult(r)));

        VehiclePageQuery phoneQuery = new VehiclePageQuery();
        phoneQuery.setStoreId(user.storeId());
        phoneQuery.setCustomerPhone(normalizedKeyword);
        phoneQuery.setPageNo(1);
        phoneQuery.setPageSize(20);
        vehicleService.pageQuery(phoneQuery).records()
                .forEach(r -> resultMap.putIfAbsent(r.getId(), toSearchResult(r)));

        List<VehicleSearchResult> results = resultMap.values().stream()
                .limit(20)
                .toList();
        return ApiResponse.success(results);
    }

    private VehicleSearchResult toSearchResult(VehicleResponse r) {
        return new VehicleSearchResult(r.getId(), r.getFrameNo(), r.getModel(),
                r.getBatteryNo(), r.getCustomerId(), r.getCustomerName(), r.getCustomerPhone());
    }

    private CurrentUser requireCurrentUser() {
        return CurrentUserContext.get()
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "缺少用户上下文"));
    }

    public record VehicleSearchResult(Long id, String frameNo, String model,
                                       String batteryNo, Long customerId,
                                       String customerName, String customerPhone) {}
}
