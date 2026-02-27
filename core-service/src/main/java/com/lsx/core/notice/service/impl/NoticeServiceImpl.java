package com.lsx.core.notice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lsx.core.common.Util.UserContext;
import com.lsx.core.community.entity.Community;
import com.lsx.core.community.mapper.CommunityMapper;
import com.lsx.core.house.entity.UserHouse;
import com.lsx.core.house.mapper.UserHouseMapper;
import com.lsx.core.house.service.HouseService;
import com.lsx.core.house.vo.HouseResult;
import com.lsx.core.notice.dto.NoticeCreateDTO;
import com.lsx.core.notice.dto.NoticeVO;
import com.lsx.core.notice.dto.NoticeReadStatDTO;
import com.lsx.core.notice.entity.SysNotice;
import com.lsx.core.notice.entity.SysNoticeRead;
import com.lsx.core.notice.mapper.SysNoticeMapper;
import com.lsx.core.notice.mapper.SysNoticeReadMapper;
import com.lsx.core.notice.service.NoticeService;
import com.lsx.core.repair.entity.Repair;
import com.lsx.core.repair.mapper.RepairMapper;
import com.lsx.core.user.entity.User;
import com.lsx.core.user.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.baomidou.mybatisplus.extension.toolkit.Db.updateById;

@Service
@Slf4j
public class NoticeServiceImpl extends ServiceImpl<SysNoticeMapper, SysNotice> implements NoticeService  {

    @Autowired
    private SysNoticeMapper noticeMapper;
    @Autowired
    private SysNoticeReadMapper noticeReadMapper;
    @Autowired
    private UserHouseMapper userHouseMapper;
    @Autowired
    private HouseService houseService;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private CommunityMapper communityMapper;

    // 辅助方法：判断是否已删除
    private boolean isDeleted(SysNotice notice) {
        return notice != null && notice.getDeleted() != null && notice.getDeleted() == 1;
    }

    // 辅助方法：判断是否未删除
    private boolean isNotDeleted(SysNotice notice) {
        return notice != null && (notice.getDeleted() == null || notice.getDeleted() == 0);
    }

    @Override
    @Transactional
    public Long createNotice(NoticeCreateDTO dto, Long adminId) {
        SysNotice notice = new SysNotice();
        BeanUtils.copyProperties(dto, notice);
        notice.setCreatorId(adminId);
        notice.setCreateTime(LocalDateTime.now());
        notice.setUpdateTime(LocalDateTime.now());

        String role = com.lsx.core.common.Util.UserContext.getRole();
        Long adminCommunityId = com.lsx.core.common.Util.UserContext.getCommunityId();

        if (!"super_admin".equalsIgnoreCase(role)) {
            if (adminCommunityId == null) {
                throw new RuntimeException("管理员未绑定社区，无法发布公告");
            }
            notice.setTargetType("COMMUNITY");
            notice.setCommunityId(adminCommunityId);
            Community c = communityMapper.selectById(adminCommunityId);
            if (c != null) {
                notice.setCommunityName(c.getName());
            }
        } else {
            if (dto.getCommunityId() != null) {
                notice.setTargetType("COMMUNITY");
                notice.setCommunityId(dto.getCommunityId());
                Community c = communityMapper.selectById(dto.getCommunityId());
                if (c != null) {
                    notice.setCommunityName(c.getName());
                }
            }
        }

        if ("PUBLISHED".equalsIgnoreCase(dto.getPublishStatus())) {
            notice.setPublishStatus("PUBLISHED");
            notice.setPublishTime(LocalDateTime.now());
        } else {
            notice.setPublishStatus("DRAFT");
        }

        if (notice.getTopFlag() == null) {
            notice.setTopFlag(false);
        }
        notice.setDeleted(0);
        noticeMapper.insert(notice);
        return notice.getId();
    }

    @Override
    public Page<NoticeVO> listNotices(Long userId, Integer pageNum, Integer pageSize) {
        /**
         * 1. 查询用户绑定房屋
         */
        List<UserHouse> userHouseList = userHouseMapper.selectList(
                new QueryWrapper<UserHouse>()
                        .eq("user_id", userId)
                        .eq("status", "1")
        );

        Set<String> communityNames = new HashSet<>();
        Set<String> buildingNos = new HashSet<>();

        for (UserHouse uh : userHouseList) {
            try {
                HouseResult hr = houseService.getHouseInfoById(uh.getHouseId());
                if (hr != null) {
                    if (hr.getCommunityName() != null) communityNames.add(hr.getCommunityName());
                    if (hr.getBuildingNo() != null) buildingNos.add(hr.getBuildingNo());
                }
            } catch (Exception e) {
                log.warn("获取房屋信息失败: {}", e.getMessage());
            }
        }

        /**
         * 2. 构造分页
         */
        Page<SysNotice> page = new Page<>(pageNum, pageSize);

        /**
         * 3. 查询公告（关键点：OR 逻辑必须放在同级！）
         */
        QueryWrapper<SysNotice> wrapper = new QueryWrapper<>();
        wrapper.eq("publish_status", "PUBLISHED")
                .eq("deleted", 0)  // 这里是对的，因为deleted是Integer类型
                .and(w -> w
                        .eq("target_type", "ALL")
                        .or(!communityNames.isEmpty(),
                                c -> c.eq("target_type", "COMMUNITY")
                                        .in("community_name", communityNames))
                        .or(!buildingNos.isEmpty(),
                                b -> b.eq("target_type", "BUILDING")
                                        .in("building_no", buildingNos))
                        .or(u -> u.eq("target_type", "USER")
                                .eq("target_user_id", userId))
                )
                // 未过期
                .and(w -> w.isNull("expire_time")
                        .or()
                        .gt("expire_time", LocalDateTime.now())
                );

        Page<SysNotice> noticePage = noticeMapper.selectPage(page, wrapper);

        /**
         * 4. 查询已读状态
         */
        List<Long> noticeIds = noticePage.getRecords().stream()
                .map(SysNotice::getId)
                .collect(Collectors.toList());

        Map<Long, Boolean> readMap = new HashMap<>();
        if (!noticeIds.isEmpty()) {
            List<SysNoticeRead> readList = noticeReadMapper.selectList(
                    new QueryWrapper<SysNoticeRead>()
                            .eq("user_id", userId)
                            .in("notice_id", noticeIds)
            );

            readList.forEach(r -> readMap.put(r.getNoticeId(), true));
        }

        /**
         * 5. 转成 VO
         */
        List<NoticeVO> voList = noticePage.getRecords().stream()
                .map(n -> {
                    NoticeVO vo = new NoticeVO();
                    BeanUtils.copyProperties(n, vo);
                    vo.setRead(readMap.getOrDefault(n.getId(), false));   // 未读 = false
                    return vo;
                })
                .collect(Collectors.toList());

        /**
         * 6. 排序（未读优先 → 置顶优先 → 时间倒序）
         */
        voList.sort(Comparator
                .comparing(NoticeVO::getRead)                                   // false(未读) 在前
                .thenComparing(NoticeVO::getTopFlag, Comparator.reverseOrder())  // top=1 在前
                .thenComparing(NoticeVO::getPublishTime, Comparator.reverseOrder()) // 时间倒序
        );

        /**
         * 7. 返回分页
         */
        Page<NoticeVO> result = new Page<>(pageNum, pageSize, noticePage.getTotal());
        result.setRecords(voList);

        return result;
    }

    @Override
    @Transactional
    public void markAsRead(Long noticeId, Long userId) {
        SysNotice notice = noticeMapper.selectById(noticeId);
        if (isDeleted(notice)) {  // 使用辅助方法
            throw new RuntimeException("公告不存在");
        }

        QueryWrapper<SysNoticeRead> wrapper = new QueryWrapper<>();
        wrapper.eq("notice_id", noticeId).eq("user_id", userId);
        SysNoticeRead readRecord = noticeReadMapper.selectOne(wrapper);
        if (readRecord == null) {
            SysNoticeRead record = new SysNoticeRead();
            record.setNoticeId(noticeId);
            record.setUserId(userId);
            record.setStatus("READ");
            record.setReadTime(LocalDateTime.now());
            record.setCreateTime(LocalDateTime.now());
            noticeReadMapper.insert(record);
        } else if (!"READ".equals(readRecord.getStatus())) {
            readRecord.setStatus("READ");
            readRecord.setReadTime(LocalDateTime.now());
            noticeReadMapper.updateById(readRecord);
        }
    }

    @Override
    @Transactional
    public void deleteNotice(Long noticeId, Long adminId) {
        SysNotice notice = noticeMapper.selectById(noticeId);
        if (notice == null) {
            throw new RuntimeException("公告不存在或已删除");
        }
        String role = UserContext.getRole();
        Long adminCommunityId = UserContext.getCommunityId();
        if (!"super_admin".equalsIgnoreCase(role)) {
            if (notice.getCommunityId() == null || adminCommunityId == null || !adminCommunityId.equals(notice.getCommunityId())) {
                throw new RuntimeException("无权操作其他社区公告");
            }
        }

        // ❶ 直接调用 deleteById
        int rows = noticeMapper.deleteById(noticeId);
        if (rows == 0) {
            throw new RuntimeException("删除失败");
        }

        log.info("管理员[{}]已删除公告[{}]", adminId, noticeId);
    }

    @Override
    public SysNotice getById(Long id) {
        return noticeMapper.selectById(id);
    }

    @Override
    public void updateNotice(Long id, NoticeCreateDTO dto, Long adminId) {
        // 1. 查询公告是否存在
        SysNotice notice = this.getById(id);
        if (notice == null) {
            throw new RuntimeException("公告不存在或已被删除");
        }
        String role = UserContext.getRole();
        Long adminCommunityId = UserContext.getCommunityId();
        if (!"super_admin".equalsIgnoreCase(role)) {
            if (notice.getCommunityId() == null || adminCommunityId == null || !adminCommunityId.equals(notice.getCommunityId())) {
                throw new RuntimeException("无权操作其他社区公告");
            }
        }

        // 2. 更新基本信息
        notice.setTitle(dto.getTitle());
        notice.setContent(dto.getContent());
        notice.setTopFlag(dto.getTopFlag());
        notice.setTargetType(dto.getTargetType());
        // 如果 DTO 包含状态，也可以更新
        if (dto.getPublishStatus() != null) {
            notice.setPublishStatus(dto.getPublishStatus());
        }

        // 3. 更新过期时间
        // 前端已修复为传递 ISO 标准时间，这里可以直接赋值
        if (dto.getExpireTime() != null) {
            notice.setExpireTime(dto.getExpireTime());
        } else {
            // 如果需要支持"清除过期时间"，可以取消下面这行的注释
            // notice.setExpireTime(null);
        }

        // 4. 更新修改人信息（如果有相关字段）
        // notice.setUpdateBy(adminId);
        // notice.setUpdateTime(LocalDateTime.now());

        // 5. 执行更新
        this.updateById(notice);
    }

    @Override
    @Transactional
    public void publishNotice(Long id, Long adminId) {
        SysNotice notice = noticeMapper.selectById(id);
        if (notice == null || isDeleted(notice)) {
            throw new RuntimeException("公告不存在");
        }
        notice.setPublishStatus("PUBLISHED");
        notice.setPublishTime(LocalDateTime.now());
        updateById(notice);
    }

    @Override
    @Transactional
    public void publishNotice(Long id, Long adminId, Long communityId) {
        SysNotice notice = noticeMapper.selectById(id);
        if (notice == null || isDeleted(notice)) {
            throw new RuntimeException("公告不存在");
        }

        String role = UserContext.getRole();
        Long adminCommunityId = UserContext.getCommunityId();

        if (!"super_admin".equalsIgnoreCase(role)) {
            Long targetCommunityId = communityId != null ? communityId : adminCommunityId;
            if (adminCommunityId == null) {
                throw new RuntimeException("管理员未绑定社区，无法发布公告");
            }
            if (targetCommunityId == null || !adminCommunityId.equals(targetCommunityId)) {
                throw new RuntimeException("无权向其他社区发布公告");
            }
            notice.setTargetType("COMMUNITY");
            notice.setCommunityId(targetCommunityId);
            Community c = communityMapper.selectById(targetCommunityId);
            if (c != null) {
                notice.setCommunityName(c.getName());
            }
        } else {
            if (communityId != null) {
                notice.setTargetType("COMMUNITY");
                notice.setCommunityId(communityId);
                Community c = communityMapper.selectById(communityId);
                if (c != null) {
                    notice.setCommunityName(c.getName());
                }
            }
        }

        notice.setPublishStatus("PUBLISHED");
        notice.setPublishTime(LocalDateTime.now());
        updateById(notice);
    }

    @Override
    @Transactional
    public void offlineNotice(Long id, Long adminId) {
        SysNotice notice = noticeMapper.selectById(id);
        if (notice == null || isDeleted(notice)) {
            throw new RuntimeException("公告不存在");
        }
        String role = UserContext.getRole();
        Long adminCommunityId = UserContext.getCommunityId();
        if (!"super_admin".equalsIgnoreCase(role)) {
            if (notice.getCommunityId() == null || adminCommunityId == null || !adminCommunityId.equals(notice.getCommunityId())) {
                throw new RuntimeException("无权操作其他社区公告");
            }
        }
        notice.setPublishStatus("OFFLINE");
        updateById(notice);
    }

    @Override
    @Transactional
    public void batchDelete(List<Long> noticeIds, Long adminId) {
        if (noticeIds == null || noticeIds.isEmpty()) {
            throw new RuntimeException("公告ID列表不能为空");
        }
        String role = UserContext.getRole();
        Long adminCommunityId = UserContext.getCommunityId();
        if (!"super_admin".equalsIgnoreCase(role)) {
            List<SysNotice> list = noticeMapper.selectBatchIds(noticeIds);
            for (SysNotice n : list) {
                if (n != null) {
                    if (n.getCommunityId() == null || adminCommunityId == null || !adminCommunityId.equals(n.getCommunityId())) {
                        throw new RuntimeException("存在非本社区公告，无法批量删除");
                    }
                }
            }
        }
        noticeMapper.deleteBatchIds(noticeIds);
    }

    @Override
    @Transactional
    public void batchOffline(List<Long> noticeIds, Long adminId) {
        if (noticeIds == null || noticeIds.isEmpty()) {
            throw new RuntimeException("公告ID列表不能为空");
        }
        List<SysNotice> notices = noticeMapper.selectBatchIds(noticeIds);
        for (SysNotice n : notices) {
            if (n != null && isNotDeleted(n)) {
                String role = UserContext.getRole();
                Long adminCommunityId = UserContext.getCommunityId();
                if (!"super_admin".equalsIgnoreCase(role)) {
                    if (n.getCommunityId() == null || adminCommunityId == null || !adminCommunityId.equals(n.getCommunityId())) {
                        throw new RuntimeException("存在非本社区公告，无法下架");
                    }
                }
                n.setPublishStatus("OFFLINE");
                updateById(n);
            }
        }
    }

    @Override
    public NoticeReadStatDTO getReadStat(Long noticeId) {
        SysNotice notice = noticeMapper.selectById(noticeId);
        if (notice == null || isDeleted(notice)) {
            throw new RuntimeException("公告不存在");
        }
        int totalUsers = countTargetUsers(notice);
        Long readCountLong = noticeReadMapper.selectCount(new QueryWrapper<SysNoticeRead>()
                .eq("notice_id", noticeId)
                .eq("status", "READ"));
        int readCount = readCountLong == null ? 0 : readCountLong.intValue();
        int unreadCount = Math.max(totalUsers - readCount, 0);
        double rate = totalUsers == 0 ? 0.0 : Math.round((readCount * 10000.0 / totalUsers)) / 100.0;
        NoticeReadStatDTO dto = new NoticeReadStatDTO();
        dto.setNoticeId(noticeId);
        dto.setTitle(notice.getTitle());
        dto.setTotalUsers(totalUsers);
        dto.setReadCount(readCount);
        dto.setUnreadCount(unreadCount);
        dto.setReadRate(rate);
        return dto;
    }

    private int countTargetUsers(SysNotice notice) {
        QueryWrapper<UserHouse> query = new QueryWrapper<>();
        query.eq("status", "审核通过");
        String target = notice.getTargetType();
        if ("COMMUNITY".equalsIgnoreCase(target) || "BUILDING".equalsIgnoreCase(target)) {
            StringBuilder sql = new StringBuilder("SELECT id FROM sys_house WHERE 1=1");
            if ("COMMUNITY".equalsIgnoreCase(target) && notice.getCommunityName() != null) {
                sql.append(" AND community_name = '").append(notice.getCommunityName()).append("'");
            }
            if ("BUILDING".equalsIgnoreCase(target)) {
                if (notice.getCommunityName() != null) {
                    sql.append(" AND community_name = '").append(notice.getCommunityName()).append("'");
                }
                if (notice.getBuildingNo() != null) {
                    sql.append(" AND building_no = '").append(notice.getBuildingNo()).append("'");
                }
            }
            query.inSql("house_id", sql.toString());
        }
        List<UserHouse> list = userHouseMapper.selectList(query);
        return (int) list.stream().map(UserHouse::getUserId).distinct().count();
    }
}
