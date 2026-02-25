package com.lsx.core.notice.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lsx.core.notice.dto.NoticeCreateDTO;
import com.lsx.core.notice.dto.NoticeVO;
import com.lsx.core.notice.dto.NoticeReadStatDTO;
import com.lsx.core.notice.entity.SysNotice;
import java.util.List;

public interface NoticeService extends IService<SysNotice> {

    Long createNotice(NoticeCreateDTO dto, Long adminId);

    Page<NoticeVO> listNotices(Long userId, Integer pageNum, Integer pageSize);

    void markAsRead(Long noticeId, Long userId);

    void deleteNotice(Long noticeId, Long adminId);

    SysNotice getById(Long id);

    void updateNotice(Long id, NoticeCreateDTO dto, Long adminId);

    void publishNotice(Long id, Long adminId);

    void publishNotice(Long id, Long adminId, Long communityId);

    void offlineNotice(Long id, Long adminId);

    void batchDelete(List<Long> noticeIds, Long adminId);

    void batchOffline(List<Long> noticeIds, Long adminId);

    NoticeReadStatDTO getReadStat(Long noticeId);
}

