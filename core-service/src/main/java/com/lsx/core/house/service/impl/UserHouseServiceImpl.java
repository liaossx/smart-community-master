package com.lsx.core.house.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lsx.core.house.entity.House;
import com.lsx.core.house.entity.UserHouse;
import com.lsx.core.house.mapper.HouseMapper;
import com.lsx.core.house.mapper.UserHouseMapper;
import com.lsx.core.house.service.UserHouseService;
import com.lsx.core.user.entity.User;
import com.lsx.core.user.mapper.UserMapper;
import org.apache.catalina.realm.UserDatabaseRealm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserHouseServiceImpl extends ServiceImpl<UserHouseMapper, UserHouse> implements UserHouseService {
    @Autowired
    private UserHouseMapper userHouseMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private HouseMapper houseMapper;
    @Override
    public List<House> getHouseByUserId(Long userId) {

        // 用户ID为空 → 返回空列表
        if (userId == null) {
            return Collections.emptyList();
        }

        // 查询用户
        User user = userMapper.selectById(userId);
        if (user == null) {
            return Collections.emptyList();
        }

        // 查询用户绑定的房屋ID
        LambdaQueryWrapper<UserHouse> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserHouse::getUserId, userId)
                .eq(UserHouse::getStatus, "审核通过");

        List<UserHouse> relationList = userHouseMapper.selectList(wrapper);

        if (CollectionUtils.isEmpty(relationList)) {
            return Collections.emptyList();
        }

        // 提取房屋ID
        List<Long> houseIds = relationList.stream()
                .map(UserHouse::getHouseId)
                .collect(Collectors.toList());

        // 查询房屋列表
        List<House> houseList = houseMapper.selectBatchIds(houseIds);

        return houseList == null ? Collections.emptyList() : houseList;
    }
}
