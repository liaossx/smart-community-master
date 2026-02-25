package com.lsx.core.parking.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lsx.core.parking.entity.ParkingSpacePlate;

import java.util.List;

public interface ParkingPlateService extends IService<ParkingSpacePlate> {

    void bindPlate(Long userId, Long spaceId, String plateNo);
    void unbindPlate(Long userId, Long spaceId, String plateNo);
    List<String> getPlatesBySpace(Long userId, Long spaceId);
}
