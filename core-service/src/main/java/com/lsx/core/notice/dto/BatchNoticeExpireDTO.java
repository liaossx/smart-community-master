package com.lsx.core.notice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "批量设置公告过期时间参数")
public class BatchNoticeExpireDTO {

    @Schema(description = "公告ID列表", required = true)
    @NotEmpty(message = "公告ID列表不能为空")
    private List<Long> noticeIds;

    @Schema(description = "过期时间类型", required = true)
    @NotNull(message = "过期时间类型不能为空")
    private NoticeExpireDTO.ExpireType expireType;

    @Schema(description = "自定义过期时间")
    private LocalDateTime customExpireTime;

    @Schema(description = "天数")
    private Integer days;
}