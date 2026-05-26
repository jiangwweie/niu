package com.xiaoniu.aftermarket.workorder.application;

import com.xiaoniu.aftermarket.common.api.ErrorCode;
import com.xiaoniu.aftermarket.common.enums.ChargeType;
import com.xiaoniu.aftermarket.common.enums.PartSource;
import com.xiaoniu.aftermarket.common.exception.BusinessException;
import com.xiaoniu.aftermarket.inventory.dto.InventoryInboundCommand;
import com.xiaoniu.aftermarket.inventory.service.InventoryService;
import com.xiaoniu.aftermarket.part.dto.CreatePartCommand;
import com.xiaoniu.aftermarket.part.entity.PartEntity;
import com.xiaoniu.aftermarket.part.service.PartService;
import com.xiaoniu.aftermarket.workorder.dto.AddWorkOrderChargeItemCommand;
import com.xiaoniu.aftermarket.workorder.service.WorkOrderService;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AddTempPartChargeApplicationService {

    private static final String CREATE_SOURCE_WORK_ORDER_TEMP = "WORK_ORDER_TEMP";

    private final PartService partService;
    private final InventoryService inventoryService;
    private final WorkOrderService workOrderService;

    public AddTempPartChargeApplicationService(PartService partService,
                                               InventoryService inventoryService,
                                               WorkOrderService workOrderService) {
        this.partService = partService;
        this.inventoryService = inventoryService;
        this.workOrderService = workOrderService;
    }

    @Transactional
    public Result execute(Command command) {
        CreatePartCommand createPart = new CreatePartCommand();
        createPart.setStoreId(command.storeId());
        createPart.setOperatorId(command.operatorId());
        createPart.setPartName(command.partName());
        createPart.setOfficialPartNo(command.officialPartNo());
        createPart.setModel(command.model());
        createPart.setCategoryCode(command.categoryCode());
        createPart.setReferenceCostPrice(command.unitCost() != null ? command.unitCost() : BigDecimal.ZERO);
        createPart.setDefaultBarcode(command.barcode());
        createPart.setLocationRemark(command.locationRemark());
        createPart.setCreateSource(CREATE_SOURCE_WORK_ORDER_TEMP);
        createPart.setRemark(command.remark());

        PartEntity part;
        String source = StringUtils.hasText(command.source()) ? command.source().trim().toUpperCase() : PartSource.THIRD_PARTY.getCode();
        if (PartSource.OFFICIAL.getCode().equals(source)) {
            if (!StringUtils.hasText(command.officialPartNo())) {
                throw new BusinessException(ErrorCode.PART_OFFICIAL_CODE_REQUIRED);
            }
            part = partService.createOfficialPart(createPart);
        } else {
            part = partService.createThirdPartyPart(createPart);
        }

        InventoryInboundCommand inbound = new InventoryInboundCommand();
        inbound.setStoreId(command.storeId());
        inbound.setOperatorId(command.operatorId());
        inbound.setPartId(part.getId());
        inbound.setQuantity(command.quantity());
        inbound.setUnitCost(command.unitCost());
        inbound.setBarcode(command.barcode());
        inbound.setLocationRemark(command.locationRemark());
        inbound.setReason("工单临时配件入库");
        inbound.setRemark(command.remark());
        inventoryService.inbound(inbound);

        AddWorkOrderChargeItemCommand addCharge = new AddWorkOrderChargeItemCommand();
        addCharge.setStoreId(command.storeId());
        addCharge.setChargeType(ChargeType.PART.getCode());
        addCharge.setItemName(command.partName());
        addCharge.setPartId(part.getId());
        addCharge.setQuantity(command.quantity());
        addCharge.setUnit(StringUtils.hasText(command.unit()) ? command.unit() : "件");
        addCharge.setUnitPrice(command.unitPrice());
        addCharge.setRemark(command.remark());
        addCharge.setTempPart(true);
        Long chargeItemId = workOrderService.addChargeItem(command.workOrderId(), addCharge);

        return new Result(part.getId(), chargeItemId);
    }

    public record Command(
            Long storeId,
            Long operatorId,
            Long workOrderId,
            String source,
            String partName,
            String officialPartNo,
            String barcode,
            String model,
            String categoryCode,
            Integer quantity,
            String unit,
            BigDecimal unitPrice,
            BigDecimal unitCost,
            String locationRemark,
            String remark
    ) {
    }

    public record Result(Long partId, Long chargeItemId) {
    }
}
