package com.lsx.core.house.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lsx.core.house.entity.House;
import com.lsx.core.common.enums.HouseStatusEnum;
import com.lsx.core.house.entity.UserHouse;
import com.lsx.core.house.mapper.HouseMapper;
import com.lsx.core.house.mapper.UserHouseMapper;
import com.lsx.core.house.service.HouseService;
import com.lsx.core.house.vo.HouseResult;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class HouseServiceImpl extends ServiceImpl<HouseMapper, House> implements HouseService {

    @Autowired
    private UserHouseMapper userHouseMapper;
    @Autowired
    private HouseMapper houseMapper;

    @Override
    public HouseResult getHouseInfoById(Long houseId) {
        //非空校验
        if (houseId == null){
            throw new RuntimeException("房屋ID不能为空");
        }
        HouseResult houseResult = baseMapper.getHouseInfoById(houseId);
        if (houseResult == null){
            throw new RuntimeException("房屋信息不存在");
        }
        return houseResult;
    }

    @Override
    public List<HouseResult> getAllHouseInfo() {
        List<HouseResult> houseResult = baseMapper.getAllHouseInfo();
        if (houseResult == null){
            throw new RuntimeException("房屋信息不存在");
        }
        return houseResult;
    }
    //查询某栋楼某单元的ID

    //更改房屋状态
    @Override
    public  Boolean updateHouseStatus(Long id, String status) {
        //字符校 验合法状态为：待审核、审核通过、审核不通过

        //非空校验
        if (id == null){
            throw new RuntimeException("ID不能为空");
        }
        if (status == null){
            throw new RuntimeException("状态不能为空");
        }
        UserHouse userHouse= userHouseMapper.selectById(id);
        if (userHouse == null) {
            throw new RuntimeException("房屋关联记录不存在（ID：" + id + "）");
        }
        HouseStatusEnum.validateStatus(status);
        Long houseId= userHouse.getHouseId();

        long houseCount = houseMapper.selectCount(Wrappers.<House>lambdaQuery()
                .eq(House::getId, houseId));
        if (houseCount == 0) {
            throw new RuntimeException("房屋信息不存在（房屋ID：" + houseId + "）");
        }
        int updateRows = userHouseMapper.updateStatus(id, status.trim());
        return updateRows > 0; // 只有更新成功（影响行数>0）才返回 true
    }

    @Override
    public List<HouseResult> getHouseInfoByIds(List<Long> houseIds) {
        if (houseIds == null || houseIds.isEmpty()) {
            return Collections.emptyList();
        }

        QueryWrapper<House> query = new QueryWrapper<>();
        query.in("id", houseIds);
        List<House> houses = this.list(query); // 使用 IService 的 list 方法

        return houses.stream().map(house -> {
            HouseResult result = new HouseResult();
            BeanUtils.copyProperties(house, result);
            return result;
        }).collect(Collectors.toList());
    }
}
