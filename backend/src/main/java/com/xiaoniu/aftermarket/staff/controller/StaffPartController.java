package com.xiaoniu.aftermarket.staff.controller;

import com.xiaoniu.aftermarket.common.api.ApiResponse;
import com.xiaoniu.aftermarket.common.context.CurrentUser;
import com.xiaoniu.aftermarket.common.context.CurrentUserContext;
import com.xiaoniu.aftermarket.common.exception.BusinessException;
import com.xiaoniu.aftermarket.common.api.ErrorCode;
import com.xiaoniu.aftermarket.common.enums.CommonStatus;
import com.xiaoniu.aftermarket.common.pagination.PageResponse;
import com.xiaoniu.aftermarket.part.dto.CreatePartCommand;
import com.xiaoniu.aftermarket.part.dto.PartCreateResponse;
import com.xiaoniu.aftermarket.part.dto.PartLookupResponse;
import com.xiaoniu.aftermarket.part.dto.PartQueryRequest;
import com.xiaoniu.aftermarket.part.dto.PartQueryResponse;
import com.xiaoniu.aftermarket.part.entity.PartEntity;
import com.xiaoniu.aftermarket.part.service.PartService;
import com.xiaoniu.aftermarket.staff.dto.StaffPartCreateRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/staff/parts")
public class StaffPartController {

    private static final String PART_READ_AUTHORITIES = "hasAnyAuthority("
            + "'PART_VIEW', 'PART_MANAGE', 'PART_CREATE', "
            + "'INVENTORY_VIEW', 'INVENTORY_INBOUND', "
            + "'WORK_ORDER_CREATE', 'WORK_ORDER_UPDATE')";

    private final PartService partService;

    public StaffPartController(PartService partService) {
        this.partService = partService;
    }

    @PreAuthorize(PART_READ_AUTHORITIES)
    @GetMapping
    public ApiResponse<PageResponse<StaffPartListItem>> listParts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String partCode,
            @RequestParam(required = false) String partName,
            @RequestParam(required = false) String officialPartNo,
            @RequestParam(required = false) String model,
            @RequestParam(required = false) String categoryCode,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) Integer pageNo,
            @RequestParam(required = false) Integer pageSize) {

        CurrentUser user = requireCurrentUser();
        PartQueryRequest request = new PartQueryRequest();
        request.setStoreId(user.storeId());
        request.setKeyword(keyword);
        request.setPartCode(partCode);
        request.setPartName(partName);
        request.setOfficialPartNo(officialPartNo);
        request.setModel(model);
        request.setCategoryCode(categoryCode);
        request.setSource(source);
        request.setStatus("ENABLED");
        request.setPageNo(pageNo);
        request.setPageSize(pageSize);
        PageResponse<PartQueryResponse> result = partService.pageQuery(request);
        return ApiResponse.success(result.map(StaffPartListItem::from));
    }

    @PreAuthorize(PART_READ_AUTHORITIES)
    @GetMapping("/lookup")
    public ApiResponse<PartLookupResponse> lookup(@RequestParam(required = false) String code,
                                                  @RequestParam(required = false) String barcode) {
        CurrentUser user = requireCurrentUser();
        return ApiResponse.success(partService.lookup(user.storeId(), resolveLookupCode(code, barcode)));
    }

    @PreAuthorize(PART_READ_AUTHORITIES)
    @GetMapping("/{partId}")
    public ApiResponse<StaffPartDetail> getPart(@PathVariable Long partId) {
        CurrentUser user = requireCurrentUser();
        PartEntity entity = partService.getById(partId);
        if (entity == null || !user.storeId().equals(entity.getStoreId())
                || !CommonStatus.ENABLED.getCode().equals(entity.getStatus())) {
            throw new BusinessException(ErrorCode.PART_NOT_FOUND, "配件不存在");
        }
        return ApiResponse.success(StaffPartDetail.from(entity));
    }

    @PreAuthorize("hasAnyAuthority('PART_MANAGE', 'PART_CREATE')")
    @PostMapping("/official")
    public ApiResponse<PartCreateResponse> createOfficialPart(
            @Valid @RequestBody StaffPartCreateRequest request) {
        validateSourceConsistency(request.source(), "OFFICIAL");
        CurrentUser user = requireCurrentUser();
        CreatePartCommand command = buildCreateCommand(user, request);
        command.setOfficialPartNo(request.officialPartNo());
        PartEntity part = partService.createOfficialPart(command);
        return ApiResponse.success(PartCreateResponse.from(part));
    }

    @PreAuthorize("hasAnyAuthority('PART_MANAGE', 'PART_CREATE')")
    @PostMapping("/third-party")
    public ApiResponse<PartCreateResponse> createThirdPartyPart(
            @Valid @RequestBody StaffPartCreateRequest request) {
        validateSourceConsistency(request.source(), "THIRD_PARTY");
        CurrentUser user = requireCurrentUser();
        CreatePartCommand command = buildCreateCommand(user, request);
        command.setOfficialPartNo(request.officialPartNo());
        PartEntity part = partService.createThirdPartyPart(command);
        return ApiResponse.success(PartCreateResponse.from(part));
    }

    /**
     * source 可选；如果传了，必须与 URL 语义一致，否则返回 400。
     */
    private void validateSourceConsistency(String source, String expected) {
        if (source != null && !source.isBlank()
                && !source.trim().equalsIgnoreCase(expected)) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST,
                    "source 与接口语义不一致，应为 " + expected);
        }
    }

    private String resolveLookupCode(String code, String barcode) {
        if (code != null) {
            return code;
        }
        if (barcode != null) {
            return barcode;
        }
        throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "请提供扫码编码");
    }

    private CreatePartCommand buildCreateCommand(CurrentUser user, StaffPartCreateRequest request) {
        CreatePartCommand command = new CreatePartCommand();
        command.setStoreId(user.storeId());
        command.setOperatorId(user.userId());
        command.setPartName(request.partName());
        command.setOfficialPartNo(request.officialPartNo());
        command.setModel(request.model());
        command.setCategoryCode(request.categoryCode());
        command.setReferenceCostPrice(request.costPrice());
        command.setDefaultSalePrice(request.salePrice());
        command.setExternalBarcode(request.externalBarcode());
        command.setLocationRemark(request.locationRemark());
        command.setRemark(request.remark());
        return command;
    }

    private CurrentUser requireCurrentUser() {
        return CurrentUserContext.get()
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "缺少用户上下文"));
    }
}
