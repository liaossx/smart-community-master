package com.lsx.core.topic.dto;

import lombok.Data;
@Data
public class TopicCommentDTO {
    private Long userId;
    private String content;
    private Long parentId; // 👈 新增，楼中楼关键
    private Long rootId;
}