package com.lsx.core.parking.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lsx.core.parking.dto.ParkingAuthorizeDTO;
import com.lsx.core.parking.dto.ParkingSpaceBindDTO;
import com.lsx.core.parking.entity.ParkingSpace;
import com.lsx.core.parking.vo.ParkingAuthorizeVO;
import com.lsx.core.parking.vo.ParkingSpaceRemainVO;
import com.lsx.core.parking.vo.ParkingSpaceVO;

import java.util.List;

public interface ParkingSpaceService extends IService<ParkingSpace> {

    ParkingSpaceRemainVO getRemaining(Long communityId);

    Boolean bindSpace(ParkingSpaceBindDTO dto);

    List<ParkingSpaceVO> listMySpaces(Long userId);

    Boolean authorizeSpace(Long spaceId, ParkingAuthorizeDTO dto);

    IPage<ParkingAuthorizeVO> listMyAuthorizes(Long userId, Integer pageNum, Integer pageSize);

    IPage<ParkingSpaceVO> adminListSpaces(com.lsx.core.parking.dto.ParkingSpaceQueryDTO dto);
}



