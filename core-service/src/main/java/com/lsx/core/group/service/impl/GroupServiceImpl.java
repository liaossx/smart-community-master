package com.lsx.core.group.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lsx.core.group.dto.GroupCreateDTO;
import com.lsx.core.group.dto.GroupFinishDTO;
import com.lsx.core.group.dto.GroupJoinDTO;
import com.lsx.core.group.entity.GroupActivity;
import com.lsx.core.group.entity.GroupMember;
import com.lsx.core.group.enums.GroupStatusEnum;
import com.lsx.core.group.mapper.GroupActivityMapper;
import com.lsx.core.group.mapper.GroupMemberMapper;
import com.lsx.core.group.service.GroupService;
import com.lsx.core.group.vo.GroupMemberVO;
import com.lsx.core.group.vo.GroupVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class GroupServiceImpl extends ServiceImpl<GroupActivityMapper, GroupActivity> implements GroupService {

    @Autowired
    private GroupActivityMapper groupActivityMapper;
    @Autowired
    private GroupMemberMapper groupMemberMapper;

    @Override
    @Transactional
    public Long createGroup(GroupCreateDTO dto) {
        if (dto == null || dto.getSponsorId() == null) {
            throw new RuntimeException("发起人不能为空");
        }
        if (!StringUtils.hasText(dto.getSubject())) {
            throw new RuntimeException("拼团主题不能为空");
        }
        if (dto.getTargetCount() == null || dto.getTargetCount() <= 0) {
            throw new RuntimeException("成团人数必须大于0");
        }
        GroupActivity activity = new GroupActivity();
        BeanUtils.copyProperties(dto, activity);
        activity.setStatus(GroupStatusEnum.ONGOING.name());
        activity.setJoinedCount(1);
        activity.setCreateTime(LocalDateTime.now());
        activity.setUpdateTime(LocalDateTime.now());
        groupActivityMapper.insert(activity);

        GroupMember sponsor = new GroupMember();
        sponsor.setGroupId(activity.getId());
        sponsor.setUserId(dto.getSponsorId());
        sponsor.setRole("SPONSOR");
        sponsor.setStatus("JOINED");
        sponsor.setJoinTime(LocalDateTime.now());
        sponsor.setUpdateTime(LocalDateTime.now());
        groupMemberMapper.insert(sponsor);

        log.info("用户[{}]发起拼团[{}]", dto.getSponsorId(), activity.getId());
        notifyStatusChange(activity);
        return activity.getId();
    }

    @Override
    @Transactional
    public Boolean joinGroup(Long groupId, GroupJoinDTO dto) {
        GroupActivity activity = loadAndValidateGroup(groupId);
        if (!GroupStatusEnum.ONGOING.name().equals(activity.getStatus())) {
            throw new RuntimeException("当前拼团已结束");
        }
        if (activity.getDeadline() != null && LocalDateTime.now().isAfter(activity.getDeadline())) {
            throw new RuntimeException("拼团已过截止时间");
        }
        if (dto == null || dto.getUserId() == null) {
            throw new RuntimeException("用户信息缺失");
        }
        LambdaQueryWrapper<GroupMember> wrapper = Wrappers.<GroupMember>lambdaQuery()
                .eq(GroupMember::getGroupId, groupId)
                .eq(GroupMember::getUserId, dto.getUserId());
        GroupMember existing = groupMemberMapper.selectOne(wrapper);
        if (existing != null && !"CANCELLED".equals(existing.getStatus())) {
            return true; // 幂等控制
        }

        GroupMember member = new GroupMember();
        member.setGroupId(groupId);
        member.setUserId(dto.getUserId());
        member.setRole("MEMBER");
        member.setStatus("JOINED");
        member.setJoinTime(LocalDateTime.now());
        member.setUpdateTime(LocalDateTime.now());
        groupMemberMapper.insert(member);

        activity.setJoinedCount(activity.getJoinedCount() + 1);
        activity.setUpdateTime(LocalDateTime.now());
        groupActivityMapper.updateById(activity);

        if (activity.getJoinedCount() >= activity.getTargetCount()) {
            markGroupSuccess(activity);
        }
        return true;
    }

    @Override
    public GroupVO getGroupDetail(Long groupId) {
        GroupActivity activity = groupActivityMapper.selectById(groupId);
        if (activity == null) {
            throw new RuntimeException("拼团不存在");
        }
        GroupVO vo = new GroupVO();
        BeanUtils.copyProperties(activity, vo);
        vo.setMembers(loadMemberVOs(groupId));
        return vo;
    }

    @Override
    @Transactional
    public Boolean finishGroup(Long groupId, GroupFinishDTO dto) {
        GroupActivity activity = loadAndValidateGroup(groupId);
        if (!GroupStatusEnum.ONGOING.name().equals(activity.getStatus())) {
            throw new RuntimeException("该拼团已经处理过");
        }
        boolean success = Boolean.TRUE.equals(dto.getSuccess());
        activity.setStatus(success ? GroupStatusEnum.SUCCESS.name() : GroupStatusEnum.FAILED.name());
        activity.setRemark(dto.getRemark());
        activity.setFinishTime(LocalDateTime.now());
        activity.setUpdateTime(LocalDateTime.now());
        groupActivityMapper.updateById(activity);

        updateMemberStatus(groupId, success ? "COMPLETED" : "FAILED");
        notifyStatusChange(activity);
        log.info("操作者[{}]手动结束拼团[{}]，结果{}", dto.getOperatorId(), groupId, activity.getStatus());
        return true;
    }

    private GroupActivity loadAndValidateGroup(Long groupId) {
        GroupActivity activity = groupActivityMapper.selectById(groupId);
        if (activity == null) {
            throw new RuntimeException("拼团不存在");
        }
        return activity;
    }

    private void markGroupSuccess(GroupActivity activity) {
        activity.setStatus(GroupStatusEnum.SUCCESS.name());
        activity.setFinishTime(LocalDateTime.now());
        activity.setUpdateTime(LocalDateTime.now());
        groupActivityMapper.updateById(activity);
        updateMemberStatus(activity.getId(), "COMPLETED");
        notifyStatusChange(activity);
    }

    private void updateMemberStatus(Long groupId, String status) {
        List<GroupMember> members = groupMemberMapper.selectList(
                Wrappers.<GroupMember>lambdaQuery().eq(GroupMember::getGroupId, groupId));
        if (CollectionUtils.isEmpty(members)) {
            return;
        }
        for (GroupMember member : members) {
            member.setStatus(status);
            member.setUpdateTime(LocalDateTime.now());
            groupMemberMapper.updateById(member);
        }
    }

    private List<GroupMemberVO> loadMemberVOs(Long groupId) {
        List<GroupMember> members = groupMemberMapper.selectList(
                Wrappers.<GroupMember>lambdaQuery()
                        .eq(GroupMember::getGroupId, groupId)
                        .orderByAsc(GroupMember::getJoinTime));
        if (CollectionUtils.isEmpty(members)) {
            return Collections.emptyList();
        }
        return members.stream().map(member -> {
            GroupMemberVO vo = new GroupMemberVO();
            vo.setUserId(member.getUserId());
            vo.setRole(member.getRole());
            vo.setStatus(member.getStatus());
            vo.setJoinTime(member.getJoinTime());
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 预留扩展：可接入消息队列/WebSocket，实时提醒成员
     */
    private void notifyStatusChange(GroupActivity activity) {
        log.info("拼团[{}]状态变更为{}", activity.getId(), activity.getStatus());
    }
}

