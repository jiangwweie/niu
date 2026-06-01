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
import com.xiaoniu.aftermarket.part.dto.PartDeleteCheckResponse;
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
    private static final Long OTHER_STORE_ID = 99L;
    private static final Long OPERATOR_ID = 1L;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Autowired
    private PartService partService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanPartTables() {
        jdbcTemplate.execute("DELETE FROM inventory_stock");
        jdbcTemplate.execute("DELETE FROM part_barcode");
        jdbcTemplate.execute("DELETE FROM part");
        jdbcTemplate.execute("DELETE FROM sequence_daily");
    }

    @Test
    void createOfficialPartSuccessfully() {
        CreatePartCommand command = buildOfficialCommand("官方刹车片", "OF-BRAKE-001");
        command.setDefaultSalePrice(new BigDecimal("39.90"));

        PartEntity result = partService.createOfficialPart(command);

        assertNotNull(result);
        assertNotNull(result.getId());
        assertEquals(STORE_ID, result.getStoreId());
        assertEquals("OF-BRAKE-001", result.getPartCode());
        assertEquals("OF-BRAKE-001", result.getOfficialPartNo());
        assertEquals("官方刹车片", result.getPartName());
        assertEquals(PartSource.OFFICIAL.getCode(), result.getSource());
        assertEquals(CommonStatus.ENABLED.getCode(), result.getStatus());
        assertEquals(0, new BigDecimal("39.90").compareTo(result.getDefaultSalePrice()));
        assertEquals("OF-BRAKE-001", result.getDefaultBarcode());
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
        assertEquals(result.getPartCode(), result.getDefaultBarcode());
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
    void createOfficialPartWithManualDefaultBarcodePersistsPrimaryBarcode() {
        CreatePartCommand command = buildOfficialCommand("官方条码件", "OFF-BC-001");
        command.setDefaultBarcode("OFF-BC-PRIMARY");
        command.setExternalBarcode("OFF-BC-OUTER");

        PartEntity result = partService.createOfficialPart(command);

        assertEquals("OFF-BC-PRIMARY", result.getDefaultBarcode());
        PartBarcodeEntity primary = findBarcode(STORE_ID, "OFF-BC-PRIMARY");
        assertTrue(primary.getPrimaryBarcode());
        assertEquals("SYSTEM", primary.getBarcodeType());
        PartBarcodeEntity external = findBarcode(STORE_ID, "OFF-BC-OUTER");
        assertFalse(external.getPrimaryBarcode());
        assertEquals("MANUAL", external.getBarcodeType());
    }

    @Test
    void createThirdPartyPartWithDuplicateDefaultBarcodeFails() {
        CreatePartCommand first = buildThirdPartyCommand("三方A");
        first.setDefaultBarcode("DUP-DEFAULT-001");
        partService.createThirdPartyPart(first);

        CreatePartCommand second = buildThirdPartyCommand("三方B");
        second.setDefaultBarcode("DUP-DEFAULT-001");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> partService.createThirdPartyPart(second));
        assertEquals(ErrorCode.COMMON_BAD_REQUEST, ex.getErrorCode());
        assertEquals("条码已被其他配件使用", ex.getMessage());
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

        partService.disablePart(STORE_ID, part.getId());

        PartEntity updated = partService.getById(part.getId());
        assertEquals(CommonStatus.DISABLED.getCode(), updated.getStatus());
    }

    @Test
    void enablePartSuccessfully() {
        PartEntity part = partService.createOfficialPart(buildOfficialCommand("刹车片", "ENA-001"));
        partService.disablePart(STORE_ID, part.getId());
        partService.enablePart(STORE_ID, part.getId());

        PartEntity updated = partService.getById(part.getId());
        assertEquals(CommonStatus.ENABLED.getCode(), updated.getStatus());
    }

    @Test
    void disableNonExistentPartFails() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> partService.disablePart(STORE_ID, 99999L));
        assertEquals(ErrorCode.PART_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void deletePartWithStockFails() {
        PartEntity part = partService.createOfficialPart(buildOfficialCommand("有库存配件", "DEL-STOCK-001"));
        jdbcTemplate.update("""
                INSERT INTO inventory_stock (store_id, part_id, actual_qty, available_qty, reserved_qty)
                VALUES (?, ?, 1, 1, 0)
                """, STORE_ID, part.getId());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> partService.deletePart(STORE_ID, part.getId(), OPERATOR_ID));
        assertEquals(ErrorCode.PART_HAS_STOCK, ex.getErrorCode());
        assertEquals("该配件当前仍有库存，不能删除。请先通过库存调整处理库存后再删除。", ex.getMessage());
    }

    @Test
    void deletePartWithoutStockSoftDeletesAndPageQueryHidesIt() {
        PartEntity part = partService.createOfficialPart(buildOfficialCommand("零库存配件", "DEL-ZERO-001"));
        partService.createBarcode(STORE_ID, part.getId(), "DEL-ZERO-BC-001", OPERATOR_ID);
        jdbcTemplate.update("""
                INSERT INTO inventory_stock (store_id, part_id, actual_qty, available_qty, reserved_qty)
                VALUES (?, ?, 0, 0, 0)
                """, STORE_ID, part.getId());

        partService.deletePart(STORE_ID, part.getId(), OPERATOR_ID);

        PartEntity deleted = partService.getById(part.getId());
        assertEquals(1, deleted.getDeleted());
        assertEquals(CommonStatus.DISABLED.getCode(), deleted.getStatus());
        assertNotNull(deleted.getUpdatedAt());
        Integer barcodeDeleted = jdbcTemplate.queryForObject(
                "SELECT deleted FROM part_barcode WHERE store_id = ? AND part_id = ? AND barcode = ?",
                Integer.class, STORE_ID, part.getId(), "DEL-ZERO-BC-001");
        assertEquals(1, barcodeDeleted);
        assertNull(partService.getByBarcode(STORE_ID, "DEL-ZERO-BC-001"));
        PartQueryRequest request = new PartQueryRequest();
        request.setStoreId(STORE_ID);
        request.setPartCode("DEL-ZERO-001");
        assertEquals(0, partService.pageQuery(request).total());
    }

    @Test
    void deletePartWithWorkOrderReferenceFails() {
        PartEntity part = partService.createOfficialPart(buildOfficialCommand("工单引用配件", "DEL-WO-001"));
        jdbcTemplate.update("""
                INSERT INTO work_order_charge_item
                    (store_id, work_order_id, charge_type, item_name, part_id, quantity, unit_price,
                     line_amount, cost_price_snapshot, line_cost_amount, inventory_affecting, status, created_by)
                VALUES (?, ?, 'PART', ?, ?, ?, ?, ?, ?, ?, ?, 'ACTIVE', ?)
                """, STORE_ID, 90001L, "工单引用配件", part.getId(), 1, new BigDecimal("30.00"),
                new BigDecimal("30.00"), new BigDecimal("10.50"), new BigDecimal("10.50"), 1, OPERATOR_ID);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> partService.deletePart(STORE_ID, part.getId(), OPERATOR_ID));
        assertEquals(ErrorCode.PART_HAS_STOCK, ex.getErrorCode());
    }

    @Test
    void getDeleteCheckReturnsStructuredReasons() {
        PartEntity part = partService.createOfficialPart(buildOfficialCommand("删除校验配件", "DEL-CHECK-001"));
        jdbcTemplate.update("""
                INSERT INTO inventory_stock (id, store_id, part_id, actual_qty, available_qty, reserved_qty)
                VALUES (98001, ?, ?, 2, 1, 1)
                """, STORE_ID, part.getId());
        jdbcTemplate.update("""
                INSERT INTO inventory_flow
                    (store_id, inventory_stock_id, part_id, flow_type, quantity_delta,
                     actual_before, actual_after, available_before, available_after,
                     reserved_before, reserved_after, business_type, operator_id, operated_at, reason, created_by)
                VALUES (?, 98001, ?, 'INBOUND', 2, 0, 2, 0, 2, 0, 0, 'TEST', ?, CURRENT_TIMESTAMP, '测试', ?)
                """, STORE_ID, part.getId(), OPERATOR_ID, OPERATOR_ID);
        jdbcTemplate.update("""
                INSERT INTO work_order_charge_item
                    (store_id, work_order_id, charge_type, item_name, part_id, quantity, unit_price,
                     line_amount, cost_price_snapshot, line_cost_amount, inventory_affecting, status, created_by)
                VALUES (?, ?, 'PART', ?, ?, ?, ?, ?, ?, ?, ?, 'ACTIVE', ?)
                """, STORE_ID, 99001L, "删除校验配件", part.getId(), 1, new BigDecimal("20.00"),
                new BigDecimal("20.00"), new BigDecimal("8.00"), new BigDecimal("8.00"), 1, OPERATOR_ID);

        PartDeleteCheckResponse response = partService.getDeleteCheck(STORE_ID, part.getId());

        assertFalse(response.canDelete());
        assertEquals(2, response.stockSummary().actualQty());
        assertEquals(1, response.stockSummary().availableQty());
        assertEquals(1, response.stockSummary().reservedQty());
        assertEquals(1L, response.referenceSummary().inventoryFlowCount());
        assertEquals(1L, response.referenceSummary().workOrderChargeItemCount());
        assertEquals(1, response.referenceSummary().sampleWorkOrderIds().size());
        assertTrue(response.reasons().stream().anyMatch(reason -> reason.contains("预占库存")));
        assertTrue(response.reasons().stream().anyMatch(reason -> reason.contains("当前仍有库存")));
        assertTrue(response.reasons().stream().anyMatch(reason -> reason.contains("库存流水")));
        assertTrue(response.reasons().stream().anyMatch(reason -> reason.contains("工单记录")));
    }

    @Test
    void updatePartSuccessfully() {
        PartEntity part = partService.createOfficialPart(buildOfficialCommand("原始名称", "UPD-001"));

        UpdatePartCommand command = new UpdatePartCommand();
        command.setPartId(part.getId());
        command.setStoreId(STORE_ID);
        command.setPartName("更新后名称");
        command.setModel("V2");
        command.setDefaultSalePrice(new BigDecimal("88.80"));
        partService.updatePart(command);

        PartEntity updated = partService.getById(part.getId());
        assertEquals("更新后名称", updated.getPartName());
        assertEquals("V2", updated.getModel());
        assertEquals("UPD-001", updated.getPartCode());
        assertEquals(0, new BigDecimal("88.80").compareTo(updated.getDefaultSalePrice()));
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
        CreatePartCommand firstCommand = buildOfficialCommand("刹车片A", "PG-001");
        firstCommand.setDefaultSalePrice(new BigDecimal("28.00"));
        partService.createOfficialPart(firstCommand);
        partService.createOfficialPart(buildOfficialCommand("刹车片B", "PG-002"));
        partService.createThirdPartyPart(buildThirdPartyCommand("电池C"));

        PartQueryRequest request = new PartQueryRequest();
        request.setStoreId(STORE_ID);
        request.setPageNo(1);
        request.setPageSize(10);

        PageResponse<PartQueryResponse> response = partService.pageQuery(request);
        assertEquals(3, response.total());
        assertEquals(3, response.records().size());
        PartQueryResponse first = response.records().stream()
                .filter(item -> "PG-001".equals(item.getPartCode()))
                .findFirst()
                .orElseThrow();
        assertEquals(0, new BigDecimal("28.00").compareTo(first.getDefaultSalePrice()));
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
        assertFalse(barcode.getPrimaryBarcode());

        PartEntity found = partService.getByBarcode(STORE_ID, "BARCODE-001");
        assertNotNull(found);
        assertEquals(part.getId(), found.getId());
    }

    @Test
    void createBarcodeSetsDefaultBarcodeOnPart() {
        PartEntity part = partService.createOfficialPart(buildOfficialCommand("刹车片", "BC-002"));
        assertEquals("BC-002", part.getDefaultBarcode());

        partService.createBarcode(STORE_ID, part.getId(), "BARCODE-002", OPERATOR_ID);

        PartEntity updated = partService.getById(part.getId());
        assertEquals("BC-002", updated.getDefaultBarcode());
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
        assertEquals("UB-001", afterCreate.getDefaultBarcode());

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
        assertEquals("GS-001", found.getDefaultBarcode());

        PartEntity notFound = partService.getByBarcode(STORE_ID, "NON-EXISTENT");
        assertNull(notFound);
    }

    // --- updateDefaultBarcode(null) demote scenario (Fix for round 3) ---

    @Test
    void updateDefaultBarcodeToNullDemotesOldPrimary() {
        PartEntity part = partService.createOfficialPart(buildOfficialCommand("刹车片", "NULL-001"));
        partService.createBarcode(STORE_ID, part.getId(), "TO-BE-DEMOTED", OPERATOR_ID);
        partService.updateDefaultBarcode(part.getId(), "TO-BE-DEMOTED");

        PartEntity afterCreate = partService.getById(part.getId());
        assertEquals("TO-BE-DEMOTED", afterCreate.getDefaultBarcode());
        PartBarcodeEntity primary = findBarcode(STORE_ID, "TO-BE-DEMOTED");
        assertTrue(primary.getPrimaryBarcode());

        partService.updateDefaultBarcode(part.getId(), null);

        PartEntity afterNull = partService.getById(part.getId());
        assertNull(afterNull.getDefaultBarcode());

        PartBarcodeEntity afterDemote = findBarcode(STORE_ID, "TO-BE-DEMOTED");
        assertEquals(false, afterDemote.getPrimaryBarcode());
    }

    @Test
    void updateDefaultBarcodeToNullThenCreateNewAutoPromotes() {
        PartEntity part = partService.createOfficialPart(buildOfficialCommand("刹车片", "NULL-002"));
        partService.createBarcode(STORE_ID, part.getId(), "OLD-BC", OPERATOR_ID);
        partService.updateDefaultBarcode(part.getId(), "OLD-BC");

        partService.updateDefaultBarcode(part.getId(), null);

        PartBarcodeEntity afterDemote = findBarcode(STORE_ID, "OLD-BC");
        assertFalse(afterDemote.getPrimaryBarcode());

        PartBarcodeEntity newBc = partService.createBarcode(
                STORE_ID, part.getId(), "FRESH-BC", OPERATOR_ID);
        assertTrue(newBc.getPrimaryBarcode());

        PartEntity afterFresh = partService.getById(part.getId());
        assertEquals("FRESH-BC", afterFresh.getDefaultBarcode());
    }

    @Test
    void updateDefaultBarcodeNullWhenNoPrimaryIsNoop() {
        PartEntity part = partService.createOfficialPart(buildOfficialCommand("刹车片", "NULL-003"));
        assertEquals("NULL-003", part.getDefaultBarcode());

        partService.updateDefaultBarcode(part.getId(), null);

        PartEntity after = partService.getById(part.getId());
        assertNull(after.getDefaultBarcode());
    }

    @Test
    void lookupRepairsHistoricalDefaultBarcodeFromPartCode() {
        jdbcTemplate.update("""
                INSERT INTO part (id, store_id, part_code, official_part_no, part_name, model, source, category_code,
                                  reference_cost_price, default_sale_price, default_barcode, location_remark, create_source, status, remark, created_by)
                VALUES (99001, ?, 'LEGACY-PART-001', NULL, '历史配件', 'NQi', 'THIRD_PARTY', 'BATTERY',
                        10.00, 20.00, NULL, NULL, 'NORMAL', 'ENABLED', NULL, ?)
                """, STORE_ID, OPERATOR_ID);

        PartEntity found = partService.findVisiblePartByCode(STORE_ID, "LEGACY-PART-001");

        assertNotNull(found);
        assertEquals("LEGACY-PART-001", found.getDefaultBarcode());
        PartBarcodeEntity primary = findBarcode(STORE_ID, "LEGACY-PART-001");
        assertTrue(primary.getPrimaryBarcode());
        assertEquals("SYSTEM", primary.getBarcodeType());
    }

    // --- updatePart cross-store rejection ---

    @Test
    void updatePartWithWrongStoreIdFails() {
        PartEntity part = partService.createOfficialPart(buildOfficialCommand("刹车片", "XS-UPD-001"));

        UpdatePartCommand command = new UpdatePartCommand();
        command.setPartId(part.getId());
        command.setStoreId(OTHER_STORE_ID);
        command.setPartName("试图跨门店修改");

        assertThrows(BusinessException.class, () -> partService.updatePart(command));

        PartEntity unchanged = partService.getById(part.getId());
        assertEquals("刹车片", unchanged.getPartName());
    }

    @Test
    void updatePartWithNullStoreIdFails() {
        PartEntity part = partService.createOfficialPart(buildOfficialCommand("刹车片", "XS-UPD-003"));

        UpdatePartCommand command = new UpdatePartCommand();
        command.setPartId(part.getId());
        command.setStoreId(null);
        command.setPartName("试图绕过门店校验");

        assertThrows(BusinessException.class, () -> partService.updatePart(command));

        PartEntity unchanged = partService.getById(part.getId());
        assertEquals("刹车片", unchanged.getPartName());
    }

    @Test
    void updatePartWithCorrectStoreIdSucceeds() {
        PartEntity part = partService.createOfficialPart(buildOfficialCommand("刹车片", "XS-UPD-002"));

        UpdatePartCommand command = new UpdatePartCommand();
        command.setPartId(part.getId());
        command.setStoreId(STORE_ID);
        command.setPartName("正确门店修改");

        partService.updatePart(command);

        PartEntity updated = partService.getById(part.getId());
        assertEquals("正确门店修改", updated.getPartName());
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
                    e.setBarcodeType(rs.getString("barcode_type"));
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
