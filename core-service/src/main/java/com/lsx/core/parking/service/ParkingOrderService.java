package com.lsx.core.parking.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lsx.core.parking.dto.ParkingOrderCreateDTO;
import com.lsx.core.parking.dto.ParkingOrderPayDTO;
import com.lsx.core.parking.entity.ParkingOrder;
import com.lsx.core.parking.vo.ParkingOrderVO;

public interface ParkingOrderService extends IService<ParkingOrder> {

    Long createOrder(ParkingOrderCreateDTO dto);

    IPage<ParkingOrderVO> listMyOrders(Long userId, Integer pageNum, Integer pageSize);

    Boolean payOrder(Long orderId, ParkingOrderPayDTO dto);

    IPage<ParkingOrder> adminListOrders(Integer pageNum, Integer pageSize, String plateNo, String status);
}


