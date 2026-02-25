package com.lsx.core.group.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class GroupCreateDTO {
    private Long sponsorId;
    private String subject;
    private String description;
    private Integer targetCount;
    private LocalDateTime deadline;
}

