package com.lsx.core.house.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_house")
public class UserHouse {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long houseId;
    private String status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
