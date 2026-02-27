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

import com.lsx.core.activity.dto.SignupRecordDTO;

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
        
        if (a.getId() != null) {
            // 编辑逻辑
            SysActivity exist = this.getById(a.getId());
            if (exist == null) {
                throw new RuntimeException("活动不存在");
            }
            
            // 权限校验：只能编辑本社区活动
            String role = UserContext.getRole();
            if (!"super_admin".equalsIgnoreCase(role)) {
                if (cid != null && !cid.equals(exist.getCommunityId())) {
                    throw new RuntimeException("无权编辑其他社区活动");
                }
            }
            
            // 更新允许修改的字段
            exist.setTitle(a.getTitle());
            exist.setContent(a.getContent());
            exist.setStartTime(a.getStartTime());
            exist.setLocation(a.getLocation());
            exist.setMaxCount(a.getMaxCount());
            if (a.getCoverUrl() != null) {
                 exist.setCoverUrl(a.getCoverUrl());
            }
            if (a.getStatus() != null) {
                exist.setStatus(a.getStatus());
            }
            
            this.updateById(exist);
            return exist.getId();
        } else {
            // 新增逻辑
            a.setCommunityId(cid);
            // 如果前端没传状态，默认设为 ONLINE，但根据你的业务需求，这里可以改为 DRAFT
            // 这里我们信任前端传来的状态 (PUBLISHED 或 DRAFT)
            // 只有当前端完全没传 status 时，才给一个默认值
            if (a.getStatus() == null || a.getStatus().trim().isEmpty()) {
                a.setStatus("PUBLISHED"); // 默认改为 PUBLISHED，或者根据你的需求设为 DRAFT
            }
            if (a.getSignupCount() == null) {
                a.setSignupCount(0);
            }
            if (a.getCreateTime() == null) {
                a.setCreateTime(LocalDateTime.now());
            }
            this.save(a);
            return a.getId();
        }
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
        
        // 兼容 ONLINE 和 PUBLISHED 两种状态
        if (!"ONLINE".equals(a.getStatus()) && !"PUBLISHED".equals(a.getStatus())) {
             throw new RuntimeException("活动不可报名");
        }
        
        if (a.getMaxCount() != null && a.getSignupCount() != null && a.getSignupCount() >= a.getMaxCount()) {
            throw new RuntimeException("名额已满");
        }
        
        // 检查是否已报名
        Long count = signupMapper.selectCount(new QueryWrapper<SysActivitySignup>()
                .eq("activity_id", activityId)
                .eq("user_id", userId));
        if (count > 0) throw new RuntimeException("您已报名该活动");

        SysActivitySignup s = new SysActivitySignup();
        s.setActivityId(activityId);
        s.setUserId(userId);
        s.setSignupTime(LocalDateTime.now());
        signupMapper.insert(s);
        a.setSignupCount(a.getSignupCount() == null ? 1 : a.getSignupCount() + 1);
        this.updateById(a);
        return true;
    }
    
    @Override
    public IPage<SignupRecordDTO> getSignupList(Long activityId, Integer pageNum, Integer pageSize) {
        Page<SignupRecordDTO> page = new Page<>(pageNum, pageSize);
        // 权限校验：只能查看本社区的活动报名（或自己的活动）
        // 这里简单处理：检查活动是否存在
        SysActivity a = this.getById(activityId);
        if (a == null) throw new RuntimeException("活动不存在");
        
        // 社区隔离检查
        String role = UserContext.getRole();
        Long cid = UserContext.getCommunityId();
        if (!"super_admin".equalsIgnoreCase(role)) {
            if (cid != null && !cid.equals(a.getCommunityId())) {
                 throw new RuntimeException("无权查看其他社区活动报名");
            }
        }
        
        return signupMapper.selectSignupList(page, activityId);
    }
}
