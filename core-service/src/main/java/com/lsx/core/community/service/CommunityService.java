package com.lsx.core.community.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lsx.core.community.entity.Community;

import java.util.List;

public interface CommunityService extends IService<Community> {
    List<Community> getListByRole();
}
