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
import com.xiaoniu.aftermarket.customer.service.CustomerService;
import com.xiaoniu.aftermarket.common.enums.WorkOrderStatus;
import com.xiaoniu.aftermarket.workorder.dto.WorkOrderQueryResponse;
import com.xiaoniu.aftermarket.workorder.entity.WorkOrderEntity;
import com.xiaoniu.aftermarket.workorder.mapper.WorkOrderMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class CustomerServiceImpl implements CustomerService {

    private static final List<String> ACTIVE_WORK_ORDER_STATUSES = List.of(
            WorkOrderStatus.DRAFT.getCode(),
            WorkOrderStatus.REPAIRING.getCode(),
            WorkOrderStatus.REPAIR_DONE.getCode()
    );

    private final CustomerMapper customerMapper;
    private final VehicleMapper vehicleMapper;
    private final WorkOrderMapper workOrderMapper;

    public CustomerServiceImpl(CustomerMapper customerMapper,
                               VehicleMapper vehicleMapper,
                               WorkOrderMapper workOrderMapper) {
        this.customerMapper = customerMapper;
        this.vehicleMapper = vehicleMapper;
        this.workOrderMapper = workOrderMapper;
    }

    @Override
    public PageResponse<CustomerResponse> pageQuery(CustomerPageQuery query) {
        int pageNo = query.normalizedPageNo();
        int pageSize = query.normalizedPageSize();

        LambdaQueryWrapper<CustomerEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CustomerEntity::getStoreId, query.getStoreId());
        wrapper.eq(CustomerEntity::getDeleted, 0);

        if (StringUtils.hasText(query.getKeyword())) {
            String kw = query.getKeyword().trim();
            wrapper.and(w -> w
                .like(CustomerEntity::getCustomerName, kw)
                .or()
                .like(CustomerEntity::getPhone, kw)
                .or()
                .like(CustomerEntity::getRemark, kw));
        }
        if (StringUtils.hasText(query.getPhone())) {
            wrapper.eq(CustomerEntity::getPhone, query.getPhone());
        }
        if (StringUtils.hasText(query.getCustomerName())) {
            wrapper.like(CustomerEntity::getCustomerName, query.getCustomerName());
        }

        long total = customerMapper.selectCount(wrapper);
        if (total == 0) {
            return new PageResponse<>(List.of(), pageNo, pageSize, 0);
        }
        wrapper.orderByDesc(CustomerEntity::getCreatedAt);
        wrapper.last("LIMIT " + pageSize + " OFFSET " + (long) (pageNo - 1) * pageSize);
        List<CustomerEntity> entities = customerMapper.selectList(wrapper);

        List<CustomerResponse> responseList = entities.stream()
                .map(this::toResponse)
                .toList();

        // enrich with vehicle count and last repair time
        for (CustomerResponse cr : responseList) {
            Long count = vehicleMapper.selectCount(
                    new LambdaQueryWrapper<VehicleEntity>()
                            .eq(VehicleEntity::getCustomerId, cr.getId())
                            .eq(VehicleEntity::getDeleted, 0));
            cr.setVehicleCount(count.intValue());

            WorkOrderEntity lastOrder = workOrderMapper.selectOne(
                    new LambdaQueryWrapper<WorkOrderEntity>()
                            .eq(WorkOrderEntity::getCustomerId, cr.getId())
                            .eq(WorkOrderEntity::getDeleted, 0)
                            .orderByDesc(WorkOrderEntity::getCreatedAt)
                            .last("LIMIT 1"));
            if (lastOrder != null) {
                cr.setLastRepairAt(lastOrder.getCreatedAt());
            }
        }

        return new PageResponse<>(responseList, pageNo, pageSize, total);
    }

    @Override
    public CustomerDetailResponse getDetail(Long customerId, Long storeId) {
        CustomerEntity customer = customerMapper.selectOne(
                new LambdaQueryWrapper<CustomerEntity>()
                        .eq(CustomerEntity::getId, customerId)
                        .eq(CustomerEntity::getStoreId, storeId)
                        .eq(CustomerEntity::getDeleted, 0));
        if (customer == null) {
            throw new BusinessException(ErrorCode.CUSTOMER_NOT_FOUND);
        }

        CustomerDetailResponse detail = new CustomerDetailResponse();
        detail.setId(customer.getId());
        detail.setCustomerName(customer.getCustomerName());
        detail.setPhone(customer.getPhone());
        detail.setRemark(customer.getRemark());
        detail.setCreatedAt(customer.getCreatedAt());

        // load vehicles
        List<VehicleEntity> vehicles = vehicleMapper.selectList(
                new LambdaQueryWrapper<VehicleEntity>()
                        .eq(VehicleEntity::getCustomerId, customerId)
                        .eq(VehicleEntity::getDeleted, 0));
        detail.setVehicles(vehicles.stream().map(this::toVehicleBrief).toList());

        // load recent 20 work orders
        List<WorkOrderEntity> workOrders = workOrderMapper.selectList(
                new LambdaQueryWrapper<WorkOrderEntity>()
                        .eq(WorkOrderEntity::getCustomerId, customerId)
                        .eq(WorkOrderEntity::getStoreId, storeId)
                        .eq(WorkOrderEntity::getDeleted, 0)
                        .orderByDesc(WorkOrderEntity::getCreatedAt)
                        .last("LIMIT 20"));
        detail.setRecentWorkOrders(workOrders.stream().map(this::toWorkOrderResponse).toList());

        return detail;
    }

    @Override
    public Long create(Long storeId, Long operatorId, CreateCustomerRequest request) {
        // check phone uniqueness within store
        if (StringUtils.hasText(request.phone())) {
            Long existCount = customerMapper.selectCount(
                    new LambdaQueryWrapper<CustomerEntity>()
                            .eq(CustomerEntity::getStoreId, storeId)
                            .eq(CustomerEntity::getPhone, request.phone())
                            .eq(CustomerEntity::getDeleted, 0));
            if (existCount > 0) {
                throw new BusinessException(ErrorCode.CUSTOMER_PHONE_DUPLICATED);
            }
        }

        CustomerEntity entity = new CustomerEntity();
        entity.setStoreId(storeId);
        entity.setCustomerName(request.customerName());
        entity.setPhone(request.phone());
        entity.setRemark(request.remark());
        entity.setCreatedBy(operatorId);
        entity.setDeleted(0);
        customerMapper.insert(entity);
        return entity.getId();
    }

    @Override
    public void update(Long customerId, Long storeId, Long operatorId, UpdateCustomerRequest request) {
        CustomerEntity entity = customerMapper.selectOne(
                new LambdaQueryWrapper<CustomerEntity>()
                        .eq(CustomerEntity::getId, customerId)
                        .eq(CustomerEntity::getStoreId, storeId)
                        .eq(CustomerEntity::getDeleted, 0));
        if (entity == null) {
            throw new BusinessException(ErrorCode.CUSTOMER_NOT_FOUND);
        }

        // check phone uniqueness excluding self
        if (StringUtils.hasText(request.phone())) {
            Long existCount = customerMapper.selectCount(
                    new LambdaQueryWrapper<CustomerEntity>()
                            .eq(CustomerEntity::getStoreId, storeId)
                            .eq(CustomerEntity::getPhone, request.phone())
                            .ne(CustomerEntity::getId, customerId)
                            .eq(CustomerEntity::getDeleted, 0));
            if (existCount > 0) {
                throw new BusinessException(ErrorCode.CUSTOMER_PHONE_DUPLICATED);
            }
        }

        entity.setCustomerName(request.customerName());
        entity.setPhone(request.phone());
        entity.setRemark(request.remark());
        entity.setUpdatedBy(operatorId);
        customerMapper.updateById(entity);
    }

    @Override
    public void delete(Long customerId, Long storeId, Long operatorId) {
        CustomerEntity entity = customerMapper.selectOne(
                new LambdaQueryWrapper<CustomerEntity>()
                        .eq(CustomerEntity::getId, customerId)
                        .eq(CustomerEntity::getStoreId, storeId)
                        .eq(CustomerEntity::getDeleted, 0));
        if (entity == null) {
            throw new BusinessException(ErrorCode.CUSTOMER_NOT_FOUND);
        }
        Long activeOrders = workOrderMapper.selectCount(
                new LambdaQueryWrapper<WorkOrderEntity>()
                        .eq(WorkOrderEntity::getStoreId, storeId)
                        .eq(WorkOrderEntity::getCustomerId, customerId)
                        .eq(WorkOrderEntity::getDeleted, 0)
                        .in(WorkOrderEntity::getStatus, ACTIVE_WORK_ORDER_STATUSES));
        if (activeOrders > 0) {
            throw new BusinessException(ErrorCode.CUSTOMER_HAS_ACTIVE_ORDERS);
        }
        entity.setDeleted(1);
        entity.setUpdatedBy(operatorId);
        entity.setUpdatedAt(LocalDateTime.now());
        customerMapper.updateById(entity);
    }

    private CustomerResponse toResponse(CustomerEntity entity) {
        CustomerResponse r = new CustomerResponse();
        r.setId(entity.getId());
        r.setCustomerName(entity.getCustomerName());
        r.setPhone(entity.getPhone());
        r.setRemark(entity.getRemark());
        r.setCreatedAt(entity.getCreatedAt());
        return r;
    }

    private VehicleBriefResponse toVehicleBrief(VehicleEntity entity) {
        VehicleBriefResponse r = new VehicleBriefResponse();
        r.setId(entity.getId());
        r.setModel(entity.getModel());
        r.setFrameNo(entity.getFrameNo());
        r.setBatteryNo(entity.getBatteryNo());
        r.setRemark(entity.getRemark());
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
