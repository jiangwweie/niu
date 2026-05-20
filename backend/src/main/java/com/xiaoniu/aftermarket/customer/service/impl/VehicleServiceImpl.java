package com.xiaoniu.aftermarket.customer.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xiaoniu.aftermarket.common.api.ErrorCode;
import com.xiaoniu.aftermarket.common.exception.BusinessException;
import com.xiaoniu.aftermarket.common.pagination.PageResponse;
import com.xiaoniu.aftermarket.customer.dto.*;
import com.xiaoniu.aftermarket.customer.entity.CustomerEntity;
import com.xiaoniu.aftermarket.customer.entity.VehicleEntity;
import com.xiaoniu.aftermarket.customer.mapper.CustomerMapper;
import com.xiaoniu.aftermarket.customer.mapper.VehicleMapper;
import com.xiaoniu.aftermarket.customer.service.VehicleService;
import com.xiaoniu.aftermarket.workorder.dto.WorkOrderQueryResponse;
import com.xiaoniu.aftermarket.workorder.entity.WorkOrderEntity;
import com.xiaoniu.aftermarket.workorder.mapper.WorkOrderMapper;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class VehicleServiceImpl implements VehicleService {

    private final VehicleMapper vehicleMapper;
    private final CustomerMapper customerMapper;
    private final WorkOrderMapper workOrderMapper;

    public VehicleServiceImpl(VehicleMapper vehicleMapper,
                              CustomerMapper customerMapper,
                              WorkOrderMapper workOrderMapper) {
        this.vehicleMapper = vehicleMapper;
        this.customerMapper = customerMapper;
        this.workOrderMapper = workOrderMapper;
    }

    @Override
    public PageResponse<VehicleResponse> pageQuery(VehiclePageQuery query) {
        int pageNo = query.normalizedPageNo();
        int pageSize = query.normalizedPageSize();

        LambdaQueryWrapper<VehicleEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VehicleEntity::getStoreId, query.getStoreId());
        wrapper.eq(VehicleEntity::getDeleted, 0);

        // keyword search: frameNo LIKE, model LIKE
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.and(w -> w
                .like(VehicleEntity::getFrameNo, query.getKeyword())
                .or()
                .like(VehicleEntity::getModel, query.getKeyword()));
        }
        if (StringUtils.hasText(query.getVin())) {
            wrapper.like(VehicleEntity::getFrameNo, query.getVin());
        }
        if (StringUtils.hasText(query.getModel())) {
            wrapper.like(VehicleEntity::getModel, query.getModel());
        }

        // customer phone search: find matching customer IDs first
        if (StringUtils.hasText(query.getCustomerPhone())) {
            List<Long> customerIds = customerMapper.selectList(
                    new LambdaQueryWrapper<CustomerEntity>()
                            .eq(CustomerEntity::getStoreId, query.getStoreId())
                            .like(CustomerEntity::getPhone, query.getCustomerPhone())
                            .eq(CustomerEntity::getDeleted, 0)
                            .select(CustomerEntity::getId))
                    .stream().map(CustomerEntity::getId).toList();
            if (customerIds.isEmpty()) {
                return new PageResponse<>(List.of(), pageNo, pageSize, 0);
            }
            wrapper.in(VehicleEntity::getCustomerId, customerIds);
        }

        long total = vehicleMapper.selectCount(wrapper);
        if (total == 0) {
            return new PageResponse<>(List.of(), pageNo, pageSize, 0);
        }
        wrapper.orderByDesc(VehicleEntity::getCreatedAt);
        wrapper.last("LIMIT " + pageSize + " OFFSET " + (long) (pageNo - 1) * pageSize);
        List<VehicleEntity> entities = vehicleMapper.selectList(wrapper);

        // load customer info for all vehicles in page
        Map<Long, CustomerEntity> customerMap = new HashMap<>();
        Set<Long> customerIds = entities.stream()
                .map(VehicleEntity::getCustomerId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (!customerIds.isEmpty()) {
            customerMapper.selectBatchIds(customerIds)
                    .forEach(c -> customerMap.put(c.getId(), c));
        }

        List<VehicleResponse> responseList = entities.stream()
                .map(entity -> toResponse(entity, customerMap))
                .toList();

        return new PageResponse<>(responseList, pageNo, pageSize, total);
    }

    @Override
    public VehicleDetailResponse getDetail(Long vehicleId, Long storeId) {
        VehicleEntity vehicle = vehicleMapper.selectOne(
                new LambdaQueryWrapper<VehicleEntity>()
                        .eq(VehicleEntity::getId, vehicleId)
                        .eq(VehicleEntity::getStoreId, storeId)
                        .eq(VehicleEntity::getDeleted, 0));
        if (vehicle == null) {
            throw new BusinessException(ErrorCode.VEHICLE_NOT_FOUND);
        }

        VehicleDetailResponse detail = new VehicleDetailResponse();
        detail.setId(vehicle.getId());
        detail.setCustomerId(vehicle.getCustomerId());
        detail.setModel(vehicle.getModel());
        detail.setFrameNo(vehicle.getFrameNo());
        detail.setBatteryNo(vehicle.getBatteryNo());
        detail.setRemark(vehicle.getRemark());
        detail.setCreatedAt(vehicle.getCreatedAt());

        // load customer info
        if (vehicle.getCustomerId() != null) {
            CustomerEntity customer = customerMapper.selectById(vehicle.getCustomerId());
            if (customer != null && customer.getDeleted() != null && customer.getDeleted() == 0) {
                detail.setCustomerName(customer.getCustomerName());
                detail.setCustomerPhone(customer.getPhone());
            }
        }

        // load recent 20 work orders
        List<WorkOrderEntity> workOrders = workOrderMapper.selectList(
                new LambdaQueryWrapper<WorkOrderEntity>()
                        .eq(WorkOrderEntity::getVehicleId, vehicleId)
                        .eq(WorkOrderEntity::getStoreId, storeId)
                        .eq(WorkOrderEntity::getDeleted, 0)
                        .orderByDesc(WorkOrderEntity::getCreatedAt)
                        .last("LIMIT 20"));
        detail.setRecentWorkOrders(workOrders.stream().map(this::toWorkOrderResponse).toList());

        return detail;
    }

    @Override
    public Long create(Long customerId, Long storeId, Long operatorId, CreateVehicleRequest request) {
        // validate customer belongs to store
        CustomerEntity customer = customerMapper.selectOne(
                new LambdaQueryWrapper<CustomerEntity>()
                        .eq(CustomerEntity::getId, customerId)
                        .eq(CustomerEntity::getStoreId, storeId)
                        .eq(CustomerEntity::getDeleted, 0));
        if (customer == null) {
            throw new BusinessException(ErrorCode.CUSTOMER_NOT_FOUND);
        }

        // check frameNo uniqueness within store
        Long existCount = vehicleMapper.selectCount(
                new LambdaQueryWrapper<VehicleEntity>()
                        .eq(VehicleEntity::getStoreId, storeId)
                        .eq(VehicleEntity::getFrameNo, request.frameNo())
                        .eq(VehicleEntity::getDeleted, 0));
        if (existCount > 0) {
            throw new BusinessException(ErrorCode.VEHICLE_FRAME_NO_DUPLICATED);
        }

        VehicleEntity entity = new VehicleEntity();
        entity.setStoreId(storeId);
        entity.setCustomerId(customerId);
        entity.setFrameNo(request.frameNo());
        entity.setModel(request.model());
        entity.setBatteryNo(request.batteryNo());
        entity.setRemark(request.remark());
        entity.setCreatedBy(operatorId);
        entity.setDeleted(0);
        vehicleMapper.insert(entity);
        return entity.getId();
    }

    @Override
    public void update(Long vehicleId, Long storeId, Long operatorId, UpdateVehicleRequest request) {
        VehicleEntity entity = vehicleMapper.selectOne(
                new LambdaQueryWrapper<VehicleEntity>()
                        .eq(VehicleEntity::getId, vehicleId)
                        .eq(VehicleEntity::getStoreId, storeId)
                        .eq(VehicleEntity::getDeleted, 0));
        if (entity == null) {
            throw new BusinessException(ErrorCode.VEHICLE_NOT_FOUND);
        }

        // check frameNo uniqueness excluding self
        Long existCount = vehicleMapper.selectCount(
                new LambdaQueryWrapper<VehicleEntity>()
                        .eq(VehicleEntity::getStoreId, storeId)
                        .eq(VehicleEntity::getFrameNo, request.frameNo())
                        .ne(VehicleEntity::getId, vehicleId)
                        .eq(VehicleEntity::getDeleted, 0));
        if (existCount > 0) {
            throw new BusinessException(ErrorCode.VEHICLE_FRAME_NO_DUPLICATED);
        }

        entity.setFrameNo(request.frameNo());
        entity.setModel(request.model());
        entity.setBatteryNo(request.batteryNo());
        entity.setRemark(request.remark());
        entity.setUpdatedBy(operatorId);
        vehicleMapper.updateById(entity);
    }

    private VehicleResponse toResponse(VehicleEntity entity, Map<Long, CustomerEntity> customerMap) {
        VehicleResponse r = new VehicleResponse();
        r.setId(entity.getId());
        r.setCustomerId(entity.getCustomerId());
        r.setModel(entity.getModel());
        r.setFrameNo(entity.getFrameNo());
        r.setBatteryNo(entity.getBatteryNo());
        r.setRemark(entity.getRemark());
        r.setCreatedAt(entity.getCreatedAt());

        CustomerEntity customer = customerMap.get(entity.getCustomerId());
        if (customer != null) {
            r.setCustomerName(customer.getCustomerName());
            r.setCustomerPhone(customer.getPhone());
        }
        return r;
    }

    private WorkOrderQueryResponse toWorkOrderResponse(WorkOrderEntity entity) {
        WorkOrderQueryResponse r = new WorkOrderQueryResponse();
        r.setId(entity.getId());
        r.setWorkOrderNo(entity.getWorkOrderNo());
        r.setCustomerNameSnapshot(entity.getCustomerNameSnapshot());
        r.setCustomerPhoneSnapshot(entity.getCustomerPhoneSnapshot());
        r.setVehicleModelSnapshot(entity.getVehicleModelSnapshot());
        r.setFrameNoSnapshot(entity.getFrameNoSnapshot());
        r.setStatus(entity.getStatus());
        r.setReceivableAmount(entity.getReceivableAmount());
        r.setReceivedAmount(entity.getReceivedAmount());
        r.setCreatedAt(entity.getCreatedAt());
        return r;
    }
}
