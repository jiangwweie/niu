package com.xiaoniu.aftermarket.staff.controller;

import com.xiaoniu.aftermarket.common.api.ApiResponse;
import com.xiaoniu.aftermarket.common.api.ErrorCode;
import com.xiaoniu.aftermarket.common.context.CurrentUser;
import com.xiaoniu.aftermarket.common.context.CurrentUserContext;
import com.xiaoniu.aftermarket.common.exception.BusinessException;
import com.xiaoniu.aftermarket.common.pagination.PageResponse;
import com.xiaoniu.aftermarket.customer.dto.VehiclePageQuery;
import com.xiaoniu.aftermarket.customer.dto.VehicleResponse;
import com.xiaoniu.aftermarket.customer.service.VehicleService;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/staff/vehicles")
public class StaffVehicleController {

    private final VehicleService vehicleService;

    public StaffVehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    @GetMapping("/search")
    public ApiResponse<List<VehicleSearchResult>> search(
            @RequestParam String keyword) {
        CurrentUser user = requireCurrentUser();
        VehiclePageQuery query = new VehiclePageQuery();
        query.setStoreId(user.storeId());
        query.setKeyword(keyword);
        query.setPageNo(1);
        query.setPageSize(20);
        PageResponse<VehicleResponse> page = vehicleService.pageQuery(query);
        List<VehicleSearchResult> results = page.records().stream()
                .map(r -> new VehicleSearchResult(r.getId(), r.getFrameNo(), r.getModel(),
                        r.getCustomerId(), r.getCustomerName()))
                .toList();
        return ApiResponse.success(results);
    }

    private CurrentUser requireCurrentUser() {
        return CurrentUserContext.get()
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "缺少用户上下文"));
    }

    public record VehicleSearchResult(Long id, String frameNo, String model,
                                       Long customerId, String customerName) {}
}
