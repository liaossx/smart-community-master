package com.lsx.core.group.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lsx.core.group.dto.GroupCreateDTO;
import com.lsx.core.group.dto.GroupFinishDTO;
import com.lsx.core.group.dto.GroupJoinDTO;
import com.lsx.core.group.entity.GroupActivity;
import com.lsx.core.group.vo.GroupVO;

public interface GroupService extends IService<GroupActivity> {
    Long createGroup(GroupCreateDTO dto);

    Boolean joinGroup(Long groupId, GroupJoinDTO dto);

    GroupVO getGroupDetail(Long groupId);

    Boolean finishGroup(Long groupId, GroupFinishDTO dto);
}

