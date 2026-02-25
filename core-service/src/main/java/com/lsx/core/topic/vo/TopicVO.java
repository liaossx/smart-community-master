package com.lsx.core.topic.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class TopicVO {
    private Long id;
    private Long userId;
    private String title;
    private String content;
    private List<String> imageUrls;
    private String status;
    private Integer likeCount;
    private Integer commentCount;
    private Long auditBy;
    private String auditRemark;
    private LocalDateTime auditTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private List<TopicCommentVO> latestComments;  // 最新3条评论
}

