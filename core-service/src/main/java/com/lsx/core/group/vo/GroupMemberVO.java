package com.lsx.core.group.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class GroupMemberVO {
    private Long userId;
    private String role;      // SPONSOR / MEMBER
    private String status;    // JOINED / COMPLETED / FAILED / CANCELLED
    private LocalDateTime joinTime;
}

