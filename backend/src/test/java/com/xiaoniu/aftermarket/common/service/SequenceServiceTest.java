package com.xiaoniu.aftermarket.common.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.xiaoniu.aftermarket.common.exception.BusinessException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
class SequenceServiceTest {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Autowired
    private SequenceService sequenceService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanSequenceTable() {
        jdbcTemplate.execute("DELETE FROM sequence_daily");
    }

    @Test
    void workOrderFormatIsCorrect() {
        String result = sequenceService.next("WORK_ORDER");
        String today = LocalDate.now().format(DATE_FMT);
        assertTrue(result.startsWith("WO" + today));
        assertEquals(14, result.length());
        assertEquals("WO" + today + "0001", result);
    }

    @Test
    void paymentFormatIsCorrect() {
        String result = sequenceService.next("PAYMENT");
        String today = LocalDate.now().format(DATE_FMT);
        assertEquals("PAY" + today + "0001", result);
    }

    @Test
    void refundFormatIsCorrect() {
        String result = sequenceService.next("REFUND");
        String today = LocalDate.now().format(DATE_FMT);
        assertEquals("REF" + today + "0001", result);
    }

    @Test
    void reimbursementFormatIsCorrect() {
        String result = sequenceService.next("REIMBURSEMENT");
        String today = LocalDate.now().format(DATE_FMT);
        assertEquals("REIM" + today + "0001", result);
    }

    @Test
    void partCodeFormatIsCorrect() {
        String result = sequenceService.next("PART_CODE");
        String today = LocalDate.now().format(DATE_FMT);
        assertEquals("TP" + today + "0001", result);
    }

    @Test
    void consecutiveCallsIncrementSequence() {
        String first = sequenceService.next("WORK_ORDER");
        String second = sequenceService.next("WORK_ORDER");
        String third = sequenceService.next("WORK_ORDER");

        assertEquals("WO" + LocalDate.now().format(DATE_FMT) + "0001", first);
        assertEquals("WO" + LocalDate.now().format(DATE_FMT) + "0002", second);
        assertEquals("WO" + LocalDate.now().format(DATE_FMT) + "0003", third);
    }

    @Test
    void differentTypesMaintainIndependentSequences() {
        String wo = sequenceService.next("WORK_ORDER");
        String pay = sequenceService.next("PAYMENT");
        String wo2 = sequenceService.next("WORK_ORDER");

        String today = LocalDate.now().format(DATE_FMT);
        assertEquals("WO" + today + "0001", wo);
        assertEquals("PAY" + today + "0001", pay);
        assertEquals("WO" + today + "0002", wo2);
    }

    @Test
    void unknownSeqTypeThrowsBusinessException() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> sequenceService.next("UNKNOWN_TYPE"));
        assertEquals("未知编号类型: UNKNOWN_TYPE", ex.getMessage());
    }

    @Test
    void concurrentGenerationProducesNoDuplicates() throws Exception {
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        List<String> results = Collections.synchronizedList(new ArrayList<>());
        AtomicBoolean errorOccurred = new AtomicBoolean(false);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    String seq = sequenceService.next("PAYMENT");
                    synchronized (results) {
                        results.add(seq);
                    }
                } catch (Exception e) {
                    errorOccurred.set(true);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await();
        executor.shutdown();

        assertEquals(threadCount, results.size(), "Expected " + threadCount + " sequences");
        assertEquals(threadCount, results.stream().distinct().count(), "All sequences must be unique");

        String today = LocalDate.now().format(DATE_FMT);
        String prefix = "PAY" + today;
        assertTrue(results.stream().allMatch(s -> s.startsWith(prefix)),
                "All sequences should have correct prefix");
    }
}
