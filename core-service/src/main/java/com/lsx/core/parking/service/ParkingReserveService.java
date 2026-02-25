package com.lsx.core.parking.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lsx.core.parking.dto.ParkingReserveAdminCancelDTO;
import com.lsx.core.parking.dto.ParkingReserveCancelDTO;
import com.lsx.core.parking.dto.ParkingReserveCreateDTO;
import com.lsx.core.parking.dto.ParkingReserveQueryDTO;
import com.lsx.core.parking.entity.ParkingReserve;
import com.lsx.core.parking.vo.ParkingReserveVO;

public interface ParkingReserveService extends IService<ParkingReserve> {

    Long createReserve(ParkingReserveCreateDTO dto);

    Boolean cancelReserve(ParkingReserveCancelDTO dto);

    Boolean adminCancelReserve(ParkingReserveAdminCancelDTO dto);

    IPage<ParkingReserveVO> listMyReserves(Long userId, ParkingReserveQueryDTO dto);

    IPage<ParkingReserveVO> adminListReserves(ParkingReserveQueryDTO dto);
}

