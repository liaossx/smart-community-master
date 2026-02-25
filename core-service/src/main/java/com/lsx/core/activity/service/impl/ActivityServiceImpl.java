package com.lsx.core.activity.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lsx.core.activity.entity.SysActivity;
import com.lsx.core.activity.entity.SysActivitySignup;
import com.lsx.core.activity.mapper.SysActivityMapper;
import com.lsx.core.activity.mapper.SysActivitySignupMapper;
import com.lsx.core.activity.service.ActivityService;
import com.lsx.core.common.Util.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ActivityServiceImpl extends ServiceImpl<SysActivityMapper, SysActivity> implements ActivityService {

    @Autowired
    private SysActivitySignupMapper signupMapper;

    @Override
    public IPage<SysActivity> list(String status, Integer pageNum, Integer pageSize) {
        Page<SysActivity> page = new Page<>(pageNum, pageSize);
        QueryWrapper<SysActivity> qw = new QueryWrapper<>();
        String role = UserContext.getRole();
        Long cid = UserContext.getCommunityId();
        if (!"super_admin".equalsIgnoreCase(role)) {
            if (cid != null) qw.eq("community_id", cid);
        }
        if (status != null && status.trim().length() > 0) {
            qw.eq("status", status);
        }
        qw.orderByDesc("start_time");
        return this.page(page, qw);
    }

    @Override
    public SysActivity detail(Long id) {
        return this.getById(id);
    }

    @Override
    public Long publish(SysActivity a) {
        Long cid = UserContext.getCommunityId();
        a.setCommunityId(cid);
        a.setStatus("ONLINE");
        a.setSignupCount(0);
        a.setCreateTime(LocalDateTime.now());
        this.save(a);
        return a.getId();
    }

    @Override
    public boolean deleteByIdWithCheck(Long id) {
        SysActivity a = this.getById(id);
        if (a == null) return false;
        String role = UserContext.getRole();
        Long cid = UserContext.getCommunityId();
        if (!"super_admin".equalsIgnoreCase(role)) {
            if (cid == null || a.getCommunityId() == null || !cid.equals(a.getCommunityId())) {
                throw new RuntimeException("无权删除其他社区活动");
            }
        }
        return this.removeById(id);
    }

    @Override
    public boolean join(Long activityId, Long userId) {
        SysActivity a = this.getById(activityId);
        if (a == null) throw new RuntimeException("活动不存在");
        if (!"ONLINE".equals(a.getStatus())) throw new RuntimeException("活动不可报名");
        if (a.getMaxCount() != null && a.getSignupCount() != null && a.getSignupCount() >= a.getMaxCount()) {
            throw new RuntimeException("名额已满");
        }
        SysActivitySignup s = new SysActivitySignup();
        s.setActivityId(activityId);
        s.setUserId(userId);
        s.setSignupTime(LocalDateTime.now());
        signupMapper.insert(s);
        a.setSignupCount(a.getSignupCount() == null ? 1 : a.getSignupCount() + 1);
        this.updateById(a);
        return true;
    }
}
