package com.lsx.core.property.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("sys_fee") // MyBatis-Plus 注解，JPA 用 @Entity
public class SysFee {
    @TableId(type = IdType.AUTO)
    private Long id;                 // 主键ID
    private Long houseId;            // 房屋ID
    private Long communityId;        // 小区ID（可为null）
    private String buildingNo;       // 楼栋号
    private String feeCycle;         // 收费周期（如2025-01）
    private BigDecimal feeAmount;    // 账单金额
    private String feeType = "物业费"; // 费用类型
    private String status = "UNPAID";// 状态（UNPAID/PAID/OVERDUE）

    private LocalDateTime dueDate;  // 彻底改为 String

    private LocalDateTime createTime;// 创建时间
    private LocalDateTime updateTime;// 更新时间
    private String remark;           // 备注
}