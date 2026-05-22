package com.xiaoniu.aftermarket.workorder.controller;

public record MarkRepairDoneRequest(
        String noChargeReason,
        String noChargeRemark,
        String remark
) {}
