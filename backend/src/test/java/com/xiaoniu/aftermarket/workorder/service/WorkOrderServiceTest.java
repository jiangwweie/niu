package com.xiaoniu.aftermarket.workorder.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.xiaoniu.aftermarket.common.api.ErrorCode;
import com.xiaoniu.aftermarket.common.enums.CommonStatus;
import com.xiaoniu.aftermarket.common.enums.WorkOrderStatus;
import com.xiaoniu.aftermarket.common.exception.BusinessException;
import com.xiaoniu.aftermarket.common.pagination.PageResponse;
import com.xiaoniu.aftermarket.inventory.entity.InventoryFlowEntity;
import com.xiaoniu.aftermarket.inventory.mapper.InventoryFlowMapper;
import com.xiaoniu.aftermarket.part.dto.CreatePartCommand;
import com.xiaoniu.aftermarket.part.entity.PartEntity;
import com.xiaoniu.aftermarket.part.service.PartService;
import com.xiaoniu.aftermarket.workorder.dto.AddWorkOrderChargeItemCommand;
import com.xiaoniu.aftermarket.workorder.dto.CreateDraftWorkOrderCommand;
import com.xiaoniu.aftermarket.workorder.dto.UpdateWorkOrderChargeItemCommand;
import com.xiaoniu.aftermarket.workorder.dto.UpdateWorkOrderDraftCommand;
import com.xiaoniu.aftermarket.workorder.dto.WorkOrderChargeItemInput;
import com.xiaoniu.aftermarket.workorder.dto.WorkOrderChargeItemResponse;
import com.xiaoniu.aftermarket.workorder.dto.WorkOrderDetailResponse;
import com.xiaoniu.aftermarket.workorder.dto.WorkOrderQueryRequest;
import com.xiaoniu.aftermarket.workorder.dto.WorkOrderQueryResponse;
import com.xiaoniu.aftermarket.workorder.entity.WorkOrderChargeItemEntity;
import com.xiaoniu.aftermarket.workorder.entity.WorkOrderEntity;
import com.xiaoniu.aftermarket.workorder.mapper.WorkOrderChargeItemMapper;
import com.xiaoniu.aftermarket.workorder.mapper.WorkOrderMapper;
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
    private static final Long OPERATOR_ID = 1L;

    @Autowired
    private WorkOrderService workOrderService;

    @Autowired
    private PartService partService;

    @Autowired
    private WorkOrderMapper workOrderMapper;

    @Autowired
    private WorkOrderChargeItemMapper chargeItemMapper;

    @Autowired
    private InventoryFlowMapper inventoryFlowMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanTables() {
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
        partService.disablePart(part.getId());
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
        assertNull(item.getCostPriceSnapshot());
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

        workOrderService.removeChargeItem(woId, itemId);

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
        cmd.setChargeType("LABOR");
        cmd.setItemName(itemName);
        cmd.setQuantity(quantity);
        cmd.setUnitPrice(unitPrice);
        return cmd;
    }

    private AddWorkOrderChargeItemCommand buildOtherItem(String itemName, int quantity,
                                                          BigDecimal unitPrice) {
        AddWorkOrderChargeItemCommand cmd = new AddWorkOrderChargeItemCommand();
        cmd.setChargeType("OTHER");
        cmd.setItemName(itemName);
        cmd.setQuantity(quantity);
        cmd.setUnitPrice(unitPrice);
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
