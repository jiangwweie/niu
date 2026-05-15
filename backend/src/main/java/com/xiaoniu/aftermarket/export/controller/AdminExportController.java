package com.xiaoniu.aftermarket.export.controller;

import com.xiaoniu.aftermarket.common.api.ErrorCode;
import com.xiaoniu.aftermarket.common.context.CurrentUser;
import com.xiaoniu.aftermarket.common.context.CurrentUserContext;
import com.xiaoniu.aftermarket.common.exception.BusinessException;
import com.xiaoniu.aftermarket.common.util.DateParamParser;
import com.xiaoniu.aftermarket.export.dto.ExportFile;
import com.xiaoniu.aftermarket.export.service.ExportService;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/exports")
public class AdminExportController {

    private static final MediaType XLSX_MEDIA_TYPE = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final ExportService exportService;

    public AdminExportController(ExportService exportService) {
        this.exportService = exportService;
    }

    @GetMapping("/finance")
    @PreAuthorize("hasAuthority('EXCEL_EXPORT')")
    public ResponseEntity<byte[]> exportFinance(
            @RequestParam String reportType,
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String month,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        CurrentUser user = requireCurrentUser();
        String normalizedType = reportType == null ? "" : reportType.trim().toUpperCase();
        ExportFile file = switch (normalizedType) {
            case "DAILY" -> exportService.exportFinanceDaily(user.storeId(),
                    parseDateOrDefault(date, LocalDate.now(), "date"));
            case "MONTHLY" -> exportService.exportFinanceMonthly(user.storeId(),
                    parseMonthRequired(month));
            case "RANGE" -> {
                LocalDate start = parseDateRequired(startDate, "startDate");
                LocalDate end = parseDateRequired(endDate, "endDate");
                if (start.isAfter(end)) {
                    throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "开始日期不能晚于结束日期");
                }
                yield exportService.exportFinanceRange(user.storeId(), start, end);
            }
            default -> throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "reportType 必须为 DAILY / MONTHLY / RANGE");
        };
        return toDownloadResponse(file);
    }

    @GetMapping("/reimbursements")
    @PreAuthorize("hasAuthority('EXCEL_EXPORT')")
    public ResponseEntity<byte[]> exportReimbursements(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long applicantId,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo) {
        CurrentUser user = requireCurrentUser();
        LocalDateTime parsedFrom = DateParamParser.parseStartDateTime(dateFrom);
        LocalDateTime parsedTo = DateParamParser.parseEndDateTime(dateTo);
        ExportFile file = exportService.exportReimbursements(user.storeId(), status, applicantId, parsedFrom, parsedTo);
        return toDownloadResponse(file);
    }

    private ResponseEntity<byte[]> toDownloadResponse(ExportFile file) {
        String encodedFilename = URLEncoder.encode(file.filename(), StandardCharsets.UTF_8)
                .replace("+", "%20");
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(encodedFilename, StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(XLSX_MEDIA_TYPE)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(file.content());
    }

    private CurrentUser requireCurrentUser() {
        return CurrentUserContext.get()
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "缺少用户上下文"));
    }

    private LocalDate parseDateOrDefault(String value, LocalDate defaultValue, String paramName) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return parseDateRequired(value, paramName);
    }

    private LocalDate parseDateRequired(String value, String paramName) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, paramName + " 不能为空");
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (RuntimeException e) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, paramName + " 日期格式必须为 yyyy-MM-dd");
        }
    }

    private YearMonth parseMonthRequired(String value) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "month 不能为空");
        }
        try {
            return YearMonth.parse(value.trim());
        } catch (RuntimeException e) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "month 格式必须为 yyyy-MM");
        }
    }
}
