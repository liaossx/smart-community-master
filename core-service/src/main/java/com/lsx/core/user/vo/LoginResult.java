package com.lsx.core.user.vo;

import com.lsx.core.user.entity.User;
import lombok.Data;

@Data
public class LoginResult {
    private Long userId;
    private String username;
    private String token;
    private String role; // 用户角色
    private Long communityId; // 社区ID
    private String tokenType = "Bearer"; // Token类型

    public LoginResult(User user, String token) {
        this.userId = user.getId();
        this.username = user.getUsername();
        this.token = token;
        this.role = user.getRole(); // 假设User实体有getRole方法
        this.communityId = user.getCommunityId();
    }
}