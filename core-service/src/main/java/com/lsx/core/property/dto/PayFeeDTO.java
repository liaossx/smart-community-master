package com.lsx.core.property.dto;

import lombok.Data;

@Data
public class PayFeeDTO {
    private Long feeId;              // 账单ID
    private String payType;          // 支付方式
}