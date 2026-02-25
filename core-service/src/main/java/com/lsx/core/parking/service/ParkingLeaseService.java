package com.lsx.core.parking.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lsx.core.parking.dto.ParkingLeaseOrderCreateDTO;
import com.lsx.core.parking.dto.ParkingLeaseOrderPayDTO;
import com.lsx.core.parking.entity.ParkingOrder;
import com.lsx.core.parking.entity.ParkingSpaceLease;

public interface ParkingLeaseService {
    Long createLeaseOrder(ParkingLeaseOrderCreateDTO dto);
    void payLeaseOrder(ParkingLeaseOrderPayDTO dto); // 改成 DTO
}