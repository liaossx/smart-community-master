package com.lsx.core.notice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "即将过期公告DTO")
public class ExpiringNoticeDTO {

    @Schema(description = "公告ID")
    private Long id;

    @Schema(description = "公告标题")
    private String title;

    @Schema(description = "目标类型")
    private String targetType;

    @Schema(description = "发布时间")
    private LocalDateTime publishTime;

    @Schema(description = "过期时间")
    private LocalDateTime expireTime;

    @Schema(description = "剩余天数")
    private Long daysLeft;

    @Schema(description = "是否置顶")
    private Boolean topFlag;
}