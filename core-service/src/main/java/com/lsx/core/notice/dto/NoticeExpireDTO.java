package com.lsx.core.notice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
@Data
@Schema(description = "公告过期时间设置参数")
public class NoticeExpireDTO {

    @Schema(description = "公告ID", required = true)
    @NotNull(message = "公告ID不能为空")
    private Long noticeId;

    @Schema(description = "过期时间类型: NEVER(永不过期), CUSTOM(自定义), DAYS_7(7天), DAYS_30(30天), MONTH_3(3个月)", required = true)
    @NotNull(message = "过期时间类型不能为空")
    private ExpireType expireType;

    @Schema(description = "自定义过期时间（当expireType=CUSTOM时必填）")
    private LocalDateTime customExpireTime;

    @Schema(description = "天数（当expireType=DAYS_*时可选）")
    private Integer days;

    public enum ExpireType {
        NEVER,      // 永不过期
        CUSTOM,     // 自定义时间
        DAYS_7,     // 7天后
        DAYS_30,    // 30天后
        MONTH_3     // 3个月后
    }
}