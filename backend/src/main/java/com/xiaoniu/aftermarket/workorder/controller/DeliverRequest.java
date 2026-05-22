package com.xiaoniu.aftermarket.workorder.controller;

public record DeliverRequest(
        String noChargeReason,
        String noChargeRemark,
        String remark
) {}
