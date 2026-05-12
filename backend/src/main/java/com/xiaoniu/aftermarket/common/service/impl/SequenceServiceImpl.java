package com.xiaoniu.aftermarket.common.service.impl;

import com.xiaoniu.aftermarket.common.api.ErrorCode;
import com.xiaoniu.aftermarket.common.exception.BusinessException;
import com.xiaoniu.aftermarket.common.service.SequenceService;
import java.sql.Date;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SequenceServiceImpl implements SequenceService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private static final Map<String, String> PREFIX_MAP = Map.of(
            "WORK_ORDER", "WO",
            "PAYMENT", "PAY",
            "REFUND", "REF",
            "REIMBURSEMENT", "REIM",
            "PART_CODE", "TP"
    );

    private final JdbcTemplate jdbcTemplate;

    public SequenceServiceImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public String next(String seqType) {
        String prefix = PREFIX_MAP.get(seqType);
        if (prefix == null) {
            throw new BusinessException(ErrorCode.SEQUENCE_TYPE_UNKNOWN, "未知编号类型: " + seqType);
        }

        LocalDate today = LocalDate.now();
        String dateStr = today.format(DATE_FMT);
        Date sqlDate = Date.valueOf(today);

        int maxRetries = 5;
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            try {
                return doNext(seqType, prefix, dateStr, sqlDate);
            } catch (DataIntegrityViolationException e) {
                // Concurrent insert race: another thread inserted the row between our SELECT and INSERT.
                // Roll back and retry within the same transaction.
                if (attempt == maxRetries - 1) {
                    throw e;
                }
            }
        }
        throw new BusinessException(ErrorCode.COMMON_INTERNAL_ERROR, "序列号生成重试失败");
    }

    private String doNext(String seqType, String prefix, String dateStr, Date sqlDate) {
        List<Long> results = jdbcTemplate.queryForList(
                "SELECT current_val FROM sequence_daily WHERE seq_type = ? AND seq_date = ? FOR UPDATE",
                Long.class,
                seqType,
                sqlDate
        );

        long nextVal;
        if (!results.isEmpty()) {
            nextVal = results.get(0) + 1;
            jdbcTemplate.update(
                    "UPDATE sequence_daily SET current_val = ? WHERE seq_type = ? AND seq_date = ?",
                    nextVal,
                    seqType,
                    sqlDate
            );
        } else {
            nextVal = 1;
            jdbcTemplate.update(
                    "INSERT INTO sequence_daily (seq_type, seq_date, current_val) VALUES (?, ?, ?)",
                    seqType,
                    sqlDate,
                    nextVal
            );
        }

        return prefix + dateStr + String.format("%04d", nextVal);
    }
}
