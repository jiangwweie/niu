package com.xiaoniu.aftermarket.official.controller;

import com.xiaoniu.aftermarket.common.api.ApiResponse;
import com.xiaoniu.aftermarket.common.context.CurrentUser;
import com.xiaoniu.aftermarket.common.context.CurrentUserContext;
import com.xiaoniu.aftermarket.common.api.ErrorCode;
import com.xiaoniu.aftermarket.common.exception.BusinessException;
import com.xiaoniu.aftermarket.common.pagination.PageResponse;
import com.xiaoniu.aftermarket.common.util.DateParamParser;
import com.xiaoniu.aftermarket.official.controller.dto.MarkNoSettlementRequiredRequest;
import com.xiaoniu.aftermarket.official.controller.dto.MarkOfficialSettledRequest;
import com.xiaoniu.aftermarket.official.controller.dto.SaveOfficialOrderInfoRequest;
import com.xiaoniu.aftermarket.official.dto.MarkNoSettlementRequiredCommand;
import com.xiaoniu.aftermarket.official.dto.MarkOfficialSettledCommand;
import com.xiaoniu.aftermarket.official.dto.OfficialAfterSalesQueryRequest;
import com.xiaoniu.aftermarket.official.dto.OfficialAfterSalesQueryResponse;
import com.xiaoniu.aftermarket.official.dto.OfficialAfterSalesResponse;
import com.xiaoniu.aftermarket.official.dto.SaveOfficialOrderInfoCommand;
import com.xiaoniu.aftermarket.official.service.OfficialAfterSalesService;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
public class OfficialAfterSalesController {

    private final OfficialAfterSalesService officialAfterSalesService;

    public OfficialAfterSalesController(OfficialAfterSalesService officialAfterSalesService) {
        this.officialAfterSalesService = officialAfterSalesService;
    }

    // ========== Global page query ==========

    @GetMapping("/api/admin/official-after-sales")
    public ApiResponse<PageResponse<OfficialAfterSalesQueryResponse>> listOfficialAfterSales(
            @RequestParam(required = false) String officialOrderNo,
            @RequestParam(required = false) String workOrderNo,
            @RequestParam(required = false) String settlementStatus,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(required = false) Integer pageNo,
            @RequestParam(required = false) Integer pageSize) {
        CurrentUser user = requireCurrentUser();
        LocalDateTime parsedStart;
        LocalDateTime parsedEnd;
        try {
            parsedStart = DateParamParser.parseStartDateTime(startTime);
            parsedEnd = DateParamParser.parseEndDateTime(endTime);
        } catch (IllegalArgumentException e) {
            return ApiResponse.failure(ErrorCode.COMMON_BAD_REQUEST, e.getMessage());
        }
        OfficialAfterSalesQueryRequest request = new OfficialAfterSalesQueryRequest();
        request.setStoreId(user.storeId());
        request.setOfficialOrderNo(officialOrderNo);
        request.setWorkOrderNo(workOrderNo);
        request.setSettlementStatus(settlementStatus);
        request.setSettlementStartTime(parsedStart);
        request.setSettlementEndTime(parsedEnd);
        request.setPageNo(pageNo);
        request.setPageSize(pageSize);
        return ApiResponse.success(officialAfterSalesService.pageQuery(request));
    }

    // ========== Work-order scoped ==========

    @GetMapping("/api/admin/work-orders/{workOrderId}/official-after-sales")
    public ApiResponse<OfficialAfterSalesResponse> getOfficialAfterSalesByWorkOrder(
            @PathVariable Long workOrderId) {
        CurrentUser user = requireCurrentUser();
        OfficialAfterSalesResponse response = officialAfterSalesService.getByWorkOrderId(user.storeId(), workOrderId);
        if (response.getStoreId() != null && !user.storeId().equals(response.getStoreId())) {
            throw new BusinessException(ErrorCode.OFFICIAL_AFTER_SALES_NOT_FOUND, "官方售后记录不存在");
        }
        return ApiResponse.success(response);
    }

    @PostMapping("/api/admin/work-orders/{workOrderId}/official-after-sales/order-info")
    @PreAuthorize("hasAuthority('OFFICIAL_SETTLEMENT_MANAGE')")
    public ApiResponse<Long> saveOfficialOrderInfo(
            @PathVariable Long workOrderId,
            @Valid @RequestBody SaveOfficialOrderInfoRequest request) {
        CurrentUser user = requireCurrentUser();
        SaveOfficialOrderInfoCommand command = new SaveOfficialOrderInfoCommand();
        command.setStoreId(user.storeId());
        command.setWorkOrderId(workOrderId);
        command.setOperatorId(user.userId());
        command.setOfficialOrderNo(request.getOfficialOrderNo());
        command.setRemark(request.getRemark());
        Long id = officialAfterSalesService.saveOfficialOrderInfo(command);
        return ApiResponse.success(id);
    }

    @PostMapping("/api/admin/work-orders/{workOrderId}/official-after-sales/settle")
    @PreAuthorize("hasAuthority('OFFICIAL_SETTLEMENT_MANAGE')")
    public ApiResponse<Void> markOfficialSettled(
            @PathVariable Long workOrderId,
            @Valid @RequestBody MarkOfficialSettledRequest request) {
        CurrentUser user = requireCurrentUser();
        MarkOfficialSettledCommand command = new MarkOfficialSettledCommand();
        command.setStoreId(user.storeId());
        command.setWorkOrderId(workOrderId);
        command.setOperatorId(user.userId());
        command.setSettlementAmount(request.getSettlementAmount());
        command.setSettlementTime(request.getSettlementTime());
        command.setRemark(request.getRemark());
        officialAfterSalesService.markOfficialSettled(command);
        return ApiResponse.success(null);
    }

    @PostMapping("/api/admin/work-orders/{workOrderId}/official-after-sales/no-settlement-required")
    @PreAuthorize("hasAuthority('OFFICIAL_SETTLEMENT_MANAGE')")
    public ApiResponse<Void> markNoSettlementRequired(
            @PathVariable Long workOrderId,
            @Valid @RequestBody MarkNoSettlementRequiredRequest request) {
        CurrentUser user = requireCurrentUser();
        MarkNoSettlementRequiredCommand command = new MarkNoSettlementRequiredCommand();
        command.setStoreId(user.storeId());
        command.setWorkOrderId(workOrderId);
        command.setOperatorId(user.userId());
        command.setReason(request.getReason());
        command.setRemark(request.getRemark());
        officialAfterSalesService.markNoSettlementRequired(command);
        return ApiResponse.success(null);
    }

    private CurrentUser requireCurrentUser() {
        return CurrentUserContext.get()
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "缺少用户上下文"));
    }
}
