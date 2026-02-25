package com.lsx.core.user.vo;

import lombok.Data;


import java.time.LocalDateTime;
@Data
public class RegisterResult {
    private Long id;
    private String username;  // 登录用户名
    private String realName;  // 真实姓名
    private String phone;     // 手机号
    private String role;      // 角色：owner（业主）、admin（管理员）
    private LocalDateTime createTime;  // 创建时间
}
