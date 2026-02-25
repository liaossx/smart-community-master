package com.lsx.core.parking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class VehicleBindDTO {

    @Schema(description = "用户ID", required = true)
    private Long userId;

    @Schema(description = "车牌号", required = true)
    private String plateNo;
}