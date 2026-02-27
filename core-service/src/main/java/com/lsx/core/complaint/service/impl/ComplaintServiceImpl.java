package com.lsx.core.complaint.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lsx.core.common.Util.UserContext;
import com.lsx.core.complaint.entity.SysComplaint;
import com.lsx.core.complaint.mapper.SysComplaintMapper;
import com.lsx.core.complaint.service.ComplaintService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import com.lsx.core.complaint.dto.ComplaintDTO;

@Service
public class ComplaintServiceImpl extends ServiceImpl<SysComplaintMapper, SysComplaint> implements ComplaintService {
    @Override
    public Long submit(SysComplaint c) {
        Long cid = UserContext.getCommunityId();
        c.setCommunityId(cid);
        c.setStatus("PENDING");
        c.setCreateTime(LocalDateTime.now());
        this.save(c);
        return c.getId();
    }

    @Override
    public IPage<SysComplaint> my(Long userId, Integer pageNum, Integer pageSize) {
        Page<SysComplaint> page = new Page<>(pageNum, pageSize);
        return this.page(page, new QueryWrapper<SysComplaint>().eq("user_id", userId).orderByDesc("create_time"));
    }

    @Override
    public IPage<ComplaintDTO> adminList(Integer pageNum, Integer pageSize, String status) {
        Page<ComplaintDTO> page = new Page<>(pageNum, pageSize);
        String role = UserContext.getRole();
        Long cid = UserContext.getCommunityId();
        
        Long filterCid = null;
        if (!"super_admin".equalsIgnoreCase(role)) {
            if (cid != null) filterCid = cid;
            else filterCid = -1L; // 没绑定社区的管理员啥也看不到
        }
        
        return baseMapper.selectAdminList(page, status, filterCid);
    }

    @Override
    public boolean handle(Long id, String result) {
        SysComplaint c = this.getById(id);
        if (c == null) return false;
        String role = UserContext.getRole();
        Long cid = UserContext.getCommunityId();
        if (!"super_admin".equalsIgnoreCase(role)) {
            if (cid == null || c.getCommunityId() == null || !cid.equals(c.getCommunityId())) {
                throw new RuntimeException("无权处理其他社区投诉");
            }
        }
        c.setResult(result);
        c.setStatus("DONE");
        c.setHandleTime(LocalDateTime.now());
        return this.updateById(c);
    }
}
