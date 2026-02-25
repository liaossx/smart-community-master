package com.lsx.core.repair.vo;

import lombok.Data;

/**
 * 报修统计结果
 */
@Data
public class RepairStatsResult {
    private Integer total;      // 总报修数
    private Integer pending;    // 待处理数
    private Integer processing; // 处理中数
    private Integer completed;  // 已完成数
    private Integer cancelled;  // 已取消数
}
