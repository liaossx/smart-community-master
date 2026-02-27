package com.lsx.core.notice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_notice")
public class SysNotice {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String content;
    private String targetType;     // ALL / COMMUNITY / BUILDING / USER
    private Long targetUserId;     // 指定用户ID
    private Long communityId;      // 指定小区
    private String communityName;
    private String buildingNo;     // 指定楼栋
    private String publishStatus;  // DRAFT / PUBLISHED/OFFLINE
    private Boolean topFlag;       // 是否置顶
    private LocalDateTime publishTime;
    private LocalDateTime expireTime;
    private Long creatorId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic  // 添加这个注解！
    private Integer deleted = 0;   // 0=未删除，1=已删除
}

