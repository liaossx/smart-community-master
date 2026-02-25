package com.lsx.core.repair.dto;


import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RepairDto {
    private Long id;
    private Long userId;         // 关联业主ID（sys_user.id）
    private String buildingNo;   // 楼栋号
    private String houseNo;      // 房屋编号
    private String faultType;    // 故障类型（水管、电路等）
    private String faultDesc;    // 故障描述
    private String faultImgs;    // 故障图片URL（逗号分隔）
    private String status;       // 状态：pending（待处理）、processing（处理中）、completed（已完成）
    private String handleRemark; // 处理备注
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
