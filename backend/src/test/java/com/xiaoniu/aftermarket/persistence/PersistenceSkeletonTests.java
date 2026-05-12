package com.xiaoniu.aftermarket.persistence;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.xiaoniu.aftermarket.common.mapper.SequenceDailyMapper;
import com.xiaoniu.aftermarket.common.mapper.StoreMapper;
import com.xiaoniu.aftermarket.customer.mapper.CustomerMapper;
import com.xiaoniu.aftermarket.customer.mapper.VehicleMapper;
import com.xiaoniu.aftermarket.dict.mapper.SysDictItemMapper;
import com.xiaoniu.aftermarket.dict.mapper.SysDictTypeMapper;
import com.xiaoniu.aftermarket.inventory.mapper.InventoryFlowMapper;
import com.xiaoniu.aftermarket.inventory.mapper.InventoryStockMapper;
import com.xiaoniu.aftermarket.official.mapper.OfficialAfterSalesMapper;
import com.xiaoniu.aftermarket.part.mapper.PartBarcodeMapper;
import com.xiaoniu.aftermarket.part.mapper.PartMapper;
import com.xiaoniu.aftermarket.payment.mapper.PaymentRecordMapper;
import com.xiaoniu.aftermarket.payment.mapper.RefundRecordMapper;
import com.xiaoniu.aftermarket.reimbursement.mapper.ReimbursementMapper;
import com.xiaoniu.aftermarket.user.mapper.SysPermissionMapper;
import com.xiaoniu.aftermarket.user.mapper.SysRoleMapper;
import com.xiaoniu.aftermarket.user.mapper.SysRolePermissionMapper;
import com.xiaoniu.aftermarket.user.mapper.SysUserMapper;
import com.xiaoniu.aftermarket.user.mapper.SysUserRoleMapper;
import com.xiaoniu.aftermarket.workorder.entity.WorkOrderChargeItemEntity;
import com.xiaoniu.aftermarket.workorder.entity.WorkOrderEntity;
import com.xiaoniu.aftermarket.workorder.mapper.WorkOrderChargeItemMapper;
import com.xiaoniu.aftermarket.workorder.mapper.WorkOrderMapper;
import com.xiaoniu.aftermarket.workorder.mapper.WorkOrderStatusLogMapper;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.context.SpringBootTest;

@ActiveProfiles("test")
@SpringBootTest
class PersistenceSkeletonTests {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void mapperBeansAreScanned() {
        List<Class<?>> mapperTypes = List.of(
                StoreMapper.class,
                SequenceDailyMapper.class,
                SysUserMapper.class,
                SysRoleMapper.class,
                SysPermissionMapper.class,
                SysUserRoleMapper.class,
                SysRolePermissionMapper.class,
                SysDictTypeMapper.class,
                SysDictItemMapper.class,
                CustomerMapper.class,
                VehicleMapper.class,
                PartMapper.class,
                PartBarcodeMapper.class,
                InventoryStockMapper.class,
                InventoryFlowMapper.class,
                WorkOrderMapper.class,
                WorkOrderChargeItemMapper.class,
                WorkOrderStatusLogMapper.class,
                PaymentRecordMapper.class,
                RefundRecordMapper.class,
                OfficialAfterSalesMapper.class,
                ReimbursementMapper.class
        );

        mapperTypes.forEach(type -> assertNotNull(applicationContext.getBean(type)));
    }

    @Test
    void workOrderEntityDoesNotContainDeprecatedFeeFields() {
        List<String> fieldNames = Arrays.stream(WorkOrderEntity.class.getDeclaredFields())
                .map(Field::getName)
                .collect(Collectors.toList());

        assertFalse(fieldNames.contains("laborFee"));
        assertFalse(fieldNames.contains("otherFee"));
        assertTrue(fieldNames.contains("receivableAmount"));
    }

    @Test
    void workOrderChargeItemEntityUsesNewChargeItemModel() {
        List<String> fieldNames = Arrays.stream(WorkOrderChargeItemEntity.class.getDeclaredFields())
                .map(Field::getName)
                .collect(Collectors.toList());

        assertTrue(fieldNames.contains("chargeType"));
        assertTrue(fieldNames.contains("itemName"));
        assertTrue(fieldNames.contains("partId"));
        assertTrue(fieldNames.contains("quantity"));
        assertTrue(fieldNames.contains("unit"));
        assertTrue(fieldNames.contains("unitPrice"));
        assertTrue(fieldNames.contains("lineAmount"));
        assertTrue(fieldNames.contains("costPriceSnapshot"));
        assertTrue(fieldNames.contains("lineCostAmount"));
        assertTrue(fieldNames.contains("inventoryAffecting"));
        assertTrue(fieldNames.contains("tempPart"));
        assertFalse(fieldNames.contains("workOrderItemId"));
    }
}
