package com.lsx.core.topic.dto;

import lombok.Data;

@Data
public class TopicAuditDTO {
    private Long adminId;
    private String decision;  // APPROVED / REJECTED
    private String remark;
}

