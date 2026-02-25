package com.lsx.core.complaint.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lsx.core.complaint.entity.SysComplaint;

public interface ComplaintService extends IService<SysComplaint> {
    Long submit(SysComplaint c);
    IPage<SysComplaint> my(Long userId, Integer pageNum, Integer pageSize);
    IPage<SysComplaint> adminList(Integer pageNum, Integer pageSize, String status);
    boolean handle(Long id, String result);
}
