package com.lsx.core.group.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("biz_group_member")
public class GroupMember {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long groupId;
    private Long userId;
    private String role;          // SPONSOR / MEMBER
    private String status;        // JOINED / COMPLETED / FAILED / CANCELLED
    private LocalDateTime joinTime;
    private LocalDateTime updateTime;
}

