package com.lsx.core.parking.controller;

import com.lsx.core.common.Result.Result;
import com.lsx.core.parking.dto.VehicleBindDTO;
import com.lsx.core.parking.service.VehicleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/vehicle")
@Tag(name = "车辆管理")
public class VehicleController {

    @Autowired
    private VehicleService vehicleService;

    @PostMapping("/bind")
    @Operation(summary = "绑定车辆")
    public Result<Void> bindVehicle(@RequestBody VehicleBindDTO dto) {
        vehicleService.bindVehicle(dto);
        return Result.success();
    }
}