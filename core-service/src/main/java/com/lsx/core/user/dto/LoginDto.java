package com.lsx.core.user.dto;

import lombok.Data;

@Data
public class LoginDto {
    private String username;
    private String password;
    private String role;
}
