package com.lsx.core.topic.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lsx.core.common.Result.Result;
import com.lsx.core.topic.dto.TopicAuditDTO;
import com.lsx.core.topic.dto.TopicCommentDTO;
import com.lsx.core.topic.dto.TopicCreateDTO;
import com.lsx.core.topic.service.TopicService;
import com.lsx.core.topic.vo.TopicCommentVO;
import com.lsx.core.topic.vo.TopicVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/topic")
@Tag(name = "社区话题接口", description = "话题发布、查询、评论、点赞、审核")
@Slf4j
public class TopicController {

    @Autowired
    private TopicService topicService;

    @PostMapping
    @Operation(summary = "创建话题", description = "用户发布话题，状态为待审核")
    public Result<Long> createTopic(@RequestBody TopicCreateDTO dto) {
        try {
            Long topicId = topicService.createTopic(dto);
            return Result.success(topicId);
        } catch (RuntimeException e) {
            log.warn("创建话题失败：{}", e.getMessage());
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("创建话题异常", e);
            return Result.fail("创建失败，请稍后再试");
        }
    }

    @GetMapping("/list")
    @Operation(summary = "话题列表", description = "分页查询话题，默认只返回已审核通过的")
    public Result<Page<TopicVO>> listTopics(
            @Parameter(description = "页码", required = false) @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页大小", required = false) @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
            @Parameter(description = "状态筛选（PENDING/APPROVED/REJECTED）", required = false) @RequestParam(value = "status", required = false) String status) {
        try {
            Page<TopicVO> result = topicService.pageTopics(pageNum, pageSize, status);
            return Result.success(result);
        } catch (Exception e) {
            log.error("查询话题列表失败", e);
            return Result.fail("查询失败，请稍后再试");
        }
    }



    @PutMapping("/{id}/like")
    @Operation(summary = "点赞话题", description = "点赞话题，支持幂等操作")
    public Result<Boolean> likeTopic(@PathVariable("id") Long topicId,
                                     @Parameter(description = "用户ID", required = true) @RequestParam("userId") Long userId) {
        try {
            Boolean success = topicService.likeTopic(topicId, userId);
            return Result.success(success);
        } catch (RuntimeException e) {
            log.warn("点赞失败：{}", e.getMessage());
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("点赞异常", e);
            return Result.fail("点赞失败，请稍后再试");
        }
    }

    @PutMapping("/{id}/audit")
    @Operation(summary = "审核话题", description = "管理员审核话题，仅管理员可操作")
    public Result<Boolean> auditTopic(@PathVariable("id") Long topicId,
                                      @RequestBody TopicAuditDTO dto) {
        try {
            Boolean success = topicService.auditTopic(topicId, dto);
            return Result.success(success);
        } catch (RuntimeException e) {
            log.warn("审核失败：{}", e.getMessage());
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("审核异常", e);
            return Result.fail("审核失败，请稍后再试");
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "话题详情", description = "查看话题详情内容")
    public Result<TopicVO> getTopicDetail(@PathVariable("id") Long id) {
        try {
            return Result.success(topicService.getTopicDetail(id));
        } catch (Exception e) {
            log.error("获取话题详情失败", e);
            return Result.fail(e.getMessage());
        }
    }
    @PostMapping("/{id}/comment")
    @Operation(summary = "评论话题", description = "对已审核通过的话题进行评论或回复")
    public Result<Long> addComment(@PathVariable("id") Long topicId,
                                   @RequestBody TopicCommentDTO dto) {
        try {
            Long commentId = topicService.addComment(topicId, dto);
            return Result.success(commentId);
        } catch (Exception e) {
            log.error("评论异常", e);
            return Result.fail("评论失败，请稍后再试");
        }
    }

    @GetMapping("/{id}/comments")
    @Operation(summary = "话题评论列表", description = "查看话题评论（含楼中楼）")
    public Result<List<TopicCommentVO>> listComments(@PathVariable("id") Long topicId) {
        try {
            List<TopicCommentVO> comments = topicService.listComments(topicId);
            return Result.success(comments);
        } catch (Exception e) {
            log.error("查询评论失败", e);
            return Result.fail(e.getMessage());
        }
    }
}

