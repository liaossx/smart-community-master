package com.lsx.core.property.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class FeeHistoryDTO {
    private Long recordId;           // 缴费记录ID
    private String feeCycle;         // 对应账单周期
    private String feeType;          // 费用类型
    private BigDecimal payAmount;    // 缴费金额
    private String payType;          // 支付方式
    private LocalDateTime payTime;   // 缴费时间
    private String status;           // 缴费状态
}