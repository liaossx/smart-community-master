package com.lsx.core.parking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class ParkingCarAuditDTO {

    @Schema(description = "申请记录ID", required = true)
    private Long id;

    @Schema(description = "审核状态：APPROVED-通过，REJECTED-拒绝", required = true)
    private String status;

    @Schema(description = "拒绝原因（当状态为REJECTED时必填）")
    private String rejectReason;
}
