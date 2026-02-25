package com.lsx.core.group.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class GroupVO {
    private Long id;
    private Long sponsorId;
    private String subject;
    private String description;
    private Integer targetCount;
    private Integer joinedCount;
    private LocalDateTime deadline;
    private String status;
    private String remark;
    private LocalDateTime finishTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private List<GroupMemberVO> members;  // 成员列表
}

