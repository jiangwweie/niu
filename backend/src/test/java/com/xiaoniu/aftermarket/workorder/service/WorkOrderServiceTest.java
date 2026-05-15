package com.xiaoniu.aftermarket.workorder.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.xiaoniu.aftermarket.common.api.ErrorCode;
import com.xiaoniu.aftermarket.common.enums.CommonStatus;
import com.xiaoniu.aftermarket.common.enums.InventoryFlowType;
import com.xiaoniu.aftermarket.common.enums.WorkOrderStatus;
import com.xiaoniu.aftermarket.common.exception.BusinessException;
import com.xiaoniu.aftermarket.common.pagination.PageResponse;
import com.xiaoniu.aftermarket.inventory.dto.InventoryInboundCommand;
import com.xiaoniu.aftermarket.inventory.entity.InventoryFlowEntity;
import com.xiaoniu.aftermarket.inventory.entity.InventoryStockEntity;
import com.xiaoniu.aftermarket.inventory.mapper.InventoryFlowMapper;
import com.xiaoniu.aftermarket.inventory.mapper.InventoryStockMapper;
import com.xiaoniu.aftermarket.inventory.service.InventoryService;
import com.xiaoniu.aftermarket.payment.dto.PaymentSummaryResponse;
import com.xiaoniu.aftermarket.payment.dto.RecordPaymentCommand;
import com.xiaoniu.aftermarket.payment.dto.RecordRefundCommand;
import com.xiaoniu.aftermarket.payment.mapper.PaymentRecordMapper;
import com.xiaoniu.aftermarket.payment.mapper.RefundRecordMapper;
import com.xiaoniu.aftermarket.payment.service.PaymentService;
import com.xiaoniu.aftermarket.payment.service.RefundService;
import com.xiaoniu.aftermarket.part.dto.CreatePartCommand;
import com.xiaoniu.aftermarket.part.entity.PartEntity;
import com.xiaoniu.aftermarket.part.service.PartService;
import com.xiaoniu.aftermarket.workorder.dto.AddWorkOrderChargeItemCommand;
import com.xiaoniu.aftermarket.workorder.dto.CancelWorkOrderCommand;
import com.xiaoniu.aftermarket.workorder.dto.CreateDraftWorkOrderCommand;
import com.xiaoniu.aftermarket.workorder.dto.SettleWorkOrderCommand;
import com.xiaoniu.aftermarket.workorder.dto.SubmitWorkOrderCommand;
import com.xiaoniu.aftermarket.workorder.dto.UpdateWorkOrderChargeItemCommand;
import com.xiaoniu.aftermarket.workorder.dto.UpdateWorkOrderDraftCommand;
import com.xiaoniu.aftermarket.workorder.dto.WorkOrderChargeItemInput;
import com.xiaoniu.aftermarket.workorder.dto.WorkOrderChargeItemResponse;
import com.xiaoniu.aftermarket.workorder.dto.WorkOrderDetailResponse;
import com.xiaoniu.aftermarket.workorder.dto.WorkOrderQueryRequest;
import com.xiaoniu.aftermarket.workorder.dto.WorkOrderQueryResponse;
import com.xiaoniu.aftermarket.workorder.entity.WorkOrderChargeItemEntity;
import com.xiaoniu.aftermarket.workorder.entity.WorkOrderEntity;
import com.xiaoniu.aftermarket.workorder.entity.WorkOrderStatusLogEntity;
import com.xiaoniu.aftermarket.workorder.mapper.WorkOrderChargeItemMapper;
import com.xiaoniu.aftermarket.workorder.mapper.WorkOrderMapper;
import com.xiaoniu.aftermarket.workorder.mapper.WorkOrderStatusLogMapper;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
class WorkOrderServiceTest {

    private static final Long STORE_ID = 1L;
    private static final Long OTHER_STORE_ID = 99L;
    private static final Long OPERATOR_ID = 1L;

    @Autowired
    private WorkOrderService workOrderService;

    @Autowired
    private PartService partService;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private RefundService refundService;

    @Autowired
    private PaymentRecordMapper paymentRecordMapper;

    @Autowired
    private RefundRecordMapper refundRecordMapper;

    @Autowired
    private WorkOrderMapper workOrderMapper;

    @Autowired
    private WorkOrderChargeItemMapper chargeItemMapper;

    @Autowired
    private WorkOrderStatusLogMapper statusLogMapper;

    @Autowired
    private InventoryStockMapper inventoryStockMapper;

    @Autowired
    private InventoryFlowMapper inventoryFlowMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanTables() {
        jdbcTemplate.execute("DELETE FROM official_after_sales");
        jdbcTemplate.execute("DELETE FROM refund_record");
        jdbcTemplate.execute("DELETE FROM payment_record");
        jdbcTemplate.execute("DELETE FROM work_order_status_log");
        jdbcTemplate.execute("DELETE FROM work_order_charge_item");
        jdbcTemplate.execute("DELETE FROM work_order");
        jdbcTemplate.execute("DELETE FROM inventory_flow");
        jdbcTemplate.execute("DELETE FROM inventory_stock");
        jdbcTemplate.execute("DELETE FROM part_barcode");
        jdbcTemplate.execute("DELETE FROM part");
        jdbcTemplate.execute("DELETE FROM sequence_daily");
    }

    // --- createDraft ---

    @Test
    void createDraftSuccessfully() {
        CreateDraftWorkOrderCommand command = buildCreateCommand("张三", "13800001111", "小牛N1");

        Long id = workOrderService.createDraft(command);

        WorkOrderEntity entity = workOrderMapper.selectById(id);
        assertNotNull(entity);
        assertTrue(entity.getWorkOrderNo().startsWith("WO"));
        assertEquals(14, entity.getWorkOrderNo().length());
        assertEquals(WorkOrderStatus.DRAFT.getCode(), entity.getStatus());
        assertEquals(0, BigDecimal.ZERO.compareTo(entity.getReceivableAmount()));
        assertEquals("张三", entity.getCustomerNameSnapshot());
        assertEquals("小牛N1", entity.getVehicleModelSnapshot());
    }

    @Test
    void createDraftWithInitialChargeItems() {
        PartEntity part = createPart("刹车片", "CHG-001", new BigDecimal("50.00"));

        CreateDraftWorkOrderCommand command = buildCreateCommand("李四", "13800002222", "小牛N1");
        WorkOrderChargeItemInput labor = buildLaborInput("工时费", 1, new BigDecimal("200.00"));
        WorkOrderChargeItemInput other = buildOtherInput("拖车费", 1, new BigDecimal("100.00"));

        command.setChargeItems(List.of(
                buildPartInput(part.getId(), "刹车片更换", 2, new BigDecimal("80.00")),
                labor, other));

        Long id = workOrderService.createDraft(command);

        WorkOrderDetailResponse detail = workOrderService.getById(id);
        assertEquals(3, detail.getChargeItems().size());

        WorkOrderEntity entity = workOrderMapper.selectById(id);
        // 2*80 + 1*200 + 1*100 = 460
        assertEquals(0, new BigDecimal("460.00").compareTo(entity.getReceivableAmount()));
    }

    @Test
    void createDraftWithInitialPartChargeItem() {
        PartEntity part = createPart("电池", "CHG-002", new BigDecimal("1200.00"));

        CreateDraftWorkOrderCommand command = buildCreateCommand("王五", "13800003333", "小牛M1");
        command.setChargeItems(List.of(
                buildPartInput(part.getId(), "更换电池", 1, new BigDecimal("1500.00"))));

        Long id = workOrderService.createDraft(command);

        WorkOrderDetailResponse detail = workOrderService.getById(id);
        WorkOrderChargeItemResponse item = detail.getChargeItems().get(0);

        assertTrue(item.getInventoryAffecting());
        assertEquals(0, new BigDecimal("1200.00").compareTo(item.getCostPriceSnapshot()));
        assertEquals(0, new BigDecimal("1200.00").compareTo(item.getLineCostAmount()));
        assertEquals(0, new BigDecimal("1500.00").compareTo(item.getLineAmount()));
    }

    // --- updateDraft ---

    @Test
    void updateDraftBasicInfoSuccessfully() {
        Long id = workOrderService.createDraft(
                buildCreateCommand("赵六", "13800004444", "小牛N1"));

        UpdateWorkOrderDraftCommand updateCmd = new UpdateWorkOrderDraftCommand();
        updateCmd.setStoreId(STORE_ID);
        updateCmd.setCustomerNameSnapshot("赵六_已更新");
        updateCmd.setRepairItem("更换刹车片");
        updateCmd.setOperatorId(OPERATOR_ID);

        workOrderService.updateDraft(id, updateCmd);

        WorkOrderEntity entity = workOrderMapper.selectById(id);
        assertEquals("赵六_已更新", entity.getCustomerNameSnapshot());
        assertEquals("更换刹车片", entity.getRepairItem());
    }

    @Test
    void updateNonDraftWorkOrderFails() {
        Long id = workOrderService.createDraft(
                buildCreateCommand("测试", "13800005555", "小牛N1"));

        // Manually set status to non-DRAFT
        WorkOrderEntity entity = workOrderMapper.selectById(id);
        entity.setStatus(WorkOrderStatus.PENDING_ACCEPT.getCode());
        workOrderMapper.updateById(entity);

        UpdateWorkOrderDraftCommand updateCmd = new UpdateWorkOrderDraftCommand();
        updateCmd.setStoreId(STORE_ID);
        updateCmd.setCustomerNameSnapshot("不应该成功");
        updateCmd.setOperatorId(OPERATOR_ID);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> workOrderService.updateDraft(id, updateCmd));
        assertEquals(ErrorCode.WORK_ORDER_NOT_DRAFT, ex.getErrorCode());
    }

    // --- addChargeItem PART ---

    @Test
    void addPartChargeItemSuccessfully() {
        PartEntity part = createPart("刹车片", "ADP-001", new BigDecimal("50.00"));
        Long woId = workOrderService.createDraft(
                buildCreateCommand("客户A", "13900001111", "小牛N1"));

        AddWorkOrderChargeItemCommand cmd = buildPartItem(
                part.getId(), "更换刹车片", 2, new BigDecimal("80.00"));
        Long itemId = workOrderService.addChargeItem(woId, cmd);

        WorkOrderChargeItemEntity item = chargeItemMapper.selectById(itemId);
        assertNotNull(item);
        assertEquals("PART", item.getChargeType());
        assertEquals(2, item.getQuantity());
        assertEquals(0, new BigDecimal("80.00").compareTo(item.getUnitPrice()));
        assertEquals(0, new BigDecimal("160.00").compareTo(item.getLineAmount()));
        assertEquals(0, new BigDecimal("50.00").compareTo(item.getCostPriceSnapshot()));
        assertEquals(0, new BigDecimal("100.00").compareTo(item.getLineCostAmount()));
        assertTrue(item.getInventoryAffecting());
        assertEquals("ACTIVE", item.getStatus());

        WorkOrderEntity wo = workOrderMapper.selectById(woId);
        assertEquals(0, new BigDecimal("160.00").compareTo(wo.getReceivableAmount()));

        // Verify no inventory flow created
        assertEquals(0, inventoryFlowMapper.selectCount(null));
    }

    @Test
    void addPartChargeItemWithoutPartIdFails() {
        Long woId = workOrderService.createDraft(
                buildCreateCommand("客户B", "13900002222", "小牛N1"));

        AddWorkOrderChargeItemCommand cmd = new AddWorkOrderChargeItemCommand();
        cmd.setStoreId(STORE_ID);
        cmd.setChargeType("PART");
        cmd.setItemName("配件费");
        cmd.setQuantity(1);
        cmd.setUnitPrice(new BigDecimal("100.00"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> workOrderService.addChargeItem(woId, cmd));
        assertEquals(ErrorCode.PART_REQUIRED_FOR_PART_CHARGE, ex.getErrorCode());
    }

    @Test
    void addPartChargeItemWithDisabledPartFails() {
        PartEntity part = createPart("刹车片", "ADP-002", new BigDecimal("50.00"));
        partService.disablePart(STORE_ID, part.getId());
        Long woId = workOrderService.createDraft(
                buildCreateCommand("客户C", "13900003333", "小牛N1"));

        AddWorkOrderChargeItemCommand cmd = buildPartItem(
                part.getId(), "更换刹车片", 1, new BigDecimal("80.00"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> workOrderService.addChargeItem(woId, cmd));
        assertEquals(ErrorCode.PART_DISABLED, ex.getErrorCode());
    }

    // --- addChargeItem LABOR ---

    @Test
    void addLaborChargeItemSuccessfully() {
        Long woId = workOrderService.createDraft(
                buildCreateCommand("客户D", "13900004444", "小牛N1"));

        AddWorkOrderChargeItemCommand cmd = buildLaborItem("工时费", 1, new BigDecimal("200.00"));
        Long itemId = workOrderService.addChargeItem(woId, cmd);

        WorkOrderChargeItemEntity item = chargeItemMapper.selectById(itemId);
        assertEquals("LABOR", item.getChargeType());
        assertFalse(item.getInventoryAffecting());
        assertNull(item.getPartId());
        assertEquals(0, BigDecimal.ZERO.compareTo(item.getCostPriceSnapshot()));
        assertEquals(0, BigDecimal.ZERO.compareTo(item.getLineCostAmount()));
        assertEquals(0, new BigDecimal("200.00").compareTo(item.getLineAmount()));

        WorkOrderEntity wo = workOrderMapper.selectById(woId);
        assertEquals(0, new BigDecimal("200.00").compareTo(wo.getReceivableAmount()));
    }

    @Test
    void addLaborChargeItemWithPartIdFails() {
        PartEntity part = createPart("刹车片", "LAB-001", new BigDecimal("50.00"));
        Long woId = workOrderService.createDraft(
                buildCreateCommand("客户E", "13900005555", "小牛N1"));

        AddWorkOrderChargeItemCommand cmd = buildLaborItem("工时费", 1, new BigDecimal("200.00"));
        cmd.setPartId(part.getId());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> workOrderService.addChargeItem(woId, cmd));
        assertEquals(ErrorCode.PART_FORBIDDEN_FOR_NON_PART_CHARGE, ex.getErrorCode());
    }

    // --- addChargeItem OTHER ---

    @Test
    void addOtherChargeItemSuccessfully() {
        Long woId = workOrderService.createDraft(
                buildCreateCommand("客户F", "13900006666", "小牛N1"));

        AddWorkOrderChargeItemCommand cmd = buildOtherItem("拖车费", 1, new BigDecimal("100.00"));
        Long itemId = workOrderService.addChargeItem(woId, cmd);

        WorkOrderChargeItemEntity item = chargeItemMapper.selectById(itemId);
        assertEquals("OTHER", item.getChargeType());
        assertFalse(item.getInventoryAffecting());
        assertEquals(0, BigDecimal.ZERO.compareTo(item.getLineCostAmount()));
    }

    @Test
    void addOtherChargeItemWithPartIdFails() {
        PartEntity part = createPart("刹车片", "OTH-001", new BigDecimal("50.00"));
        Long woId = workOrderService.createDraft(
                buildCreateCommand("客户G", "13900007777", "小牛N1"));

        AddWorkOrderChargeItemCommand cmd = buildOtherItem("拖车费", 1, new BigDecimal("100.00"));
        cmd.setPartId(part.getId());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> workOrderService.addChargeItem(woId, cmd));
        assertEquals(ErrorCode.PART_FORBIDDEN_FOR_NON_PART_CHARGE, ex.getErrorCode());
    }

    // --- validation ---

    @Test
    void addChargeItemWithZeroQuantityFails() {
        Long woId = workOrderService.createDraft(
                buildCreateCommand("客户H", "13900008888", "小牛N1"));

        AddWorkOrderChargeItemCommand cmd = buildLaborItem("工时费", 0, new BigDecimal("100.00"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> workOrderService.addChargeItem(woId, cmd));
        assertEquals(ErrorCode.CHARGE_QUANTITY_INVALID, ex.getErrorCode());
    }

    @Test
    void addChargeItemWithNegativeUnitPriceFails() {
        Long woId = workOrderService.createDraft(
                buildCreateCommand("客户I", "13900009999", "小牛N1"));

        AddWorkOrderChargeItemCommand cmd = buildLaborItem("工时费", 1, new BigDecimal("-1.00"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> workOrderService.addChargeItem(woId, cmd));
        assertEquals(ErrorCode.CHARGE_PRICE_INVALID, ex.getErrorCode());
    }

    // --- updateChargeItem ---

    @Test
    void updateChargeItemAndRecalculateReceivable() {
        PartEntity part = createPart("刹车片", "UPD-001", new BigDecimal("30.00"));
        Long woId = workOrderService.createDraft(
                buildCreateCommand("客户J", "13900010000", "小牛N1"));
        Long itemId = workOrderService.addChargeItem(woId,
                buildPartItem(part.getId(), "更换刹车片", 2, new BigDecimal("80.00")));

        UpdateWorkOrderChargeItemCommand updateCmd = new UpdateWorkOrderChargeItemCommand();
        updateCmd.setStoreId(STORE_ID);
        updateCmd.setItemName("更换刹车片(更新)");
        updateCmd.setQuantity(3);
        updateCmd.setUnitPrice(new BigDecimal("100.00"));

        workOrderService.updateChargeItem(woId, itemId, updateCmd);

        WorkOrderChargeItemEntity item = chargeItemMapper.selectById(itemId);
        assertEquals("更换刹车片(更新)", item.getItemName());
        assertEquals(3, item.getQuantity());
        assertEquals(0, new BigDecimal("100.00").compareTo(item.getUnitPrice()));
        assertEquals(0, new BigDecimal("300.00").compareTo(item.getLineAmount()));
        // lineCostAmount = 3 * 30.00 = 90.00
        assertEquals(0, new BigDecimal("90.00").compareTo(item.getLineCostAmount()));

        WorkOrderEntity wo = workOrderMapper.selectById(woId);
        assertEquals(0, new BigDecimal("300.00").compareTo(wo.getReceivableAmount()));
    }

    // --- removeChargeItem ---

    @Test
    void removeChargeItemAndRecalculateReceivable() {
        PartEntity part = createPart("刹车片", "REM-001", new BigDecimal("30.00"));
        Long woId = workOrderService.createDraft(
                buildCreateCommand("客户K", "13900011111", "小牛N1"));
        Long itemId = workOrderService.addChargeItem(woId,
                buildPartItem(part.getId(), "更换刹车片", 1, new BigDecimal("80.00")));

        WorkOrderEntity woBefore = workOrderMapper.selectById(woId);
        assertEquals(0, new BigDecimal("80.00").compareTo(woBefore.getReceivableAmount()));

        workOrderService.removeChargeItem(STORE_ID, woId, itemId);

        WorkOrderChargeItemEntity item = chargeItemMapper.selectById(itemId);
        assertEquals(1, item.getDeleted());

        WorkOrderEntity woAfter = workOrderMapper.selectById(woId);
        assertEquals(0, BigDecimal.ZERO.compareTo(woAfter.getReceivableAmount()));
    }

    // --- non-draft restrictions ---

    @Test
    void nonDraftCannotAddChargeItem() {
        Long woId = workOrderService.createDraft(
                buildCreateCommand("客户L", "13900012222", "小牛N1"));

        WorkOrderEntity entity = workOrderMapper.selectById(woId);
        entity.setStatus(WorkOrderStatus.PENDING_ACCEPT.getCode());
        workOrderMapper.updateById(entity);

        AddWorkOrderChargeItemCommand cmd = buildLaborItem("工时费", 1, new BigDecimal("100.00"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> workOrderService.addChargeItem(woId, cmd));
        assertEquals(ErrorCode.WORK_ORDER_NOT_DRAFT, ex.getErrorCode());
    }

    // --- mixed charge items ---

    @Test
    void mixedChargeItemsReceivableSum() {
        PartEntity part = createPart("刹车片", "MIX-001", new BigDecimal("50.00"));
        Long woId = workOrderService.createDraft(
                buildCreateCommand("客户M", "13900013333", "小牛N1"));

        workOrderService.addChargeItem(woId,
                buildPartItem(part.getId(), "刹车片", 2, new BigDecimal("80.00")));
        workOrderService.addChargeItem(woId,
                buildLaborItem("工时费", 1, new BigDecimal("200.00")));
        workOrderService.addChargeItem(woId,
                buildOtherItem("拖车费", 1, new BigDecimal("150.00")));

        WorkOrderEntity wo = workOrderMapper.selectById(woId);
        // 2*80 + 200 + 150 = 510
        assertEquals(0, new BigDecimal("510.00").compareTo(wo.getReceivableAmount()));
    }

    // --- query ---

    @Test
    void pageQueryWorks() {
        workOrderService.createDraft(
                buildCreateCommand("客户N", "13900014444", "小牛N1"));
        workOrderService.createDraft(
                buildCreateCommand("客户O", "13900015555", "小牛M1"));

        WorkOrderQueryRequest request = new WorkOrderQueryRequest();
        request.setStoreId(STORE_ID);
        request.setPageNo(1);
        request.setPageSize(10);

        PageResponse<WorkOrderQueryResponse> response = workOrderService.pageQuery(request);
        assertEquals(2, response.total());
        assertEquals(2, response.records().size());
    }

    @Test
    void pageQueryByCustomerPhoneAndVehicleFrameNo() {
        Long firstId = workOrderService.createDraft(
                buildCreateCommand("查询客户A", "13988880001", "小牛N1"));
        Long secondId = workOrderService.createDraft(
                buildCreateCommand("查询客户B", "13988880002", "小牛M1"));
        updateFrameNo(firstId, "FRAME-QUERY-001");
        updateFrameNo(secondId, "FRAME-QUERY-002");

        WorkOrderQueryRequest phoneRequest = new WorkOrderQueryRequest();
        phoneRequest.setStoreId(STORE_ID);
        phoneRequest.setCustomerPhone("13988880001");
        PageResponse<WorkOrderQueryResponse> phonePage = workOrderService.pageQuery(phoneRequest);
        assertEquals(1, phonePage.total());
        assertEquals(firstId, phonePage.records().get(0).getId());
        assertEquals("13988880001", phonePage.records().get(0).getCustomerPhoneSnapshot());

        WorkOrderQueryRequest frameRequest = new WorkOrderQueryRequest();
        frameRequest.setStoreId(STORE_ID);
        frameRequest.setVehicleFrameNo("FRAME-QUERY-002");
        PageResponse<WorkOrderQueryResponse> framePage = workOrderService.pageQuery(frameRequest);
        assertEquals(1, framePage.total());
        assertEquals(secondId, framePage.records().get(0).getId());
        assertEquals("FRAME-QUERY-002", framePage.records().get(0).getFrameNoSnapshot());
    }

    @Test
    void pageQueryOfficialOnlyReturnsOfficialFields() {
        Long normalId = workOrderService.createDraft(
                buildCreateCommand("普通客户", "13988881111", "小牛N1"));
        Long officialId = workOrderService.createDraft(
                buildCreateCommand("官方客户", "13988882222", "小牛M1"));
        updateFrameNo(normalId, "FRAME-NORMAL-001");
        updateFrameNo(officialId, "FRAME-OFFICIAL-001");
        jdbcTemplate.update("""
                INSERT INTO official_after_sales
                    (store_id, work_order_id, is_official_after_sales, official_order_no,
                     official_settlement_status, created_by)
                VALUES (?, ?, ?, ?, ?, ?)
                """, STORE_ID, officialId, 1, "OFF-WO-QUERY-001", "PENDING", OPERATOR_ID);

        WorkOrderQueryRequest request = new WorkOrderQueryRequest();
        request.setStoreId(STORE_ID);
        request.setOfficialOnly(true);

        PageResponse<WorkOrderQueryResponse> page = workOrderService.pageQuery(request);
        assertEquals(1, page.total());
        WorkOrderQueryResponse response = page.records().get(0);
        assertEquals(officialId, response.getId());
        assertEquals("13988882222", response.getCustomerPhoneSnapshot());
        assertEquals("FRAME-OFFICIAL-001", response.getFrameNoSnapshot());
        assertTrue(response.getOfficialAfterSales());
        assertEquals("OFF-WO-QUERY-001", response.getOfficialOrderNo());
    }

    @Test
    void getDetailWorks() {
        PartEntity part = createPart("刹车片", "DTL-001", new BigDecimal("50.00"));
        Long woId = workOrderService.createDraft(
                buildCreateCommand("客户P", "13900016666", "小牛N1"));
        workOrderService.addChargeItem(woId,
                buildPartItem(part.getId(), "刹车片", 1, new BigDecimal("80.00")));
        workOrderService.addChargeItem(woId,
                buildLaborItem("工时费", 1, new BigDecimal("200.00")));

        WorkOrderDetailResponse detail = workOrderService.getById(woId);
        assertNotNull(detail);
        assertNotNull(detail.getWorkOrderNo());
        assertEquals(WorkOrderStatus.DRAFT.getCode(), detail.getStatus());
        assertEquals(2, detail.getChargeItems().size());
        assertEquals(0, new BigDecimal("280.00").compareTo(detail.getReceivableAmount()));
    }

    // --- cross-store boundary tests ---

    @Test
    void addChargeItemWithCrossStorePartFails() {
        // Create part in OTHER_STORE_ID
        CreatePartCommand partCmd = new CreatePartCommand();
        partCmd.setStoreId(OTHER_STORE_ID);
        partCmd.setOperatorId(OPERATOR_ID);
        partCmd.setPartName("刹车片");
        partCmd.setOfficialPartNo("XS-WO-001");
        partCmd.setReferenceCostPrice(new BigDecimal("50.00"));
        PartEntity part = partService.createOfficialPart(partCmd);

        Long woId = workOrderService.createDraft(
                buildCreateCommand("客户XS1", "13900020001", "小牛N1"));

        AddWorkOrderChargeItemCommand cmd = buildPartItemForStore(
                STORE_ID, part.getId(), "跨店配件", 1, new BigDecimal("80.00"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> workOrderService.addChargeItem(woId, cmd));
        assertEquals(ErrorCode.PART_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void createDraftWithCrossStorePartInInitialItemsFails() {
        PartEntity part = createPart("刹车片", "XS-WO-002", new BigDecimal("50.00"));

        CreateDraftWorkOrderCommand command = buildCreateCommand(
                "客户XS2", "13900020002", "小牛N1");
        command.setChargeItems(List.of(
                buildPartInput(part.getId(), "跨店配件", 1, new BigDecimal("80.00"))));

        // Part is in STORE_ID, work order is in STORE_ID -> should succeed
        Long id = workOrderService.createDraft(command);
        assertNotNull(id);

        // Now create a work order in OTHER_STORE and try to reference the same part
        CreateDraftWorkOrderCommand crossStoreCmd = buildCreateCommand(
                "客户XS3", "13900020003", "小牛N1");
        crossStoreCmd.setStoreId(OTHER_STORE_ID);
        crossStoreCmd.setChargeItems(List.of(
                buildPartInput(part.getId(), "跨店配件", 1, new BigDecimal("80.00"))));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> workOrderService.createDraft(crossStoreCmd));
        assertEquals(ErrorCode.PART_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void updateDraftWithCrossStoreWorkOrderFails() {
        Long woId = workOrderService.createDraft(
                buildCreateCommand("客户XS4", "13900020004", "小牛N1"));

        UpdateWorkOrderDraftCommand updateCmd = new UpdateWorkOrderDraftCommand();
        updateCmd.setStoreId(OTHER_STORE_ID);
        updateCmd.setCustomerNameSnapshot("不应该成功");
        updateCmd.setOperatorId(OPERATOR_ID);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> workOrderService.updateDraft(woId, updateCmd));
        assertEquals(ErrorCode.WORK_ORDER_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void addChargeItemToCrossStoreWorkOrderFails() {
        Long woId = workOrderService.createDraft(
                buildCreateCommand("客户XS5", "13900020005", "小牛N1"));

        AddWorkOrderChargeItemCommand cmd = buildLaborItem("工时费", 1, new BigDecimal("100.00"));
        cmd.setStoreId(OTHER_STORE_ID);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> workOrderService.addChargeItem(woId, cmd));
        assertEquals(ErrorCode.WORK_ORDER_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void removeChargeItemToCrossStoreWorkOrderFails() {
        Long woId = workOrderService.createDraft(
                buildCreateCommand("客户XS6", "13900020006", "小牛N1"));
        Long itemId = workOrderService.addChargeItem(woId,
                buildLaborItem("工时费", 1, new BigDecimal("100.00")));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> workOrderService.removeChargeItem(OTHER_STORE_ID, woId, itemId));
        assertEquals(ErrorCode.WORK_ORDER_NOT_FOUND, ex.getErrorCode());
    }

    // --- storeId null tests ---

    @Test
    void updateDraftWithNullStoreIdFails() {
        Long woId = workOrderService.createDraft(
                buildCreateCommand("客户NPE1", "13900030001", "小牛N1"));

        UpdateWorkOrderDraftCommand updateCmd = new UpdateWorkOrderDraftCommand();
        updateCmd.setStoreId(null);
        updateCmd.setCustomerNameSnapshot("不应该成功");
        updateCmd.setOperatorId(OPERATOR_ID);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> workOrderService.updateDraft(woId, updateCmd));
        assertEquals(ErrorCode.COMMON_BAD_REQUEST, ex.getErrorCode());
    }

    @Test
    void addChargeItemWithNullStoreIdFails() {
        Long woId = workOrderService.createDraft(
                buildCreateCommand("客户NPE2", "13900030002", "小牛N1"));

        AddWorkOrderChargeItemCommand cmd = buildLaborItem("工时费", 1, new BigDecimal("100.00"));
        cmd.setStoreId(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> workOrderService.addChargeItem(woId, cmd));
        assertEquals(ErrorCode.COMMON_BAD_REQUEST, ex.getErrorCode());
    }

    @Test
    void updateChargeItemWithNullStoreIdFails() {
        Long woId = workOrderService.createDraft(
                buildCreateCommand("客户NPE3", "13900030003", "小牛N1"));
        Long itemId = workOrderService.addChargeItem(woId,
                buildLaborItem("工时费", 1, new BigDecimal("100.00")));

        UpdateWorkOrderChargeItemCommand updateCmd = new UpdateWorkOrderChargeItemCommand();
        updateCmd.setStoreId(null);
        updateCmd.setQuantity(2);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> workOrderService.updateChargeItem(woId, itemId, updateCmd));
        assertEquals(ErrorCode.COMMON_BAD_REQUEST, ex.getErrorCode());
    }

    @Test
    void removeChargeItemWithNullStoreIdFails() {
        Long woId = workOrderService.createDraft(
                buildCreateCommand("客户NPE4", "13900030004", "小牛N1"));
        Long itemId = workOrderService.addChargeItem(woId,
                buildLaborItem("工时费", 1, new BigDecimal("100.00")));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> workOrderService.removeChargeItem(null, woId, itemId));
        assertEquals(ErrorCode.COMMON_BAD_REQUEST, ex.getErrorCode());
    }

    // --- cost snapshot normalization ---

    @Test
    void laborChargeItemCostPriceIsZero() {
        Long woId = workOrderService.createDraft(
                buildCreateCommand("客户COST1", "13900031001", "小牛N1"));
        Long itemId = workOrderService.addChargeItem(woId,
                buildLaborItem("工时费", 1, new BigDecimal("200.00")));

        WorkOrderChargeItemEntity item = chargeItemMapper.selectById(itemId);
        assertEquals(0, BigDecimal.ZERO.compareTo(item.getCostPriceSnapshot()));
        assertEquals(0, BigDecimal.ZERO.compareTo(item.getLineCostAmount()));
    }

    @Test
    void otherChargeItemCostPriceIsZero() {
        Long woId = workOrderService.createDraft(
                buildCreateCommand("客户COST2", "13900031002", "小牛N1"));
        Long itemId = workOrderService.addChargeItem(woId,
                buildOtherItem("拖车费", 1, new BigDecimal("100.00")));

        WorkOrderChargeItemEntity item = chargeItemMapper.selectById(itemId);
        assertEquals(0, BigDecimal.ZERO.compareTo(item.getCostPriceSnapshot()));
        assertEquals(0, BigDecimal.ZERO.compareTo(item.getLineCostAmount()));
    }

    @Test
    void partChargeItemWithNullCostPriceUsesZero() {
        // Create part with null referenceCostPrice
        CreatePartCommand partCmd = new CreatePartCommand();
        partCmd.setStoreId(STORE_ID);
        partCmd.setOperatorId(OPERATOR_ID);
        partCmd.setPartName("无成本价配件");
        partCmd.setOfficialPartNo("COST-NULL-001");
        partCmd.setReferenceCostPrice(null);
        PartEntity part = partService.createOfficialPart(partCmd);

        Long woId = workOrderService.createDraft(
                buildCreateCommand("客户COST3", "13900031003", "小牛N1"));
        Long itemId = workOrderService.addChargeItem(woId,
                buildPartItem(part.getId(), "更换无成本配件", 3, new BigDecimal("50.00")));

        WorkOrderChargeItemEntity item = chargeItemMapper.selectById(itemId);
        assertEquals(0, BigDecimal.ZERO.compareTo(item.getCostPriceSnapshot()));
        // lineCostAmount = 3 * 0 = 0
        assertEquals(0, BigDecimal.ZERO.compareTo(item.getLineCostAmount()));
        // lineAmount = 3 * 50 = 150
        assertEquals(0, new BigDecimal("150.00").compareTo(item.getLineAmount()));
        assertTrue(item.getInventoryAffecting());
    }

    @Test
    void createDraftWithInitialItemsNoInventoryFlow() {
        PartEntity part = createPart("刹车片", "NF-001", new BigDecimal("50.00"));
        CreateDraftWorkOrderCommand command = buildCreateCommand("客户NF", "13900032001", "小牛N1");
        command.setChargeItems(List.of(
                buildPartInput(part.getId(), "刹车片更换", 2, new BigDecimal("80.00")),
                buildLaborInput("工时费", 1, new BigDecimal("200.00"))));

        workOrderService.createDraft(command);

        assertEquals(0, inventoryFlowMapper.selectCount(null));
    }

    // --- submit ---

    @Test
    void submitDraftSuccessfullyChangesStatusAndWritesLog() {
        Long woId = workOrderService.createDraft(
                buildCreateCommand("提交客户1", "13900040001", "小牛N1"));

        workOrderService.submit(buildSubmitCommand(woId));

        WorkOrderEntity workOrder = workOrderMapper.selectById(woId);
        assertEquals(WorkOrderStatus.PENDING_ACCEPT.getCode(), workOrder.getStatus());
        assertEquals(OPERATOR_ID, workOrder.getSubmittedBy());
        assertNotNull(workOrder.getSubmittedAt());

        List<WorkOrderStatusLogEntity> logs = statusLogMapper.selectByWorkOrderId(woId);
        WorkOrderStatusLogEntity submitLog = logs.get(logs.size() - 1);
        assertEquals(WorkOrderStatus.DRAFT.getCode(), submitLog.getFromStatus());
        assertEquals(WorkOrderStatus.PENDING_ACCEPT.getCode(), submitLog.getToStatus());
        assertEquals("SUBMIT", submitLog.getActionType());
        assertEquals("提交工单", submitLog.getReason());
    }

    @Test
    void submitWithPartChargeItemReservesInventoryAndWritesFlow() {
        PartEntity part = createPart("轮胎", "SUB-001", new BigDecimal("30.00"));
        inbound(part.getId(), 10);
        Long woId = workOrderService.createDraft(
                buildCreateCommand("提交客户2", "13900040002", "小牛N1"));
        workOrderService.addChargeItem(woId,
                buildPartItem(part.getId(), "更换轮胎", 3, new BigDecimal("80.00")));

        workOrderService.submit(buildSubmitCommand(woId));

        InventoryStockEntity stock = inventoryStockMapper.selectByStoreIdAndPartId(STORE_ID, part.getId());
        assertEquals(10, stock.getActualQty());
        assertEquals(7, stock.getAvailableQty());
        assertEquals(3, stock.getReservedQty());

        List<InventoryFlowEntity> flows = inventoryFlowMapper.selectList(null);
        InventoryFlowEntity reserveFlow = flows.stream()
                .filter(flow -> InventoryFlowType.RESERVE.getCode().equals(flow.getFlowType()))
                .findFirst()
                .orElseThrow();
        assertEquals(3, reserveFlow.getQuantityDelta());
        assertEquals(10, reserveFlow.getActualBefore());
        assertEquals(10, reserveFlow.getActualAfter());
        assertEquals(10, reserveFlow.getAvailableBefore());
        assertEquals(7, reserveFlow.getAvailableAfter());
        assertEquals(0, reserveFlow.getReservedBefore());
        assertEquals(3, reserveFlow.getReservedAfter());
        assertEquals("WORK_ORDER", reserveFlow.getBusinessType());
        assertEquals(woId, reserveFlow.getBusinessId());
        assertEquals(woId, reserveFlow.getWorkOrderId());
    }

    @Test
    void submitWithOnlyLaborAndOtherDoesNotWriteInventoryFlow() {
        Long woId = workOrderService.createDraft(
                buildCreateCommand("提交客户3", "13900040003", "小牛N1"));
        workOrderService.addChargeItem(woId,
                buildLaborItem("工时费", 1, new BigDecimal("200.00")));
        workOrderService.addChargeItem(woId,
                buildOtherItem("拖车费", 1, new BigDecimal("100.00")));

        workOrderService.submit(buildSubmitCommand(woId));

        assertEquals(WorkOrderStatus.PENDING_ACCEPT.getCode(), workOrderMapper.selectById(woId).getStatus());
        assertEquals(0, countReserveFlows());
    }

    @Test
    void submitWithWrongStoreIdFails() {
        PartEntity part = createPart("跨店提交配件", "SUB-XS-001", new BigDecimal("30.00"));
        inbound(part.getId(), 10);
        Long woId = workOrderService.createDraft(
                buildCreateCommand("提交客户XS", "13900040014", "小牛N1"));
        workOrderService.addChargeItem(woId,
                buildPartItem(part.getId(), "跨店提交配件", 2, new BigDecimal("80.00")));
        int statusLogCountBefore = statusLogMapper.selectByWorkOrderId(woId).size();

        SubmitWorkOrderCommand command = buildSubmitCommand(woId);
        command.setStoreId(OTHER_STORE_ID);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> workOrderService.submit(command));
        assertEquals(ErrorCode.WORK_ORDER_NOT_FOUND, ex.getErrorCode());
        assertEquals(WorkOrderStatus.DRAFT.getCode(), workOrderMapper.selectById(woId).getStatus());
        InventoryStockEntity stock = inventoryStockMapper.selectByStoreIdAndPartId(STORE_ID, part.getId());
        assertEquals(10, stock.getAvailableQty());
        assertEquals(0, stock.getReservedQty());
        assertEquals(0, countReserveFlows());
        assertEquals(statusLogCountBefore, statusLogMapper.selectByWorkOrderId(woId).size());
    }

    @Test
    void submitWithNullStoreIdFails() {
        PartEntity part = createPart("空门店提交配件", "SUB-NULL-STORE-001", new BigDecimal("30.00"));
        inbound(part.getId(), 10);
        Long woId = workOrderService.createDraft(
                buildCreateCommand("提交客户NS", "13900040015", "小牛N1"));
        workOrderService.addChargeItem(woId,
                buildPartItem(part.getId(), "空门店提交配件", 2, new BigDecimal("80.00")));
        int statusLogCountBefore = statusLogMapper.selectByWorkOrderId(woId).size();

        SubmitWorkOrderCommand command = buildSubmitCommand(woId);
        command.setStoreId(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> workOrderService.submit(command));
        assertEquals(ErrorCode.COMMON_BAD_REQUEST, ex.getErrorCode());
        assertEquals(WorkOrderStatus.DRAFT.getCode(), workOrderMapper.selectById(woId).getStatus());
        InventoryStockEntity stock = inventoryStockMapper.selectByStoreIdAndPartId(STORE_ID, part.getId());
        assertEquals(10, stock.getAvailableQty());
        assertEquals(0, stock.getReservedQty());
        assertEquals(0, countReserveFlows());
        assertEquals(statusLogCountBefore, statusLogMapper.selectByWorkOrderId(woId).size());
    }

    @Test
    void submitWithNullOperatorIdFails() {
        PartEntity part = createPart("空操作人提交配件", "SUB-NULL-OP-001", new BigDecimal("30.00"));
        inbound(part.getId(), 10);
        Long woId = workOrderService.createDraft(
                buildCreateCommand("提交客户NO", "13900040016", "小牛N1"));
        workOrderService.addChargeItem(woId,
                buildPartItem(part.getId(), "空操作人提交配件", 2, new BigDecimal("80.00")));
        int statusLogCountBefore = statusLogMapper.selectByWorkOrderId(woId).size();

        SubmitWorkOrderCommand command = buildSubmitCommand(woId);
        command.setOperatorId(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> workOrderService.submit(command));
        assertEquals(ErrorCode.COMMON_BAD_REQUEST, ex.getErrorCode());
        assertEquals(WorkOrderStatus.DRAFT.getCode(), workOrderMapper.selectById(woId).getStatus());
        InventoryStockEntity stock = inventoryStockMapper.selectByStoreIdAndPartId(STORE_ID, part.getId());
        assertEquals(10, stock.getAvailableQty());
        assertEquals(0, stock.getReservedQty());
        assertEquals(0, countReserveFlows());
        assertEquals(statusLogCountBefore, statusLogMapper.selectByWorkOrderId(woId).size());
    }

    @Test
    void submitNonDraftFailsWithoutInventoryChangeOrFlow() {
        PartEntity part = createPart("刹车盘", "SUB-002", new BigDecimal("50.00"));
        inbound(part.getId(), 5);
        Long woId = workOrderService.createDraft(
                buildCreateCommand("提交客户4", "13900040004", "小牛N1"));
        workOrderService.addChargeItem(woId,
                buildPartItem(part.getId(), "更换刹车盘", 2, new BigDecimal("100.00")));

        WorkOrderEntity entity = workOrderMapper.selectById(woId);
        entity.setStatus(WorkOrderStatus.PENDING_ACCEPT.getCode());
        workOrderMapper.updateById(entity);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> workOrderService.submit(buildSubmitCommand(woId)));
        assertEquals(ErrorCode.WORK_ORDER_NOT_DRAFT, ex.getErrorCode());

        InventoryStockEntity stock = inventoryStockMapper.selectByStoreIdAndPartId(STORE_ID, part.getId());
        assertEquals(5, stock.getAvailableQty());
        assertEquals(0, stock.getReservedQty());
        assertEquals(0, countReserveFlows());
    }

    @Test
    void submitFailsWhenInventoryNotEnoughAndRollsBack() {
        PartEntity part = createPart("控制器", "SUB-003", new BigDecimal("200.00"));
        inbound(part.getId(), 2);
        Long woId = workOrderService.createDraft(
                buildCreateCommand("提交客户5", "13900040005", "小牛N1"));
        workOrderService.addChargeItem(woId,
                buildPartItem(part.getId(), "更换控制器", 3, new BigDecimal("300.00")));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> workOrderService.submit(buildSubmitCommand(woId)));
        assertEquals(ErrorCode.INVENTORY_AVAILABLE_NOT_ENOUGH, ex.getErrorCode());

        assertEquals(WorkOrderStatus.DRAFT.getCode(), workOrderMapper.selectById(woId).getStatus());
        InventoryStockEntity stock = inventoryStockMapper.selectByStoreIdAndPartId(STORE_ID, part.getId());
        assertEquals(2, stock.getAvailableQty());
        assertEquals(0, stock.getReservedQty());
        assertEquals(0, countReserveFlows());
    }

    @Test
    void submitFailsWhenInventoryStockMissing() {
        PartEntity part = createPart("灯泡", "SUB-004", new BigDecimal("10.00"));
        Long woId = workOrderService.createDraft(
                buildCreateCommand("提交客户6", "13900040006", "小牛N1"));
        workOrderService.addChargeItem(woId,
                buildPartItem(part.getId(), "更换灯泡", 1, new BigDecimal("30.00")));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> workOrderService.submit(buildSubmitCommand(woId)));
        assertEquals(ErrorCode.PART_STOCK_NOT_FOUND, ex.getErrorCode());
        assertEquals(WorkOrderStatus.DRAFT.getCode(), workOrderMapper.selectById(woId).getStatus());
        assertEquals(0, countReserveFlows());
    }

    @Test
    void submitFailsWhenPartDisabledAfterDraft() {
        PartEntity part = createPart("轴承", "SUB-005", new BigDecimal("20.00"));
        inbound(part.getId(), 5);
        Long woId = workOrderService.createDraft(
                buildCreateCommand("提交客户7", "13900040007", "小牛N1"));
        workOrderService.addChargeItem(woId,
                buildPartItem(part.getId(), "更换轴承", 1, new BigDecimal("60.00")));
        partService.disablePart(STORE_ID, part.getId());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> workOrderService.submit(buildSubmitCommand(woId)));
        assertEquals(ErrorCode.PART_DISABLED, ex.getErrorCode());
        assertEquals(WorkOrderStatus.DRAFT.getCode(), workOrderMapper.selectById(woId).getStatus());
        assertEquals(0, countReserveFlows());
    }

    @Test
    void submitAggregatesSamePartChargeItemsBeforeReserve() {
        PartEntity part = createPart("把手", "SUB-006", new BigDecimal("15.00"));
        inbound(part.getId(), 5);
        Long woId = workOrderService.createDraft(
                buildCreateCommand("提交客户8", "13900040008", "小牛N1"));
        workOrderService.addChargeItem(woId,
                buildPartItem(part.getId(), "左把手", 2, new BigDecimal("40.00")));
        workOrderService.addChargeItem(woId,
                buildPartItem(part.getId(), "右把手", 3, new BigDecimal("40.00")));

        workOrderService.submit(buildSubmitCommand(woId));

        InventoryStockEntity stock = inventoryStockMapper.selectByStoreIdAndPartId(STORE_ID, part.getId());
        assertEquals(0, stock.getAvailableQty());
        assertEquals(5, stock.getReservedQty());
        InventoryFlowEntity reserveFlow = inventoryFlowMapper.selectList(null).stream()
                .filter(flow -> InventoryFlowType.RESERVE.getCode().equals(flow.getFlowType()))
                .findFirst()
                .orElseThrow();
        assertEquals(5, reserveFlow.getQuantityDelta());
        assertEquals(1, countReserveFlows());
    }

    @Test
    void submitAggregatedSamePartFailsWhenTotalQuantityNotEnough() {
        PartEntity part = createPart("坐垫", "SUB-007", new BigDecimal("50.00"));
        inbound(part.getId(), 4);
        Long woId = workOrderService.createDraft(
                buildCreateCommand("提交客户9", "13900040009", "小牛N1"));
        workOrderService.addChargeItem(woId,
                buildPartItem(part.getId(), "坐垫A", 2, new BigDecimal("80.00")));
        workOrderService.addChargeItem(woId,
                buildPartItem(part.getId(), "坐垫B", 3, new BigDecimal("80.00")));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> workOrderService.submit(buildSubmitCommand(woId)));
        assertEquals(ErrorCode.INVENTORY_AVAILABLE_NOT_ENOUGH, ex.getErrorCode());
        InventoryStockEntity stock = inventoryStockMapper.selectByStoreIdAndPartId(STORE_ID, part.getId());
        assertEquals(4, stock.getAvailableQty());
        assertEquals(0, stock.getReservedQty());
        assertEquals(WorkOrderStatus.DRAFT.getCode(), workOrderMapper.selectById(woId).getStatus());
        assertEquals(0, countReserveFlows());
    }

    @Test
    void repeatedSubmitFailsWithoutDuplicateReserve() {
        PartEntity part = createPart("脚撑", "SUB-008", new BigDecimal("25.00"));
        inbound(part.getId(), 5);
        Long woId = workOrderService.createDraft(
                buildCreateCommand("提交客户10", "13900040010", "小牛N1"));
        workOrderService.addChargeItem(woId,
                buildPartItem(part.getId(), "更换脚撑", 2, new BigDecimal("70.00")));

        workOrderService.submit(buildSubmitCommand(woId));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> workOrderService.submit(buildSubmitCommand(woId)));

        assertEquals(ErrorCode.WORK_ORDER_NOT_DRAFT, ex.getErrorCode());
        InventoryStockEntity stock = inventoryStockMapper.selectByStoreIdAndPartId(STORE_ID, part.getId());
        assertEquals(3, stock.getAvailableQty());
        assertEquals(2, stock.getReservedQty());
        assertEquals(1, countReserveFlows());
    }

    @Test
    void submitRecalculatesReceivableAmountFromActiveChargeItems() {
        Long woId = workOrderService.createDraft(
                buildCreateCommand("提交客户11", "13900040011", "小牛N1"));
        workOrderService.addChargeItem(woId,
                buildLaborItem("工时费", 2, new BigDecimal("100.00")));
        WorkOrderEntity workOrder = workOrderMapper.selectById(woId);
        workOrder.setReceivableAmount(new BigDecimal("999.00"));
        workOrderMapper.updateById(workOrder);

        workOrderService.submit(buildSubmitCommand(woId));

        assertEquals(0, new BigDecimal("200.00")
                .compareTo(workOrderMapper.selectById(woId).getReceivableAmount()));
    }

    @Test
    void submitFailureRollsBackPreviousPartReserveInSameTransaction() {
        PartEntity enoughPart = createPart("后视镜", "SUB-009", new BigDecimal("20.00"));
        PartEntity shortPart = createPart("电机", "SUB-010", new BigDecimal("500.00"));
        inbound(enoughPart.getId(), 5);
        inbound(shortPart.getId(), 1);
        Long woId = workOrderService.createDraft(
                buildCreateCommand("提交客户12", "13900040012", "小牛N1"));
        workOrderService.addChargeItem(woId,
                buildPartItem(enoughPart.getId(), "更换后视镜", 2, new BigDecimal("60.00")));
        workOrderService.addChargeItem(woId,
                buildPartItem(shortPart.getId(), "更换电机", 2, new BigDecimal("800.00")));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> workOrderService.submit(buildSubmitCommand(woId)));
        assertEquals(ErrorCode.INVENTORY_AVAILABLE_NOT_ENOUGH, ex.getErrorCode());

        InventoryStockEntity enoughStock = inventoryStockMapper.selectByStoreIdAndPartId(STORE_ID, enoughPart.getId());
        InventoryStockEntity shortStock = inventoryStockMapper.selectByStoreIdAndPartId(STORE_ID, shortPart.getId());
        assertEquals(5, enoughStock.getAvailableQty());
        assertEquals(0, enoughStock.getReservedQty());
        assertEquals(1, shortStock.getAvailableQty());
        assertEquals(0, shortStock.getReservedQty());
        assertEquals(WorkOrderStatus.DRAFT.getCode(), workOrderMapper.selectById(woId).getStatus());
        assertEquals(0, countReserveFlows());
    }

    @Test
    void submittedWorkOrderCannotUseDraftEditMethods() {
        Long woId = workOrderService.createDraft(
                buildCreateCommand("提交客户13", "13900040013", "小牛N1"));
        Long itemId = workOrderService.addChargeItem(woId,
                buildLaborItem("工时费", 1, new BigDecimal("100.00")));
        workOrderService.submit(buildSubmitCommand(woId));

        UpdateWorkOrderDraftCommand updateDraftCommand = new UpdateWorkOrderDraftCommand();
        updateDraftCommand.setStoreId(STORE_ID);
        updateDraftCommand.setCustomerNameSnapshot("不应成功");
        updateDraftCommand.setOperatorId(OPERATOR_ID);
        assertEquals(ErrorCode.WORK_ORDER_NOT_DRAFT, assertThrows(BusinessException.class,
                () -> workOrderService.updateDraft(woId, updateDraftCommand)).getErrorCode());

        assertEquals(ErrorCode.WORK_ORDER_NOT_DRAFT, assertThrows(BusinessException.class,
                () -> workOrderService.addChargeItem(woId,
                        buildLaborItem("追加工时", 1, new BigDecimal("50.00")))).getErrorCode());

        UpdateWorkOrderChargeItemCommand updateItemCommand = new UpdateWorkOrderChargeItemCommand();
        updateItemCommand.setStoreId(STORE_ID);
        updateItemCommand.setQuantity(2);
        assertEquals(ErrorCode.WORK_ORDER_NOT_DRAFT, assertThrows(BusinessException.class,
                () -> workOrderService.updateChargeItem(woId, itemId, updateItemCommand)).getErrorCode());

        assertEquals(ErrorCode.WORK_ORDER_NOT_DRAFT, assertThrows(BusinessException.class,
                () -> workOrderService.removeChargeItem(STORE_ID, woId, itemId)).getErrorCode());
    }

    // --- settle ---

    @Test
    void settleSubmittedWorkOrderSuccessfully() {
        Long woId = createSubmittedLaborWorkOrder(new BigDecimal("100.00"));
        recordPayment(woId, new BigDecimal("100.00"));
        int statusLogCountBefore = statusLogMapper.selectByWorkOrderId(woId).size();

        workOrderService.settle(buildSettleCommand(woId));

        WorkOrderEntity workOrder = workOrderMapper.selectById(woId);
        assertEquals(WorkOrderStatus.SETTLED.getCode(), workOrder.getStatus());
        assertEquals(OPERATOR_ID, workOrder.getSettledBy());
        assertNotNull(workOrder.getSettledAt());
        List<WorkOrderStatusLogEntity> logs = statusLogMapper.selectByWorkOrderId(woId);
        assertEquals(statusLogCountBefore + 1, logs.size());
        WorkOrderStatusLogEntity settleLog = logs.get(logs.size() - 1);
        assertEquals(WorkOrderStatus.PENDING_ACCEPT.getCode(), settleLog.getFromStatus());
        assertEquals(WorkOrderStatus.SETTLED.getCode(), settleLog.getToStatus());
        assertEquals("SETTLE", settleLog.getActionType());
    }

    @Test
    void settleWithPartChargeItemConsumesReservedInventory() {
        PartEntity part = createPart("结算轮胎", "SET-001", new BigDecimal("30.00"));
        inbound(part.getId(), 10);
        Long woId = createSubmittedPartWorkOrder(part.getId(), 3, new BigDecimal("80.00"));
        recordPayment(woId, new BigDecimal("240.00"));

        workOrderService.settle(buildSettleCommand(woId));

        InventoryStockEntity stock = inventoryStockMapper.selectByStoreIdAndPartId(STORE_ID, part.getId());
        assertEquals(7, stock.getActualQty());
        assertEquals(7, stock.getAvailableQty());
        assertEquals(0, stock.getReservedQty());
        InventoryFlowEntity consumeFlow = inventoryFlowMapper.selectList(null).stream()
                .filter(flow -> InventoryFlowType.CONSUME.getCode().equals(flow.getFlowType()))
                .findFirst()
                .orElseThrow();
        assertEquals(3, consumeFlow.getQuantityDelta());
        assertEquals(10, consumeFlow.getActualBefore());
        assertEquals(7, consumeFlow.getActualAfter());
        assertEquals(7, consumeFlow.getAvailableBefore());
        assertEquals(7, consumeFlow.getAvailableAfter());
        assertEquals(3, consumeFlow.getReservedBefore());
        assertEquals(0, consumeFlow.getReservedAfter());
        assertEquals("WORK_ORDER", consumeFlow.getBusinessType());
        assertEquals(woId, consumeFlow.getWorkOrderId());
    }

    @Test
    void settleLaborOnlyWorkOrderDoesNotWriteInventoryFlow() {
        Long woId = createSubmittedLaborWorkOrder(new BigDecimal("100.00"));
        recordPayment(woId, new BigDecimal("100.00"));

        workOrderService.settle(buildSettleCommand(woId));

        assertEquals(WorkOrderStatus.SETTLED.getCode(), workOrderMapper.selectById(woId).getStatus());
        assertEquals(0, inventoryFlowMapper.selectCount(null));
    }

    @Test
    void draftWorkOrderCannotSettle() {
        Long woId = workOrderService.createDraft(
                buildCreateCommand("结算草稿", "13900060001", "小牛N1"));
        int statusLogCountBefore = statusLogMapper.selectByWorkOrderId(woId).size();

        BusinessException ex = assertThrows(BusinessException.class,
                () -> workOrderService.settle(buildSettleCommand(woId)));

        assertEquals(ErrorCode.WORK_ORDER_SETTLE_NOT_ALLOWED, ex.getErrorCode());
        assertEquals(WorkOrderStatus.DRAFT.getCode(), workOrderMapper.selectById(woId).getStatus());
        assertEquals(0, countConsumeFlows());
        assertEquals(statusLogCountBefore, statusLogMapper.selectByWorkOrderId(woId).size());
    }

    @Test
    void cancelledWorkOrderCannotSettle() {
        Long woId = createSubmittedLaborWorkOrder(new BigDecimal("100.00"));
        recordPayment(woId, new BigDecimal("100.00"));
        WorkOrderEntity workOrder = workOrderMapper.selectById(woId);
        workOrder.setStatus(WorkOrderStatus.CANCELLED.getCode());
        workOrderMapper.updateById(workOrder);
        int statusLogCountBefore = statusLogMapper.selectByWorkOrderId(woId).size();

        BusinessException ex = assertThrows(BusinessException.class,
                () -> workOrderService.settle(buildSettleCommand(woId)));

        assertEquals(ErrorCode.WORK_ORDER_SETTLE_NOT_ALLOWED, ex.getErrorCode());
        assertEquals(WorkOrderStatus.CANCELLED.getCode(), workOrderMapper.selectById(woId).getStatus());
        assertEquals(0, countConsumeFlows());
        assertEquals(statusLogCountBefore, statusLogMapper.selectByWorkOrderId(woId).size());
    }

    @Test
    void repeatedSettleFailsWithoutDuplicateConsumeOrLog() {
        PartEntity part = createPart("重复结算件", "SET-002", new BigDecimal("20.00"));
        inbound(part.getId(), 5);
        Long woId = createSubmittedPartWorkOrder(part.getId(), 2, new BigDecimal("50.00"));
        recordPayment(woId, new BigDecimal("100.00"));
        workOrderService.settle(buildSettleCommand(woId));
        int statusLogCountBefore = statusLogMapper.selectByWorkOrderId(woId).size();

        BusinessException ex = assertThrows(BusinessException.class,
                () -> workOrderService.settle(buildSettleCommand(woId)));

        assertEquals(ErrorCode.WORK_ORDER_SETTLE_NOT_ALLOWED, ex.getErrorCode());
        InventoryStockEntity stock = inventoryStockMapper.selectByStoreIdAndPartId(STORE_ID, part.getId());
        assertEquals(3, stock.getActualQty());
        assertEquals(0, stock.getReservedQty());
        assertEquals(1, countConsumeFlows());
        assertEquals(statusLogCountBefore, statusLogMapper.selectByWorkOrderId(woId).size());
    }

    @Test
    void settleFailsWhenReceivedAmountNotEnough() {
        PartEntity part = createPart("金额不足件", "SET-003", new BigDecimal("20.00"));
        inbound(part.getId(), 5);
        Long woId = createSubmittedPartWorkOrder(part.getId(), 2, new BigDecimal("50.00"));
        recordPayment(woId, new BigDecimal("99.00"));
        InventoryStockEntity before = inventoryStockMapper.selectByStoreIdAndPartId(STORE_ID, part.getId());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> workOrderService.settle(buildSettleCommand(woId)));

        assertEquals(ErrorCode.WORK_ORDER_RECEIVED_AMOUNT_NOT_ENOUGH, ex.getErrorCode());
        assertUnchangedDraftLikeSettleFailure(woId, WorkOrderStatus.PENDING_ACCEPT.getCode());
        assertStockUnchanged(part.getId(), before);
    }

    @Test
    void settleFailsWhenRefundMakesReceivedAmountNotEnough() {
        Long woId = createSubmittedLaborWorkOrder(new BigDecimal("100.00"));
        recordPayment(woId, new BigDecimal("100.00"));
        recordRefund(woId, new BigDecimal("1.00"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> workOrderService.settle(buildSettleCommand(woId)));

        assertEquals(ErrorCode.WORK_ORDER_RECEIVED_AMOUNT_NOT_ENOUGH, ex.getErrorCode());
        assertUnchangedDraftLikeSettleFailure(woId, WorkOrderStatus.PENDING_ACCEPT.getCode());
    }

    @Test
    void settleAllowsOverPayment() {
        Long woId = createSubmittedLaborWorkOrder(new BigDecimal("100.00"));
        recordPayment(woId, new BigDecimal("120.00"));

        workOrderService.settle(buildSettleCommand(woId));

        WorkOrderEntity workOrder = workOrderMapper.selectById(woId);
        assertEquals(WorkOrderStatus.SETTLED.getCode(), workOrder.getStatus());
        assertEquals(0, new BigDecimal("120.00").compareTo(workOrder.getReceivedAmount()));
    }

    @Test
    void settleAggregatesSamePartBeforeConsume() {
        PartEntity part = createPart("汇总结算件", "SET-004", new BigDecimal("20.00"));
        inbound(part.getId(), 5);
        Long woId = workOrderService.createDraft(
                buildCreateCommand("结算汇总", "13900060004", "小牛N1"));
        workOrderService.addChargeItem(woId, buildPartItem(part.getId(), "件1", 2, new BigDecimal("30.00")));
        workOrderService.addChargeItem(woId, buildPartItem(part.getId(), "件2", 3, new BigDecimal("30.00")));
        workOrderService.submit(buildSubmitCommand(woId));
        recordPayment(woId, new BigDecimal("150.00"));

        workOrderService.settle(buildSettleCommand(woId));

        InventoryStockEntity stock = inventoryStockMapper.selectByStoreIdAndPartId(STORE_ID, part.getId());
        assertEquals(0, stock.getActualQty());
        assertEquals(0, stock.getAvailableQty());
        assertEquals(0, stock.getReservedQty());
        assertEquals(1, countConsumeFlows());
        InventoryFlowEntity flow = inventoryFlowMapper.selectList(null).stream()
                .filter(f -> InventoryFlowType.CONSUME.getCode().equals(f.getFlowType()))
                .findFirst()
                .orElseThrow();
        assertEquals(5, flow.getQuantityDelta());
    }

    @Test
    void settleFailsWhenAggregatedReservedQtyNotEnough() {
        PartEntity part = createPart("预占不足件", "SET-005", new BigDecimal("20.00"));
        inbound(part.getId(), 5);
        Long woId = createSubmittedPartWorkOrder(part.getId(), 5, new BigDecimal("30.00"));
        recordPayment(woId, new BigDecimal("150.00"));
        InventoryStockEntity stock = inventoryStockMapper.selectByStoreIdAndPartId(STORE_ID, part.getId());
        stock.setReservedQty(4);
        inventoryStockMapper.updateById(stock);
        InventoryStockEntity before = inventoryStockMapper.selectByStoreIdAndPartId(STORE_ID, part.getId());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> workOrderService.settle(buildSettleCommand(woId)));

        assertEquals(ErrorCode.INVENTORY_RESERVED_NOT_ENOUGH, ex.getErrorCode());
        assertStockUnchanged(part.getId(), before);
        assertUnchangedDraftLikeSettleFailure(woId, WorkOrderStatus.PENDING_ACCEPT.getCode());
    }

    @Test
    void settleRollbackWhenOneOfMultiplePartsReservedQtyNotEnough() {
        PartEntity enoughPart = createPart("足够件", "SET-006", new BigDecimal("20.00"));
        PartEntity shortPart = createPart("不足件", "SET-007", new BigDecimal("20.00"));
        inbound(enoughPart.getId(), 5);
        inbound(shortPart.getId(), 5);
        Long woId = workOrderService.createDraft(
                buildCreateCommand("结算回滚", "13900060007", "小牛N1"));
        workOrderService.addChargeItem(woId, buildPartItem(enoughPart.getId(), "足够件", 2, new BigDecimal("30.00")));
        workOrderService.addChargeItem(woId, buildPartItem(shortPart.getId(), "不足件", 3, new BigDecimal("30.00")));
        workOrderService.submit(buildSubmitCommand(woId));
        recordPayment(woId, new BigDecimal("150.00"));
        InventoryStockEntity shortStock = inventoryStockMapper.selectByStoreIdAndPartId(STORE_ID, shortPart.getId());
        shortStock.setReservedQty(2);
        inventoryStockMapper.updateById(shortStock);
        InventoryStockEntity enoughBefore = inventoryStockMapper.selectByStoreIdAndPartId(STORE_ID, enoughPart.getId());
        InventoryStockEntity shortBefore = inventoryStockMapper.selectByStoreIdAndPartId(STORE_ID, shortPart.getId());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> workOrderService.settle(buildSettleCommand(woId)));

        assertEquals(ErrorCode.INVENTORY_RESERVED_NOT_ENOUGH, ex.getErrorCode());
        assertStockUnchanged(enoughPart.getId(), enoughBefore);
        assertStockUnchanged(shortPart.getId(), shortBefore);
        assertEquals(0, countConsumeFlows());
        assertEquals(WorkOrderStatus.PENDING_ACCEPT.getCode(), workOrderMapper.selectById(woId).getStatus());
    }

    @Test
    void settleWithNullStoreIdFails() {
        Long woId = createSubmittedLaborWorkOrder(new BigDecimal("100.00"));
        recordPayment(woId, new BigDecimal("100.00"));
        int statusLogCountBefore = statusLogMapper.selectByWorkOrderId(woId).size();
        SettleWorkOrderCommand command = buildSettleCommand(woId);
        command.setStoreId(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> workOrderService.settle(command));

        assertEquals(ErrorCode.COMMON_BAD_REQUEST, ex.getErrorCode());
        assertUnchangedDraftLikeSettleFailure(woId, WorkOrderStatus.PENDING_ACCEPT.getCode());
        assertEquals(statusLogCountBefore, statusLogMapper.selectByWorkOrderId(woId).size());
    }

    @Test
    void settleWithWrongStoreIdFails() {
        Long woId = createSubmittedLaborWorkOrder(new BigDecimal("100.00"));
        recordPayment(woId, new BigDecimal("100.00"));
        int statusLogCountBefore = statusLogMapper.selectByWorkOrderId(woId).size();
        SettleWorkOrderCommand command = buildSettleCommand(woId);
        command.setStoreId(OTHER_STORE_ID);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> workOrderService.settle(command));

        assertEquals(ErrorCode.WORK_ORDER_NOT_FOUND, ex.getErrorCode());
        assertUnchangedDraftLikeSettleFailure(woId, WorkOrderStatus.PENDING_ACCEPT.getCode());
        assertEquals(statusLogCountBefore, statusLogMapper.selectByWorkOrderId(woId).size());
    }

    @Test
    void settleWithNullOperatorIdFails() {
        Long woId = createSubmittedLaborWorkOrder(new BigDecimal("100.00"));
        recordPayment(woId, new BigDecimal("100.00"));
        int statusLogCountBefore = statusLogMapper.selectByWorkOrderId(woId).size();
        SettleWorkOrderCommand command = buildSettleCommand(woId);
        command.setOperatorId(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> workOrderService.settle(command));

        assertEquals(ErrorCode.OPERATOR_REQUIRED, ex.getErrorCode());
        assertUnchangedDraftLikeSettleFailure(woId, WorkOrderStatus.PENDING_ACCEPT.getCode());
        assertEquals(statusLogCountBefore, statusLogMapper.selectByWorkOrderId(woId).size());
    }

    @Test
    void settleRecalculatesReceivableAmount() {
        Long woId = createSubmittedLaborWorkOrder(new BigDecimal("100.00"));
        recordPayment(woId, new BigDecimal("100.00"));
        WorkOrderEntity workOrder = workOrderMapper.selectById(woId);
        workOrder.setReceivableAmount(new BigDecimal("999.00"));
        workOrderMapper.updateById(workOrder);

        workOrderService.settle(buildSettleCommand(woId));

        assertEquals(0, new BigDecimal("100.00")
                .compareTo(workOrderMapper.selectById(woId).getReceivableAmount()));
    }

    @Test
    void settleRecalculatesReceivedAmount() {
        Long woId = createSubmittedLaborWorkOrder(new BigDecimal("100.00"));
        recordPayment(woId, new BigDecimal("120.00"));
        recordRefund(woId, new BigDecimal("20.00"));
        WorkOrderEntity workOrder = workOrderMapper.selectById(woId);
        workOrder.setReceivedAmount(new BigDecimal("999.00"));
        workOrderMapper.updateById(workOrder);

        workOrderService.settle(buildSettleCommand(woId));

        assertEquals(0, new BigDecimal("100.00")
                .compareTo(workOrderMapper.selectById(woId).getReceivedAmount()));
    }

    @Test
    void paymentSummaryCannotSettleAfterWorkOrderSettled() {
        Long woId = createSubmittedLaborWorkOrder(new BigDecimal("100.00"));
        recordPayment(woId, new BigDecimal("100.00"));
        workOrderService.settle(buildSettleCommand(woId));

        PaymentSummaryResponse summary = paymentService.getPaymentSummary(STORE_ID, woId);

        assertFalse(summary.getCanSettle());
        assertEquals(WorkOrderStatus.SETTLED.getCode(), workOrderMapper.selectById(woId).getStatus());
    }

    // --- cancel ---

    @Test
    void cancelDraftWorkOrderSuccessfully() {
        Long woId = workOrderService.createDraft(
                buildCreateCommand("取消草稿", "13900070001", "小牛N1"));
        int statusLogCountBefore = statusLogMapper.selectByWorkOrderId(woId).size();

        workOrderService.cancel(buildCancelCommand(woId));

        WorkOrderEntity workOrder = workOrderMapper.selectById(woId);
        assertEquals(WorkOrderStatus.CANCELLED.getCode(), workOrder.getStatus());
        assertEquals(OPERATOR_ID, workOrder.getCancelledBy());
        assertNotNull(workOrder.getCancelledAt());
        assertEquals("测试取消", workOrder.getCancelReason());
        assertEquals(0, inventoryFlowMapper.selectCount(null));
        List<WorkOrderStatusLogEntity> logs = statusLogMapper.selectByWorkOrderId(woId);
        assertEquals(statusLogCountBefore + 1, logs.size());
        WorkOrderStatusLogEntity cancelLog = logs.get(logs.size() - 1);
        assertEquals(WorkOrderStatus.DRAFT.getCode(), cancelLog.getFromStatus());
        assertEquals(WorkOrderStatus.CANCELLED.getCode(), cancelLog.getToStatus());
        assertEquals("CANCEL", cancelLog.getActionType());
    }

    @Test
    void cancelSubmittedWorkOrderSuccessfully() {
        Long woId = createSubmittedLaborWorkOrder(new BigDecimal("100.00"));
        int statusLogCountBefore = statusLogMapper.selectByWorkOrderId(woId).size();

        workOrderService.cancel(buildCancelCommand(woId));

        assertEquals(WorkOrderStatus.CANCELLED.getCode(), workOrderMapper.selectById(woId).getStatus());
        List<WorkOrderStatusLogEntity> logs = statusLogMapper.selectByWorkOrderId(woId);
        assertEquals(statusLogCountBefore + 1, logs.size());
        assertEquals("CANCEL", logs.get(logs.size() - 1).getActionType());
    }

    @Test
    void cancelSubmittedPartWorkOrderReleasesReservedInventory() {
        PartEntity part = createPart("取消释放件", "CAN-001", new BigDecimal("20.00"));
        inbound(part.getId(), 10);
        Long woId = createSubmittedPartWorkOrder(part.getId(), 3, new BigDecimal("50.00"));

        workOrderService.cancel(buildCancelCommand(woId));

        InventoryStockEntity stock = inventoryStockMapper.selectByStoreIdAndPartId(STORE_ID, part.getId());
        assertEquals(10, stock.getActualQty());
        assertEquals(10, stock.getAvailableQty());
        assertEquals(0, stock.getReservedQty());
        InventoryFlowEntity releaseFlow = inventoryFlowMapper.selectList(null).stream()
                .filter(flow -> InventoryFlowType.RELEASE.getCode().equals(flow.getFlowType()))
                .findFirst()
                .orElseThrow();
        assertEquals(3, releaseFlow.getQuantityDelta());
        assertEquals(10, releaseFlow.getActualBefore());
        assertEquals(10, releaseFlow.getActualAfter());
        assertEquals(7, releaseFlow.getAvailableBefore());
        assertEquals(10, releaseFlow.getAvailableAfter());
        assertEquals(3, releaseFlow.getReservedBefore());
        assertEquals(0, releaseFlow.getReservedAfter());
        assertEquals("WORK_ORDER", releaseFlow.getBusinessType());
        assertEquals(woId, releaseFlow.getWorkOrderId());
    }

    @Test
    void cancelSubmittedLaborOnlyWorkOrderDoesNotWriteInventoryFlow() {
        Long woId = createSubmittedLaborWorkOrder(new BigDecimal("100.00"));

        workOrderService.cancel(buildCancelCommand(woId));

        assertEquals(WorkOrderStatus.CANCELLED.getCode(), workOrderMapper.selectById(woId).getStatus());
        assertEquals(0, inventoryFlowMapper.selectCount(null));
    }

    @Test
    void settledWorkOrderCannotCancel() {
        PartEntity part = createPart("已结算取消件", "CAN-002", new BigDecimal("20.00"));
        inbound(part.getId(), 5);
        Long woId = createSubmittedPartWorkOrder(part.getId(), 2, new BigDecimal("50.00"));
        recordPayment(woId, new BigDecimal("100.00"));
        workOrderService.settle(buildSettleCommand(woId));
        InventoryStockEntity before = inventoryStockMapper.selectByStoreIdAndPartId(STORE_ID, part.getId());
        int statusLogCountBefore = statusLogMapper.selectByWorkOrderId(woId).size();

        BusinessException ex = assertThrows(BusinessException.class,
                () -> workOrderService.cancel(buildCancelCommand(woId)));

        assertEquals(ErrorCode.WORK_ORDER_CANCEL_NOT_ALLOWED, ex.getErrorCode());
        assertEquals(WorkOrderStatus.SETTLED.getCode(), workOrderMapper.selectById(woId).getStatus());
        assertStockUnchanged(part.getId(), before);
        assertEquals(0, countReleaseFlows());
        assertEquals(statusLogCountBefore, statusLogMapper.selectByWorkOrderId(woId).size());
    }

    @Test
    void repeatedCancelFailsWithoutDuplicateReleaseOrLog() {
        PartEntity part = createPart("重复取消件", "CAN-003", new BigDecimal("20.00"));
        inbound(part.getId(), 5);
        Long woId = createSubmittedPartWorkOrder(part.getId(), 2, new BigDecimal("50.00"));
        workOrderService.cancel(buildCancelCommand(woId));
        int statusLogCountBefore = statusLogMapper.selectByWorkOrderId(woId).size();
        InventoryStockEntity before = inventoryStockMapper.selectByStoreIdAndPartId(STORE_ID, part.getId());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> workOrderService.cancel(buildCancelCommand(woId)));

        assertEquals(ErrorCode.WORK_ORDER_CANCEL_NOT_ALLOWED, ex.getErrorCode());
        assertStockUnchanged(part.getId(), before);
        assertEquals(1, countReleaseFlows());
        assertEquals(statusLogCountBefore, statusLogMapper.selectByWorkOrderId(woId).size());
    }

    @Test
    void cancelAggregatesSamePartBeforeRelease() {
        PartEntity part = createPart("汇总取消件", "CAN-004", new BigDecimal("20.00"));
        inbound(part.getId(), 5);
        Long woId = workOrderService.createDraft(
                buildCreateCommand("取消汇总", "13900070004", "小牛N1"));
        workOrderService.addChargeItem(woId, buildPartItem(part.getId(), "件1", 2, new BigDecimal("30.00")));
        workOrderService.addChargeItem(woId, buildPartItem(part.getId(), "件2", 3, new BigDecimal("30.00")));
        workOrderService.submit(buildSubmitCommand(woId));

        workOrderService.cancel(buildCancelCommand(woId));

        InventoryStockEntity stock = inventoryStockMapper.selectByStoreIdAndPartId(STORE_ID, part.getId());
        assertEquals(5, stock.getActualQty());
        assertEquals(5, stock.getAvailableQty());
        assertEquals(0, stock.getReservedQty());
        assertEquals(1, countReleaseFlows());
        InventoryFlowEntity flow = inventoryFlowMapper.selectList(null).stream()
                .filter(f -> InventoryFlowType.RELEASE.getCode().equals(f.getFlowType()))
                .findFirst()
                .orElseThrow();
        assertEquals(5, flow.getQuantityDelta());
    }

    @Test
    void cancelFailsWhenAggregatedReservedQtyNotEnough() {
        PartEntity part = createPart("取消预占不足件", "CAN-005", new BigDecimal("20.00"));
        inbound(part.getId(), 5);
        Long woId = createSubmittedPartWorkOrder(part.getId(), 5, new BigDecimal("30.00"));
        InventoryStockEntity stock = inventoryStockMapper.selectByStoreIdAndPartId(STORE_ID, part.getId());
        stock.setReservedQty(4);
        inventoryStockMapper.updateById(stock);
        InventoryStockEntity before = inventoryStockMapper.selectByStoreIdAndPartId(STORE_ID, part.getId());
        int statusLogCountBefore = statusLogMapper.selectByWorkOrderId(woId).size();

        BusinessException ex = assertThrows(BusinessException.class,
                () -> workOrderService.cancel(buildCancelCommand(woId)));

        assertEquals(ErrorCode.INVENTORY_RESERVED_NOT_ENOUGH, ex.getErrorCode());
        assertStockUnchanged(part.getId(), before);
        assertCancelFailureNoChange(woId, WorkOrderStatus.PENDING_ACCEPT.getCode(), statusLogCountBefore);
    }

    @Test
    void cancelRollbackWhenOneOfMultiplePartsReservedQtyNotEnough() {
        PartEntity enoughPart = createPart("取消足够件", "CAN-006", new BigDecimal("20.00"));
        PartEntity shortPart = createPart("取消不足件", "CAN-007", new BigDecimal("20.00"));
        inbound(enoughPart.getId(), 5);
        inbound(shortPart.getId(), 5);
        Long woId = workOrderService.createDraft(
                buildCreateCommand("取消回滚", "13900070007", "小牛N1"));
        workOrderService.addChargeItem(woId, buildPartItem(enoughPart.getId(), "足够件", 2, new BigDecimal("30.00")));
        workOrderService.addChargeItem(woId, buildPartItem(shortPart.getId(), "不足件", 3, new BigDecimal("30.00")));
        workOrderService.submit(buildSubmitCommand(woId));
        InventoryStockEntity shortStock = inventoryStockMapper.selectByStoreIdAndPartId(STORE_ID, shortPart.getId());
        shortStock.setReservedQty(2);
        inventoryStockMapper.updateById(shortStock);
        InventoryStockEntity enoughBefore = inventoryStockMapper.selectByStoreIdAndPartId(STORE_ID, enoughPart.getId());
        InventoryStockEntity shortBefore = inventoryStockMapper.selectByStoreIdAndPartId(STORE_ID, shortPart.getId());
        int statusLogCountBefore = statusLogMapper.selectByWorkOrderId(woId).size();

        BusinessException ex = assertThrows(BusinessException.class,
                () -> workOrderService.cancel(buildCancelCommand(woId)));

        assertEquals(ErrorCode.INVENTORY_RESERVED_NOT_ENOUGH, ex.getErrorCode());
        assertStockUnchanged(enoughPart.getId(), enoughBefore);
        assertStockUnchanged(shortPart.getId(), shortBefore);
        assertCancelFailureNoChange(woId, WorkOrderStatus.PENDING_ACCEPT.getCode(), statusLogCountBefore);
    }

    @Test
    void cancelWithNullStoreIdFails() {
        Long woId = createSubmittedLaborWorkOrder(new BigDecimal("100.00"));
        int statusLogCountBefore = statusLogMapper.selectByWorkOrderId(woId).size();
        CancelWorkOrderCommand command = buildCancelCommand(woId);
        command.setStoreId(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> workOrderService.cancel(command));

        assertEquals(ErrorCode.COMMON_BAD_REQUEST, ex.getErrorCode());
        assertCancelFailureNoChange(woId, WorkOrderStatus.PENDING_ACCEPT.getCode(), statusLogCountBefore);
    }

    @Test
    void cancelWithWrongStoreIdFails() {
        Long woId = createSubmittedLaborWorkOrder(new BigDecimal("100.00"));
        int statusLogCountBefore = statusLogMapper.selectByWorkOrderId(woId).size();
        CancelWorkOrderCommand command = buildCancelCommand(woId);
        command.setStoreId(OTHER_STORE_ID);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> workOrderService.cancel(command));

        assertEquals(ErrorCode.WORK_ORDER_NOT_FOUND, ex.getErrorCode());
        assertCancelFailureNoChange(woId, WorkOrderStatus.PENDING_ACCEPT.getCode(), statusLogCountBefore);
    }

    @Test
    void cancelWithNullOperatorIdFails() {
        Long woId = createSubmittedLaborWorkOrder(new BigDecimal("100.00"));
        int statusLogCountBefore = statusLogMapper.selectByWorkOrderId(woId).size();
        CancelWorkOrderCommand command = buildCancelCommand(woId);
        command.setOperatorId(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> workOrderService.cancel(command));

        assertEquals(ErrorCode.OPERATOR_REQUIRED, ex.getErrorCode());
        assertCancelFailureNoChange(woId, WorkOrderStatus.PENDING_ACCEPT.getCode(), statusLogCountBefore);
    }

    @Test
    void cancelWithBlankReasonFails() {
        Long woId = createSubmittedLaborWorkOrder(new BigDecimal("100.00"));
        int statusLogCountBefore = statusLogMapper.selectByWorkOrderId(woId).size();
        CancelWorkOrderCommand command = buildCancelCommand(woId);
        command.setReason(" ");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> workOrderService.cancel(command));

        assertEquals(ErrorCode.WORK_ORDER_CANCEL_REASON_REQUIRED, ex.getErrorCode());
        assertCancelFailureNoChange(woId, WorkOrderStatus.PENDING_ACCEPT.getCode(), statusLogCountBefore);
    }

    @Test
    void cancelPaidUnsettledWorkOrderDoesNotAutoRefund() {
        Long woId = createSubmittedLaborWorkOrder(new BigDecimal("100.00"));
        recordPayment(woId, new BigDecimal("100.00"));
        BigDecimal receivedBefore = workOrderMapper.selectById(woId).getReceivedAmount();
        long paymentCountBefore = paymentRecordMapper.selectCount(null);
        long refundCountBefore = refundRecordMapper.selectCount(null);

        workOrderService.cancel(buildCancelCommand(woId));

        WorkOrderEntity workOrder = workOrderMapper.selectById(woId);
        assertEquals(WorkOrderStatus.CANCELLED.getCode(), workOrder.getStatus());
        assertEquals(0, receivedBefore.compareTo(workOrder.getReceivedAmount()));
        assertEquals(paymentCountBefore, paymentRecordMapper.selectCount(null));
        assertEquals(refundCountBefore, refundRecordMapper.selectCount(null));
    }

    // --- helpers ---

    private PartEntity createPart(String name, String partCode, BigDecimal costPrice) {
        CreatePartCommand cmd = new CreatePartCommand();
        cmd.setStoreId(STORE_ID);
        cmd.setOperatorId(OPERATOR_ID);
        cmd.setPartName(name);
        cmd.setOfficialPartNo(partCode);
        cmd.setReferenceCostPrice(costPrice);
        return partService.createOfficialPart(cmd);
    }

    private void inbound(Long partId, int quantity) {
        InventoryInboundCommand command = new InventoryInboundCommand();
        command.setStoreId(STORE_ID);
        command.setPartId(partId);
        command.setQuantity(quantity);
        command.setUnitCost(new BigDecimal("10.00"));
        command.setOperatorId(OPERATOR_ID);
        command.setReason("测试入库");
        inventoryService.inbound(command);
    }

    private SubmitWorkOrderCommand buildSubmitCommand(Long workOrderId) {
        SubmitWorkOrderCommand command = new SubmitWorkOrderCommand();
        command.setStoreId(STORE_ID);
        command.setWorkOrderId(workOrderId);
        command.setOperatorId(OPERATOR_ID);
        command.setRemark("测试提交");
        return command;
    }

    private SettleWorkOrderCommand buildSettleCommand(Long workOrderId) {
        SettleWorkOrderCommand command = new SettleWorkOrderCommand();
        command.setStoreId(STORE_ID);
        command.setWorkOrderId(workOrderId);
        command.setOperatorId(OPERATOR_ID);
        command.setRemark("测试结算");
        return command;
    }

    private CancelWorkOrderCommand buildCancelCommand(Long workOrderId) {
        CancelWorkOrderCommand command = new CancelWorkOrderCommand();
        command.setStoreId(STORE_ID);
        command.setWorkOrderId(workOrderId);
        command.setOperatorId(OPERATOR_ID);
        command.setReason("测试取消");
        return command;
    }

    private Long createSubmittedLaborWorkOrder(BigDecimal amount) {
        Long workOrderId = workOrderService.createDraft(
                buildCreateCommand("结算客户", "13900060000", "小牛N1"));
        workOrderService.addChargeItem(workOrderId, buildLaborItem("工时费", 1, amount));
        workOrderService.submit(buildSubmitCommand(workOrderId));
        return workOrderId;
    }

    private Long createSubmittedPartWorkOrder(Long partId, int quantity, BigDecimal unitPrice) {
        Long workOrderId = workOrderService.createDraft(
                buildCreateCommand("结算配件客户", "13900060002", "小牛N1"));
        workOrderService.addChargeItem(workOrderId,
                buildPartItem(partId, "结算配件", quantity, unitPrice));
        workOrderService.submit(buildSubmitCommand(workOrderId));
        return workOrderId;
    }

    private void recordPayment(Long workOrderId, BigDecimal amount) {
        RecordPaymentCommand command = new RecordPaymentCommand();
        command.setStoreId(STORE_ID);
        command.setWorkOrderId(workOrderId);
        command.setAmount(amount);
        command.setPaymentMethod("WECHAT");
        command.setReceiverId(OPERATOR_ID);
        command.setOperatorId(OPERATOR_ID);
        paymentService.recordPayment(command);
    }

    private void recordRefund(Long workOrderId, BigDecimal amount) {
        RecordRefundCommand command = new RecordRefundCommand();
        command.setStoreId(STORE_ID);
        command.setWorkOrderId(workOrderId);
        command.setAmount(amount);
        command.setRefundMethod("WECHAT");
        command.setOperatorId(OPERATOR_ID);
        command.setReason("测试退款");
        refundService.recordRefund(command);
    }

    private long countReserveFlows() {
        return inventoryFlowMapper.selectList(null).stream()
                .filter(flow -> InventoryFlowType.RESERVE.getCode().equals(flow.getFlowType()))
                .count();
    }

    private long countConsumeFlows() {
        return inventoryFlowMapper.selectList(null).stream()
                .filter(flow -> InventoryFlowType.CONSUME.getCode().equals(flow.getFlowType()))
                .count();
    }

    private long countReleaseFlows() {
        return inventoryFlowMapper.selectList(null).stream()
                .filter(flow -> InventoryFlowType.RELEASE.getCode().equals(flow.getFlowType()))
                .count();
    }

    private void assertUnchangedDraftLikeSettleFailure(Long workOrderId, String expectedStatus) {
        assertEquals(expectedStatus, workOrderMapper.selectById(workOrderId).getStatus());
        assertEquals(0, countConsumeFlows());
    }

    private void assertCancelFailureNoChange(Long workOrderId, String expectedStatus,
                                             int expectedStatusLogCount) {
        assertEquals(expectedStatus, workOrderMapper.selectById(workOrderId).getStatus());
        assertEquals(0, countReleaseFlows());
        assertEquals(expectedStatusLogCount, statusLogMapper.selectByWorkOrderId(workOrderId).size());
    }

    private void assertStockUnchanged(Long partId, InventoryStockEntity before) {
        InventoryStockEntity after = inventoryStockMapper.selectByStoreIdAndPartId(STORE_ID, partId);
        assertEquals(before.getActualQty(), after.getActualQty());
        assertEquals(before.getAvailableQty(), after.getAvailableQty());
        assertEquals(before.getReservedQty(), after.getReservedQty());
    }

    private void updateFrameNo(Long workOrderId, String frameNo) {
        WorkOrderEntity entity = workOrderMapper.selectById(workOrderId);
        entity.setFrameNoSnapshot(frameNo);
        workOrderMapper.updateById(entity);
    }

    private CreateDraftWorkOrderCommand buildCreateCommand(String name, String phone,
                                                            String vehicleModel) {
        CreateDraftWorkOrderCommand command = new CreateDraftWorkOrderCommand();
        command.setStoreId(STORE_ID);
        command.setCustomerNameSnapshot(name);
        command.setCustomerPhoneSnapshot(phone);
        command.setVehicleModelSnapshot(vehicleModel);
        command.setRepairItem("维修");
        command.setOperatorId(OPERATOR_ID);
        return command;
    }

    private AddWorkOrderChargeItemCommand buildPartItem(Long partId, String itemName,
                                                         int quantity, BigDecimal unitPrice) {
        AddWorkOrderChargeItemCommand cmd = new AddWorkOrderChargeItemCommand();
        cmd.setStoreId(STORE_ID);
        cmd.setChargeType("PART");
        cmd.setItemName(itemName);
        cmd.setPartId(partId);
        cmd.setQuantity(quantity);
        cmd.setUnitPrice(unitPrice);
        return cmd;
    }

    private AddWorkOrderChargeItemCommand buildLaborItem(String itemName, int quantity,
                                                          BigDecimal unitPrice) {
        AddWorkOrderChargeItemCommand cmd = new AddWorkOrderChargeItemCommand();
        cmd.setStoreId(STORE_ID);
        cmd.setChargeType("LABOR");
        cmd.setItemName(itemName);
        cmd.setQuantity(quantity);
        cmd.setUnitPrice(unitPrice);
        return cmd;
    }

    private AddWorkOrderChargeItemCommand buildOtherItem(String itemName, int quantity,
                                                          BigDecimal unitPrice) {
        AddWorkOrderChargeItemCommand cmd = new AddWorkOrderChargeItemCommand();
        cmd.setStoreId(STORE_ID);
        cmd.setChargeType("OTHER");
        cmd.setItemName(itemName);
        cmd.setQuantity(quantity);
        cmd.setUnitPrice(unitPrice);
        return cmd;
    }

    private AddWorkOrderChargeItemCommand buildPartItemForStore(Long storeId, Long partId,
                                                                String itemName, int quantity,
                                                                BigDecimal unitPrice) {
        AddWorkOrderChargeItemCommand cmd = buildPartItem(partId, itemName, quantity, unitPrice);
        cmd.setStoreId(storeId);
        return cmd;
    }

    private WorkOrderChargeItemInput buildPartInput(Long partId, String itemName,
                                                     int quantity, BigDecimal unitPrice) {
        WorkOrderChargeItemInput input = new WorkOrderChargeItemInput();
        input.setChargeType("PART");
        input.setItemName(itemName);
        input.setPartId(partId);
        input.setQuantity(quantity);
        input.setUnitPrice(unitPrice);
        return input;
    }

    private WorkOrderChargeItemInput buildLaborInput(String itemName, int quantity,
                                                      BigDecimal unitPrice) {
        WorkOrderChargeItemInput input = new WorkOrderChargeItemInput();
        input.setChargeType("LABOR");
        input.setItemName(itemName);
        input.setQuantity(quantity);
        input.setUnitPrice(unitPrice);
        return input;
    }

    private WorkOrderChargeItemInput buildOtherInput(String itemName, int quantity,
                                                      BigDecimal unitPrice) {
        WorkOrderChargeItemInput input = new WorkOrderChargeItemInput();
        input.setChargeType("OTHER");
        input.setItemName(itemName);
        input.setQuantity(quantity);
        input.setUnitPrice(unitPrice);
        return input;
    }
}
