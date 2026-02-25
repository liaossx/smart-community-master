package com.lsx.core.repair.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class RepairResult {
    private Long id; // 报修记录ID（业主可能需要根据ID咨询进度）

    // 房屋信息（关联展示，方便业主识别具体房屋）
    private String communityName; // 小区名称（来自 sys_house.community_name）
    private String buildingNo;    // 楼栋号（来自 sys_house.building_no）
    private String houseNo;       // 房屋编号（来自 sys_house.house_no）

    // 报修核心信息
    private String faultType;     // 故障类型（如“水管漏水”“电路短路”）
    private String faultDesc;     // 故障描述（业主填写的具体问题）
    private List<String> faultImgs; // 故障图片URL列表（拆分逗号分隔的字符串，方便前端展示）

    // 进度相关信息
    private String status;        // 报修状态（转中文显示更友好，如“待处理”“已完成”）
    private String statusDesc;    // 状态中文描述（可选，如 pending→“待物业处理”）
    private String handleRemark;  // 处理备注（物业填写的处理结果）

    // 时间信息
    private LocalDateTime createTime; // 报修提交时间
    private LocalDateTime updateTime; // 最近更新时间（如处理完成时间）
}