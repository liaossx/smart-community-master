package com.lsx.core.parking.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("biz_parking_reserve")
public class ParkingReserve {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long spaceId;

    private LocalDateTime reserveStartTime;

    private LocalDateTime reserveEndTime;

    /**
     * RESERVED / EXPIRED / CANCELLED
     */
    private String status;

    /**
     * 取消原因（用户或管理员）
     */
    private String cancelReason;

    /**
     * 取消人类型 USER / ADMIN
     */
    private String cancelBy;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}

