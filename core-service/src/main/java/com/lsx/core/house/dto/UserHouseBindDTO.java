package com.lsx.core.house.dto;

import lombok.Data;

@Data // Lombok 注解，自动生成 getter/setter，简化代码
public class UserHouseBindDTO {
    private Long userId;   // 对应前端传递的 userId
    private Long houseId;  // 对应前端传递的 houseId
}