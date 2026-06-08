package com.xiaoniu.aftermarket.export.service.impl;

import static com.xiaoniu.aftermarket.common.util.SearchKeywordUtils.buildContainsPattern;
import static com.xiaoniu.aftermarket.common.util.SearchKeywordUtils.containsCondition;
import static com.xiaoniu.aftermarket.common.util.SearchKeywordUtils.normalize;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xiaoniu.aftermarket.common.api.ErrorCode;
import com.xiaoniu.aftermarket.common.enums.ReimbursementStatus;
import com.xiaoniu.aftermarket.common.exception.BusinessException;
import com.xiaoniu.aftermarket.export.dto.ExportFile;
import com.xiaoniu.aftermarket.export.service.ExportService;
import com.xiaoniu.aftermarket.finance.dto.FinanceReportResponse;
import com.xiaoniu.aftermarket.finance.service.FinanceService;
import com.xiaoniu.aftermarket.reimbursement.entity.ReimbursementEntity;
import com.xiaoniu.aftermarket.reimbursement.mapper.ReimbursementMapper;
import com.xiaoniu.aftermarket.user.entity.SysUserEntity;
import com.xiaoniu.aftermarket.user.mapper.SysUserMapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ExportServiceImpl implements ExportService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Set<String> REIMBURSEMENT_STATUS_CODES = Arrays.stream(ReimbursementStatus.values())
            .map(ReimbursementStatus::getCode)
            .collect(Collectors.toUnmodifiableSet());

    private final FinanceService financeService;
    private final ReimbursementMapper reimbursementMapper;
    private final SysUserMapper userMapper;

    public ExportServiceImpl(FinanceService financeService,
                             ReimbursementMapper reimbursementMapper,
                             SysUserMapper userMapper) {
        this.financeService = financeService;
        this.reimbursementMapper = reimbursementMapper;
        this.userMapper = userMapper;
    }

    /**
     * Excel 导出与页面查询条件一致，复用 FinanceService 的查询逻辑
     */
    @Override
    public ExportFile exportFinanceDaily(Long storeId, LocalDate date) {
        FinanceReportResponse report = financeService.queryDaily(storeId, date);
        return exportFinanceReport(report, "finance_daily_" + DATE_FORMATTER.format(date) + ".xlsx", "财务日报");
    }

    @Override
    public ExportFile exportFinanceMonthly(Long storeId, YearMonth month) {
        FinanceReportResponse report = financeService.queryMonthly(storeId, month.getYear(), month.getMonthValue());
        return exportFinanceReport(report, "finance_monthly_" + month + ".xlsx", "财务月报");
    }

    @Override
    public ExportFile exportFinanceRange(Long storeId, LocalDate startDate, LocalDate endDate) {
        FinanceReportResponse report = financeService.queryRange(storeId, startDate, endDate);
        return exportFinanceReport(report,
                "finance_range_" + DATE_FORMATTER.format(startDate) + "_" + DATE_FORMATTER.format(endDate) + ".xlsx",
                "财务区间报表");
    }

    @Override
    public ExportFile exportReimbursements(Long storeId, String status, Long applicantId,
                                           String reimbursementNo, String applicantName,
                                           LocalDateTime dateFrom, LocalDateTime dateTo) {
        if (storeId == null) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST);
        }
        if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "开始日期不能晚于结束日期");
        }
        // 报销状态校验：status 必须是有效的 ReimbursementStatus 枚举值
        String normalizedStatus = normalizeStatus(status);
        QueryWrapper<ReimbursementEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("store_id", storeId).eq("deleted", 0);
        if (StringUtils.hasText(normalizedStatus)) {
            wrapper.eq("status", normalizedStatus);
        }
        if (applicantId != null) {
            wrapper.eq("applicant_id", applicantId);
        }
        String normalizedReimbursementNo = normalize(reimbursementNo);
        if (normalizedReimbursementNo != null) {
            wrapper.apply(containsCondition("reimbursement_no"), buildContainsPattern(normalizedReimbursementNo));
        }
        String normalizedApplicantName = normalize(applicantName);
        if (normalizedApplicantName != null) {
            List<Long> userIds = userMapper.selectList(
                    new QueryWrapper<SysUserEntity>()
                            .eq("store_id", storeId)
                            .eq("deleted", 0)
                            .apply(containsCondition("real_name"), buildContainsPattern(normalizedApplicantName))
                            .select("id"))
                    .stream()
                    .map(SysUserEntity::getId)
                    .toList();
            if (userIds.isEmpty()) {
                return exportReimbursementLedger(List.of(), "reimbursements_" + buildRangeForFilename(dateFrom, dateTo) + ".xlsx");
            }
            wrapper.in("applicant_id", userIds);
        }
        if (dateFrom != null) {
            wrapper.ge("submitted_at", dateFrom);
        }
        if (dateTo != null) {
            wrapper.le("submitted_at", dateTo);
        }
        wrapper.orderByDesc("submitted_at").orderByDesc("id");

        List<ReimbursementEntity> reimbursements = reimbursementMapper.selectList(wrapper);
        String range = buildRangeForFilename(dateFrom, dateTo);
        return exportReimbursementLedger(reimbursements, "reimbursements_" + range + ".xlsx");
    }

    private ExportFile exportFinanceReport(FinanceReportResponse report, String filename, String sheetName) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(sheetName);
            CellStyle moneyStyle = moneyStyle(workbook);
            writeHeader(sheet.createRow(0), List.of("指标", "值"));
            int rowIndex = 1;
            rowIndex = writeTextMetric(sheet, rowIndex, "期间开始", formatDate(report.getPeriodStart()));
            rowIndex = writeTextMetric(sheet, rowIndex, "期间结束", formatDate(report.getPeriodEnd()));
            // 客户支付净收入 = 收款 - 退款
            rowIndex = writeMoneyMetric(sheet, rowIndex, "customerIncome", report.getCustomerIncome(), moneyStyle);
            // 官方结算收入单独列示
            rowIndex = writeMoneyMetric(sheet, rowIndex, "officialIncome", report.getOfficialIncome(), moneyStyle);
            // 配件成本来自结算消耗时记录的成本口径
            rowIndex = writeMoneyMetric(sheet, rowIndex, "partsCost", report.getPartsCost(), moneyStyle);
            // 报销成本：仅 CONFIRMED 状态的报销计入成本
            rowIndex = writeMoneyMetric(sheet, rowIndex, "reimbursementCost", report.getReimbursementCost(), moneyStyle);
            rowIndex = writeMoneyMetric(sheet, rowIndex, "totalIncome", report.getTotalIncome(), moneyStyle);
            rowIndex = writeMoneyMetric(sheet, rowIndex, "totalCost", report.getTotalCost(), moneyStyle);
            // 利润 = 总收入 - 总成本，不允许手动直接改利润
            rowIndex = writeMoneyMetric(sheet, rowIndex, "profit", report.getProfit(), moneyStyle);
            rowIndex = writeNumberMetric(sheet, rowIndex, "settledWorkOrderCount", report.getSettledWorkOrderCount());
            writeNumberMetric(sheet, rowIndex, "confirmedReimbursementCount", report.getConfirmedReimbursementCount());
            autosize(sheet, 2);
            return new ExportFile(filename, toBytes(workbook));
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.COMMON_INTERNAL_ERROR, "导出财务报表失败");
        }
    }

    private ExportFile exportReimbursementLedger(List<ReimbursementEntity> records, String filename) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("报销台账");
            CellStyle moneyStyle = moneyStyle(workbook);
            writeHeader(sheet.createRow(0), List.of(
                    "报销编号", "报销ID", "报销人ID", "用途", "申请金额", "状态", "备注", "提交时间",
                    "确认金额", "确认人", "确认时间", "驳回人", "驳回时间", "驳回原因", "是否计入成本"
            ));
            int rowIndex = 1;
            for (ReimbursementEntity entity : records) {
                Row row = sheet.createRow(rowIndex++);
                writeText(row, 0, entity.getReimbursementNo());
                writeNumber(row, 1, entity.getId());
                writeNumber(row, 2, entity.getApplicantId());
                writeText(row, 3, entity.getPurpose());
                writeMoney(row, 4, entity.getAmount(), moneyStyle);
                writeText(row, 5, entity.getStatus());
                writeText(row, 6, entity.getRemark());
                writeText(row, 7, formatDateTime(entity.getSubmittedAt()));
                writeMoney(row, 8, entity.getConfirmedAmount(), moneyStyle);
                writeNumber(row, 9, entity.getConfirmedBy());
                writeText(row, 10, formatDateTime(entity.getConfirmedAt()));
                writeNumber(row, 11, entity.getRejectedBy());
                writeText(row, 12, formatDateTime(entity.getRejectedAt()));
                writeText(row, 13, entity.getRejectReason());
                // 报销成本口径：只有 CONFIRMED 状态的报销才计入成本
                writeText(row, 14, ReimbursementStatus.CONFIRMED.getCode().equals(entity.getStatus()) ? "是" : "否");
            }
            autosize(sheet, 15);
            return new ExportFile(filename, toBytes(workbook));
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.COMMON_INTERNAL_ERROR, "导出报销台账失败");
        }
    }

    private String normalizeStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        String normalized = status.trim().toUpperCase();
        if (!REIMBURSEMENT_STATUS_CODES.contains(normalized)) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "报销状态无效");
        }
        return normalized;
    }

    private String buildRangeForFilename(LocalDateTime dateFrom, LocalDateTime dateTo) {
        if (dateFrom == null && dateTo == null) {
            return "all";
        }
        String from = dateFrom == null ? "start" : DATE_FORMATTER.format(dateFrom.toLocalDate());
        String to = dateTo == null ? "end" : DATE_FORMATTER.format(dateTo.toLocalDate());
        return from + "_" + to;
    }

    private void writeHeader(Row row, List<String> headers) {
        for (int i = 0; i < headers.size(); i++) {
            writeText(row, i, headers.get(i));
        }
    }

    private int writeTextMetric(Sheet sheet, int rowIndex, String name, String value) {
        Row row = sheet.createRow(rowIndex);
        writeText(row, 0, name);
        writeText(row, 1, value);
        return rowIndex + 1;
    }

    private int writeMoneyMetric(Sheet sheet, int rowIndex, String name, BigDecimal value, CellStyle style) {
        Row row = sheet.createRow(rowIndex);
        writeText(row, 0, name);
        writeMoney(row, 1, value, style);
        return rowIndex + 1;
    }

    private int writeNumberMetric(Sheet sheet, int rowIndex, String name, Integer value) {
        Row row = sheet.createRow(rowIndex);
        writeText(row, 0, name);
        writeNumber(row, 1, value == null ? 0 : value.longValue());
        return rowIndex + 1;
    }

    private void writeText(Row row, int column, String value) {
        row.createCell(column).setCellValue(value == null ? "" : value);
    }

    private void writeMoney(Row row, int column, BigDecimal value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value == null ? 0D : value.doubleValue());
        cell.setCellStyle(style);
    }

    private void writeNumber(Row row, int column, Long value) {
        Cell cell = row.createCell(column);
        if (value != null) {
            cell.setCellValue(value);
        }
    }

    private CellStyle moneyStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        DataFormat dataFormat = workbook.createDataFormat();
        style.setDataFormat(dataFormat.getFormat("0.00"));
        return style;
    }

    private void autosize(Sheet sheet, int columnCount) {
        for (int i = 0; i < columnCount; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private String formatDate(LocalDate value) {
        return value == null ? "" : DATE_FORMATTER.format(value);
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? "" : DATE_TIME_FORMATTER.format(value);
    }

    private byte[] toBytes(Workbook workbook) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        return outputStream.toByteArray();
    }
}
