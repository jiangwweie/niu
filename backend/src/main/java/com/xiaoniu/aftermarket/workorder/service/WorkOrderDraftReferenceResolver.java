package com.xiaoniu.aftermarket.workorder.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xiaoniu.aftermarket.common.api.ErrorCode;
import com.xiaoniu.aftermarket.common.exception.BusinessException;
import com.xiaoniu.aftermarket.customer.entity.CustomerEntity;
import com.xiaoniu.aftermarket.customer.entity.VehicleEntity;
import com.xiaoniu.aftermarket.customer.mapper.CustomerMapper;
import com.xiaoniu.aftermarket.customer.mapper.VehicleMapper;
import com.xiaoniu.aftermarket.workorder.dto.CreateDraftWorkOrderCommand;
import com.xiaoniu.aftermarket.workorder.dto.DraftSnapshotWritable;
import com.xiaoniu.aftermarket.workorder.dto.UpdateWorkOrderDraftCommand;
import org.springframework.stereotype.Service;

@Service
public class WorkOrderDraftReferenceResolver {

    private final CustomerMapper customerMapper;
    private final VehicleMapper vehicleMapper;

    public WorkOrderDraftReferenceResolver(CustomerMapper customerMapper, VehicleMapper vehicleMapper) {
        this.customerMapper = customerMapper;
        this.vehicleMapper = vehicleMapper;
    }

    public void resolve(CreateDraftWorkOrderCommand command) {
        ResolvedDraftReference resolved = resolve(command.getStoreId(), command.getCustomerId(), command.getVehicleId());
        apply(command, resolved);
    }

    public void resolve(UpdateWorkOrderDraftCommand command) {
        if (command.getCustomerId() == null && command.getVehicleId() == null) {
            return;
        }
        ResolvedDraftReference resolved = resolve(command.getStoreId(), command.getCustomerId(), command.getVehicleId());
        apply(command, resolved);
    }

    private ResolvedDraftReference resolve(Long storeId, Long customerId, Long vehicleId) {
        VehicleEntity vehicle = null;
        if (vehicleId != null) {
            vehicle = vehicleMapper.selectOne(
                    new LambdaQueryWrapper<VehicleEntity>()
                            .eq(VehicleEntity::getId, vehicleId)
                            .eq(VehicleEntity::getStoreId, storeId)
                            .eq(VehicleEntity::getDeleted, 0));
            if (vehicle == null) {
                throw new BusinessException(ErrorCode.VEHICLE_NOT_FOUND, "车辆不存在");
            }
            if (customerId != null && !customerId.equals(vehicle.getCustomerId())) {
                throw new BusinessException(ErrorCode.VEHICLE_NOT_IN_CUSTOMER);
            }
            customerId = vehicle.getCustomerId();
        }

        CustomerEntity customer = null;
        if (customerId != null) {
            customer = customerMapper.selectOne(
                    new LambdaQueryWrapper<CustomerEntity>()
                            .eq(CustomerEntity::getId, customerId)
                            .eq(CustomerEntity::getStoreId, storeId)
                            .eq(CustomerEntity::getDeleted, 0));
            if (customer == null) {
                throw new BusinessException(ErrorCode.CUSTOMER_NOT_FOUND, "客户不存在");
            }
            if (vehicle != null && !customer.getId().equals(vehicle.getCustomerId())) {
                throw new BusinessException(ErrorCode.VEHICLE_NOT_IN_CUSTOMER);
            }
        }
        return new ResolvedDraftReference(customer, vehicle);
    }

    private void apply(DraftSnapshotWritable command, ResolvedDraftReference resolved) {
        if (resolved.vehicle() != null) {
            command.setCustomerId(resolved.vehicle().getCustomerId());
            command.setVehicleModelSnapshot(resolved.vehicle().getModel());
            command.setFrameNoSnapshot(resolved.vehicle().getFrameNo());
            command.setBatteryNoSnapshot(resolved.vehicle().getBatteryNo());
        }
        if (resolved.customer() != null) {
            command.setCustomerNameSnapshot(resolved.customer().getCustomerName());
            command.setCustomerPhoneSnapshot(resolved.customer().getPhone());
        }
    }

    private record ResolvedDraftReference(CustomerEntity customer, VehicleEntity vehicle) {
    }
}
