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

import com.lsx.core.visitor.dto.VisitorDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class VisitorServiceImpl extends ServiceImpl<SysVisitorMapper, SysVisitor> implements VisitorService {
    
    private static final Logger log = LoggerFactory.getLogger(VisitorServiceImpl.class);

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
    public IPage<VisitorDTO> adminList(Integer pageNum, Integer pageSize, String status, String keyword) {
        Page<VisitorDTO> page = new Page<>(pageNum, pageSize);
        String role = UserContext.getRole();
        Long cid = UserContext.getCommunityId();
        
        log.info("开始执行 Mapper 查询: communityId={}, role={}, keyword={}, status={}", cid, role, keyword, status);
        
        Long filterCid = null;
        if (!"super_admin".equalsIgnoreCase(role)) {
            if (cid != null) filterCid = cid;
            else filterCid = -1L;
        }
        
        try {
            IPage<VisitorDTO> result = baseMapper.selectAdminList(page, status, keyword, filterCid);
            log.info("Mapper 查询返回: size={}", result.getRecords().size());
            return result;
        } catch (Exception e) {
            log.error("Mapper 执行失败!", e);
            throw e;
        }
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
