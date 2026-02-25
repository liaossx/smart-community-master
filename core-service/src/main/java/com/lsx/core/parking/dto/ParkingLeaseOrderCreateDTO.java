package com.lsx.core.parking.dto;

import lombok.Data;

@Data
public class ParkingLeaseOrderCreateDTO {

    private Long userId;
    private Long spaceId;

    /**
     * MONTHLY / YEARLY / PERPETUAL
     */
    private String leaseType;
}