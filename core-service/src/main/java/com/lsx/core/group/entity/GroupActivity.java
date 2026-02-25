package com.lsx.core.group.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("biz_group_activity")
public class GroupActivity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long sponsorId;
    private String subject;
    private String description;
    private Integer targetCount;
    private Integer joinedCount;
    private LocalDateTime deadline;
    private String status;        // ONGOING / SUCCESS / FAILED / CANCELLED
    private String remark;
    private LocalDateTime finishTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

