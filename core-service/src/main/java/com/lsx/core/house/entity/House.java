package com.lsx.core.house.entity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("sys_house")
public class House {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String communityName;// 小区名称
    private String buildingNo;   // 楼栋号
    private String houseNo;      // 房屋编号
    private Integer isDefault;   // 是否默认房屋：1是、0否
    private Integer bindStatus;
    private BigDecimal area;
}