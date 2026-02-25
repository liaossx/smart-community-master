package com.lsx.core.notice.service;

import com.lsx.core.notice.dto.BatchNoticeExpireDTO;
import com.lsx.core.notice.dto.ExpiringNoticeDTO;
import com.lsx.core.notice.dto.NoticeExpireDTO;

import java.util.List;

public interface ISysNoticeService {

    /**
     * 设置公告过期时间
     */
    void setNoticeExpire(NoticeExpireDTO dto);

    /**
     * 批量设置公告过期时间
     */
    void batchSetNoticeExpire(BatchNoticeExpireDTO dto);

    /**
     * 清除过期时间（设为永不过期）
     */
    void clearNoticeExpire(Long noticeId);

    /**
     * 延长公告过期时间
     */
    void extendNoticeExpire(Long noticeId, Integer days);

    /**
     * 批量延长公告过期时间
     */
    void batchExtendNoticeExpire(List<Long> noticeIds, Integer days);

    /**
     * 查询即将过期的公告
     */
    List<ExpiringNoticeDTO> getExpiringSoonNotices(Integer daysThreshold);
}