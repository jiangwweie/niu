package com.xiaoniu.aftermarket.inventory.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.xiaoniu.aftermarket.common.api.ErrorCode;
import com.xiaoniu.aftermarket.common.enums.CommonStatus;
import com.xiaoniu.aftermarket.common.enums.InventoryFlowType;
import com.xiaoniu.aftermarket.common.enums.PartSource;
import com.xiaoniu.aftermarket.common.exception.BusinessException;
import com.xiaoniu.aftermarket.common.pagination.PageResponse;
import com.xiaoniu.aftermarket.inventory.dto.InventoryAdjustCommand;
import com.xiaoniu.aftermarket.inventory.dto.InventoryFlowQueryRequest;
import com.xiaoniu.aftermarket.inventory.dto.InventoryFlowQueryResponse;
import com.xiaoniu.aftermarket.inventory.dto.InventoryInboundCommand;
import com.xiaoniu.aftermarket.inventory.dto.InventoryStockQueryResponse;
import com.xiaoniu.aftermarket.inventory.entity.InventoryFlowEntity;
import com.xiaoniu.aftermarket.inventory.entity.InventoryStockEntity;
import com.xiaoniu.aftermarket.inventory.mapper.InventoryFlowMapper;
import com.xiaoniu.aftermarket.inventory.mapper.InventoryStockMapper;
import com.xiaoniu.aftermarket.part.dto.CreatePartCommand;
import com.xiaoniu.aftermarket.part.entity.PartEntity;
import com.xiaoniu.aftermarket.part.mapper.PartMapper;
import com.xiaoniu.aftermarket.part.service.PartService;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
class InventoryServiceTest {

    private static final Long STORE_ID = 1L;
    private static final Long OTHER_STORE_ID = 99L;
    private static final Long OPERATOR_ID = 1L;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private PartService partService;

    @Autowired
    private InventoryStockMapper inventoryStockMapper;

    @Autowired
    private InventoryFlowMapper inventoryFlowMapper;

    @Autowired
    private PartMapper partMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanInventoryTables() {
        jdbcTemplate.execute("DELETE FROM inventory_flow");
        jdbcTemplate.execute("DELETE FROM inventory_stock");
        jdbcTemplate.execute("DELETE FROM part_barcode");
        jdbcTemplate.execute("DELETE FROM part");
        jdbcTemplate.execute("DELETE FROM sequence_daily");
    }

    @Test
    void inboundNewPartCreatesStockAndFlow() {
        PartEntity part = createOfficialPart("刹车片", "INB-001");

        InventoryInboundCommand command = buildInboundCommand(part.getId(), 10);
        inventoryService.inbound(command);

        InventoryStockEntity stock = inventoryService.getByPartId(STORE_ID, part.getId());
        assertNotNull(stock);
        assertEquals(10, stock.getActualQty());
        assertEquals(10, stock.getAvailableQty());
        assertEquals(0, stock.getReservedQty());
        assertNotNull(stock.getLastFlowId());
        assertNotNull(stock.getLastChangedAt());
    }

    @Test
    void inboundNewPartCreatesInboundFlow() {
        PartEntity part = createOfficialPart("刹车片", "FLW-001");

        InventoryInboundCommand command = buildInboundCommand(part.getId(), 5);
        inventoryService.inbound(command);

        InventoryStockEntity stock = inventoryService.getByPartId(STORE_ID, part.getId());
        InventoryFlowEntity flow = inventoryFlowMapper.selectById(stock.getLastFlowId());
        assertNotNull(flow);
        assertEquals(InventoryFlowType.INBOUND.getCode(), flow.getFlowType());
        assertEquals(5, flow.getQuantityDelta());
        assertEquals(0, flow.getActualBefore());
        assertEquals(5, flow.getActualAfter());
        assertEquals(0, flow.getAvailableBefore());
        assertEquals(5, flow.getAvailableAfter());
        assertEquals(0, flow.getReservedBefore());
        assertEquals(0, flow.getReservedAfter());
        assertEquals(OPERATOR_ID, flow.getOperatorId());
    }

    @Test
    void inboundSamePartTwiceAccumulatesCorrectly() {
        PartEntity part = createOfficialPart("刹车片", "ACC-001");

        inventoryService.inbound(buildInboundCommand(part.getId(), 10));
        inventoryService.inbound(buildInboundCommand(part.getId(), 5));

        InventoryStockEntity stock = inventoryService.getByPartId(STORE_ID, part.getId());
        assertEquals(15, stock.getActualQty());
        assertEquals(15, stock.getAvailableQty());
        assertEquals(0, stock.getReservedQty());
    }

    @Test
    void inboundGeneratesCorrectFlows() {
        PartEntity part = createOfficialPart("刹车片", "FLW2-001");

        inventoryService.inbound(buildInboundCommand(part.getId(), 10));
        inventoryService.inbound(buildInboundCommand(part.getId(), 5));

        InventoryStockEntity stock = inventoryService.getByPartId(STORE_ID, part.getId());
        InventoryFlowEntity secondFlow = inventoryFlowMapper.selectById(stock.getLastFlowId());
        assertEquals(10, secondFlow.getActualBefore());
        assertEquals(15, secondFlow.getActualAfter());
    }

    @Test
    void inboundWithZeroQtyFails() {
        PartEntity part = createOfficialPart("刹车片", "QTY-001");

        InventoryInboundCommand command = buildInboundCommand(part.getId(), 0);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> inventoryService.inbound(command));
        assertEquals(ErrorCode.INVENTORY_QTY_MUST_POSITIVE, ex.getErrorCode());
    }

    @Test
    void inboundWithNegativeQtyFails() {
        PartEntity part = createOfficialPart("刹车片", "QTY-002");

        InventoryInboundCommand command = buildInboundCommand(part.getId(), -1);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> inventoryService.inbound(command));
        assertEquals(ErrorCode.INVENTORY_QTY_MUST_POSITIVE, ex.getErrorCode());
    }

    @Test
    void inboundDisabledPartFails() {
        PartEntity part = createOfficialPart("刹车片", "DIS-001");
        partService.disablePart(STORE_ID, part.getId());

        InventoryInboundCommand command = buildInboundCommand(part.getId(), 10);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> inventoryService.inbound(command));
        assertEquals(ErrorCode.PART_DISABLED, ex.getErrorCode());
    }

    @Test
    void positiveAdjustSuccessfully() {
        PartEntity part = createOfficialPart("刹车片", "ADJ-001");
        inventoryService.inbound(buildInboundCommand(part.getId(), 10));

        InventoryAdjustCommand command = buildAdjustCommand(part.getId(), 5, "盘点多出");
        inventoryService.adjust(command);

        InventoryStockEntity stock = inventoryService.getByPartId(STORE_ID, part.getId());
        assertEquals(15, stock.getActualQty());
        assertEquals(15, stock.getAvailableQty());
    }

    @Test
    void positiveAdjustGeneratesFlow() {
        PartEntity part = createOfficialPart("刹车片", "ADJ2-001");
        inventoryService.inbound(buildInboundCommand(part.getId(), 10));
        inventoryService.adjust(buildAdjustCommand(part.getId(), 5, "盘点多出"));

        InventoryStockEntity stock = inventoryService.getByPartId(STORE_ID, part.getId());
        InventoryFlowEntity flow = inventoryFlowMapper.selectById(stock.getLastFlowId());
        assertEquals(InventoryFlowType.ADJUST.getCode(), flow.getFlowType());
        assertEquals(5, flow.getQuantityDelta());
        assertEquals(10, flow.getActualBefore());
        assertEquals(15, flow.getActualAfter());
        assertEquals("盘点多出", flow.getReason());
    }

    @Test
    void negativeAdjustSuccessfully() {
        PartEntity part = createOfficialPart("刹车片", "ADJ3-001");
        inventoryService.inbound(buildInboundCommand(part.getId(), 10));

        InventoryAdjustCommand command = buildAdjustCommand(part.getId(), -3, "损耗");
        inventoryService.adjust(command);

        InventoryStockEntity stock = inventoryService.getByPartId(STORE_ID, part.getId());
        assertEquals(7, stock.getActualQty());
        assertEquals(7, stock.getAvailableQty());
    }

    @Test
    void negativeAdjustWouldMakeAvailableNegativeFails() {
        PartEntity part = createOfficialPart("刹车片", "ADJ4-001");
        inventoryService.inbound(buildInboundCommand(part.getId(), 5));

        InventoryAdjustCommand command = buildAdjustCommand(part.getId(), -10, "错误调整");
        BusinessException ex = assertThrows(BusinessException.class,
                () -> inventoryService.adjust(command));
        assertEquals(ErrorCode.INVENTORY_ADJUST_WOULD_NEGATIVE, ex.getErrorCode());
    }

    @Test
    void adjustWithZeroDeltaFails() {
        PartEntity part = createOfficialPart("刹车片", "ADJ5-001");
        inventoryService.inbound(buildInboundCommand(part.getId(), 10));

        InventoryAdjustCommand command = buildAdjustCommand(part.getId(), 0, "原因");
        BusinessException ex = assertThrows(BusinessException.class,
                () -> inventoryService.adjust(command));
        assertEquals(ErrorCode.INVENTORY_ADJUST_ZERO, ex.getErrorCode());
    }

    @Test
    void adjustWithoutReasonFails() {
        PartEntity part = createOfficialPart("刹车片", "ADJ6-001");
        inventoryService.inbound(buildInboundCommand(part.getId(), 10));

        InventoryAdjustCommand command = buildAdjustCommand(part.getId(), 5, null);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> inventoryService.adjust(command));
        assertEquals(ErrorCode.INVENTORY_ADJUST_REASON_REQUIRED, ex.getErrorCode());
    }

    @Test
    void adjustNonExistentStockFails() {
        PartEntity part = createOfficialPart("刹车片", "ADJ7-001");

        InventoryAdjustCommand command = buildAdjustCommand(part.getId(), 5, "原因");
        BusinessException ex = assertThrows(BusinessException.class,
                () -> inventoryService.adjust(command));
        assertEquals(ErrorCode.PART_STOCK_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void queryStockListSuccessfully() {
        PartEntity part1 = createOfficialPart("刹车片", "QRY-001");
        PartEntity part2 = createOfficialPart("电池", "QRY-002");
        inventoryService.inbound(buildInboundCommand(part1.getId(), 10));
        inventoryService.inbound(buildInboundCommand(part2.getId(), 5));

        PageResponse<InventoryStockQueryResponse> response =
                inventoryService.pageQuery(STORE_ID, null, null, 1, 10);

        assertEquals(2, response.total());
        assertEquals(2, response.records().size());
    }

    @Test
    void queryFlowListSuccessfully() {
        PartEntity part = createOfficialPart("刹车片", "FLQ-001");
        inventoryService.inbound(buildInboundCommand(part.getId(), 10));
        inventoryService.adjust(buildAdjustCommand(part.getId(), 5, "盘点"));

        InventoryFlowQueryRequest request = new InventoryFlowQueryRequest();
        request.setStoreId(STORE_ID);
        request.setPageNo(1);
        request.setPageSize(10);

        PageResponse<InventoryFlowQueryResponse> response =
                inventoryService.pageFlowQuery(request);

        assertEquals(2, response.total());
    }

    @Test
    void queryFlowListFilterByType() {
        PartEntity part = createOfficialPart("刹车片", "FLF-001");
        inventoryService.inbound(buildInboundCommand(part.getId(), 10));
        inventoryService.adjust(buildAdjustCommand(part.getId(), 2, "盘点"));

        InventoryFlowQueryRequest request = new InventoryFlowQueryRequest();
        request.setStoreId(STORE_ID);
        request.setFlowType(InventoryFlowType.ADJUST.getCode());
        request.setPageNo(1);
        request.setPageSize(10);

        PageResponse<InventoryFlowQueryResponse> response =
                inventoryService.pageFlowQuery(request);

        assertEquals(1, response.total());
        assertEquals(InventoryFlowType.ADJUST.getCode(),
                response.records().get(0).getFlowType());
    }

    @Test
    void queryStockByPartCodeFilter() {
        PartEntity part1 = createOfficialPart("刹车片", "SRC-001");
        PartEntity part2 = createOfficialPart("电池", "SRC-002");
        inventoryService.inbound(buildInboundCommand(part1.getId(), 10));
        inventoryService.inbound(buildInboundCommand(part2.getId(), 5));

        PageResponse<InventoryStockQueryResponse> response =
                inventoryService.pageQuery(STORE_ID, "SRC-001", null, 1, 10);

        assertEquals(1, response.total());
        assertEquals("SRC-001", response.records().get(0).getPartCode());
    }

    // --- unitCost persistence tests (Task 6.1) ---

    @Test
    void inboundWithUnitCostPersistsToFlow() {
        PartEntity part = createOfficialPart("刹车片", "UC-001");
        BigDecimal unitCost = new BigDecimal("25.50");

        inventoryService.inbound(buildInboundCommand(part.getId(), 10, unitCost));

        InventoryStockEntity stock = inventoryService.getByPartId(STORE_ID, part.getId());
        InventoryFlowEntity flow = inventoryFlowMapper.selectById(stock.getLastFlowId());
        assertNotNull(flow.getUnitCost());
        assertEquals(0, unitCost.compareTo(flow.getUnitCost()));
    }

    @Test
    void inboundWithUnitCostUpdatesPartCostPrice() {
        PartEntity part = createOfficialPart("刹车片", "UC-002");
        BigDecimal newCost = new BigDecimal("33.00");

        inventoryService.inbound(buildInboundCommand(part.getId(), 10, newCost));

        PartEntity updated = partMapper.selectById(part.getId());
        assertNotNull(updated.getReferenceCostPrice());
        assertEquals(0, newCost.compareTo(updated.getReferenceCostPrice()));
    }

    @Test
    void inboundWithoutUnitCostDoesNotUpdatePartCostPrice() {
        PartEntity part = createOfficialPart("刹车片", "UC-003");
        BigDecimal originalCost = part.getReferenceCostPrice();

        inventoryService.inbound(buildInboundCommand(part.getId(), 10));

        PartEntity afterInbound = partMapper.selectById(part.getId());
        if (originalCost != null) {
            assertEquals(0, originalCost.compareTo(afterInbound.getReferenceCostPrice()));
        } else {
            assertNull(afterInbound.getReferenceCostPrice());
        }
    }

    @Test
    void inboundWithNegativeUnitCostFails() {
        PartEntity part = createOfficialPart("刹车片", "UC-004");

        InventoryInboundCommand command = buildInboundCommand(part.getId(), 10,
                new BigDecimal("-1.00"));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> inventoryService.inbound(command));
        assertEquals(ErrorCode.INBOUND_UNIT_COST_NEGATIVE, ex.getErrorCode());
    }

    @Test
    void flowQueryReturnsUnitCost() {
        PartEntity part = createOfficialPart("刹车片", "UC-005");
        BigDecimal unitCost = new BigDecimal("18.75");
        inventoryService.inbound(buildInboundCommand(part.getId(), 5, unitCost));

        InventoryFlowQueryRequest request = new InventoryFlowQueryRequest();
        request.setStoreId(STORE_ID);
        request.setFlowType(InventoryFlowType.INBOUND.getCode());
        request.setPageNo(1);
        request.setPageSize(10);

        PageResponse<InventoryFlowQueryResponse> response =
                inventoryService.pageFlowQuery(request);

        assertEquals(1, response.total());
        assertNotNull(response.records().get(0).getUnitCost());
        assertEquals(0, unitCost.compareTo(response.records().get(0).getUnitCost()));
    }

    private PartEntity createOfficialPart(String name, String partCode) {
        CreatePartCommand command = new CreatePartCommand();
        command.setStoreId(STORE_ID);
        command.setOperatorId(OPERATOR_ID);
        command.setPartName(name);
        command.setOfficialPartNo(partCode);
        command.setReferenceCostPrice(new BigDecimal("10.50"));
        return partService.createOfficialPart(command);
    }

    private InventoryInboundCommand buildInboundCommand(Long partId, int quantity) {
        InventoryInboundCommand command = new InventoryInboundCommand();
        command.setStoreId(STORE_ID);
        command.setPartId(partId);
        command.setQuantity(quantity);
        command.setOperatorId(OPERATOR_ID);
        command.setReason("普通入库");
        return command;
    }

    private InventoryInboundCommand buildInboundCommand(Long partId, int quantity,
                                                         BigDecimal unitCost) {
        InventoryInboundCommand command = buildInboundCommand(partId, quantity);
        command.setUnitCost(unitCost);
        return command;
    }

    private InventoryAdjustCommand buildAdjustCommand(Long partId, int delta, String reason) {
        InventoryAdjustCommand command = new InventoryAdjustCommand();
        command.setStoreId(STORE_ID);
        command.setPartId(partId);
        command.setQuantityDelta(delta);
        command.setOperatorId(OPERATOR_ID);
        command.setReason(reason);
        return command;
    }

    // --- Cross-store boundary tests (Fix #2) ---

    @Test
    void inboundWithCrossStorePartIdFails() {
        PartEntity part = createOfficialPart("刹车片", "XS-001");

        InventoryInboundCommand command = buildInboundCommand(part.getId(), 10);
        command.setStoreId(OTHER_STORE_ID);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> inventoryService.inbound(command));
        assertTrue(ex.getMessage().contains("配件不属于当前门店"));
    }

    @Test
    void adjustWithCrossStorePartIdFails() {
        PartEntity part = createOfficialPart("刹车片", "XS-002");
        inventoryService.inbound(buildInboundCommand(part.getId(), 10));

        InventoryAdjustCommand command = buildAdjustCommand(part.getId(), -5, "调整");
        command.setStoreId(OTHER_STORE_ID);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> inventoryService.adjust(command));
        assertTrue(ex.getMessage().contains("配件不属于当前门店"));
    }

    @Test
    void inboundWithWrongStoreDoesNotCreateStock() {
        PartEntity part = createOfficialPart("刹车片", "XS-003");

        InventoryInboundCommand command = buildInboundCommand(part.getId(), 10);
        command.setStoreId(OTHER_STORE_ID);

        assertThrows(BusinessException.class,
                () -> inventoryService.inbound(command));

        InventoryStockEntity stock = inventoryService.getByPartId(STORE_ID, part.getId());
        if (stock != null) {
            assertEquals(0, stock.getActualQty());
        }
    }
}
