package com.xiaoniu.aftermarket.part.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.xiaoniu.aftermarket.common.api.ErrorCode;
import com.xiaoniu.aftermarket.common.enums.CommonStatus;
import com.xiaoniu.aftermarket.common.enums.PartSource;
import com.xiaoniu.aftermarket.common.exception.BusinessException;
import com.xiaoniu.aftermarket.common.pagination.PageResponse;
import com.xiaoniu.aftermarket.part.dto.CreatePartCommand;
import com.xiaoniu.aftermarket.part.dto.PartQueryRequest;
import com.xiaoniu.aftermarket.part.dto.PartQueryResponse;
import com.xiaoniu.aftermarket.part.dto.UpdatePartCommand;
import com.xiaoniu.aftermarket.part.entity.PartEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
class PartServiceTest {

    private static final Long STORE_ID = 1L;
    private static final Long OPERATOR_ID = 1L;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

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
    void createOfficialPartSuccessfully() {
        CreatePartCommand command = buildOfficialCommand("官方刹车片", "OF-BRAKE-001");

        PartEntity result = partService.createOfficialPart(command);

        assertNotNull(result);
        assertNotNull(result.getId());
        assertEquals(STORE_ID, result.getStoreId());
        assertEquals("OF-BRAKE-001", result.getPartCode());
        assertEquals("OF-BRAKE-001", result.getOfficialPartNo());
        assertEquals("官方刹车片", result.getPartName());
        assertEquals(PartSource.OFFICIAL.getCode(), result.getSource());
        assertEquals(CommonStatus.ENABLED.getCode(), result.getStatus());
    }

    @Test
    void createThirdPartyPartSuccessfully() {
        CreatePartCommand command = buildThirdPartyCommand("第三方电池");

        PartEntity result = partService.createThirdPartyPart(command);

        assertNotNull(result);
        assertNotNull(result.getId());
        String today = LocalDate.now().format(DATE_FMT);
        assertTrue(result.getPartCode().startsWith("TP" + today));
        assertEquals("TP" + today + "0001", result.getPartCode());
        assertEquals("第三方电池", result.getPartName());
        assertEquals(PartSource.THIRD_PARTY.getCode(), result.getSource());
        assertEquals(CommonStatus.ENABLED.getCode(), result.getStatus());
    }

    @Test
    void createThirdPartyPartCodeUsesSequence() {
        PartEntity first = partService.createThirdPartyPart(buildThirdPartyCommand("电池A"));
        PartEntity second = partService.createThirdPartyPart(buildThirdPartyCommand("电池B"));

        String today = LocalDate.now().format(DATE_FMT);
        assertEquals("TP" + today + "0001", first.getPartCode());
        assertEquals("TP" + today + "0002", second.getPartCode());
    }

    @Test
    void createPartWithDuplicateCodeFails() {
        CreatePartCommand cmd1 = buildOfficialCommand("刹车片A", "DUP-CODE-001");
        partService.createOfficialPart(cmd1);

        CreatePartCommand cmd2 = buildOfficialCommand("刹车片B", "DUP-CODE-001");
        BusinessException ex = assertThrows(BusinessException.class,
                () -> partService.createOfficialPart(cmd2));
        assertEquals(ErrorCode.PART_CODE_DUPLICATE, ex.getErrorCode());
    }

    @Test
    void createPartWithoutNameFails() {
        CreatePartCommand command = new CreatePartCommand();
        command.setStoreId(STORE_ID);
        command.setOperatorId(OPERATOR_ID);
        command.setPartName(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> partService.createOfficialPart(command));
        assertEquals(ErrorCode.PART_NAME_REQUIRED, ex.getErrorCode());
    }

    @Test
    void createOfficialPartWithoutOfficialNoFails() {
        CreatePartCommand command = new CreatePartCommand();
        command.setStoreId(STORE_ID);
        command.setOperatorId(OPERATOR_ID);
        command.setPartName("无品号配件");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> partService.createOfficialPart(command));
        assertEquals(ErrorCode.PART_OFFICIAL_CODE_REQUIRED, ex.getErrorCode());
    }

    @Test
    void disablePartSuccessfully() {
        PartEntity part = partService.createOfficialPart(buildOfficialCommand("刹车片", "DIS-001"));
        assertEquals(CommonStatus.ENABLED.getCode(), part.getStatus());

        partService.disablePart(part.getId());

        PartEntity updated = partService.getById(part.getId());
        assertEquals(CommonStatus.DISABLED.getCode(), updated.getStatus());
    }

    @Test
    void enablePartSuccessfully() {
        PartEntity part = partService.createOfficialPart(buildOfficialCommand("刹车片", "ENA-001"));
        partService.disablePart(part.getId());
        partService.enablePart(part.getId());

        PartEntity updated = partService.getById(part.getId());
        assertEquals(CommonStatus.ENABLED.getCode(), updated.getStatus());
    }

    @Test
    void disableNonExistentPartFails() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> partService.disablePart(99999L));
        assertEquals(ErrorCode.PART_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void updatePartSuccessfully() {
        PartEntity part = partService.createOfficialPart(buildOfficialCommand("原始名称", "UPD-001"));

        UpdatePartCommand command = new UpdatePartCommand();
        command.setPartId(part.getId());
        command.setPartName("更新后名称");
        command.setModel("V2");
        partService.updatePart(command);

        PartEntity updated = partService.getById(part.getId());
        assertEquals("更新后名称", updated.getPartName());
        assertEquals("V2", updated.getModel());
        assertEquals("UPD-001", updated.getPartCode());
    }

    @Test
    void getByPartCodeSuccessfully() {
        partService.createOfficialPart(buildOfficialCommand("刹车片", "FIND-001"));

        PartEntity found = partService.getByPartCode(STORE_ID, "FIND-001");
        assertNotNull(found);
        assertEquals("FIND-001", found.getPartCode());
    }

    @Test
    void pageQuerySuccessfully() {
        partService.createOfficialPart(buildOfficialCommand("刹车片A", "PG-001"));
        partService.createOfficialPart(buildOfficialCommand("刹车片B", "PG-002"));
        partService.createThirdPartyPart(buildThirdPartyCommand("电池C"));

        PartQueryRequest request = new PartQueryRequest();
        request.setStoreId(STORE_ID);
        request.setPageNo(1);
        request.setPageSize(10);

        PageResponse<PartQueryResponse> response = partService.pageQuery(request);
        assertEquals(3, response.total());
        assertEquals(3, response.records().size());
    }

    @Test
    void pageQueryWithSourceFilter() {
        partService.createOfficialPart(buildOfficialCommand("官方A", "FILT-001"));
        partService.createOfficialPart(buildOfficialCommand("官方B", "FILT-002"));
        partService.createThirdPartyPart(buildThirdPartyCommand("三方A"));

        PartQueryRequest request = new PartQueryRequest();
        request.setStoreId(STORE_ID);
        request.setSource(PartSource.THIRD_PARTY.getCode());

        PageResponse<PartQueryResponse> response = partService.pageQuery(request);
        assertEquals(1, response.total());
        assertEquals(PartSource.THIRD_PARTY.getCode(), response.records().get(0).getSource());
    }

    private CreatePartCommand buildOfficialCommand(String name, String officialPartNo) {
        CreatePartCommand command = new CreatePartCommand();
        command.setStoreId(STORE_ID);
        command.setOperatorId(OPERATOR_ID);
        command.setPartName(name);
        command.setOfficialPartNo(officialPartNo);
        command.setReferenceCostPrice(new BigDecimal("10.50"));
        return command;
    }

    private CreatePartCommand buildThirdPartyCommand(String name) {
        CreatePartCommand command = new CreatePartCommand();
        command.setStoreId(STORE_ID);
        command.setOperatorId(OPERATOR_ID);
        command.setPartName(name);
        command.setReferenceCostPrice(new BigDecimal("5.00"));
        return command;
    }
}
