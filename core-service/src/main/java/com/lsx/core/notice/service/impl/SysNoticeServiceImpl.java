package com.lsx.core.notice.service.impl;

import com.alibaba.nacos.client.naming.utils.CollectionUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.lsx.core.notice.dto.BatchNoticeExpireDTO;
import com.lsx.core.notice.dto.ExpiringNoticeDTO;
import com.lsx.core.notice.dto.NoticeExpireDTO;
import com.lsx.core.notice.entity.SysNotice;
import com.lsx.core.notice.mapper.SysNoticeMapper;
import com.lsx.core.notice.service.ISysNoticeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
@Slf4j
@Service
public class SysNoticeServiceImpl implements ISysNoticeService {

    @Autowired
    private SysNoticeMapper noticeMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setNoticeExpire(NoticeExpireDTO dto) {
        // 验证公告是否存在且未删除
        SysNotice notice = noticeMapper.selectById(dto.getNoticeId());
        if (notice == null || (notice.getDeleted() != null && notice.getDeleted() == 1)) {
            throw new RuntimeException("公告不存在或已被删除");
        }

        // 计算过期时间
        LocalDateTime expireTime = calculateExpireTime(dto);

        // 更新数据库
        SysNotice updateNotice = new SysNotice();
        updateNotice.setId(dto.getNoticeId());
        updateNotice.setExpireTime(expireTime);
        updateNotice.setUpdateTime(LocalDateTime.now());

        int result = noticeMapper.updateById(updateNotice);
        if (result == 0) {
            throw new RuntimeException("设置过期时间失败");
        }

        log.info("设置公告过期时间成功, noticeId: {}, expireTime: {}", dto.getNoticeId(), expireTime);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchSetNoticeExpire(BatchNoticeExpireDTO dto) {
        if (CollectionUtils.isEmpty(dto.getNoticeIds())) {
            throw new RuntimeException("公告ID列表不能为空");
        }

        // 验证所有公告是否存在且未删除
        List<SysNotice> notices = noticeMapper.selectList(
                new QueryWrapper<SysNotice>()
                        .in("id", dto.getNoticeIds())
                        .ne("deleted", 1)  // 不等于1就是未删除
        );

        if (notices.size() != dto.getNoticeIds().size()) {
            // 找出不存在的ID
            List<Long> foundIds = notices.stream()
                    .map(SysNotice::getId)
                    .collect(Collectors.toList());
            List<Long> notFoundIds = dto.getNoticeIds().stream()
                    .filter(id -> !foundIds.contains(id))
                    .collect(Collectors.toList());
            throw new RuntimeException("以下公告不存在或已被删除: " + notFoundIds);
        }

        // 计算过期时间
        LocalDateTime expireTime = calculateBatchExpireTime(dto);

        // 批量更新
        SysNotice updateNotice = new SysNotice();
        updateNotice.setExpireTime(expireTime);
        updateNotice.setUpdateTime(LocalDateTime.now());

        UpdateWrapper<SysNotice> wrapper = new UpdateWrapper<>();
        wrapper.in("id", dto.getNoticeIds())
                .ne("deleted", 1);  // 更新未删除的记录

        int result = noticeMapper.update(updateNotice, wrapper);

        log.info("批量设置公告过期时间成功, noticeIds: {}, count: {}, expireTime: {}",
                dto.getNoticeIds(), result, expireTime);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clearNoticeExpire(Long noticeId) {
        SysNotice notice = noticeMapper.selectById(noticeId);
        if (notice == null || (notice.getDeleted() != null && notice.getDeleted() == 1)) {
            throw new RuntimeException("公告不存在或已被删除");
        }

        // 使用UpdateWrapper明确指定要更新的字段
        UpdateWrapper<SysNotice> wrapper = new UpdateWrapper<>();
        wrapper.eq("id", noticeId)
                .eq("deleted", 0)
                .set("expire_time", null)  // 明确设置expire_time为NULL
                .set("update_time", LocalDateTime.now());

        int result = noticeMapper.update(null, wrapper);
        if (result == 0) {
            throw new RuntimeException("清除过期时间失败");
        }

        log.info("清除公告过期时间成功, noticeId: {}", noticeId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void extendNoticeExpire(Long noticeId, Integer days) {
        if (days == null || days <= 0) {
            throw new RuntimeException("延长时间必须大于0天");
        }

        SysNotice notice = noticeMapper.selectById(noticeId);
        if (notice == null || (notice.getDeleted() != null && notice.getDeleted() == 1)) {
            throw new RuntimeException("公告不存在或已被删除");
        }

        LocalDateTime newExpireTime;
        if (notice.getExpireTime() == null) {
            // 如果原来永不过期，就基于当前时间计算
            newExpireTime = LocalDateTime.now().plusDays(days);
        } else if (notice.getExpireTime().isBefore(LocalDateTime.now())) {
            // 如果已过期，就从当前时间开始计算
            newExpireTime = LocalDateTime.now().plusDays(days);
        } else {
            // 如果未过期，就延长指定天数
            newExpireTime = notice.getExpireTime().plusDays(days);
        }

        SysNotice updateNotice = new SysNotice();
        updateNotice.setId(noticeId);
        updateNotice.setExpireTime(newExpireTime);
        updateNotice.setUpdateTime(LocalDateTime.now());

        int result = noticeMapper.updateById(updateNotice);
        if (result == 0) {
            throw new RuntimeException("延长过期时间失败");
        }

        log.info("延长公告过期时间成功, noticeId: {}, days: {}, newExpireTime: {}",
                noticeId, days, newExpireTime);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchExtendNoticeExpire(List<Long> noticeIds, Integer days) {
        if (CollectionUtils.isEmpty(noticeIds)) {
            throw new RuntimeException("公告ID列表不能为空");
        }

        if (days == null || days <= 0) {
            throw new RuntimeException("延长时间必须大于0天");
        }

        int successCount = 0;
        int failCount = 0;
        List<Long> failedIds = new java.util.ArrayList<>();

        for (Long noticeId : noticeIds) {
            try {
                extendNoticeExpire(noticeId, days);
                successCount++;
            } catch (Exception e) {
                log.warn("延长公告过期时间失败, noticeId: {}, error: {}", noticeId, e.getMessage());
                failCount++;
                failedIds.add(noticeId);
            }
        }

        if (failCount > 0) {
            log.warn("批量延长公告过期时间部分失败, total: {}, success: {}, fail: {}, failedIds: {}",
                    noticeIds.size(), successCount, failCount, failedIds);
        } else {
            log.info("批量延长公告过期时间全部成功, total: {}, days: {}", noticeIds.size(), days);
        }
    }

    @Override
    public List<ExpiringNoticeDTO> getExpiringSoonNotices(Integer daysThreshold) {
        if (daysThreshold == null || daysThreshold <= 0) {
            daysThreshold = 7; // 默认7天内过期
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime thresholdTime = now.plusDays(daysThreshold);

        // 查询即将过期的公告
        List<SysNotice> notices = noticeMapper.selectList(
                new QueryWrapper<SysNotice>()
                        .eq("deleted", 0)
                        .eq("publish_status", "PUBLISHED")
                        .isNotNull("expire_time")
                        .gt("expire_time", now) // 未过期
                        .le("expire_time", thresholdTime) // 即将过期
                        .orderByAsc("expire_time") // 按过期时间升序（最近过期的在前）
        );

        // 转换为DTO，计算剩余天数
        return notices.stream()
                .map(notice -> {
                    ExpiringNoticeDTO dto = new ExpiringNoticeDTO();
                    dto.setId(notice.getId());
                    dto.setTitle(notice.getTitle());
                    dto.setTargetType(notice.getTargetType());
                    dto.setPublishTime(notice.getPublishTime());
                    dto.setExpireTime(notice.getExpireTime());
                    dto.setTopFlag(notice.getTopFlag());

                    // 计算剩余天数
                    if (notice.getExpireTime() != null) {
                        long daysLeft = java.time.Duration.between(now, notice.getExpireTime()).toDays();
                        dto.setDaysLeft(daysLeft);
                    }

                    return dto;
                })
                .collect(Collectors.toList());
    }
    /**
     * 计算过期时间 - 单个公告
     */
    private LocalDateTime calculateExpireTime(NoticeExpireDTO dto) {
        switch (dto.getExpireType()) {
            case NEVER:
                return null;

            case CUSTOM:
                if (dto.getCustomExpireTime() == null) {
                    throw new RuntimeException("自定义过期时间不能为空");
                }
                if (dto.getCustomExpireTime().isBefore(LocalDateTime.now())) {
                    throw new RuntimeException("过期时间不能早于当前时间");
                }
                return dto.getCustomExpireTime();

            case DAYS_7:
                return LocalDateTime.now().plusDays(7);

            case DAYS_30:
                return LocalDateTime.now().plusDays(30);

            case MONTH_3:
                return LocalDateTime.now().plusMonths(3);

            default:
                // 如果有days参数，使用days参数
                if (dto.getDays() != null && dto.getDays() > 0) {
                    return LocalDateTime.now().plusDays(dto.getDays());
                }
                throw new RuntimeException("不支持的过期时间类型: " + dto.getExpireType());
        }
    }

    /**
     * 计算过期时间 - 批量设置
     */
    private LocalDateTime calculateBatchExpireTime(BatchNoticeExpireDTO dto) {
        NoticeExpireDTO singleDto = new NoticeExpireDTO();
        singleDto.setExpireType(dto.getExpireType());
        singleDto.setCustomExpireTime(dto.getCustomExpireTime());
        singleDto.setDays(dto.getDays());

        return calculateExpireTime(singleDto);
    }

    /**
     * 辅助方法：判断公告是否有效
     */
    private boolean isValidNotice(SysNotice notice) {
        return notice != null &&
                (notice.getDeleted() == null || notice.getDeleted() == 0);
    }
}