package com.lsx.core.user.dto;


import lombok.Data;

@Data
public class UserInfoDTO {
    private Long userId;          // 用户ID（对应sys_user.id）
    private String username;      // 用户名
    private String name;          // 真实姓名（对应realName）
    private String community;     // 社区名（来自sys_house）
    private String room;          // 房屋号（如"1栋101"，来自sys_house）
    private String role;          // 用户角色
}