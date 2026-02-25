package com.lsx.core.notice.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lsx.core.common.Result.Result;
import com.lsx.core.common.Util.UserContext;
import com.lsx.core.notice.dto.*;
import com.lsx.core.notice.entity.SysNotice;
import com.lsx.core.notice.service.ISysNoticeService;
import com.lsx.core.notice.service.NoticeService;
import com.lsx.core.user.entity.User;
import com.lsx.core.user.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lsx.core.notice.mapper.SysNoticeMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notice")
@Slf4j
@Tag(name = "物业公告接口", description = "公告发布、查询、已读、删除相关接口")
public class NoticeController {

    @Autowired
    private NoticeService noticeService;
    @Autowired
    private ISysNoticeService sysNoticeService;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private SysNoticeMapper noticeMapper;


    @PostMapping
    @Operation(summary = "管理员发布公告", description = "支持指定小区/楼栋或全体业主")
    public Result<Long> createNotice(@RequestBody NoticeCreateDTO dto,
                                     @Parameter(description = "管理员ID", required = true) @RequestParam("adminId") Long adminId) {
        try {
            Long noticeId = noticeService.createNotice(dto, adminId);
            return Result.success(noticeId);
        } catch (Exception e) {
            log.error("发布公告失败", e);
            return Result.fail("发布失败：" + e.getMessage());
        }
    }

    @GetMapping("/admin/list")
    @Operation(summary = "后台公告管理列表")
    public Result<Page<AdminNoticeListItemDTO>> adminList(
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "publishStatus", required = false) String publishStatus,
            @RequestParam(value = "topFlag", required = false) Boolean topFlag) {
        try {
            Page<SysNotice> page = new Page<>(pageNum, pageSize);
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            QueryWrapper<SysNotice> qw = new QueryWrapper<>();
            if (title != null && !title.trim().isEmpty()) {
                qw.like("title", title.trim());
            }
            if (publishStatus != null && !publishStatus.trim().isEmpty()) {
                qw.eq("publish_status", publishStatus.trim());
            }
            if (topFlag != null) {
                qw.eq("top_flag", topFlag ? 1 : 0);
            }
            String role = UserContext.getRole();
            Long communityId = UserContext.getCommunityId();
            if (!"super_admin".equalsIgnoreCase(role)) {
                if (communityId != null) {
                    qw.eq("community_id", communityId);
                } else {
                    qw.eq("id", -1L);
                }
            }
            qw.eq("deleted", 0).orderByDesc("top_flag").orderByDesc("publish_time").orderByDesc("create_time");
            Page<SysNotice> noticePage = noticeMapper.selectPage(page, qw);
            List<AdminNoticeListItemDTO> records = noticePage.getRecords().stream().map(n -> {
                AdminNoticeListItemDTO dto = new AdminNoticeListItemDTO();
                dto.setId(n.getId());
                dto.setTitle(n.getTitle());
                dto.setPublishStatus(n.getPublishStatus());
                dto.setTopFlag(n.getTopFlag());
                dto.setTargetType(n.getTargetType());
                dto.setCommunityName(n.getCommunityName());
                dto.setBuildingNo(n.getBuildingNo());
                dto.setPublishTime(n.getPublishTime() == null ? null : n.getPublishTime().format(fmt));
                dto.setExpireTime(n.getExpireTime() == null ? null : n.getExpireTime().format(fmt));
                User creator = n.getCreatorId() == null ? null : userMapper.selectById(n.getCreatorId());
                dto.setCreatorName(creator == null ? null : creator.getRealName());
                NoticeReadStatDTO stat = noticeService.getReadStat(n.getId());
                int readCount = stat.getReadCount();
                int total = stat.getTotalUsers();
                dto.setReadCount(readCount);
                dto.setTotalCount(total);
                return dto;
            }).collect(Collectors.toList());
            Page<AdminNoticeListItemDTO> result = new Page<>(pageNum, pageSize);
            result.setRecords(records);
            result.setTotal(noticePage.getTotal());
            result.setPages(noticePage.getPages());
            result.setCurrent(noticePage.getCurrent());
            return Result.success(result);
        } catch (Exception e) {
            log.error("后台公告列表查询失败", e);
            return Result.fail("查询失败：" + e.getMessage());
        }
    }

    @PutMapping("/{id}/publish")
    @Operation(summary = "发布公告")
    public Result<Long> publish(@PathVariable("id") Long id,
                                @RequestParam("adminId") Long adminId,
                                @RequestParam(value = "communityId", required = false) Long communityId) {
        try {
            noticeService.publishNotice(id, adminId, communityId);
            return Result.success(id);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("发布公告异常", e);
            return Result.fail("发布失败，请稍后再试");
        }
    }

    @PutMapping("/{id}/offline")
    @Operation(summary = "下架公告")
    public Result<String> offline(@PathVariable("id") Long id,
                                  @RequestParam("adminId") Long adminId) {
        try {
            noticeService.offlineNotice(id, adminId);
            return Result.success("下架成功");
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("下架公告异常", e);
            return Result.fail("下架失败，请稍后再试");
        }
    }

    @PostMapping("/batch/delete")
    @Operation(summary = "批量删除公告")
    public Result<String> batchDelete(@RequestBody IdListDTO body,
                                      @RequestParam("adminId") Long adminId) {
        try {
            noticeService.batchDelete(body.getNoticeIds(), adminId);
            return Result.success("批量删除成功");
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("批量删除公告异常", e);
            return Result.fail("批量删除失败，请稍后再试");
        }
    }

    @PostMapping("/batch/offline")
    @Operation(summary = "批量下架公告")
    public Result<String> batchOffline(@RequestBody IdListDTO body,
                                       @RequestParam("adminId") Long adminId) {
        try {
            noticeService.batchOffline(body.getNoticeIds(), adminId);
            return Result.success("批量下架成功");
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("批量下架公告异常", e);
            return Result.fail("批量下架失败，请稍后再试");
        }
    }

    @GetMapping("/{id}/read-stat")
    @Operation(summary = "公告阅读统计")
    public Result<NoticeReadStatDTO> readStat(@PathVariable("id") Long id) {
        try {
            NoticeReadStatDTO dto = noticeService.getReadStat(id);
            return Result.success(dto);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("查询公告阅读统计异常", e);
            return Result.fail("查询失败，请稍后再试");
        }
    }
    // 修改Controller方法
    @GetMapping("/list")
    @Operation(summary = "业主查询公告", description = "默认时间倒序，未读优先")
    public Result<Page<NoticeListDTO>> listNotices(
            @Parameter(description = "业主ID", required = true) @RequestParam("userId") Long userId,
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        try {
            Page<NoticeVO> result = noticeService.listNotices(userId, pageNum, pageSize);

            // 转换为前端需要的DTO格式
            List<NoticeListDTO> dtoList = result.getRecords().stream()
                    .map(vo -> {
                        NoticeListDTO dto = new NoticeListDTO();
                        dto.setId(vo.getId());
                        dto.setTitle(vo.getTitle());
                        dto.setReadFlag(vo.getRead() ? 1 : 0); // 布尔值转数字
                        dto.setTargetType(vo.getTargetType());
                        dto.setContent(vo.getContent());

                        // 格式化时间为yyyy-MM-dd HH:mm:ss
                        if (vo.getPublishTime() != null) {
                            dto.setPublishTime(vo.getPublishTime()
                                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                        }

                        return dto;
                    })
                    .collect(Collectors.toList());

            // 构建新的分页结果
            Page<NoticeListDTO> pageResult = new Page<>(pageNum, pageSize);
            pageResult.setRecords(dtoList);
            pageResult.setTotal(result.getTotal());
            pageResult.setPages(result.getPages());

            return Result.success(pageResult);
        } catch (Exception e) {
            log.error("查询公告失败", e);
            return Result.fail("查询失败：" + e.getMessage());
        }
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "业主标记公告已读")
    public Result<String> markNoticeRead(@PathVariable("id") Long noticeId,
                                         HttpServletRequest request) {
        try {
            Long userId = UserContext.getCurrentUserId();
            if (userId == null) {
                return Result.fail("未登录");
            }

            noticeService.markAsRead(noticeId, userId);
            return Result.success("标记成功");

        } catch (RuntimeException e) {
            log.warn("标记已读失败：{}", e.getMessage());
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("标记公告已读异常", e);
            return Result.fail("标记失败，请稍后再试");
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "管理员删除公告")
    public Result<String> deleteNotice(@PathVariable("id") Long noticeId,
                                       @Parameter(description = "管理员ID", required = true) @RequestParam("adminId") Long adminId) {
        try {
            noticeService.deleteNotice(noticeId, adminId);
            return Result.success("删除成功");
        } catch (RuntimeException e) {
            log.warn("删除公告失败：{}", e.getMessage());
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("删除公告异常", e);
            return Result.fail("删除失败，请稍后再试");
        }
    }
    @PostMapping("/expire/set")
    @Operation(summary = "设置单个公告过期时间", description = "管理员可以设置公告的过期时间")
    public Result<String> setNoticeExpire(@Validated @RequestBody NoticeExpireDTO dto,
                                          @Parameter(description = "管理员ID", required = true)
                                          @RequestParam("adminId") Long adminId) {
        try {
            // 这里可以添加管理员权限验证
            sysNoticeService.setNoticeExpire(dto);
            log.info("管理员[{}]设置了公告[{}]的过期时间", adminId, dto.getNoticeId());
            return Result.success("设置成功");
        } catch (RuntimeException e) {
            log.warn("设置过期时间失败：{}", e.getMessage());
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("设置公告过期时间异常", e);
            return Result.fail("设置失败，请稍后再试");
        }
    }

    @PostMapping("/expire/batch-set")
    @Operation(summary = "批量设置公告过期时间", description = "管理员批量设置公告过期时间")
    public Result<String> batchSetNoticeExpire(@Validated @RequestBody BatchNoticeExpireDTO dto,
                                               @Parameter(description = "管理员ID", required = true)
                                               @RequestParam("adminId") Long adminId) {
        try {
            sysNoticeService.batchSetNoticeExpire(dto);
            log.info("管理员[{}]批量设置了{}条公告的过期时间", adminId, dto.getNoticeIds().size());
            return Result.success("批量设置成功");
        } catch (RuntimeException e) {
            log.warn("批量设置过期时间失败：{}", e.getMessage());
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("批量设置公告过期时间异常", e);
            return Result.fail("批量设置失败，请稍后再试");
        }
    }

    @PostMapping("/expire/clear/{noticeId}")
    @Operation(summary = "清除公告过期时间（永不过期）", description = "将公告设置为永不过期")
    public Result<String> clearNoticeExpire(@PathVariable("noticeId") Long noticeId,
                                            @Parameter(description = "管理员ID", required = true)
                                            @RequestParam("adminId") Long adminId) {
        try {
            sysNoticeService.clearNoticeExpire(noticeId);
            log.info("管理员[{}]清除了公告[{}]的过期时间", adminId, noticeId);
            return Result.success("清除成功");
        } catch (RuntimeException e) {
            log.warn("清除过期时间失败：{}", e.getMessage());
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("清除公告过期时间异常", e);
            return Result.fail("清除失败，请稍后再试");
        }
    }

    @PostMapping("/expire/extend/{noticeId}")
    @Operation(summary = "延长公告过期时间", description = "在原有基础上延长指定天数")
    public Result<String> extendNoticeExpire(@PathVariable("noticeId") Long noticeId,
                                             @Parameter(description = "延长的天数", required = true)
                                             @RequestParam("days") Integer days,
                                             @Parameter(description = "管理员ID", required = true)
                                             @RequestParam("adminId") Long adminId) {
        try {
            if (days == null || days <= 0) {
                return Result.fail("延长时间必须大于0天");
            }

            sysNoticeService.extendNoticeExpire(noticeId, days);
            log.info("管理员[{}]将公告[{}]过期时间延长了{}天", adminId, noticeId, days);
            return Result.success("延长成功");
        } catch (RuntimeException e) {
            log.warn("延长过期时间失败：{}", e.getMessage());
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("延长公告过期时间异常", e);
            return Result.fail("延长失败，请稍后再试");
        }
    }

    @PostMapping("/expire/batch-extend")
    @Operation(summary = "批量延长公告过期时间", description = "批量延长多个公告的过期时间")
    public Result<String> batchExtendNoticeExpire(
            @Parameter(description = "公告ID列表，用逗号分隔", required = true)
            @RequestParam("noticeIds") List<Long> noticeIds,
            @Parameter(description = "延长的天数", required = true)
            @RequestParam("days") Integer days,
            @Parameter(description = "管理员ID", required = true)
            @RequestParam("adminId") Long adminId) {
        try {
            if (noticeIds == null || noticeIds.isEmpty()) {
                return Result.fail("公告ID列表不能为空");
            }

            if (days == null || days <= 0) {
                return Result.fail("延长时间必须大于0天");
            }

            sysNoticeService.batchExtendNoticeExpire(noticeIds, days);
            log.info("管理员[{}]批量延长了{}条公告的过期时间，延长时间：{}天", adminId, noticeIds.size(), days);
            return Result.success("批量延长成功");
        } catch (RuntimeException e) {
            log.warn("批量延长过期时间失败：{}", e.getMessage());
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("批量延长公告过期时间异常", e);
            return Result.fail("批量延长失败，请稍后再试");
        }
    }

    @GetMapping("/expire/expiring-soon")
    @Operation(summary = "查询即将过期的公告", description = "查询在指定天数内即将过期的公告")
    public Result<List<ExpiringNoticeDTO>> getExpiringSoonNotices(
            @Parameter(description = "过期天数阈值，默认7天")
            @RequestParam(value = "days", defaultValue = "7") Integer days,
            @Parameter(description = "管理员ID", required = true)
            @RequestParam("adminId") Long adminId) {
        try {
            List<ExpiringNoticeDTO> noticeIds = sysNoticeService.getExpiringSoonNotices(days);
            log.info("管理员[{}]查询到{}条即将在{}天内过期的公告", adminId, noticeIds.size(), days);
            return Result.success(noticeIds);
        } catch (Exception e) {
            log.error("查询即将过期公告异常", e);
            return Result.fail("查询失败，请稍后再试");
        }
    }

    @GetMapping("/detail/{id}")
    @Operation(summary = "公告详情")
    public Result<SysNotice> getNoticeDetail(@PathVariable Long id) {
        SysNotice notice = noticeService.getById(id);
        if (notice == null) {
            return Result.fail("公告不存在");
        }
        return Result.success(notice);
    }

    @PutMapping("/{id}")
    @Operation(summary = "管理员修改公告")
    public Result<String> updateNotice(@PathVariable("id") Long id,
                                       @RequestBody NoticeCreateDTO dto,
                                       @RequestParam("adminId") Long adminId) {
        try {
            // 需要在 Service 层实现 updateNotice 方法
            noticeService.updateNotice(id, dto, adminId);
            return Result.success("修改成功");
        } catch (Exception e) {
            log.error("修改公告失败", e);
            return Result.fail("修改失败：" + e.getMessage());
        }
    }

}

