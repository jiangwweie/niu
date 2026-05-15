package com.xiaoniu.aftermarket.part.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.xiaoniu.aftermarket.common.enums.CommonStatus;
import com.xiaoniu.aftermarket.common.exception.BusinessException;
import com.xiaoniu.aftermarket.common.api.ErrorCode;
import com.xiaoniu.aftermarket.part.dto.CreatePartCommand;
import com.xiaoniu.aftermarket.part.entity.PartEntity;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
class PartEnableDisableStoreIsolationTest {

    private static final Long STORE_ID = 1L;
    private static final Long OTHER_STORE_ID = 99L;
    private static final Long OPERATOR_ID = 1L;

    @Autowired
    private PartService partService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanPartTables() {
        jdbcTemplate.execute("DELETE FROM part_barcode");
        jdbcTemplate.execute("DELETE FROM part");
        jdbcTemplate.execute("DELETE FROM sequence_daily");
    }

    @Test
    void enablePart_sameStore_succeeds() {
        PartEntity part = createOfficialPart("启用测试配件", "ENA-ISO-001");
        partService.disablePart(STORE_ID, part.getId());

        partService.enablePart(STORE_ID, part.getId());

        PartEntity updated = partService.getById(part.getId());
        assertEquals(CommonStatus.ENABLED.getCode(), updated.getStatus());
    }

    @Test
    void disablePart_sameStore_succeeds() {
        PartEntity part = createOfficialPart("停用测试配件", "DIS-ISO-001");

        partService.disablePart(STORE_ID, part.getId());

        PartEntity updated = partService.getById(part.getId());
        assertEquals(CommonStatus.DISABLED.getCode(), updated.getStatus());
    }

    @Test
    void enablePart_crossStore_fails() {
        PartEntity part = createOfficialPart("跨门店启用测试", "ENA-ISO-002");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> partService.enablePart(OTHER_STORE_ID, part.getId()));
        assertEquals(ErrorCode.COMMON_BAD_REQUEST, ex.getErrorCode());
    }

    @Test
    void disablePart_crossStore_fails() {
        PartEntity part = createOfficialPart("跨门店停用测试", "DIS-ISO-002");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> partService.disablePart(OTHER_STORE_ID, part.getId()));
        assertEquals(ErrorCode.COMMON_BAD_REQUEST, ex.getErrorCode());
    }

    @Test
    void enablePart_nonExistent_fails() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> partService.enablePart(STORE_ID, 99999L));
        assertEquals(ErrorCode.PART_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void disablePart_nonExistent_fails() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> partService.disablePart(STORE_ID, 99999L));
        assertEquals(ErrorCode.PART_NOT_FOUND, ex.getErrorCode());
    }

    private PartEntity createOfficialPart(String name, String officialPartNo) {
        CreatePartCommand command = new CreatePartCommand();
        command.setStoreId(STORE_ID);
        command.setOperatorId(OPERATOR_ID);
        command.setPartName(name);
        command.setOfficialPartNo(officialPartNo);
        command.setReferenceCostPrice(new BigDecimal("10.00"));
        return partService.createOfficialPart(command);
    }
}