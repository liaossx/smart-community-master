package com.lsx.core.property.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class GenerateFeeDTO {
    private String communityName;    // 小区名称（为空则全小区）
    private String buildingNo;       // 楼栋号（为空则全楼栋）
    private String feeCycle;         // 收费周期（如2025-01）
    private String dueDate;   // 截止日期
    private BigDecimal unitPrice;    // 单价（元/㎡，管理员输入）
}