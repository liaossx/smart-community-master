package com.lsx.core.topic.dto;

import lombok.Data;

import java.util.List;

@Data
public class TopicCreateDTO {
    private Long userId;
    private String title;
    private String content;
    private List<String> imageUrls;
}

