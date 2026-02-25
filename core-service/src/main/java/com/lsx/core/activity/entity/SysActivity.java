package com.lsx.core.activity.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_activity")
public class SysActivity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long communityId;
    private String title;
    private String content;
    private LocalDateTime startTime;
    private String location;
    private Integer maxCount;
    private Integer signupCount;
    private String status;
    private String coverUrl;
    private LocalDateTime createTime;
}
