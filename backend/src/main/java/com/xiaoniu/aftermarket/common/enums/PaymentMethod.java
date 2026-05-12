package com.xiaoniu.aftermarket.common.enums;

import lombok.Getter;

@Getter
public enum PaymentMethod {
    WECHAT("WECHAT"),
    ALIPAY("ALIPAY"),
    UNIONPAY("UNIONPAY"),
    CASH("CASH"),
    OTHER("OTHER");

    private final String code;

    PaymentMethod(String code) {
        this.code = code;
    }
}
