package com.lsx.core.parking.service;

import com.lsx.core.parking.dto.ParkingGateEnterDTO;
import com.lsx.core.parking.dto.ParkingGateExitDTO;
import com.lsx.core.parking.dto.ParkingGateOpenDTO;
import com.lsx.core.parking.vo.ParkingGateExitResult;

public interface ParkingGateService {

    /**
     * 固定车位开闸
     */


     void enterGate(ParkingGateEnterDTO dto);

    ParkingGateExitResult exitGate(ParkingGateExitDTO dto);
}