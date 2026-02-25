package com.lsx.core.community.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_community")
public class Community {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String address;
    @TableField(exist = false)
    private String contact;
    private String phone;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
