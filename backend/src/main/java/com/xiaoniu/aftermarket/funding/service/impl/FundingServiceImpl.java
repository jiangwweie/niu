package com.xiaoniu.aftermarket.funding.service.impl;

import static com.xiaoniu.aftermarket.common.util.SearchKeywordUtils.buildContainsPattern;
import static com.xiaoniu.aftermarket.common.util.SearchKeywordUtils.containsCondition;
import static com.xiaoniu.aftermarket.common.util.SearchKeywordUtils.normalize;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xiaoniu.aftermarket.common.api.ErrorCode;
import com.xiaoniu.aftermarket.common.exception.BusinessException;
import com.xiaoniu.aftermarket.common.pagination.PageResponse;
import com.xiaoniu.aftermarket.common.service.SequenceService;
import com.xiaoniu.aftermarket.export.dto.ExportFile;
import com.xiaoniu.aftermarket.funding.dto.FundingDtos.ApplicationResponse;
import com.xiaoniu.aftermarket.funding.dto.FundingDtos.AttachmentResponse;
import com.xiaoniu.aftermarket.funding.dto.FundingDtos.ChangeLogResponse;
import com.xiaoniu.aftermarket.funding.dto.FundingDtos.ContractResponse;
import com.xiaoniu.aftermarket.funding.dto.FundingDtos.FundingDetailResponse;
import com.xiaoniu.aftermarket.funding.dto.FundingDtos.FundingImportResultResponse;
import com.xiaoniu.aftermarket.funding.dto.FundingDtos.FundingPaymentResponse;
import com.xiaoniu.aftermarket.funding.dto.FundingDtos.FundingSummaryResponse;
import com.xiaoniu.aftermarket.funding.dto.FundingDtos.InstallmentPlanResponse;
import com.xiaoniu.aftermarket.funding.dto.FundingDtos.LedgerResponse;
import com.xiaoniu.aftermarket.funding.dto.FundingDtos.RecordFundingPaymentRequest;
import com.xiaoniu.aftermarket.funding.dto.FundingDtos.SaveApplicationRequest;
import com.xiaoniu.aftermarket.funding.dto.FundingDtos.SaveContractRequest;
import com.xiaoniu.aftermarket.funding.dto.FundingDtos.UpdateLedgerRequest;
import com.xiaoniu.aftermarket.funding.entity.FundingApplicationEntity;
import com.xiaoniu.aftermarket.funding.entity.FundingAttachmentEntity;
import com.xiaoniu.aftermarket.funding.entity.FundingContractEntity;
import com.xiaoniu.aftermarket.funding.entity.FundingImportBatchEntity;
import com.xiaoniu.aftermarket.funding.entity.FundingInstallmentPlanEntity;
import com.xiaoniu.aftermarket.funding.entity.FundingLedgerChangeLogEntity;
import com.xiaoniu.aftermarket.funding.entity.FundingLedgerEntity;
import com.xiaoniu.aftermarket.funding.entity.FundingPaymentRecordEntity;
import com.xiaoniu.aftermarket.funding.mapper.FundingApplicationMapper;
import com.xiaoniu.aftermarket.funding.mapper.FundingAttachmentMapper;
import com.xiaoniu.aftermarket.funding.mapper.FundingContractMapper;
import com.xiaoniu.aftermarket.funding.mapper.FundingImportBatchMapper;
import com.xiaoniu.aftermarket.funding.mapper.FundingInstallmentPlanMapper;
import com.xiaoniu.aftermarket.funding.mapper.FundingLedgerChangeLogMapper;
import com.xiaoniu.aftermarket.funding.mapper.FundingLedgerMapper;
import com.xiaoniu.aftermarket.funding.mapper.FundingPaymentRecordMapper;
import com.xiaoniu.aftermarket.funding.service.FundingService;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FundingServiceImpl implements FundingService {

    private static final String APP_DRAFT = "DRAFT";
    private static final String APP_PENDING = "PENDING_AUDIT";
    private static final String APP_APPROVED = "APPROVED";
    private static final String APP_REJECTED = "REJECTED";
    private static final String APP_CONTRACT_PENDING = "CONTRACT_PENDING";
    private static final String APP_CONTRACT_CONFIRMED = "CONTRACT_CONFIRMED";
    private static final String APP_LEDGER_CREATED = "LEDGER_CREATED";

    private static final String CONTRACT_UPLOADED = "UPLOADED";
    private static final String CONTRACT_CONFIRMED = "CONFIRMED";
    private static final String CONTRACT_VOIDED = "VOIDED";

    private static final String LEDGER_NORMAL = "NORMAL";
    private static final String LEDGER_PARTIAL_PAID = "PARTIAL_PAID";
    private static final String LEDGER_SETTLED = "SETTLED";
    private static final String LEDGER_OVERDUE = "OVERDUE";
    private static final String LEDGER_VOIDED = "VOIDED";
    private static final String IMPORT_IMPORTED = "IMPORTED";
    private static final String IMPORT_FAILED = "FAILED";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final FundingApplicationMapper applicationMapper;
    private final FundingContractMapper contractMapper;
    private final FundingLedgerMapper ledgerMapper;
    private final FundingInstallmentPlanMapper planMapper;
    private final FundingPaymentRecordMapper paymentMapper;
    private final FundingAttachmentMapper attachmentMapper;
    private final FundingImportBatchMapper importBatchMapper;
    private final FundingLedgerChangeLogMapper changeLogMapper;
    private final SequenceService sequenceService;
    private final Path uploadRoot;

    public FundingServiceImpl(FundingApplicationMapper applicationMapper,
                              FundingContractMapper contractMapper,
                              FundingLedgerMapper ledgerMapper,
                              FundingInstallmentPlanMapper planMapper,
                              FundingPaymentRecordMapper paymentMapper,
                              FundingAttachmentMapper attachmentMapper,
                              FundingImportBatchMapper importBatchMapper,
                              FundingLedgerChangeLogMapper changeLogMapper,
                              SequenceService sequenceService,
                              @Value("${app.funding.upload-dir:uploads/funding}") String uploadDir) {
        this.applicationMapper = applicationMapper;
        this.contractMapper = contractMapper;
        this.ledgerMapper = ledgerMapper;
        this.planMapper = planMapper;
        this.paymentMapper = paymentMapper;
        this.attachmentMapper = attachmentMapper;
        this.importBatchMapper = importBatchMapper;
        this.changeLogMapper = changeLogMapper;
        this.sequenceService = sequenceService;
        this.uploadRoot = Path.of(uploadDir).toAbsolutePath().normalize();
    }

    @Override
    public PageResponse<ApplicationResponse> listApplications(Long storeId, String keyword, String status, int pageNo, int pageSize) {
        QueryWrapper<FundingApplicationEntity> wrapper = baseStoreWrapper(storeId);
        applyKeyword(wrapper, keyword);
        if (StringUtils.hasText(status)) {
            wrapper.eq("status", status.trim());
        }
        return page(wrapper, pageNo, pageSize, "created_at", applicationMapper, this::toApplicationResponse);
    }

    @Override
    @Transactional
    public ApplicationResponse createApplication(Long storeId, Long operatorId, SaveApplicationRequest request) {
        validateUserContext(storeId, operatorId);
        FundingApplicationEntity entity = new FundingApplicationEntity();
        entity.setStoreId(storeId);
        entity.setApplicationNo(sequenceService.next("FUNDING_APPLICATION"));
        entity.setStatus(APP_DRAFT);
        applyApplicationFields(entity, request);
        entity.setCreatedBy(operatorId);
        applicationMapper.insert(entity);
        return toApplicationResponse(entity);
    }

    @Override
    @Transactional
    public ApplicationResponse updateApplication(Long storeId, Long operatorId, Long id, SaveApplicationRequest request) {
        FundingApplicationEntity entity = loadApplication(storeId, id);
        if (APP_LEDGER_CREATED.equals(entity.getStatus()) || APP_CONTRACT_CONFIRMED.equals(entity.getStatus())) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "已确认合同或已生成台账的资料不可直接编辑");
        }
        applyApplicationFields(entity, request);
        entity.setUpdatedBy(operatorId);
        applicationMapper.updateById(entity);
        return toApplicationResponse(entity);
    }

    @Override
    @Transactional
    public ApplicationResponse submitApplication(Long storeId, Long operatorId, Long id) {
        FundingApplicationEntity entity = loadApplication(storeId, id);
        if (!List.of(APP_DRAFT, APP_REJECTED).contains(entity.getStatus())) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "当前状态不允许提交审核");
        }
        entity.setStatus(APP_PENDING);
        entity.setSubmittedBy(operatorId);
        entity.setSubmittedAt(LocalDateTime.now());
        entity.setAuditRemark(null);
        entity.setUpdatedBy(operatorId);
        applicationMapper.updateById(entity);
        return toApplicationResponse(entity);
    }

    @Override
    @Transactional
    public ApplicationResponse approveApplication(Long storeId, Long operatorId, Long id) {
        FundingApplicationEntity entity = loadApplication(storeId, id);
        if (!APP_PENDING.equals(entity.getStatus())) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "只有待审核资料可以同意");
        }
        entity.setStatus(APP_CONTRACT_PENDING);
        entity.setAuditRemark("同意");
        entity.setAuditedBy(operatorId);
        entity.setAuditedAt(LocalDateTime.now());
        entity.setUpdatedBy(operatorId);
        applicationMapper.updateById(entity);
        return toApplicationResponse(entity);
    }

    @Override
    @Transactional
    public ApplicationResponse rejectApplication(Long storeId, Long operatorId, Long id, String reason) {
        if (!StringUtils.hasText(reason)) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "不同意原因不能为空");
        }
        FundingApplicationEntity entity = loadApplication(storeId, id);
        if (!APP_PENDING.equals(entity.getStatus())) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "只有待审核资料可以不同意");
        }
        entity.setStatus(APP_REJECTED);
        entity.setAuditRemark(reason.trim());
        entity.setAuditedBy(operatorId);
        entity.setAuditedAt(LocalDateTime.now());
        entity.setUpdatedBy(operatorId);
        applicationMapper.updateById(entity);
        return toApplicationResponse(entity);
    }

    @Override
    public FundingDetailResponse getApplicationDetail(Long storeId, Long id) {
        FundingApplicationEntity application = loadApplication(storeId, id);
        FundingContractEntity contract = findContractByApplication(storeId, id);
        FundingLedgerEntity ledger = contract == null ? null : findLedgerByContract(storeId, contract.getId());
        return detail(application, contract, ledger);
    }

    @Override
    @Transactional
    public ContractResponse saveContract(Long storeId, Long operatorId, Long applicationId, SaveContractRequest request) {
        FundingApplicationEntity application = loadApplication(storeId, applicationId);
        if (!List.of(APP_CONTRACT_PENDING, APP_APPROVED).contains(application.getStatus())) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "资料同意后才能上传合同");
        }
        FundingContractEntity contract = findContractByApplication(storeId, applicationId);
        if (contract == null) {
            contract = new FundingContractEntity();
            contract.setStoreId(storeId);
            contract.setApplicationId(applicationId);
            contract.setContractNo(StringUtils.hasText(request.contractNo())
                    ? request.contractNo().trim()
                    : sequenceService.next("FUNDING_CONTRACT"));
            contract.setStatus(CONTRACT_UPLOADED);
            contract.setCreatedBy(operatorId);
        } else if (CONTRACT_CONFIRMED.equals(contract.getStatus())) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "已确认合同不可覆盖");
        }
        contract.setContractType(trimRequired(request.contractType(), "合同类型不能为空"));
        contract.setSignedDate(request.signedDate());
        contract.setRemark(request.remark());
        contract.setUpdatedBy(operatorId);
        if (contract.getId() == null) {
            contractMapper.insert(contract);
        } else {
            contractMapper.updateById(contract);
        }
        return toContractResponse(contract);
    }

    @Override
    @Transactional
    public LedgerResponse confirmContract(Long storeId, Long operatorId, Long contractId) {
        FundingContractEntity contract = loadContract(storeId, contractId);
        if (CONTRACT_VOIDED.equals(contract.getStatus())) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "作废合同不可确认");
        }
        FundingApplicationEntity application = loadApplication(storeId, contract.getApplicationId());
        FundingLedgerEntity existing = findLedgerByContract(storeId, contractId);
        if (existing != null) {
            return toLedgerResponse(existing);
        }

        LocalDateTime now = LocalDateTime.now();
        contract.setStatus(CONTRACT_CONFIRMED);
        contract.setConfirmedBy(operatorId);
        contract.setConfirmedAt(now);
        contract.setUpdatedBy(operatorId);
        contractMapper.updateById(contract);

        application.setStatus(APP_CONTRACT_CONFIRMED);
        application.setUpdatedBy(operatorId);
        applicationMapper.updateById(application);

        FundingLedgerEntity ledger = buildLedgerFromApplication(application, contract, operatorId);
        ledgerMapper.insert(ledger);
        createInstallmentPlans(application, ledger, operatorId);
        refreshLedgerAmounts(ledger, operatorId);

        application.setStatus(APP_LEDGER_CREATED);
        application.setUpdatedBy(operatorId);
        applicationMapper.updateById(application);
        return toLedgerResponse(loadLedger(storeId, ledger.getId()));
    }

    @Override
    @Transactional
    public ContractResponse voidContract(Long storeId, Long operatorId, Long contractId, String reason) {
        if (!StringUtils.hasText(reason)) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "作废原因不能为空");
        }
        FundingContractEntity contract = loadContract(storeId, contractId);
        if (findLedgerByContract(storeId, contractId) != null) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "已生成台账的合同不可作废");
        }
        contract.setStatus(CONTRACT_VOIDED);
        contract.setVoidReason(reason.trim());
        contract.setUpdatedBy(operatorId);
        contractMapper.updateById(contract);
        return toContractResponse(contract);
    }

    @Override
    public PageResponse<LedgerResponse> listLedgers(Long storeId, String keyword, String status, int pageNo, int pageSize) {
        QueryWrapper<FundingLedgerEntity> wrapper = baseStoreWrapper(storeId);
        String normalized = normalize(keyword);
        if (normalized != null) {
            String pattern = buildContainsPattern(normalized);
            wrapper.and(q -> q.apply(containsCondition("ledger_no"), pattern)
                    .or().apply(containsCondition("customer_name"), pattern)
                    .or().apply(containsCondition("phone"), pattern)
                    .or().apply(containsCondition("vehicle_model"), pattern)
                    .or().apply(containsCondition("group_leader"), pattern));
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq("status", status.trim());
        }
        return page(wrapper, pageNo, pageSize, "created_at", ledgerMapper, this::toLedgerResponse);
    }

    @Override
    public FundingDetailResponse getLedgerDetail(Long storeId, Long ledgerId) {
        FundingLedgerEntity ledger = loadLedger(storeId, ledgerId);
        FundingApplicationEntity application = loadApplication(storeId, ledger.getApplicationId());
        FundingContractEntity contract = loadContract(storeId, ledger.getContractId());
        return detail(application, contract, ledger);
    }

    @Override
    @Transactional
    public LedgerResponse updateLedger(Long storeId, Long operatorId, Long ledgerId, UpdateLedgerRequest request) {
        FundingLedgerEntity ledger = loadLedger(storeId, ledgerId);
        String remark = request.changeRemark();
        change(ledger, "customerName", ledger.getCustomerName(), request.customerName(), v -> ledger.setCustomerName(v), operatorId, remark);
        change(ledger, "phone", ledger.getPhone(), request.phone(), v -> ledger.setPhone(v), operatorId, remark);
        change(ledger, "idCardNo", ledger.getIdCardNo(), request.idCardNo(), v -> ledger.setIdCardNo(v), operatorId, remark);
        change(ledger, "vehicleModel", ledger.getVehicleModel(), request.vehicleModel(), v -> ledger.setVehicleModel(v), operatorId, remark);
        if (request.pickupDate() != null && !Objects.equals(ledger.getPickupDate(), request.pickupDate())) {
            writeLog(ledger, "pickupDate", String.valueOf(ledger.getPickupDate()), String.valueOf(request.pickupDate()), operatorId, remark);
            ledger.setPickupDate(request.pickupDate());
        }
        change(ledger, "paymentType", ledger.getPaymentType(), request.paymentType(), v -> ledger.setPaymentType(v), operatorId, remark);
        changeAmount(ledger, "purchaseCost", ledger.getPurchaseCost(), request.purchaseCost(), v -> ledger.setPurchaseCost(v), operatorId, remark);
        changeAmount(ledger, "incentiveAmount", ledger.getIncentiveAmount(), request.incentiveAmount(), v -> ledger.setIncentiveAmount(v), operatorId, remark);
        changeAmount(ledger, "upstreamAmount", ledger.getUpstreamAmount(), request.upstreamAmount(), v -> ledger.setUpstreamAmount(v), operatorId, remark);
        changeAmount(ledger, "totalCost", ledger.getTotalCost(), request.totalCost(), v -> ledger.setTotalCost(v), operatorId, remark);
        changeAmount(ledger, "retailPrice", ledger.getRetailPrice(), request.retailPrice(), v -> ledger.setRetailPrice(v), operatorId, remark);
        changeAmount(ledger, "receivableAmount", ledger.getReceivableAmount(), request.receivableAmount(), v -> ledger.setReceivableAmount(v), operatorId, remark);
        change(ledger, "groupLeader", ledger.getGroupLeader(), request.groupLeader(), v -> ledger.setGroupLeader(v), operatorId, remark);
        change(ledger, "handlerName", ledger.getHandlerName(), request.handlerName(), v -> ledger.setHandlerName(v), operatorId, remark);
        change(ledger, "status", ledger.getStatus(), request.status(), v -> ledger.setStatus(v), operatorId, remark);
        change(ledger, "remark", ledger.getRemark(), request.remark(), v -> ledger.setRemark(v), operatorId, remark);
        if (nz(ledger.getReceivableAmount()).compareTo(nz(ledger.getReceivedAmount())) < 0) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "应收金额不能小于已收金额");
        }
        ledger.setOutstandingAmount(nz(ledger.getReceivableAmount()).subtract(nz(ledger.getReceivedAmount())).setScale(2, RoundingMode.HALF_UP));
        validateLedgerStatus(ledger);
        ledger.setUpdatedBy(operatorId);
        ledgerMapper.updateById(ledger);
        return toLedgerResponse(loadLedger(storeId, ledgerId));
    }

    @Override
    @Transactional
    public FundingPaymentResponse recordPayment(Long storeId, Long operatorId, Long ledgerId, RecordFundingPaymentRequest request) {
        FundingLedgerEntity ledger = loadLedger(storeId, ledgerId);
        if (LEDGER_VOIDED.equals(ledger.getStatus())) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "作废台账不可登记收款");
        }
        BigDecimal amount = normalizePositiveAmount(request.amount(), "收款金额必须大于0");
        if (nz(ledger.getReceivedAmount()).add(amount).compareTo(nz(ledger.getReceivableAmount())) > 0) {
            throw new BusinessException(ErrorCode.PAYMENT_EXCEEDS_RECEIVABLE);
        }
        FundingPaymentRecordEntity payment = new FundingPaymentRecordEntity();
        payment.setStoreId(storeId);
        payment.setLedgerId(ledgerId);
        payment.setInstallmentPlanId(request.installmentPlanId());
        payment.setPaymentNo(sequenceService.next("FUNDING_PAYMENT"));
        payment.setAmount(amount);
        payment.setPaymentMethod(trimRequired(request.paymentMethod(), "收款方式不能为空"));
        payment.setPaidAt(request.paidAt() != null ? request.paidAt() : LocalDateTime.now());
        payment.setOperatorId(operatorId);
        payment.setRemark(request.remark());
        payment.setCreatedBy(operatorId);
        paymentMapper.insert(payment);
        if (request.installmentPlanId() != null) {
            FundingInstallmentPlanEntity plan = loadPlan(storeId, ledgerId, request.installmentPlanId());
            plan.setReceivedAmount(nz(plan.getReceivedAmount()).add(amount).setScale(2, RoundingMode.HALF_UP));
            plan.setStatus(planStatus(plan));
            plan.setUpdatedBy(operatorId);
            planMapper.updateById(plan);
        }
        refreshLedgerAmounts(ledger, operatorId);
        return toPaymentResponse(payment);
    }

    @Override
    public FundingSummaryResponse summary(Long storeId) {
        long applicationCount = applicationMapper.selectCount(this.<FundingApplicationEntity>baseStoreWrapper(storeId));
        long pendingAuditCount = applicationMapper.selectCount(this.<FundingApplicationEntity>baseStoreWrapper(storeId).eq("status", APP_PENDING));
        List<FundingLedgerEntity> ledgers = ledgerMapper.selectList(this.<FundingLedgerEntity>baseStoreWrapper(storeId));
        BigDecimal receivable = BigDecimal.ZERO;
        BigDecimal received = BigDecimal.ZERO;
        BigDecimal outstanding = BigDecimal.ZERO;
        for (FundingLedgerEntity ledger : ledgers) {
            receivable = receivable.add(nz(ledger.getReceivableAmount()));
            received = received.add(nz(ledger.getReceivedAmount()));
            outstanding = outstanding.add(nz(ledger.getOutstandingAmount()));
        }
        long overdue = planMapper.selectCount(this.<FundingInstallmentPlanEntity>baseStoreWrapper(storeId)
                .lt("due_date", LocalDate.now())
                .ne("status", "PAID"));
        return new FundingSummaryResponse(applicationCount, pendingAuditCount, ledgers.size(),
                receivable.setScale(2, RoundingMode.HALF_UP),
                received.setScale(2, RoundingMode.HALF_UP),
                outstanding.setScale(2, RoundingMode.HALF_UP),
                overdue);
    }

    @Override
    @Transactional
    public FundingImportResultResponse importLedgers(Long storeId, Long operatorId, MultipartFile file) throws IOException {
        validateUserContext(storeId, operatorId);
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "导入文件不能为空");
        }
        FundingImportBatchEntity batch = new FundingImportBatchEntity();
        batch.setStoreId(storeId);
        batch.setBatchNo(sequenceService.next("FUNDING_IMPORT"));
        batch.setOriginalFilename(file.getOriginalFilename() == null ? "funding-import.xlsx" : file.getOriginalFilename());
        batch.setStatus("PENDING");
        batch.setTotalRows(0);
        batch.setSuccessRows(0);
        batch.setFailedRows(0);
        batch.setCreatedBy(operatorId);
        importBatchMapper.insert(batch);

        int total = 0;
        int success = 0;
        List<String> errors = new ArrayList<>();
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getNumberOfSheets() == 0 ? null : workbook.getSheetAt(0);
            if (sheet == null || sheet.getLastRowNum() < 1) {
                throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "导入文件没有可识别的数据行");
            }
            Map<String, Integer> headers = headerIndexes(sheet.getRow(0));
            DataFormatter formatter = new DataFormatter();
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (isSkippedImportRow(row, headers, formatter)) {
                    continue;
                }
                total++;
                try {
                    ImportLedgerRow imported = parseImportRow(row, headers, formatter);
                    createImportedLedger(storeId, operatorId, imported);
                    success++;
                } catch (RuntimeException e) {
                    errors.add("第" + (i + 1) + "行：" + e.getMessage());
                }
            }
        } catch (BusinessException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "导入文件解析失败：" + e.getMessage());
        }

        batch.setTotalRows(total);
        batch.setSuccessRows(success);
        batch.setFailedRows(errors.size());
        batch.setStatus(success > 0 ? IMPORT_IMPORTED : IMPORT_FAILED);
        batch.setErrorSummary(String.join("；", errors).substring(0, Math.min(1024, String.join("；", errors).length())));
        batch.setUpdatedBy(operatorId);
        importBatchMapper.updateById(batch);
        return new FundingImportResultResponse(batch.getId(), batch.getBatchNo(), batch.getStatus(), total, success, errors.size(), errors);
    }

    @Override
    public ExportFile exportLedgers(Long storeId, String keyword, String status) {
        QueryWrapper<FundingLedgerEntity> wrapper = baseStoreWrapper(storeId);
        String normalized = normalize(keyword);
        if (normalized != null) {
            String pattern = buildContainsPattern(normalized);
            wrapper.and(q -> q.apply(containsCondition("ledger_no"), pattern)
                    .or().apply(containsCondition("customer_name"), pattern)
                    .or().apply(containsCondition("phone"), pattern)
                    .or().apply(containsCondition("vehicle_model"), pattern)
                    .or().apply(containsCondition("group_leader"), pattern));
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq("status", status.trim());
        }
        wrapper.orderByDesc("created_at").orderByDesc("id");
        List<FundingLedgerEntity> ledgers = ledgerMapper.selectList(wrapper);
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("资方台账");
            CellStyle moneyStyle = moneyStyle(workbook);
            writeHeader(sheet.createRow(0), List.of("台账编号", "客户", "电话", "身份证号", "车型", "提车日期",
                    "付款方式", "进货成本", "激励", "上级费用", "总计成本", "零售价格", "应收总计",
                    "已收金额", "未收金额", "组长", "经办人", "状态", "合同编号", "合同状态", "创建时间", "备注"));
            int rowIndex = 1;
            for (FundingLedgerEntity ledger : ledgers) {
                FundingContractEntity contract = loadContract(storeId, ledger.getContractId());
                Row row = sheet.createRow(rowIndex++);
                writeText(row, 0, ledger.getLedgerNo());
                writeText(row, 1, ledger.getCustomerName());
                writeText(row, 2, ledger.getPhone());
                writeText(row, 3, ledger.getIdCardNo());
                writeText(row, 4, ledger.getVehicleModel());
                writeText(row, 5, formatDate(ledger.getPickupDate()));
                writeText(row, 6, paymentTypeText(ledger.getPaymentType()));
                writeMoney(row, 7, ledger.getPurchaseCost(), moneyStyle);
                writeMoney(row, 8, ledger.getIncentiveAmount(), moneyStyle);
                writeMoney(row, 9, ledger.getUpstreamAmount(), moneyStyle);
                writeMoney(row, 10, ledger.getTotalCost(), moneyStyle);
                writeMoney(row, 11, ledger.getRetailPrice(), moneyStyle);
                writeMoney(row, 12, ledger.getReceivableAmount(), moneyStyle);
                writeMoney(row, 13, ledger.getReceivedAmount(), moneyStyle);
                writeMoney(row, 14, ledger.getOutstandingAmount(), moneyStyle);
                writeText(row, 15, ledger.getGroupLeader());
                writeText(row, 16, ledger.getHandlerName());
                writeText(row, 17, ledgerStatusText(ledger.getStatus()));
                writeText(row, 18, contract.getContractNo());
                writeText(row, 19, contractStatusText(contract.getStatus()));
                writeText(row, 20, formatDateTime(ledger.getCreatedAt()));
                writeText(row, 21, ledger.getRemark());
            }
            autosize(sheet, 22);
            return new ExportFile("funding_ledgers_" + LocalDate.now() + ".xlsx", toBytes(workbook));
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.COMMON_INTERNAL_ERROR, "导出资方台账失败");
        }
    }

    @Override
    @Transactional
    public AttachmentResponse uploadAttachment(Long storeId, Long operatorId, String ownerType, Long ownerId, String attachmentType,
                                               MultipartFile file, String remark) throws IOException {
        validateOwner(storeId, ownerType, ownerId);
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "上传文件不能为空");
        }
        String original = file.getOriginalFilename() != null ? file.getOriginalFilename() : "upload";
        String safe = original.replaceAll("[\\\\/\\r\\n]", "_");
        String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        Path dir = uploadRoot.resolve(String.valueOf(storeId)).resolve(date).normalize();
        Files.createDirectories(dir);
        String stored = UUID.randomUUID() + "-" + safe;
        Path target = dir.resolve(stored).normalize();
        if (!target.startsWith(uploadRoot)) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "文件路径非法");
        }
        file.transferTo(target);

        FundingAttachmentEntity attachment = new FundingAttachmentEntity();
        attachment.setStoreId(storeId);
        attachment.setOwnerType(trimRequired(ownerType, "附件归属不能为空"));
        attachment.setOwnerId(ownerId);
        attachment.setAttachmentType(trimRequired(attachmentType, "附件类型不能为空"));
        attachment.setOriginalFilename(original);
        attachment.setStoredFilename(stored);
        attachment.setStoragePath(target.toString());
        attachment.setContentType(file.getContentType());
        attachment.setFileSize(file.getSize());
        attachment.setRemark(remark);
        attachment.setCreatedBy(operatorId);
        attachmentMapper.insert(attachment);
        return toAttachmentResponse(attachment);
    }

    @Override
    public Resource loadAttachment(Long storeId, Long attachmentId) {
        FundingAttachmentEntity attachment = loadAttachmentEntity(storeId, attachmentId);
        FileSystemResource resource = new FileSystemResource(attachment.getStoragePath());
        if (!resource.exists()) {
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND, "附件文件不存在");
        }
        return resource;
    }

    @Override
    public String attachmentFilename(Long storeId, Long attachmentId) {
        return loadAttachmentEntity(storeId, attachmentId).getOriginalFilename();
    }

    private void createImportedLedger(Long storeId, Long operatorId, ImportLedgerRow imported) {
        BigDecimal received = nz(imported.receivedAmount());
        if (received.compareTo(nz(imported.receivableAmount())) > 0) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "收款总计不能大于应收总计");
        }
        LocalDateTime now = LocalDateTime.now();
        FundingApplicationEntity application = new FundingApplicationEntity();
        application.setStoreId(storeId);
        application.setApplicationNo(sequenceService.next("FUNDING_APPLICATION"));
        application.setCustomerName(imported.customerName());
        application.setPhone(imported.phone());
        application.setIdCardNo(imported.idCardNo());
        application.setVehicleModel(imported.vehicleModel());
        application.setPickupDate(imported.pickupDate());
        application.setPaymentType(imported.paymentType());
        application.setPurchaseCost(imported.purchaseCost());
        application.setIncentiveAmount(imported.incentiveAmount());
        application.setUpstreamAmount(imported.upstreamAmount());
        application.setTotalCost(imported.totalCost());
        application.setRetailPrice(imported.retailPrice());
        application.setReceivableAmount(imported.receivableAmount());
        application.setDownPayment(imported.downPayment());
        application.setInstallmentCount(imported.installmentCount());
        application.setInstallmentAmount(imported.installmentAmount());
        application.setFirstDueDate(imported.firstDueDate());
        application.setGroupLeader(imported.groupLeader());
        application.setHandlerName(imported.handlerName());
        application.setAddOnRemark(imported.addOnRemark());
        application.setStatus(APP_LEDGER_CREATED);
        application.setAuditRemark("Excel导入");
        application.setSubmittedBy(operatorId);
        application.setSubmittedAt(now);
        application.setAuditedBy(operatorId);
        application.setAuditedAt(now);
        application.setRemark(imported.remark());
        application.setCreatedBy(operatorId);
        applicationMapper.insert(application);

        FundingContractEntity contract = new FundingContractEntity();
        contract.setStoreId(storeId);
        contract.setApplicationId(application.getId());
        contract.setContractNo(sequenceService.next("FUNDING_CONTRACT"));
        contract.setContractType(imported.paymentType());
        contract.setSignedDate(imported.pickupDate());
        contract.setStatus(CONTRACT_CONFIRMED);
        contract.setConfirmedBy(operatorId);
        contract.setConfirmedAt(now);
        contract.setRemark("Excel导入生成线下合同记录");
        contract.setCreatedBy(operatorId);
        contractMapper.insert(contract);

        FundingLedgerEntity ledger = buildLedgerFromApplication(application, contract, operatorId);
        ledgerMapper.insert(ledger);
        createInstallmentPlans(application, ledger, operatorId);

        if (received.compareTo(BigDecimal.ZERO) > 0) {
            FundingPaymentRecordEntity payment = new FundingPaymentRecordEntity();
            payment.setStoreId(storeId);
            payment.setLedgerId(ledger.getId());
            payment.setPaymentNo(sequenceService.next("FUNDING_PAYMENT"));
            payment.setAmount(received);
            payment.setPaymentMethod("TRANSFER");
            payment.setPaidAt(now);
            payment.setOperatorId(operatorId);
            payment.setRemark("Excel导入收款总计");
            payment.setCreatedBy(operatorId);
            paymentMapper.insert(payment);
        }
        refreshLedgerAmounts(ledger, operatorId);
    }

    private ImportLedgerRow parseImportRow(Row row, Map<String, Integer> headers, DataFormatter formatter) {
        String customerName = requiredText(row, headers, formatter, "姓名", "客户姓名不能为空");
        String groupLeader = defaultText(text(row, headers, formatter, "组长"), "未填写组长");
        LocalDate pickupDate = parseDate(row, headers, formatter, "日期");
        if (pickupDate == null) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "日期不能为空");
        }
        String paymentType = normalizePaymentType(text(row, headers, formatter, "付款方式"));
        BigDecimal purchaseCost = amount(row, headers, formatter, "进货成本");
        BigDecimal incentive = amount(row, headers, formatter, "激励");
        BigDecimal upstream = amount(row, headers, formatter, "上级");
        BigDecimal totalCost = amount(row, headers, formatter, "总计成本");
        if (totalCost.compareTo(BigDecimal.ZERO) == 0) {
            totalCost = purchaseCost.add(incentive).add(upstream).setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal retailPrice = amount(row, headers, formatter, "零售价格");
        BigDecimal downPayment = amount(row, headers, formatter, "首付");
        List<BigDecimal> installments = installmentAmounts(row, headers, formatter);
        BigDecimal installmentAmount = installments.isEmpty() ? BigDecimal.ZERO.setScale(2) : installments.get(0);
        BigDecimal receivable = amount(row, headers, formatter, "应收总计");
        if (receivable.compareTo(BigDecimal.ZERO) == 0) {
            receivable = downPayment.add(installments.stream().reduce(BigDecimal.ZERO, BigDecimal::add)).setScale(2, RoundingMode.HALF_UP);
        }
        if (receivable.compareTo(BigDecimal.ZERO) <= 0 && retailPrice.compareTo(BigDecimal.ZERO) > 0) {
            receivable = retailPrice;
        }
        if (receivable.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "应收总计必须大于0");
        }
        return new ImportLedgerRow(
                customerName,
                defaultText(text(row, headers, formatter, "电话"), "-"),
                defaultText(text(row, headers, formatter, "身份证号"), "-"),
                defaultText(text(row, headers, formatter, "车型"), "未填写车型"),
                pickupDate,
                paymentType,
                purchaseCost,
                incentive,
                upstream,
                totalCost,
                retailPrice,
                receivable,
                downPayment,
                installments.size(),
                installmentAmount,
                installments.isEmpty() ? null : pickupDate.plusMonths(1),
                groupLeader,
                "Excel导入",
                text(row, headers, formatter, "加装"),
                "Excel导入"
                        + optionalRemark("序号", text(row, headers, formatter, "序号"))
                        + optionalRemark("应收额", text(row, headers, formatter, "应收额")),
                amount(row, headers, formatter, "收款总计")
        );
    }

    private Map<String, Integer> headerIndexes(Row headerRow) {
        if (headerRow == null) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "导入文件缺少表头");
        }
        Map<String, Integer> indexes = new HashMap<>();
        DataFormatter formatter = new DataFormatter();
        for (Cell cell : headerRow) {
            String value = normalizeHeader(formatter.formatCellValue(cell));
            if (StringUtils.hasText(value)) {
                indexes.put(value, cell.getColumnIndex());
            }
        }
        return indexes;
    }

    private boolean isSkippedImportRow(Row row, Map<String, Integer> headers, DataFormatter formatter) {
        if (row == null) {
            return true;
        }
        boolean allBlank = true;
        for (Cell cell : row) {
            if (StringUtils.hasText(formatter.formatCellValue(cell))) {
                allBlank = false;
                break;
            }
        }
        if (allBlank) {
            return true;
        }
        String first = formatter.formatCellValue(row.getCell(0));
        String normalizedFirst = first == null ? "" : first.trim();
        if (List.of("收款栏", "小计", "合计", "总计").contains(normalizedFirst)) {
            return true;
        }
        return !StringUtils.hasText(text(row, headers, formatter, "姓名"));
    }

    private String requiredText(Row row, Map<String, Integer> headers, DataFormatter formatter, String header, String message) {
        String value = text(row, headers, formatter, header);
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, message);
        }
        return value.trim();
    }

    private String text(Row row, Map<String, Integer> headers, DataFormatter formatter, String header) {
        Cell cell = cell(row, headers, header);
        if (cell == null) {
            return "";
        }
        String value = formatter.formatCellValue(cell);
        if (!StringUtils.hasText(value) || "/".equals(value.trim())) {
            return "";
        }
        return value.trim();
    }

    private Cell cell(Row row, Map<String, Integer> headers, String header) {
        Integer index = headers.get(normalizeHeader(header));
        return index == null ? null : row.getCell(index);
    }

    private BigDecimal amount(Row row, Map<String, Integer> headers, DataFormatter formatter, String header) {
        Cell cell = cell(row, headers, header);
        if (cell == null) {
            return BigDecimal.ZERO.setScale(2);
        }
        if (cell.getCellType() == CellType.NUMERIC) {
            return importAmount(BigDecimal.valueOf(cell.getNumericCellValue()).setScale(2, RoundingMode.HALF_UP), header);
        }
        if (cell.getCellType() == CellType.FORMULA && cell.getCachedFormulaResultType() == CellType.NUMERIC) {
            return importAmount(BigDecimal.valueOf(cell.getNumericCellValue()).setScale(2, RoundingMode.HALF_UP), header);
        }
        String value = formatter.formatCellValue(cell);
        if (!StringUtils.hasText(value) || "/".equals(value.trim())) {
            return BigDecimal.ZERO.setScale(2);
        }
        String normalized = value.replace(",", "").replace("￥", "").trim();
        try {
            return importAmount(new BigDecimal(normalized).setScale(2, RoundingMode.HALF_UP), header);
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, header + "金额格式不正确");
        }
    }

    private BigDecimal importAmount(BigDecimal amount, String header) {
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, header + "金额不能为负数");
        }
        return amount;
    }

    private LocalDate parseDate(Row row, Map<String, Integer> headers, DataFormatter formatter, String header) {
        Cell cell = cell(row, headers, header);
        if (cell == null) {
            return null;
        }
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getLocalDateTimeCellValue().toLocalDate();
        }
        String value = formatter.formatCellValue(cell);
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (RuntimeException e) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, header + "日期格式必须为 yyyy-MM-dd");
        }
    }

    private List<BigDecimal> installmentAmounts(Row row, Map<String, Integer> headers, DataFormatter formatter) {
        List<BigDecimal> values = new ArrayList<>();
        for (String header : List.of("一期", "二期", "三期", "四期", "五期", "六期", "七期", "八期", "九期")) {
            BigDecimal value = amount(row, headers, formatter, header);
            if (value.compareTo(BigDecimal.ZERO) > 0) {
                values.add(value);
            }
        }
        return values;
    }

    private String normalizePaymentType(String value) {
        if ("全款".equals(value) || "FULL".equalsIgnoreCase(value)) {
            return "FULL";
        }
        return "INSTALLMENT";
    }

    private String normalizeHeader(String value) {
        return value == null ? "" : value.replaceAll("\\s+", "").trim();
    }

    private String defaultText(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value.trim() : defaultValue;
    }

    private String optionalRemark(String label, String value) {
        return StringUtils.hasText(value) ? "；" + label + "：" + value.trim() : "";
    }

    private FundingLedgerEntity buildLedgerFromApplication(FundingApplicationEntity app, FundingContractEntity contract, Long operatorId) {
        FundingLedgerEntity ledger = new FundingLedgerEntity();
        ledger.setStoreId(app.getStoreId());
        ledger.setApplicationId(app.getId());
        ledger.setContractId(contract.getId());
        ledger.setLedgerNo(sequenceService.next("FUNDING_LEDGER"));
        ledger.setCustomerName(app.getCustomerName());
        ledger.setPhone(app.getPhone());
        ledger.setIdCardNo(app.getIdCardNo());
        ledger.setVehicleModel(app.getVehicleModel());
        ledger.setPickupDate(app.getPickupDate());
        ledger.setPaymentType(app.getPaymentType());
        ledger.setPurchaseCost(app.getPurchaseCost());
        ledger.setIncentiveAmount(app.getIncentiveAmount());
        ledger.setUpstreamAmount(app.getUpstreamAmount());
        ledger.setTotalCost(app.getTotalCost());
        ledger.setRetailPrice(app.getRetailPrice());
        ledger.setReceivableAmount(app.getReceivableAmount());
        ledger.setReceivedAmount(BigDecimal.ZERO.setScale(2));
        ledger.setOutstandingAmount(app.getReceivableAmount());
        ledger.setGroupLeader(app.getGroupLeader());
        ledger.setHandlerName(app.getHandlerName());
        ledger.setStatus(LEDGER_NORMAL);
        ledger.setRemark(app.getRemark());
        ledger.setCreatedBy(operatorId);
        return ledger;
    }

    private void createInstallmentPlans(FundingApplicationEntity app, FundingLedgerEntity ledger, Long operatorId) {
        BigDecimal downPayment = nz(app.getDownPayment());
        int phase = 0;
        if (downPayment.compareTo(BigDecimal.ZERO) > 0) {
            insertPlan(app.getStoreId(), ledger.getId(), phase++, "首付", app.getPickupDate(), downPayment, operatorId);
        }
        int count = app.getInstallmentCount() != null ? Math.max(app.getInstallmentCount(), 0) : 0;
        BigDecimal installment = nz(app.getInstallmentAmount());
        LocalDate firstDue = app.getFirstDueDate() != null ? app.getFirstDueDate() : app.getPickupDate();
        for (int i = 1; i <= count; i++) {
            insertPlan(app.getStoreId(), ledger.getId(), phase++, "第" + i + "期", firstDue.plusMonths(i - 1L), installment, operatorId);
        }
        if (phase == 0) {
            insertPlan(app.getStoreId(), ledger.getId(), 0, "全款", app.getPickupDate(), app.getReceivableAmount(), operatorId);
        }
    }

    private void insertPlan(Long storeId, Long ledgerId, int phaseNo, String phaseName, LocalDate dueDate,
                            BigDecimal amount, Long operatorId) {
        FundingInstallmentPlanEntity plan = new FundingInstallmentPlanEntity();
        plan.setStoreId(storeId);
        plan.setLedgerId(ledgerId);
        plan.setPhaseNo(phaseNo);
        plan.setPhaseName(phaseName);
        plan.setDueDate(dueDate);
        plan.setReceivableAmount(nz(amount));
        plan.setReceivedAmount(BigDecimal.ZERO.setScale(2));
        plan.setStatus(planStatus(plan));
        plan.setCreatedBy(operatorId);
        planMapper.insert(plan);
    }

    private void refreshLedgerAmounts(FundingLedgerEntity ledger, Long operatorId) {
        List<FundingPaymentRecordEntity> payments = paymentMapper.selectList(this.<FundingPaymentRecordEntity>baseStoreWrapper(ledger.getStoreId())
                .eq("ledger_id", ledger.getId()));
        BigDecimal received = payments.stream()
                .map(FundingPaymentRecordEntity::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        ledger.setReceivedAmount(received);
        ledger.setOutstandingAmount(nz(ledger.getReceivableAmount()).subtract(received).setScale(2, RoundingMode.HALF_UP));
        if (ledger.getOutstandingAmount().compareTo(BigDecimal.ZERO) == 0) {
            ledger.setStatus(LEDGER_SETTLED);
        } else if (received.compareTo(BigDecimal.ZERO) > 0) {
            ledger.setStatus(LEDGER_PARTIAL_PAID);
        } else if (hasOverduePlan(ledger.getStoreId(), ledger.getId())) {
            ledger.setStatus(LEDGER_OVERDUE);
        } else {
            ledger.setStatus(LEDGER_NORMAL);
        }
        ledger.setUpdatedBy(operatorId);
        ledgerMapper.updateById(ledger);
    }

    private void validateLedgerStatus(FundingLedgerEntity ledger) {
        String status = ledger.getStatus();
        if (status == null) {
            return;
        }
        BigDecimal received = nz(ledger.getReceivedAmount());
        BigDecimal outstanding = nz(ledger.getOutstandingAmount());
        boolean overdue = outstanding.compareTo(BigDecimal.ZERO) > 0 && hasOverduePlan(ledger.getStoreId(), ledger.getId());
        switch (status) {
            case LEDGER_SETTLED -> {
                if (outstanding.compareTo(BigDecimal.ZERO) != 0) {
                    throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "仍有未收金额的台账不能标记为已结清");
                }
            }
            case LEDGER_PARTIAL_PAID -> {
                if (received.compareTo(BigDecimal.ZERO) <= 0 || outstanding.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "部分收款状态必须同时存在已收金额和未收金额");
                }
            }
            case LEDGER_NORMAL -> {
                if (received.compareTo(BigDecimal.ZERO) > 0 || overdue) {
                    throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "有收款或逾期计划的台账不能标记为正常");
                }
            }
            case LEDGER_OVERDUE -> {
                if (!overdue) {
                    throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "没有逾期未收计划的台账不能标记为逾期");
                }
            }
            case "ABNORMAL", LEDGER_VOIDED -> {
                // Manual exception states are allowed for operational handling.
            }
            default -> throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "不支持的台账状态");
        }
    }

    private boolean hasOverduePlan(Long storeId, Long ledgerId) {
        return planMapper.selectCount(this.<FundingInstallmentPlanEntity>baseStoreWrapper(storeId)
                .eq("ledger_id", ledgerId)
                .lt("due_date", LocalDate.now())
                .ne("status", "PAID")) > 0;
    }

    private String planStatus(FundingInstallmentPlanEntity plan) {
        BigDecimal received = nz(plan.getReceivedAmount());
        BigDecimal receivable = nz(plan.getReceivableAmount());
        if (received.compareTo(receivable) >= 0) {
            return "PAID";
        }
        if (received.compareTo(BigDecimal.ZERO) > 0) {
            return "PARTIAL_PAID";
        }
        if (plan.getDueDate() != null && plan.getDueDate().isBefore(LocalDate.now())) {
            return "OVERDUE";
        }
        return "PENDING";
    }

    private void applyApplicationFields(FundingApplicationEntity entity, SaveApplicationRequest request) {
        entity.setCustomerName(trimRequired(request.customerName(), "客户姓名不能为空"));
        entity.setPhone(trimRequired(request.phone(), "手机号不能为空"));
        entity.setIdCardNo(trimRequired(request.idCardNo(), "身份证号不能为空"));
        entity.setVehicleModel(trimRequired(request.vehicleModel(), "车型不能为空"));
        entity.setPickupDate(Objects.requireNonNull(request.pickupDate(), "提车日期不能为空"));
        entity.setPaymentType(trimRequired(request.paymentType(), "付款方式不能为空"));
        entity.setPurchaseCost(optionalNonNegative(request.purchaseCost(), "进货成本不能为负数"));
        entity.setIncentiveAmount(optionalNonNegative(request.incentiveAmount(), "激励不能为负数"));
        entity.setUpstreamAmount(optionalNonNegative(request.upstreamAmount(), "上级费用不能为负数"));
        entity.setTotalCost(request.totalCost() != null
                ? optionalNonNegative(request.totalCost(), "总计成本不能为负数")
                : entity.getPurchaseCost().add(entity.getIncentiveAmount()).add(entity.getUpstreamAmount()).setScale(2, RoundingMode.HALF_UP));
        entity.setRetailPrice(optionalNonNegative(request.retailPrice(), "零售价格不能为负数"));
        entity.setReceivableAmount(nonNegative(request.receivableAmount(), "应收总计不能为空"));
        entity.setDownPayment(optionalNonNegative(request.downPayment(), "首付不能为负数"));
        entity.setInstallmentCount(request.installmentCount() != null ? Math.max(request.installmentCount(), 0) : 0);
        entity.setInstallmentAmount(optionalNonNegative(request.installmentAmount(), "每期金额不能为负数"));
        entity.setFirstDueDate(request.firstDueDate());
        entity.setGroupLeader(trimRequired(request.groupLeader(), "组长不能为空"));
        entity.setHandlerName(request.handlerName());
        entity.setAddOnRemark(request.addOnRemark());
        entity.setRemark(request.remark());
    }

    private FundingDetailResponse detail(FundingApplicationEntity app, FundingContractEntity contract, FundingLedgerEntity ledger) {
        Long storeId = app.getStoreId();
        List<AttachmentResponse> attachments = attachmentMapper.selectList(this.<FundingAttachmentEntity>baseStoreWrapper(storeId)
                        .and(q -> q.eq("owner_type", "APPLICATION").eq("owner_id", app.getId())
                                .or(w -> {
                                    if (contract != null) {
                                        w.eq("owner_type", "CONTRACT").eq("owner_id", contract.getId());
                                    } else {
                                        w.eq("owner_type", "__NONE__");
                                    }
                                })
                                .or(w -> {
                                    if (ledger != null) {
                                        w.eq("owner_type", "PAYMENT").eq("owner_id", ledger.getId());
                                    } else {
                                        w.eq("owner_type", "__NONE__");
                                    }
                                })))
                .stream().map(this::toAttachmentResponse).toList();
        List<InstallmentPlanResponse> plans = ledger == null ? List.of() : planMapper.selectList(this.<FundingInstallmentPlanEntity>baseStoreWrapper(storeId)
                        .eq("ledger_id", ledger.getId())
                        .orderByAsc("phase_no"))
                .stream().map(this::toPlanResponse).toList();
        List<FundingPaymentResponse> payments = ledger == null ? List.of() : paymentMapper.selectList(this.<FundingPaymentRecordEntity>baseStoreWrapper(storeId)
                        .eq("ledger_id", ledger.getId())
                        .orderByDesc("paid_at"))
                .stream().map(this::toPaymentResponse).toList();
        List<ChangeLogResponse> logs = ledger == null ? List.of() : changeLogMapper.selectList(new QueryWrapper<FundingLedgerChangeLogEntity>()
                        .eq("store_id", storeId)
                        .eq("ledger_id", ledger.getId())
                        .orderByDesc("operated_at"))
                .stream().map(this::toChangeLogResponse).toList();
        return new FundingDetailResponse(toApplicationResponse(app),
                contract == null ? null : toContractResponse(contract),
                ledger == null ? null : toLedgerResponse(ledger),
                plans,
                payments,
                attachments,
                logs);
    }

    private FundingApplicationEntity loadApplication(Long storeId, Long id) {
        FundingApplicationEntity entity = applicationMapper.selectOne(this.<FundingApplicationEntity>baseStoreWrapper(storeId).eq("id", id));
        if (entity == null) {
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND, "资方资料不存在");
        }
        return entity;
    }

    private FundingContractEntity loadContract(Long storeId, Long id) {
        FundingContractEntity entity = contractMapper.selectOne(this.<FundingContractEntity>baseStoreWrapper(storeId).eq("id", id));
        if (entity == null) {
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND, "合同不存在");
        }
        return entity;
    }

    private FundingLedgerEntity loadLedger(Long storeId, Long id) {
        FundingLedgerEntity entity = ledgerMapper.selectOne(this.<FundingLedgerEntity>baseStoreWrapper(storeId).eq("id", id));
        if (entity == null) {
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND, "台账不存在");
        }
        return entity;
    }

    private FundingInstallmentPlanEntity loadPlan(Long storeId, Long ledgerId, Long planId) {
        FundingInstallmentPlanEntity entity = planMapper.selectOne(this.<FundingInstallmentPlanEntity>baseStoreWrapper(storeId)
                .eq("ledger_id", ledgerId)
                .eq("id", planId));
        if (entity == null) {
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND, "分期计划不存在");
        }
        return entity;
    }

    private FundingContractEntity findContractByApplication(Long storeId, Long applicationId) {
        return contractMapper.selectOne(this.<FundingContractEntity>baseStoreWrapper(storeId).eq("application_id", applicationId).last("LIMIT 1"));
    }

    private FundingLedgerEntity findLedgerByContract(Long storeId, Long contractId) {
        return ledgerMapper.selectOne(this.<FundingLedgerEntity>baseStoreWrapper(storeId).eq("contract_id", contractId).last("LIMIT 1"));
    }

    private FundingAttachmentEntity loadAttachmentEntity(Long storeId, Long attachmentId) {
        FundingAttachmentEntity attachment = attachmentMapper.selectOne(this.<FundingAttachmentEntity>baseStoreWrapper(storeId).eq("id", attachmentId));
        if (attachment == null) {
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND, "附件不存在");
        }
        return attachment;
    }

    private void validateOwner(Long storeId, String ownerType, Long ownerId) {
        if (!StringUtils.hasText(ownerType) || ownerId == null) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "附件归属不能为空");
        }
        switch (ownerType.trim()) {
            case "APPLICATION" -> loadApplication(storeId, ownerId);
            case "CONTRACT" -> loadContract(storeId, ownerId);
            case "PAYMENT" -> loadLedger(storeId, ownerId);
            default -> throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "不支持的附件归属类型");
        }
    }

    private <E, T> PageResponse<T> page(QueryWrapper<E> wrapper, int pageNo, int pageSize, String orderColumn,
                                        com.baomidou.mybatisplus.core.mapper.BaseMapper<E> mapper,
                                        java.util.function.Function<E, T> converter) {
        int safePageNo = Math.max(pageNo, 1);
        int safePageSize = Math.min(Math.max(pageSize, 1), 100);
        long total = mapper.selectCount(wrapper);
        if (total == 0) {
            return new PageResponse<>(List.of(), safePageNo, safePageSize, 0);
        }
        wrapper.orderByDesc(orderColumn).orderByDesc("id")
                .last("LIMIT " + safePageSize + " OFFSET " + (long) (safePageNo - 1) * safePageSize);
        List<T> records = mapper.selectList(wrapper).stream().map(converter).toList();
        return new PageResponse<>(records, safePageNo, safePageSize, total);
    }

    private void applyKeyword(QueryWrapper<FundingApplicationEntity> wrapper, String keyword) {
        String normalized = normalize(keyword);
        if (normalized == null) {
            return;
        }
        String pattern = buildContainsPattern(normalized);
        wrapper.and(q -> q.apply(containsCondition("application_no"), pattern)
                .or().apply(containsCondition("customer_name"), pattern)
                .or().apply(containsCondition("phone"), pattern)
                .or().apply(containsCondition("vehicle_model"), pattern)
                .or().apply(containsCondition("group_leader"), pattern));
    }

    private <T> QueryWrapper<T> baseStoreWrapper(Long storeId) {
        if (storeId == null) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "storeId不能为空");
        }
        return new QueryWrapper<T>().eq("store_id", storeId).eq("deleted", 0);
    }

    private void validateUserContext(Long storeId, Long operatorId) {
        if (storeId == null || operatorId == null) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST);
        }
    }

    private String trimRequired(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, message);
        }
        return value.trim();
    }

    private BigDecimal nz(BigDecimal value) {
        return (value != null ? value : BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal nonNegative(BigDecimal value, String message) {
        if (value == null) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, message);
        }
        BigDecimal result = nz(value);
        if (result.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "金额不能为负数");
        }
        return result;
    }

    private BigDecimal optionalNonNegative(BigDecimal value, String message) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal result = nz(value);
        if (result.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, message);
        }
        return result;
    }

    private BigDecimal normalizePositiveAmount(BigDecimal value, String message) {
        BigDecimal result = nonNegative(value, message);
        if (result.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, message);
        }
        return result;
    }

    private void writeHeader(Row row, List<String> headers) {
        for (int i = 0; i < headers.size(); i++) {
            writeText(row, i, headers.get(i));
        }
    }

    private void writeText(Row row, int column, String value) {
        row.createCell(column).setCellValue(value == null ? "" : value);
    }

    private void writeMoney(Row row, int column, BigDecimal value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value == null ? 0D : value.doubleValue());
        cell.setCellStyle(style);
    }

    private CellStyle moneyStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        DataFormat dataFormat = workbook.createDataFormat();
        style.setDataFormat(dataFormat.getFormat("0.00"));
        return style;
    }

    private void autosize(Sheet sheet, int columnCount) {
        for (int i = 0; i < columnCount; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private byte[] toBytes(Workbook workbook) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        return outputStream.toByteArray();
    }

    private String formatDate(LocalDate value) {
        return value == null ? "" : DateTimeFormatter.ISO_LOCAL_DATE.format(value);
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? "" : DATE_TIME_FORMATTER.format(value);
    }

    private String paymentTypeText(String value) {
        return "FULL".equals(value) ? "全款" : "INSTALLMENT".equals(value) ? "分期" : value;
    }

    private String contractStatusText(String value) {
        return switch (value == null ? "" : value) {
            case CONTRACT_UPLOADED -> "已上传";
            case CONTRACT_CONFIRMED -> "已确认";
            case CONTRACT_VOIDED -> "已作废";
            default -> value;
        };
    }

    private String ledgerStatusText(String value) {
        return switch (value == null ? "" : value) {
            case LEDGER_NORMAL -> "正常";
            case LEDGER_PARTIAL_PAID -> "部分收款";
            case LEDGER_SETTLED -> "已结清";
            case LEDGER_OVERDUE -> "逾期";
            case LEDGER_VOIDED -> "作废";
            default -> value;
        };
    }

    private void writeLog(FundingLedgerEntity ledger, String field, String oldValue, String newValue, Long operatorId, String remark) {
        FundingLedgerChangeLogEntity log = new FundingLedgerChangeLogEntity();
        log.setStoreId(ledger.getStoreId());
        log.setLedgerId(ledger.getId());
        log.setFieldName(field);
        log.setOldValue(oldValue);
        log.setNewValue(newValue);
        log.setOperatorId(operatorId);
        log.setOperatedAt(LocalDateTime.now());
        log.setRemark(remark);
        log.setCreatedBy(operatorId);
        changeLogMapper.insert(log);
    }

    private void change(FundingLedgerEntity ledger, String field, String oldValue, String newValue,
                        java.util.function.Consumer<String> setter, Long operatorId, String remark) {
        if (newValue != null && !Objects.equals(oldValue, newValue)) {
            writeLog(ledger, field, oldValue, newValue, operatorId, remark);
            setter.accept(newValue);
        }
    }

    private void changeAmount(FundingLedgerEntity ledger, String field, BigDecimal oldValue, BigDecimal newValue,
                              java.util.function.Consumer<BigDecimal> setter, Long operatorId, String remark) {
        if (newValue != null) {
            BigDecimal normalized = nz(newValue);
            if (normalized.compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "金额不能为负数");
            }
            if (nz(oldValue).compareTo(normalized) != 0) {
                writeLog(ledger, field, String.valueOf(nz(oldValue)), String.valueOf(normalized), operatorId, remark);
                setter.accept(normalized);
            }
        }
    }

    private ApplicationResponse toApplicationResponse(FundingApplicationEntity e) {
        return new ApplicationResponse(e.getId(), e.getStoreId(), e.getApplicationNo(), e.getCustomerName(), e.getPhone(),
                e.getIdCardNo(), e.getVehicleModel(), e.getPickupDate(), e.getPaymentType(), e.getPurchaseCost(),
                e.getIncentiveAmount(), e.getUpstreamAmount(), e.getTotalCost(), e.getRetailPrice(), e.getReceivableAmount(),
                e.getDownPayment(), e.getInstallmentCount(), e.getInstallmentAmount(), e.getFirstDueDate(), e.getGroupLeader(),
                e.getHandlerName(), e.getAddOnRemark(), e.getStatus(), e.getAuditRemark(), e.getAuditedBy(), e.getAuditedAt(),
                e.getRemark(), e.getCreatedAt(), e.getUpdatedAt());
    }

    private ContractResponse toContractResponse(FundingContractEntity e) {
        return new ContractResponse(e.getId(), e.getApplicationId(), e.getContractNo(), e.getContractType(), e.getSignedDate(),
                e.getStatus(), e.getConfirmedBy(), e.getConfirmedAt(), e.getVoidReason(), e.getRemark(), e.getCreatedAt(), e.getUpdatedAt());
    }

    private LedgerResponse toLedgerResponse(FundingLedgerEntity e) {
        return new LedgerResponse(e.getId(), e.getApplicationId(), e.getContractId(), e.getLedgerNo(), e.getCustomerName(), e.getPhone(),
                e.getIdCardNo(), e.getVehicleModel(), e.getPickupDate(), e.getPaymentType(), e.getPurchaseCost(), e.getIncentiveAmount(),
                e.getUpstreamAmount(), e.getTotalCost(), e.getRetailPrice(), e.getReceivableAmount(), e.getReceivedAmount(),
                e.getOutstandingAmount(), e.getGroupLeader(), e.getHandlerName(), e.getStatus(), e.getRemark(), e.getCreatedAt(), e.getUpdatedAt());
    }

    private InstallmentPlanResponse toPlanResponse(FundingInstallmentPlanEntity e) {
        return new InstallmentPlanResponse(e.getId(), e.getLedgerId(), e.getPhaseNo(), e.getPhaseName(), e.getDueDate(),
                e.getReceivableAmount(), e.getReceivedAmount(), e.getStatus(), e.getRemark());
    }

    private FundingPaymentResponse toPaymentResponse(FundingPaymentRecordEntity e) {
        return new FundingPaymentResponse(e.getId(), e.getLedgerId(), e.getInstallmentPlanId(), e.getPaymentNo(),
                e.getAmount(), e.getPaymentMethod(), e.getPaidAt(), e.getOperatorId(), e.getRemark(), e.getCreatedAt());
    }

    private AttachmentResponse toAttachmentResponse(FundingAttachmentEntity e) {
        return new AttachmentResponse(e.getId(), e.getOwnerType(), e.getOwnerId(), e.getAttachmentType(), e.getOriginalFilename(),
                e.getContentType(), e.getFileSize(), e.getRemark(), e.getCreatedAt());
    }

    private ChangeLogResponse toChangeLogResponse(FundingLedgerChangeLogEntity e) {
        return new ChangeLogResponse(e.getId(), e.getLedgerId(), e.getFieldName(), e.getOldValue(), e.getNewValue(),
                e.getOperatorId(), e.getOperatedAt(), e.getRemark());
    }

    private record ImportLedgerRow(
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
            int installmentCount,
            BigDecimal installmentAmount,
            LocalDate firstDueDate,
            String groupLeader,
            String handlerName,
            String addOnRemark,
            String remark,
            BigDecimal receivedAmount
    ) {
    }
}
