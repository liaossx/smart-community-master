package com.lsx.core.topic.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class TopicCommentVO {

    private Long id;
    private Long topicId;

    private Long userId;
    private String username;   // 评论人昵称

    private String content;
    private LocalDateTime createTime;
    private Long parentId;
    private Long rootId;
    private List<TopicCommentVO> replies; // 楼中楼子评论列表
}