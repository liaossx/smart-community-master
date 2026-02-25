package com.lsx.core.activity.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lsx.core.activity.entity.SysActivity;

public interface ActivityService extends IService<SysActivity> {
    IPage<SysActivity> list(String status, Integer pageNum, Integer pageSize);
    SysActivity detail(Long id);
    Long publish(SysActivity a);
    boolean deleteByIdWithCheck(Long id);
    boolean join(Long activityId, Long userId);
}
