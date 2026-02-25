package com.lsx.core.parking.service;

import com.lsx.core.parking.dto.ParkingPaySuccessDTO;

public interface ParkingPaymentService {

    void paySuccess(ParkingPaySuccessDTO dto);
}