package com.lsx.core.parking.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lsx.core.parking.dto.VehicleBindDTO;
import com.lsx.core.parking.entity.Vehicle;
import com.lsx.core.parking.mapper.VehicleMapper;
import com.lsx.core.parking.service.VehicleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.time.LocalDateTime;

@Service
public class VehicleServiceImpl
        extends ServiceImpl<VehicleMapper, Vehicle>
        implements VehicleService {

    @Override
    @Transactional
    public void bindVehicle(VehicleBindDTO dto) {

        Assert.notNull(dto.getUserId(), "用户ID不能为空");
        Assert.hasText(dto.getPlateNo(), "车牌号不能为空");

        // 车牌唯一
        Vehicle exist = this.lambdaQuery()
                .eq(Vehicle::getPlateNo, dto.getPlateNo())
                .one();

        if (exist != null) {
            throw new RuntimeException("该车牌已绑定用户");
        }

        Vehicle vehicle = new Vehicle();
        vehicle.setUserId(dto.getUserId());
        vehicle.setPlateNo(dto.getPlateNo());
        vehicle.setStatus("ACTIVE");
        vehicle.setCreateTime(LocalDateTime.now());
        vehicle.setUpdateTime(LocalDateTime.now());

        this.save(vehicle);
    }
}