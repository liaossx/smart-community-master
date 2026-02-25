package com.lsx.core.property.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CurrentFeeDTO {
    private Long feeId;              // 账单ID
    private String communityName;    // 小区名称
    private String buildingNo;       // 楼栋号
    private String houseNo;          // 房屋编号
    private BigDecimal area;         // 房屋面积
    private String feeCycle;         // 收费周期
    private BigDecimal feeAmount;    // 账单金额
    private LocalDateTime dueDate;   // 截止日期
}