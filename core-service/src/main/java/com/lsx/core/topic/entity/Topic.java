package com.lsx.core.topic.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("biz_topic")
public class Topic {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String title;
    private String content;
    private String images;        // 逗号拼接的图片URL
    private String status;        // PENDING / APPROVED / REJECTED
    private Integer likeCount;
    private Integer commentCount;
    private Long auditBy;
    private String auditRemark;
    private LocalDateTime auditTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Boolean deleted;
}

