package com.lsx.core.parking.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 车位信息
 */
@Data
@TableName("biz_parking_space")
public class ParkingSpace {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long communityId;
    private String communityName;

    /** 车位编号 */
    private String spaceNo;

    /** TEMP / FIXED */
    private String spaceType;

    /**
     * AVAILABLE / DISABLED
     * ⚠️ 不再表示“是否被占用”
     */
    private String status;

    /** 逻辑删除 */
    private Boolean deleted;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}








