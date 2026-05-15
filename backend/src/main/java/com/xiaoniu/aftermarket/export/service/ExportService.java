package com.xiaoniu.aftermarket.export.service;

import com.xiaoniu.aftermarket.export.dto.ExportFile;
import java.time.LocalDate;
import java.time.YearMonth;

public interface ExportService {

    ExportFile exportFinanceDaily(Long storeId, LocalDate date);

    ExportFile exportFinanceMonthly(Long storeId, YearMonth month);

    ExportFile exportFinanceRange(Long storeId, LocalDate startDate, LocalDate endDate);

    ExportFile exportReimbursements(Long storeId, String status, Long applicantId,
                                    java.time.LocalDateTime dateFrom,
                                    java.time.LocalDateTime dateTo);
}
