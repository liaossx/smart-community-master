package com.lsx.core.parking.service;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lsx.core.parking.dto.VehicleBindDTO;
import com.lsx.core.parking.entity.Vehicle;
import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lsx.core.parking.dto.ParkingCarAuditDTO;
import com.lsx.core.parking.vo.ParkingCarAuditVO;

public interface VehicleService {
    void bindVehicle(VehicleBindDTO dto);

    IPage<ParkingCarAuditVO> listAudit(String status, Integer pageNum, Integer pageSize);

    void auditCar(ParkingCarAuditDTO dto);
}