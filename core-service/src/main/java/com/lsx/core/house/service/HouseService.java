package com.lsx.core.house.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lsx.core.house.entity.House;
import com.lsx.core.house.vo.HouseResult;

import java.util.List;

public interface HouseService extends IService<House> {
    //查询某栋某单元的id（管理员）
    HouseResult getHouseInfoById(Long houseId);
    //查询全部房屋信息（管理员）
    List<HouseResult> getAllHouseInfo();
    //更改房屋状态
    Boolean updateHouseStatus(Long id, String status);
    // 添加批量查询方法
    List<HouseResult> getHouseInfoByIds(List<Long> houseIds);

}
