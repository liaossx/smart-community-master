package com.lsx.core.repair.dto;

import lombok.Data;

import java.util.List;

/**
 * 批量更新报修状态DTO
 */
@Data
public class BatchUpdateStatusDTO {
    private List<Long> repairIds;  // 报修ID列表
    private String status;         // 目标状态
    private String remark;         // 备注（可选）
}
