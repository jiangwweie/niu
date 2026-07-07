package com.xiaoniu.aftermarket.export.controller;

import com.xiaoniu.aftermarket.common.api.ErrorCode;
import com.xiaoniu.aftermarket.common.api.ApiResponse;
import com.xiaoniu.aftermarket.common.context.CurrentUser;
import com.xiaoniu.aftermarket.common.context.CurrentUserContext;
import com.xiaoniu.aftermarket.common.exception.BusinessException;
import com.xiaoniu.aftermarket.common.util.DateParamParser;
import com.xiaoniu.aftermarket.export.dto.ExportFile;
import com.xiaoniu.aftermarket.export.service.ExportService;
import com.xiaoniu.aftermarket.importexport.dto.ImportProcessResult;
import com.xiaoniu.aftermarket.importexport.service.AdminImportExportService;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/exports")
public class AdminExportController {

    private static final MediaType XLSX_MEDIA_TYPE = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final ExportService exportService;
    private final AdminImportExportService importExportService;

    public AdminExportController(ExportService exportService,
                                 AdminImportExportService importExportService) {
        this.exportService = exportService;
        this.importExportService = importExportService;
    }

    /**
     * Excel 导出应与页面查询条件一致，复用 FinanceService 的查询逻辑
     * 权限控制：EXCEL_EXPORT
     */
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
                // 日期范围校验：开始日期不能晚于结束日期
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
            @RequestParam(required = false) String reimbursementNo,
            @RequestParam(required = false) String applicantName,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo) {
        CurrentUser user = requireCurrentUser();
        LocalDateTime parsedFrom = DateParamParser.parseStartDateTime(dateFrom);
        LocalDateTime parsedTo = DateParamParser.parseEndDateTime(dateTo);
        ExportFile file = exportService.exportReimbursements(
                user.storeId(), status, applicantId, reimbursementNo, applicantName, parsedFrom, parsedTo);
        return toDownloadResponse(file);
    }

    @GetMapping("/customers")
    @PreAuthorize("hasAuthority('EXCEL_EXPORT') and hasAuthority('CUSTOMER_VIEW')")
    public ResponseEntity<byte[]> exportCustomers(@RequestParam(required = false) String customerName,
                                                  @RequestParam(required = false) String phone) {
        CurrentUser user = requireCurrentUser();
        return toDownloadResponse(importExportService.exportCustomers(user.storeId(), customerName, phone));
    }

    @GetMapping("/parts")
    @PreAuthorize("hasAuthority('EXCEL_EXPORT') and hasAuthority('PART_VIEW')")
    public ResponseEntity<byte[]> exportParts(@RequestParam(required = false) String partCode,
                                              @RequestParam(required = false) String partName,
                                              @RequestParam(required = false) String officialPartNo,
                                              @RequestParam(required = false) String barcode,
                                              @RequestParam(required = false) String model,
                                              @RequestParam(required = false) String categoryCode,
                                              @RequestParam(required = false) String source,
                                              @RequestParam(required = false) Boolean enabled) {
        CurrentUser user = requireCurrentUser();
        return toDownloadResponse(importExportService.exportParts(
                user.storeId(), partCode, partName, officialPartNo, barcode, model, categoryCode, source, enabled));
    }

    @GetMapping("/templates/customers")
    @PreAuthorize("hasAuthority('CUSTOMER_MANAGE')")
    public ResponseEntity<byte[]> customerTemplate() {
        return toDownloadResponse(importExportService.customerTemplate());
    }

    @GetMapping("/templates/parts")
    @PreAuthorize("hasAuthority('PART_MANAGE')")
    public ResponseEntity<byte[]> partTemplate() {
        return toDownloadResponse(importExportService.partTemplate());
    }

    @PostMapping("/imports/customers")
    @PreAuthorize("hasAuthority('CUSTOMER_MANAGE')")
    public ResponseEntity<?> importCustomers(@RequestParam MultipartFile file) {
        CurrentUser user = requireCurrentUser();
        ImportProcessResult result = importExportService.importCustomers(user.storeId(), user.userId(), file);
        return result.success() ? ResponseEntity.ok(ApiResponse.success(result.summary()))
                : toDownloadResponse(result.errorFile());
    }

    @PostMapping("/imports/parts")
    @PreAuthorize("hasAuthority('PART_MANAGE')")
    public ResponseEntity<?> importParts(@RequestParam MultipartFile file) {
        CurrentUser user = requireCurrentUser();
        ImportProcessResult result = importExportService.importParts(user.storeId(), user.userId(), file);
        return result.success() ? ResponseEntity.ok(ApiResponse.success(result.summary()))
                : toDownloadResponse(result.errorFile());
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
