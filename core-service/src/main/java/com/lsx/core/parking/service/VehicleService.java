package com.lsx.core.parking.service;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lsx.core.parking.dto.VehicleBindDTO;
import com.lsx.core.parking.entity.Vehicle;
import org.apache.ibatis.annotations.Mapper;

public interface VehicleService {
    void bindVehicle(VehicleBindDTO dto);
}