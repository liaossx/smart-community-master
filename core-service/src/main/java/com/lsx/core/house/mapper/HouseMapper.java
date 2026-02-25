package com.lsx.core.house.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lsx.core.house.entity.House;
import com.lsx.core.house.vo.HouseResult;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
@Mapper
public interface HouseMapper extends BaseMapper<House> {

    @Select("SELECT id FROM sys_house WHERE building_no = #{buildingNo} AND house_no = #{houseNo}")
    Long getHouseIDByBuildingNoAndHouseNo(String buildingNo, String houseNo);

    @Select("SELECT " +
            "id, " +
            "community_name AS communityName, " +
            "building_no AS buildingNo, " +
            "house_no AS houseNo, " +
            "area " +
            "FROM sys_house " +
            "WHERE id = #{houseId}")
    HouseResult getHouseInfoById(Long houseId);

    @Select("SELECT " +
            "id, " +
            "community_name AS communityName, " +
            "building_no AS buildingNo, " +
            "house_no AS houseNo, " +
            "area " +
            "FROM sys_house")
    List<HouseResult> getAllHouseInfo();

    @Select("<script>" +
            "SELECT id, community_name AS communityName, building_no AS buildingNo, house_no AS houseNo, area " +
            "FROM sys_house WHERE id IN " +
            "<foreach collection='houseIds' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    List<HouseResult> getHouseInfoByIds(@Param("houseIds") List<Long> houseIds);
}


