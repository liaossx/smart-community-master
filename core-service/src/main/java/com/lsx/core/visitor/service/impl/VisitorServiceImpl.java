package com.lsx.core.visitor.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lsx.core.common.Util.UserContext;
import com.lsx.core.visitor.entity.SysVisitor;
import com.lsx.core.visitor.mapper.SysVisitorMapper;
import com.lsx.core.visitor.service.VisitorService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class VisitorServiceImpl extends ServiceImpl<SysVisitorMapper, SysVisitor> implements VisitorService {
    @Override
    public Long apply(SysVisitor entity) {
        Long cid = UserContext.getCommunityId();
        entity.setCommunityId(cid);
        entity.setStatus("PENDING");
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        this.save(entity);
        return entity.getId();
    }

    @Override
    public IPage<SysVisitor> myList(Long userId, Integer pageNum, Integer pageSize) {
        Page<SysVisitor> page = new Page<>(pageNum, pageSize);
        return this.page(page, new QueryWrapper<SysVisitor>().eq("user_id", userId).orderByDesc("create_time"));
    }

    @Override
    public IPage<SysVisitor> adminList(Integer pageNum, Integer pageSize, String status, String keyword) {
        Page<SysVisitor> page = new Page<>(pageNum, pageSize);
        QueryWrapper<SysVisitor> qw = new QueryWrapper<>();
        String role = UserContext.getRole();
        Long cid = UserContext.getCommunityId();
        if (!"super_admin".equalsIgnoreCase(role)) {
            if (cid != null) {
                qw.eq("community_id", cid);
            } else {
                qw.eq("id", -1L);
            }
        }
        if (status != null && status.trim().length() > 0) {
            qw.eq("status", status);
        }
        if (keyword != null && keyword.trim().length() > 0) {
            qw.and(w -> w.like("visitor_name", keyword).or().like("visitor_phone", keyword));
        }
        qw.orderByDesc("create_time");
        return this.page(page, qw);
    }

    @Override
    public boolean audit(Long id, String status, String remark) {
        SysVisitor v = this.getById(id);
        if (v == null) return false;
        String role = UserContext.getRole();
        Long cid = UserContext.getCommunityId();
        if (!"super_admin".equalsIgnoreCase(role)) {
            if (cid == null || v.getCommunityId() == null || !cid.equals(v.getCommunityId())) {
                throw new RuntimeException("无权审核其他社区访客");
            }
        }
        v.setStatus(status);
        v.setAuditRemark(remark);
        v.setUpdateTime(LocalDateTime.now());
        return this.updateById(v);
    }
}
