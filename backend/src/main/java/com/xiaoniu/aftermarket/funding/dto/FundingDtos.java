package com.xiaoniu.aftermarket.funding.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class FundingDtos {

    private FundingDtos() {
    }

    public record SaveApplicationRequest(
            @NotBlank String customerName,
            @NotBlank String phone,
            @NotBlank String idCardNo,
            @NotBlank String vehicleModel,
            @NotNull LocalDate pickupDate,
            @NotBlank String paymentType,
            BigDecimal purchaseCost,
            BigDecimal incentiveAmount,
            BigDecimal upstreamAmount,
            BigDecimal totalCost,
            BigDecimal retailPrice,
            @NotNull BigDecimal receivableAmount,
            BigDecimal downPayment,
            Integer installmentCount,
            BigDecimal installmentAmount,
            LocalDate firstDueDate,
            @NotBlank String groupLeader,
            String handlerName,
            String addOnRemark,
            String remark
    ) {
    }

    public record RejectApplicationRequest(@NotBlank String reason) {
    }

    public record SaveContractRequest(
            String contractNo,
            @NotBlank String contractType,
            LocalDate signedDate,
            String remark
    ) {
    }

    public record VoidContractRequest(@NotBlank String reason) {
    }

    public record UpdateLedgerRequest(
            String customerName,
            String phone,
            String idCardNo,
            String vehicleModel,
            LocalDate pickupDate,
            String paymentType,
            BigDecimal purchaseCost,
            BigDecimal incentiveAmount,
            BigDecimal upstreamAmount,
            BigDecimal totalCost,
            BigDecimal retailPrice,
            BigDecimal receivableAmount,
            String groupLeader,
            String handlerName,
            String status,
            String remark,
            String changeRemark
    ) {
    }

    public record RecordFundingPaymentRequest(
            Long installmentPlanId,
            @NotNull @Positive BigDecimal amount,
            @NotBlank String paymentMethod,
            LocalDateTime paidAt,
            String remark
    ) {
    }

    public record ApplicationResponse(
            Long id,
            Long storeId,
            String applicationNo,
            String customerName,
            String phone,
            String idCardNo,
            String vehicleModel,
            LocalDate pickupDate,
            String paymentType,
            BigDecimal purchaseCost,
            BigDecimal incentiveAmount,
            BigDecimal upstreamAmount,
            BigDecimal totalCost,
            BigDecimal retailPrice,
            BigDecimal receivableAmount,
            BigDecimal downPayment,
            Integer installmentCount,
            BigDecimal installmentAmount,
            LocalDate firstDueDate,
            String groupLeader,
            String handlerName,
            String addOnRemark,
            String status,
            String auditRemark,
            Long auditedBy,
            LocalDateTime auditedAt,
            String remark,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }

    public record ContractResponse(
            Long id,
            Long applicationId,
            String contractNo,
            String contractType,
            LocalDate signedDate,
            String status,
            Long confirmedBy,
            LocalDateTime confirmedAt,
            String voidReason,
            String remark,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }

    public record LedgerResponse(
            Long id,
            Long applicationId,
            Long contractId,
            String ledgerNo,
            String customerName,
            String phone,
            String idCardNo,
            String vehicleModel,
            LocalDate pickupDate,
            String paymentType,
            BigDecimal purchaseCost,
            BigDecimal incentiveAmount,
            BigDecimal upstreamAmount,
            BigDecimal totalCost,
            BigDecimal retailPrice,
            BigDecimal receivableAmount,
            BigDecimal receivedAmount,
            BigDecimal outstandingAmount,
            String groupLeader,
            String handlerName,
            String status,
            String remark,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }

    public record InstallmentPlanResponse(
            Long id,
            Long ledgerId,
            Integer phaseNo,
            String phaseName,
            LocalDate dueDate,
            BigDecimal receivableAmount,
            BigDecimal receivedAmount,
            String status,
            String remark
    ) {
    }

    public record FundingPaymentResponse(
            Long id,
            Long ledgerId,
            Long installmentPlanId,
            String paymentNo,
            BigDecimal amount,
            String paymentMethod,
            LocalDateTime paidAt,
            Long operatorId,
            String remark,
            LocalDateTime createdAt
    ) {
    }

    public record AttachmentResponse(
            Long id,
            String ownerType,
            Long ownerId,
            String attachmentType,
            String originalFilename,
            String contentType,
            Long fileSize,
            String remark,
            LocalDateTime createdAt
    ) {
    }

    public record ChangeLogResponse(
            Long id,
            Long ledgerId,
            String fieldName,
            String oldValue,
            String newValue,
            Long operatorId,
            LocalDateTime operatedAt,
            String remark
    ) {
    }

    public record FundingDetailResponse(
            ApplicationResponse application,
            ContractResponse contract,
            LedgerResponse ledger,
            List<InstallmentPlanResponse> installmentPlans,
            List<FundingPaymentResponse> payments,
            List<AttachmentResponse> attachments,
            List<ChangeLogResponse> changeLogs
    ) {
    }

    public record FundingSummaryResponse(
            long applicationCount,
            long pendingAuditCount,
            long ledgerCount,
            BigDecimal receivableTotal,
            BigDecimal receivedTotal,
            BigDecimal outstandingTotal,
            long overduePlanCount
    ) {
    }

    public record FundingImportResultResponse(
            Long batchId,
            String batchNo,
            String status,
            int totalRows,
            int successRows,
            int failedRows,
            List<String> errors
    ) {
    }
}
