package com.xiaoniu.aftermarket.inventory.application;

import com.xiaoniu.aftermarket.common.enums.CommonStatus;
import com.xiaoniu.aftermarket.common.enums.PartSource;
import com.xiaoniu.aftermarket.common.exception.BusinessException;
import com.xiaoniu.aftermarket.common.api.ErrorCode;
import com.xiaoniu.aftermarket.inventory.dto.InventoryInboundCommand;
import com.xiaoniu.aftermarket.inventory.entity.InventoryFlowEntity;
import com.xiaoniu.aftermarket.inventory.entity.InventoryStockEntity;
import com.xiaoniu.aftermarket.inventory.mapper.InventoryFlowMapper;
import com.xiaoniu.aftermarket.inventory.service.InventoryService;
import com.xiaoniu.aftermarket.part.dto.CreatePartCommand;
import com.xiaoniu.aftermarket.part.entity.PartEntity;
import com.xiaoniu.aftermarket.part.service.PartService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class CreatePartAndInboundApplicationService {

    private final PartService partService;
    private final InventoryService inventoryService;
    private final InventoryFlowMapper inventoryFlowMapper;

    public CreatePartAndInboundApplicationService(PartService partService,
                                                  InventoryService inventoryService,
                                                  InventoryFlowMapper inventoryFlowMapper) {
        this.partService = partService;
        this.inventoryService = inventoryService;
        this.inventoryFlowMapper = inventoryFlowMapper;
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
        createPart.setReferenceCostPrice(command.costPrice());
        createPart.setDefaultSalePrice(command.salePrice());
        createPart.setExternalBarcode(command.externalBarcode());
        createPart.setLocationRemark(command.locationRemark());
        createPart.setCreateSource("NORMAL");
        createPart.setRemark(command.remark());

        PartEntity part;
        String source = StringUtils.hasText(command.source())
                ? command.source().trim().toUpperCase()
                : PartSource.THIRD_PARTY.getCode();
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
        inbound.setQuantity(command.inboundQuantity());
        inbound.setUnitCost(command.unitCost());
        inbound.setBarcode(command.externalBarcode());
        inbound.setLocationRemark(command.locationRemark());
        inbound.setReason(command.reason());
        inbound.setRemark(command.remark());
        inventoryService.inbound(inbound);

        InventoryStockEntity stock = inventoryService.getByPartId(command.storeId(), part.getId());
        InventoryFlowEntity flow = stock != null && stock.getLastFlowId() != null
                ? inventoryFlowMapper.selectById(stock.getLastFlowId())
                : null;

        return new Result(
                part.getId(),
                part.getPartCode(),
                part.getPartName(),
                part.getDefaultBarcode(),
                command.externalBarcode(),
                stock != null ? stock.getActualQty() : 0,
                stock != null ? stock.getAvailableQty() : 0,
                stock != null ? stock.getReservedQty() : 0,
                flow != null ? flow.getId() : null,
                flow != null ? flow.getOperatedAt() : null
        );
    }

    public record Command(
            Long storeId,
            Long operatorId,
            String source,
            String partName,
            String officialPartNo,
            String externalBarcode,
            String model,
            String categoryCode,
            java.math.BigDecimal costPrice,
            java.math.BigDecimal salePrice,
            Integer inboundQuantity,
            java.math.BigDecimal unitCost,
            String locationRemark,
            String reason,
            String remark
    ) {
    }

    public record Result(
            Long partId,
            String partCode,
            String partName,
            String defaultBarcode,
            String externalBarcode,
            Integer actualQty,
            Integer availableQty,
            Integer reservedQty,
            Long flowId,
            java.time.LocalDateTime operatedAt
    ) {
    }
}

