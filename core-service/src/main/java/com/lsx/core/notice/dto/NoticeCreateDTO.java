package com.lsx.core.notice.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NoticeCreateDTO {
    private String title;
    private String content;
    private String targetType;      // ALL / COMMUNITY / BUILDING
    private Long communityId;
    private String communityName;
    private String buildingNo;
    private String publishStatus;   // DRAFT / PUBLISHED
    private Boolean topFlag = false;
    private LocalDateTime expireTime;
}

