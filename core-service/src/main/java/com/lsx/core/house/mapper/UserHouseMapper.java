package com.lsx.core.house.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lsx.core.house.entity.UserHouse;
import org.apache.ibatis.annotations.*;

@Mapper
public interface UserHouseMapper extends BaseMapper<UserHouse> {
    @Insert("insert into user_house (user_id,house_id) values (#{userId},#{houseId})")
    int insert(@Param("userId") Long userId, @Param("houseId") Long houseId);

    @Update("update user_house set status=#{status} where id =#{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    @Select("select house_id from user_house where user_id=#{userId}")
    int selectHouseIdByUserIdInt(Long userId);
}
