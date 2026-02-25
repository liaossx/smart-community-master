package com.lsx.core.parking.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lsx.core.parking.entity.ParkingSpacePlate;
import org.apache.ibatis.annotations.*;

import java.util.List;

public interface ParkingPlateMapper extends BaseMapper<ParkingSpacePlate> {
    // 查询某车位和车牌的记录
    @Select("SELECT * FROM biz_parking_space_plate WHERE space_id = #{spaceId} AND plate_no = #{plateNo} LIMIT 1")
    ParkingSpacePlate selectBySpaceAndPlate(@Param("spaceId") Long spaceId, @Param("plateNo") String plateNo);

    // 查询某车位下用户绑定的所有 ACTIVE 车牌
    @Select("SELECT * FROM biz_parking_space_plate WHERE space_id = #{spaceId} AND user_id = #{userId} AND status = 'ACTIVE'")
    List<ParkingSpacePlate> selectActivePlatesBySpace(@Param("spaceId") Long spaceId, @Param("userId") Long userId);

    // 插入
    @Insert("INSERT INTO biz_parking_space_plate(space_id, user_id, plate_no, status, create_time, update_time) " +
            "VALUES(#{spaceId}, #{userId}, #{plateNo}, #{status}, #{createTime}, #{updateTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ParkingSpacePlate plate);

    // 更新
    @Update("UPDATE biz_parking_space_plate SET status = #{status}, update_time = #{updateTime} WHERE id = #{id}")
    int updateById(ParkingSpacePlate plate);
}
