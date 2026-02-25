package com.lsx.core.community.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lsx.core.common.Util.UserContext;
import com.lsx.core.community.entity.Community;
import com.lsx.core.community.mapper.CommunityMapper;
import com.lsx.core.community.service.CommunityService;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class CommunityServiceImpl extends ServiceImpl<CommunityMapper, Community> implements CommunityService {

    @Override
    public List<Community> getListByRole() {
        String role = UserContext.getRole();
        Long communityId = UserContext.getCommunityId();

        if ("super_admin".equalsIgnoreCase(role)) {
            // 超级管理员：查询所有社区
            return list();
        } else if ("admin".equalsIgnoreCase(role)) {
            // 普通管理员：只查询自己归属的社区
            if (communityId != null) {
                return list(new LambdaQueryWrapper<Community>().eq(Community::getId, communityId));
            }
        } else if ("owner".equalsIgnoreCase(role)) {
             // 业主：查询自己归属的社区
             if (communityId != null) {
                return list(new LambdaQueryWrapper<Community>().eq(Community::getId, communityId));
             }
        }
        
        // 如果没有匹配的角色或没有社区ID，返回空列表
        return Collections.emptyList();
    }
}
