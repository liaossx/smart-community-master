package com.lsx.core.parking.dto;

import lombok.Data;

@Data
public class ParkingLeaseOrderPayDTO {
    private Long orderId;
    private String payChannel; // WECHAT / ALIPAY
}