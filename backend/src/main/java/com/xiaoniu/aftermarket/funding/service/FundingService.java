package com.xiaoniu.aftermarket.funding.service;

import com.xiaoniu.aftermarket.common.pagination.PageResponse;
import com.xiaoniu.aftermarket.funding.dto.FundingDtos.ApplicationResponse;
import com.xiaoniu.aftermarket.funding.dto.FundingDtos.AttachmentResponse;
import com.xiaoniu.aftermarket.funding.dto.FundingDtos.ContractResponse;
import com.xiaoniu.aftermarket.funding.dto.FundingDtos.FundingDetailResponse;
import com.xiaoniu.aftermarket.funding.dto.FundingDtos.FundingImportResultResponse;
import com.xiaoniu.aftermarket.funding.dto.FundingDtos.FundingPaymentResponse;
import com.xiaoniu.aftermarket.funding.dto.FundingDtos.FundingSummaryResponse;
import com.xiaoniu.aftermarket.funding.dto.FundingDtos.LedgerResponse;
import com.xiaoniu.aftermarket.funding.dto.FundingDtos.RecordFundingPaymentRequest;
import com.xiaoniu.aftermarket.funding.dto.FundingDtos.SaveApplicationRequest;
import com.xiaoniu.aftermarket.funding.dto.FundingDtos.SaveContractRequest;
import com.xiaoniu.aftermarket.funding.dto.FundingDtos.UpdateLedgerRequest;
import com.xiaoniu.aftermarket.export.dto.ExportFile;
import java.io.IOException;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface FundingService {

    PageResponse<ApplicationResponse> listApplications(Long storeId, String keyword, String status, int pageNo, int pageSize);

    ApplicationResponse createApplication(Long storeId, Long operatorId, SaveApplicationRequest request);

    ApplicationResponse updateApplication(Long storeId, Long operatorId, Long id, SaveApplicationRequest request);

    ApplicationResponse submitApplication(Long storeId, Long operatorId, Long id);

    ApplicationResponse approveApplication(Long storeId, Long operatorId, Long id);

    ApplicationResponse rejectApplication(Long storeId, Long operatorId, Long id, String reason);

    FundingDetailResponse getApplicationDetail(Long storeId, Long id);

    ContractResponse saveContract(Long storeId, Long operatorId, Long applicationId, SaveContractRequest request);

    LedgerResponse confirmContract(Long storeId, Long operatorId, Long contractId);

    ContractResponse voidContract(Long storeId, Long operatorId, Long contractId, String reason);

    PageResponse<LedgerResponse> listLedgers(Long storeId, String keyword, String status, int pageNo, int pageSize);

    FundingDetailResponse getLedgerDetail(Long storeId, Long ledgerId);

    LedgerResponse updateLedger(Long storeId, Long operatorId, Long ledgerId, UpdateLedgerRequest request);

    FundingPaymentResponse recordPayment(Long storeId, Long operatorId, Long ledgerId, RecordFundingPaymentRequest request);

    FundingSummaryResponse summary(Long storeId);

    FundingImportResultResponse importLedgers(Long storeId, Long operatorId, MultipartFile file) throws IOException;

    ExportFile exportLedgers(Long storeId, String keyword, String status);

    AttachmentResponse uploadAttachment(Long storeId, Long operatorId, String ownerType, Long ownerId, String attachmentType,
                                        MultipartFile file, String remark) throws IOException;

    Resource loadAttachment(Long storeId, Long attachmentId);

    String attachmentFilename(Long storeId, Long attachmentId);
}
