package com.xiaoniu.aftermarket.reimbursement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xiaoniu.aftermarket.common.api.ErrorCode;
import com.xiaoniu.aftermarket.common.enums.ReimbursementStatus;
import com.xiaoniu.aftermarket.common.exception.BusinessException;
import com.xiaoniu.aftermarket.common.pagination.PageResponse;
import com.xiaoniu.aftermarket.common.service.SequenceService;
import com.xiaoniu.aftermarket.reimbursement.dto.CancelReimbursementCommand;
import com.xiaoniu.aftermarket.reimbursement.dto.ConfirmReimbursementCommand;
import com.xiaoniu.aftermarket.reimbursement.dto.ReimbursementQueryRequest;
import com.xiaoniu.aftermarket.reimbursement.dto.ReimbursementResponse;
import com.xiaoniu.aftermarket.reimbursement.dto.RejectReimbursementCommand;
import com.xiaoniu.aftermarket.reimbursement.dto.SubmitReimbursementCommand;
import com.xiaoniu.aftermarket.reimbursement.entity.ReimbursementEntity;
import com.xiaoniu.aftermarket.reimbursement.mapper.ReimbursementMapper;
import com.xiaoniu.aftermarket.reimbursement.service.ReimbursementService;
import com.xiaoniu.aftermarket.user.entity.SysUserEntity;
import com.xiaoniu.aftermarket.user.mapper.SysUserMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ReimbursementServiceImpl implements ReimbursementService {

    private final ReimbursementMapper reimbursementMapper;
    private final SequenceService sequenceService;
    private final SysUserMapper userMapper;

    public ReimbursementServiceImpl(ReimbursementMapper reimbursementMapper,
                                    SequenceService sequenceService,
                                    SysUserMapper userMapper) {
        this.reimbursementMapper = reimbursementMapper;
        this.sequenceService = sequenceService;
        this.userMapper = userMapper;
    }

    @Override
    @Transactional
    public Long submit(SubmitReimbursementCommand command) {
        // 员工提交报销仅创建 PENDING 记录，不直接进入财务成本
        validateSubmitCommand(command);
        LocalDateTime now = LocalDateTime.now();
        ReimbursementEntity entity = new ReimbursementEntity();
        entity.setStoreId(command.getStoreId());
        entity.setReimbursementNo(sequenceService.next("REIMBURSEMENT"));
        entity.setApplicantId(command.getApplicantId());
        entity.setPurpose(command.getPurpose().trim());
        entity.setAmount(normalizeAmount(command.getAmount()));
        entity.setStatus(ReimbursementStatus.PENDING.getCode());
        entity.setSubmittedAt(now);
        entity.setRemark(command.getRemark());
        entity.setCreatedBy(command.getOperatorId());
        reimbursementMapper.insert(entity);
        return entity.getId();
    }

    @Override
    public PageResponse<ReimbursementResponse> pageQuery(ReimbursementQueryRequest request) {
        if (request == null || request.getStoreId() == null) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST);
        }
        int pageNo = request.normalizedPageNo();
        int pageSize = request.normalizedPageSize();

        QueryWrapper<ReimbursementEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("store_id", request.getStoreId()).eq("deleted", 0);
        if (StringUtils.hasText(request.getReimbursementNo())) {
            wrapper.like("reimbursement_no", request.getReimbursementNo().trim());
        }
        if (StringUtils.hasText(request.getStatus())) {
            wrapper.eq("status", request.getStatus().trim());
        }
        if (request.getApplicantId() != null) {
            wrapper.eq("applicant_id", request.getApplicantId());
        }
        if (request.getDateFrom() != null) {
            wrapper.ge("submitted_at", request.getDateFrom());
        }
        if (request.getDateTo() != null) {
            wrapper.le("submitted_at", request.getDateTo());
        }
        long total = reimbursementMapper.selectCount(wrapper);
        if (total == 0) {
            return new PageResponse<>(List.of(), pageNo, pageSize, 0);
        }

        wrapper.orderByDesc("submitted_at").orderByDesc("id")
                .last("LIMIT " + pageSize + " OFFSET " + (long) (pageNo - 1) * pageSize);
        List<ReimbursementResponse> records = reimbursementMapper.selectList(wrapper).stream()
                .map(this::toResponse)
                .toList();
        return new PageResponse<>(records, pageNo, pageSize, total);
    }

    @Override
    public ReimbursementResponse getById(Long storeId, Long reimbursementId) {
        ReimbursementEntity entity = loadInStore(storeId, reimbursementId);
        return toResponse(entity);
    }

    @Override
    @Transactional
    public void confirm(ConfirmReimbursementCommand command) {
        // 只有 CONFIRMED 状态的报销才进入财务成本核算
        validateConfirmCommand(command);
        ReimbursementEntity entity = loadInStoreForUpdate(command.getStoreId(), command.getReimbursementId());
        validatePending(entity);

        LocalDateTime now = LocalDateTime.now();
        entity.setStatus(ReimbursementStatus.CONFIRMED.getCode());
        entity.setConfirmedAmount(normalizeAmount(command.getConfirmedAmount()));
        entity.setConfirmedBy(command.getOperatorId());
        entity.setConfirmedAt(now);
        entity.setRemark(command.getRemark());
        entity.setUpdatedBy(command.getOperatorId());
        reimbursementMapper.updateById(entity);
    }

    @Override
    @Transactional
    public void reject(RejectReimbursementCommand command) {
        // 驳回报销不计入财务成本
        validateRejectCommand(command);
        ReimbursementEntity entity = loadInStoreForUpdate(command.getStoreId(), command.getReimbursementId());
        validatePending(entity);

        LocalDateTime now = LocalDateTime.now();
        entity.setStatus(ReimbursementStatus.REJECTED.getCode());
        entity.setRejectedBy(command.getOperatorId());
        entity.setRejectedAt(now);
        entity.setRejectReason(command.getRejectReason().trim());
        entity.setUpdatedBy(command.getOperatorId());
        reimbursementMapper.updateById(entity);
    }

    @Override
    @Transactional
    public void cancel(CancelReimbursementCommand command) {
        // 员工取消报销不计入财务成本
        validateCancelCommand(command);
        ReimbursementEntity entity = loadInStoreForUpdate(command.getStoreId(), command.getReimbursementId());
        validatePending(entity);

        LocalDateTime now = LocalDateTime.now();
        entity.setStatus(ReimbursementStatus.CANCELLED.getCode());
        entity.setCancelledBy(command.getOperatorId());
        entity.setCancelledAt(now);
        entity.setCancelReason(command.getCancelReason().trim());
        entity.setUpdatedBy(command.getOperatorId());
        reimbursementMapper.updateById(entity);
    }

    private void validateSubmitCommand(SubmitReimbursementCommand command) {
        if (command == null || command.getStoreId() == null || command.getApplicantId() == null
                || command.getOperatorId() == null) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST);
        }
        if (!StringUtils.hasText(command.getPurpose())) {
            throw new BusinessException(ErrorCode.REIMBURSEMENT_PURPOSE_REQUIRED);
        }
        if (command.getAmount() == null || command.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.REIMBURSEMENT_AMOUNT_INVALID);
        }
    }

    private void validateConfirmCommand(ConfirmReimbursementCommand command) {
        if (command == null || command.getStoreId() == null || command.getReimbursementId() == null
                || command.getOperatorId() == null) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST);
        }
        if (command.getConfirmedAmount() == null || command.getConfirmedAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.REIMBURSEMENT_CONFIRMED_AMOUNT_INVALID);
        }
    }

    private void validateRejectCommand(RejectReimbursementCommand command) {
        if (command == null || command.getStoreId() == null || command.getReimbursementId() == null
                || command.getOperatorId() == null) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST);
        }
        if (!StringUtils.hasText(command.getRejectReason())) {
            throw new BusinessException(ErrorCode.REIMBURSEMENT_REJECT_REASON_REQUIRED);
        }
    }

    private void validateCancelCommand(CancelReimbursementCommand command) {
        if (command == null || command.getStoreId() == null || command.getReimbursementId() == null
                || command.getOperatorId() == null) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST);
        }
        if (!StringUtils.hasText(command.getCancelReason())) {
            throw new BusinessException(ErrorCode.REIMBURSEMENT_CANCEL_REASON_REQUIRED);
        }
    }

    private void validatePending(ReimbursementEntity entity) {
        // 仅 PENDING 状态可审批/驳回/取消，防止重复操作
        if (!ReimbursementStatus.PENDING.getCode().equals(entity.getStatus())) {
            throw new BusinessException(ErrorCode.REIMBURSEMENT_STATUS_INVALID);
        }
    }

    private ReimbursementEntity loadInStore(Long storeId, Long reimbursementId) {
        if (storeId == null || reimbursementId == null) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST);
        }
        ReimbursementEntity entity = reimbursementMapper.selectById(reimbursementId);
        if (entity == null || entity.getDeleted() != null && entity.getDeleted() == 1
                || !storeId.equals(entity.getStoreId())) {
            throw new BusinessException(ErrorCode.REIMBURSEMENT_NOT_FOUND);
        }
        return entity;
    }

    private ReimbursementEntity loadInStoreForUpdate(Long storeId, Long reimbursementId) {
        // selectByIdForUpdate 加行锁，防止并发审批同一笔报销
        if (storeId == null || reimbursementId == null) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST);
        }
        ReimbursementEntity entity = reimbursementMapper.selectByIdForUpdate(reimbursementId);
        if (entity == null || entity.getDeleted() != null && entity.getDeleted() == 1
                || !storeId.equals(entity.getStoreId())) {
            throw new BusinessException(ErrorCode.REIMBURSEMENT_NOT_FOUND);
        }
        return entity;
    }

    private BigDecimal normalizeAmount(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    private ReimbursementResponse toResponse(ReimbursementEntity entity) {
        ReimbursementResponse response = ReimbursementResponse.from(entity);
        response.setApplicantName(userDisplayName(entity.getApplicantId()));
        return response;
    }

    private String userDisplayName(Long userId) {
        if (userId == null) {
            return null;
        }
        SysUserEntity user = userMapper.selectById(userId);
        if (user == null) {
            return null;
        }
        return StringUtils.hasText(user.getRealName()) ? user.getRealName() : user.getUsername();
    }
}
