package com.lsx.core.parking.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lsx.core.parking.dto.ParkingSpaceQueryDTO;
import com.lsx.core.parking.entity.ParkingSpace;
import com.lsx.core.parking.vo.ParkingSpaceVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ParkingSpaceMapper extends BaseMapper<ParkingSpace> {

    @Select("<script>" +
            "SELECT " +
            "  s.id, " +
            "  s.space_no AS spaceNo, " +
            "  s.space_no AS slot, " +
            "  s.community_name AS communityName, " +
            "  s.status AS status " +
            "FROM biz_parking_space s " +
            "WHERE s.deleted = 0 " +
            "<if test='dto.communityId != null'> " +
            "  AND s.community_id = #{dto.communityId} " +
            "</if>" +
            "<if test='dto.spaceNo != null and dto.spaceNo != \"\"'> " +
            "  AND s.space_no LIKE CONCAT('%', #{dto.spaceNo}, '%') " +
            "</if>" +
            "<if test='dto.status != null and dto.status != \"\"'> " +
            "  AND s.status = #{dto.status} " +
            "</if>" +
            "ORDER BY s.space_no ASC" +
            "</script>")
    IPage<ParkingSpaceVO> selectAdminPage(IPage<ParkingSpaceVO> page, @Param("dto") ParkingSpaceQueryDTO dto);
}








