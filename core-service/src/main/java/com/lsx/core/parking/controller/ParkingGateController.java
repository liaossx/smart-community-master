package com.lsx.core.parking.controller;

import com.lsx.core.common.Result.Result;
import com.lsx.core.parking.dto.ParkingGateEnterDTO;
import com.lsx.core.parking.dto.ParkingGateExitDTO;
import com.lsx.core.parking.dto.ParkingGateOpenDTO;
import com.lsx.core.parking.service.ParkingGateService;
import com.lsx.core.parking.vo.ParkingGateExitResult;
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
@RequestMapping("/api/parking/gate")
@Tag(name = "停车-开闸")
public class ParkingGateController {

    @Autowired
    private ParkingGateService parkingGateService;


    @PostMapping("/enter")
    @Operation(summary = "车辆入闸", description = "扫描车牌或扫码入闸")
    public Result<Void> enterGate(@RequestBody ParkingGateEnterDTO dto) {
        try {
            parkingGateService.enterGate(dto);
            return Result.success();
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("车辆入闸异常", e);
            return Result.fail("入闸失败，请稍后再试");
        }
    }

    @PostMapping("/exit")
    @Operation(summary = "车辆出闸")
    public Result<ParkingGateExitResult> exitGate(
            @RequestBody ParkingGateExitDTO dto) {

        ParkingGateExitResult result = parkingGateService.exitGate(dto);
        return Result.success(result);
    }
}
