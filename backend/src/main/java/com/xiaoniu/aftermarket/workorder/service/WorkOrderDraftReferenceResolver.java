package com.xiaoniu.aftermarket.workorder.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xiaoniu.aftermarket.common.api.ErrorCode;
import com.xiaoniu.aftermarket.common.exception.BusinessException;
import com.xiaoniu.aftermarket.customer.entity.CustomerEntity;
import com.xiaoniu.aftermarket.customer.entity.VehicleEntity;
import com.xiaoniu.aftermarket.customer.mapper.CustomerMapper;
import com.xiaoniu.aftermarket.customer.mapper.VehicleMapper;
import com.xiaoniu.aftermarket.workorder.dto.CreateDraftWorkOrderCommand;
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
        VehicleEntity vehicle = null;
        if (command.getVehicleId() != null) {
            vehicle = vehicleMapper.selectOne(
                    new LambdaQueryWrapper<VehicleEntity>()
                            .eq(VehicleEntity::getId, command.getVehicleId())
                            .eq(VehicleEntity::getStoreId, command.getStoreId())
                            .eq(VehicleEntity::getDeleted, 0));
            if (vehicle == null) {
                throw new BusinessException(ErrorCode.VEHICLE_NOT_FOUND, "车辆不存在");
            }
            if (command.getCustomerId() != null && !command.getCustomerId().equals(vehicle.getCustomerId())) {
                throw new BusinessException(ErrorCode.VEHICLE_NOT_IN_CUSTOMER);
            }
            command.setCustomerId(vehicle.getCustomerId());
            command.setVehicleModelSnapshot(vehicle.getModel());
            command.setFrameNoSnapshot(vehicle.getFrameNo());
            command.setBatteryNoSnapshot(vehicle.getBatteryNo());
        }

        if (command.getCustomerId() != null) {
            CustomerEntity customer = customerMapper.selectOne(
                    new LambdaQueryWrapper<CustomerEntity>()
                            .eq(CustomerEntity::getId, command.getCustomerId())
                            .eq(CustomerEntity::getStoreId, command.getStoreId())
                            .eq(CustomerEntity::getDeleted, 0));
            if (customer == null) {
                throw new BusinessException(ErrorCode.CUSTOMER_NOT_FOUND, "客户不存在");
            }
            if (vehicle != null && !customer.getId().equals(vehicle.getCustomerId())) {
                throw new BusinessException(ErrorCode.VEHICLE_NOT_IN_CUSTOMER);
            }
            command.setCustomerNameSnapshot(customer.getCustomerName());
            command.setCustomerPhoneSnapshot(customer.getPhone());
        }
    }
}
