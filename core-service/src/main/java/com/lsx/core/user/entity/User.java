package com.lsx.core.user.entity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data  // Lombok注解：自动生成getter/setter
@TableName("sys_user")  // 关联数据库表名
public class User {
    @TableId(type = IdType.AUTO)  // 主键自增（对应表的id字段）
    private Long id;
    private String username;  // 登录用户名
    private String password;  // 登录密码（MVP阶段暂存明文）
    private String realName;  // 真实姓名
    private String phone;     // 手机号
    private String role;      // 角色：owner（业主）、admin（管理员）
    private Long communityId; // 归属社区ID
    private Integer status;   // 状态：0-禁用，1-正常
    private LocalDateTime createTime;  // 创建时间
    private LocalDateTime updateTime;  // 更新时间
}