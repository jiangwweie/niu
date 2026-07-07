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
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WorkOrderDraftReferenceResolver {

    private static final Logger log = LoggerFactory.getLogger(WorkOrderDraftReferenceResolver.class);

    private final CustomerMapper customerMapper;
    private final VehicleMapper vehicleMapper;

    public WorkOrderDraftReferenceResolver(CustomerMapper customerMapper, VehicleMapper vehicleMapper) {
        this.customerMapper = customerMapper;
        this.vehicleMapper = vehicleMapper;
    }

    public void resolve(CreateDraftWorkOrderCommand command) {
        ResolvedDraftReference resolved = resolve(command.getStoreId(), command.getCustomerId(), command.getVehicleId());
        apply(command, resolved);
        archiveManualSnapshotBestEffort(command);
    }

    public void resolve(UpdateWorkOrderDraftCommand command) {
        if (command.getCustomerId() == null && command.getVehicleId() == null
                && !StringUtils.hasText(command.getCustomerNameSnapshot())) {
            return;
        }
        ResolvedDraftReference resolved = resolve(command.getStoreId(), command.getCustomerId(), command.getVehicleId());
        apply(command, resolved);
        archiveManualSnapshotBestEffort(command);
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
            command.setVehicleId(resolved.vehicle().getId());
            command.setVehicleModelSnapshot(resolved.vehicle().getModel());
            command.setFrameNoSnapshot(resolved.vehicle().getFrameNo());
            command.setBatteryNoSnapshot(resolved.vehicle().getBatteryNo());
        }
        if (resolved.customer() != null) {
            command.setCustomerNameSnapshot(resolved.customer().getCustomerName());
            command.setCustomerPhoneSnapshot(resolved.customer().getPhone());
        }
    }

    private void archiveManualSnapshotBestEffort(DraftSnapshotWritable command) {
        try {
            archiveManualSnapshot(command);
        } catch (RuntimeException ex) {
            log.warn("manual work order customer archive skipped storeId={} operatorId={} customerId={} vehicleId={} error={}",
                    command.getStoreId(), command.getOperatorId(), command.getCustomerId(), command.getVehicleId(),
                    ex.getClass().getSimpleName());
        }
    }

    private void archiveManualSnapshot(DraftSnapshotWritable command) {
        Long storeId = command.getStoreId();
        if (storeId == null || !StringUtils.hasText(command.getCustomerNameSnapshot())) {
            return;
        }

        Long customerId = command.getCustomerId();
        if (customerId == null) {
            CustomerEntity customer = findCustomerBySnapshot(storeId,
                    command.getCustomerPhoneSnapshot(), command.getCustomerNameSnapshot());
            if (customer == null) {
                customer = createCustomerFromSnapshot(command);
            }
            customerId = customer.getId();
            command.setCustomerId(customerId);
        }

        if (command.getVehicleId() == null && StringUtils.hasText(command.getFrameNoSnapshot())) {
            VehicleEntity vehicle = findVehicleByFrameNo(storeId, command.getFrameNoSnapshot());
            if (vehicle == null) {
                vehicle = createVehicleFromSnapshot(command, customerId);
            } else if (vehicle.getCustomerId() != null && !vehicle.getCustomerId().equals(customerId)) {
                throw new BusinessException(ErrorCode.VEHICLE_NOT_IN_CUSTOMER);
            }
            command.setVehicleId(vehicle.getId());
            command.setVehicleModelSnapshot(vehicle.getModel());
            command.setFrameNoSnapshot(vehicle.getFrameNo());
            command.setBatteryNoSnapshot(vehicle.getBatteryNo());
        }
    }

    private CustomerEntity findCustomerBySnapshot(Long storeId, String phone, String customerName) {
        if (StringUtils.hasText(phone)) {
            CustomerEntity byPhone = customerMapper.selectOne(
                    new LambdaQueryWrapper<CustomerEntity>()
                            .eq(CustomerEntity::getStoreId, storeId)
                            .eq(CustomerEntity::getPhone, phone.trim())
                            .eq(CustomerEntity::getDeleted, 0)
                            .last("LIMIT 1"));
            if (byPhone != null) {
                return byPhone;
            }
        }
        if (!StringUtils.hasText(phone) && StringUtils.hasText(customerName)) {
            return customerMapper.selectOne(
                    new LambdaQueryWrapper<CustomerEntity>()
                            .eq(CustomerEntity::getStoreId, storeId)
                            .eq(CustomerEntity::getCustomerName, customerName.trim())
                            .eq(CustomerEntity::getDeleted, 0)
                            .last("LIMIT 1"));
        }
        return null;
    }

    private VehicleEntity findVehicleByFrameNo(Long storeId, String frameNo) {
        return vehicleMapper.selectOne(
                new LambdaQueryWrapper<VehicleEntity>()
                        .eq(VehicleEntity::getStoreId, storeId)
                        .eq(VehicleEntity::getFrameNo, frameNo.trim())
                        .eq(VehicleEntity::getDeleted, 0)
                        .last("LIMIT 1"));
    }

    private CustomerEntity createCustomerFromSnapshot(DraftSnapshotWritable command) {
        LocalDateTime now = LocalDateTime.now();
        CustomerEntity customer = new CustomerEntity();
        customer.setStoreId(command.getStoreId());
        customer.setCustomerName(command.getCustomerNameSnapshot().trim());
        customer.setPhone(trimToNull(command.getCustomerPhoneSnapshot()));
        customer.setCreatedBy(command.getOperatorId());
        customer.setCreatedAt(now);
        customer.setUpdatedBy(command.getOperatorId());
        customer.setUpdatedAt(now);
        customer.setDeleted(0);
        customerMapper.insert(customer);
        return customer;
    }

    private VehicleEntity createVehicleFromSnapshot(DraftSnapshotWritable command, Long customerId) {
        LocalDateTime now = LocalDateTime.now();
        VehicleEntity vehicle = new VehicleEntity();
        vehicle.setStoreId(command.getStoreId());
        vehicle.setCustomerId(customerId);
        vehicle.setFrameNo(command.getFrameNoSnapshot().trim());
        vehicle.setModel(trimToNull(command.getVehicleModelSnapshot()));
        vehicle.setBatteryNo(trimToNull(command.getBatteryNoSnapshot()));
        vehicle.setCreatedBy(command.getOperatorId());
        vehicle.setCreatedAt(now);
        vehicle.setUpdatedBy(command.getOperatorId());
        vehicle.setUpdatedAt(now);
        vehicle.setDeleted(0);
        vehicleMapper.insert(vehicle);
        return vehicle;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private record ResolvedDraftReference(CustomerEntity customer, VehicleEntity vehicle) {
    }
}
