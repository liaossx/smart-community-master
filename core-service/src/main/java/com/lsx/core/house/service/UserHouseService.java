package com.lsx.core.house.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lsx.core.house.entity.House;
import com.lsx.core.house.entity.UserHouse;

import java.util.List;

public interface UserHouseService extends IService<UserHouse> {
    //根据用户id查询对应的房屋信息
   List<House> getHouseByUserId(Long userId);
}
