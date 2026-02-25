package com.lsx.core.parking.controller;

import com.lsx.core.common.Result.Result;
import com.lsx.core.parking.dto.ParkingPaySuccessDTO;
import com.lsx.core.parking.service.ParkingPaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/parking/pay")
@Tag(name = "停车-支付")
public class ParkingPaymentController {

    @Autowired
    private ParkingPaymentService parkingPaymentService;

    @PostMapping("/success")
    @Operation(summary = "支付成功回调（模拟）")
    public Result<Void> paySuccess(@RequestBody ParkingPaySuccessDTO dto) {
        try {
            parkingPaymentService.paySuccess(dto);
            return Result.success();
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("支付回调异常", e);
            return Result.fail("支付处理失败");
        }
    }
}