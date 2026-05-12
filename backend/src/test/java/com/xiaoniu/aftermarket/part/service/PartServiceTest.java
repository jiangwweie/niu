package com.xiaoniu.aftermarket.part.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import com.xiaoniu.aftermarket.part.entity.PartBarcodeEntity;
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
    private static final Long OTHER_STORE_ID = 2L;
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
        assertNull(result.getDefaultBarcode());
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

    // --- Barcode consistency tests (Fix #1 and #3) ---

    @Test
    void createBarcodeSuccessfully() {
        PartEntity part = partService.createOfficialPart(buildOfficialCommand("刹车片", "BC-001"));

        PartBarcodeEntity barcode = partService.createBarcode(
                STORE_ID, part.getId(), "BARCODE-001", OPERATOR_ID);

        assertNotNull(barcode);
        assertEquals("BARCODE-001", barcode.getBarcode());
        assertTrue(barcode.getPrimaryBarcode());

        PartEntity found = partService.getByBarcode(STORE_ID, "BARCODE-001");
        assertNotNull(found);
        assertEquals(part.getId(), found.getId());
    }

    @Test
    void createBarcodeSetsDefaultBarcodeOnPart() {
        PartEntity part = partService.createOfficialPart(buildOfficialCommand("刹车片", "BC-002"));
        assertNull(part.getDefaultBarcode());

        partService.createBarcode(STORE_ID, part.getId(), "BARCODE-002", OPERATOR_ID);

        PartEntity updated = partService.getById(part.getId());
        assertEquals("BARCODE-002", updated.getDefaultBarcode());
    }

    @Test
    void createBarcodeDuplicateFailsAndPartStillExists() {
        PartEntity part1 = partService.createOfficialPart(buildOfficialCommand("刹车片A", "BC-DUP-001"));
        partService.createBarcode(STORE_ID, part1.getId(), "DUP-BC", OPERATOR_ID);

        PartEntity part2 = partService.createOfficialPart(buildOfficialCommand("刹车片B", "BC-DUP-002"));

        assertThrows(BusinessException.class,
                () -> partService.createBarcode(STORE_ID, part2.getId(), "DUP-BC", OPERATOR_ID));

        PartEntity checkPart2 = partService.getById(part2.getId());
        assertNotNull(checkPart2, "part2 should still exist after barcode failure");
    }

    @Test
    void createBarcodeForNonExistentPartFails() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> partService.createBarcode(STORE_ID, 99999L, "BC-NO-PART", OPERATOR_ID));
        assertEquals(ErrorCode.PART_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void createSecondBarcodeIsNotPrimary() {
        PartEntity part = partService.createOfficialPart(buildOfficialCommand("刹车片", "BC-2ND"));

        partService.createBarcode(STORE_ID, part.getId(), "PRIMARY-BC", OPERATOR_ID);
        PartBarcodeEntity second = partService.createBarcode(
                STORE_ID, part.getId(), "SECONDARY-BC", OPERATOR_ID);

        assertFalse(second.getPrimaryBarcode());
    }

    @Test
    void updateDefaultBarcodeSyncsWithPartBarcodeTable() {
        PartEntity part = partService.createOfficialPart(buildOfficialCommand("刹车片", "UB-001"));
        partService.createBarcode(STORE_ID, part.getId(), "OLD-BC", OPERATOR_ID);

        PartEntity afterCreate = partService.getById(part.getId());
        assertEquals("OLD-BC", afterCreate.getDefaultBarcode());

        partService.updateDefaultBarcode(part.getId(), "NEW-BC");

        PartEntity afterUpdate = partService.getById(part.getId());
        assertEquals("NEW-BC", afterUpdate.getDefaultBarcode());

        PartBarcodeEntity oldPrimary = findBarcode(STORE_ID, "OLD-BC");
        assertNotNull(oldPrimary);
        assertEquals(false, oldPrimary.getPrimaryBarcode());

        PartBarcodeEntity newPrimary = findBarcode(STORE_ID, "NEW-BC");
        assertNotNull(newPrimary);
        assertEquals(true, newPrimary.getPrimaryBarcode());

        PartEntity found = partService.getByBarcode(STORE_ID, "NEW-BC");
        assertNotNull(found);
        assertEquals(part.getId(), found.getId());
    }

    @Test
    void updateDefaultBarcodeToExistingBarcodeReusesIt() {
        PartEntity part = partService.createOfficialPart(buildOfficialCommand("刹车片", "UB-002"));
        partService.createBarcode(STORE_ID, part.getId(), "OLD-BC2", OPERATOR_ID);

        PartBarcodeEntity extra = partService.createBarcode(
                STORE_ID, part.getId(), "EXTRA-BC", OPERATOR_ID);
        assertFalse(extra.getPrimaryBarcode());

        partService.updateDefaultBarcode(part.getId(), "EXTRA-BC");

        PartEntity afterUpdate = partService.getById(part.getId());
        assertEquals("EXTRA-BC", afterUpdate.getDefaultBarcode());

        PartBarcodeEntity reused = findBarcode(STORE_ID, "EXTRA-BC");
        assertTrue(reused.getPrimaryBarcode());
    }

    @Test
    void updateDefaultBarcodeToOtherPartBarcodeFails() {
        PartEntity part1 = partService.createOfficialPart(buildOfficialCommand("刹车片A", "UB-003"));
        partService.createBarcode(STORE_ID, part1.getId(), "PART1-BC", OPERATOR_ID);

        PartEntity part2 = partService.createOfficialPart(buildOfficialCommand("刹车片B", "UB-004"));
        partService.createBarcode(STORE_ID, part2.getId(), "PART2-BC", OPERATOR_ID);

        assertThrows(BusinessException.class,
                () -> partService.updateDefaultBarcode(part1.getId(), "PART2-BC"));
    }

    @Test
    void getBarcodeReturnsCorrectPartAcrossOperations() {
        PartEntity part = partService.createOfficialPart(buildOfficialCommand("刹车片", "GS-001"));
        partService.createBarcode(STORE_ID, part.getId(), "FIND-BC", OPERATOR_ID);

        PartEntity found = partService.getByBarcode(STORE_ID, "FIND-BC");
        assertEquals(part.getId(), found.getId());
        assertEquals("FIND-BC", found.getDefaultBarcode());

        PartEntity notFound = partService.getByBarcode(STORE_ID, "NON-EXISTENT");
        assertNull(notFound);
    }

    // --- helpers ---

    private PartBarcodeEntity findBarcode(Long storeId, String barcode) {
        return jdbcTemplate.queryForObject(
                "SELECT * FROM part_barcode WHERE store_id = ? AND barcode = ? AND deleted = 0",
                (rs, rowNum) -> {
                    PartBarcodeEntity e = new PartBarcodeEntity();
                    e.setId(rs.getLong("id"));
                    e.setStoreId(rs.getLong("store_id"));
                    e.setPartId(rs.getLong("part_id"));
                    e.setBarcode(rs.getString("barcode"));
                    e.setPrimaryBarcode(rs.getInt("is_primary") == 1);
                    return e;
                },
                storeId, barcode);
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

    private static void assertFalse(Boolean value) {
        org.junit.jupiter.api.Assertions.assertFalse(value);
    }
}
