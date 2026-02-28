package com.lsx.core.parking.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("biz_vehicle")
public class Vehicle {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 车牌号（唯一） */
    private String plateNo;

    /** 车主用户ID */
    private Long userId;

    private String brand;  // 品牌
    private String color;  // 颜色

    /** ACTIVE / DISABLED */
    private String status;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}