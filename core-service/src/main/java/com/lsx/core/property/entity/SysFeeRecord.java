package com.lsx.core.property.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("sys_fee_record")
public class SysFeeRecord {
    @TableId(type = IdType.AUTO)
    private Long id;                 // 主键ID
    private Long feeId;              // 关联账单ID
    private Long userId;             // 缴费人ID
    private Long houseId;            // 房屋ID
    private BigDecimal payAmount;    // 实际缴费金额
    private String payType;          // 支付方式（WECHAT/ALIPAY/CASH）
    private LocalDateTime payTime;   // 缴费时间
    private String orderNo;          // 支付订单号
    private String tradeNo;          // 支付交易号
    private String status = "FAIL";  // 缴费状态（SUCCESS/FAIL/REFUND）
    private String remark;           // 备注
}