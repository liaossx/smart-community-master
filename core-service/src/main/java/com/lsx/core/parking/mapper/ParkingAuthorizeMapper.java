package com.lsx.core.parking.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lsx.core.parking.entity.ParkingAuthorize;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ParkingAuthorizeMapper extends BaseMapper<ParkingAuthorize> {
    @Select("SELECT * FROM biz_parking_authorize WHERE space_id = #{spaceId} AND user_id = #{userId} AND status = 'ACTIVE' LIMIT 1")
    ParkingAuthorize getActiveBySpaceAndUser(@Param("spaceId") Long spaceId, @Param("userId") Long userId);
}









