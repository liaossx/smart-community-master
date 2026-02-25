package com.lsx.core.group.controller;

import com.lsx.core.common.Result.Result;
import com.lsx.core.group.dto.GroupCreateDTO;
import com.lsx.core.group.dto.GroupFinishDTO;
import com.lsx.core.group.dto.GroupJoinDTO;
import com.lsx.core.group.service.GroupService;
import com.lsx.core.group.vo.GroupVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/group")
@Tag(name = "拼团活动接口", description = "拼团发起、参团、查询进度、完成")
@Slf4j
public class GroupController {

    @Autowired
    private GroupService groupService;

    @PostMapping
    @Operation(summary = "发起拼团", description = "用户发起拼团活动")
    public Result<Long> createGroup(@RequestBody GroupCreateDTO dto) {
        try {
            Long groupId = groupService.createGroup(dto);
            return Result.success(groupId);
        } catch (RuntimeException e) {
            log.warn("发起拼团失败：{}", e.getMessage());
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("发起拼团异常", e);
            return Result.fail("发起失败，请稍后再试");
        }
    }

    @PostMapping("/{id}/join")
    @Operation(summary = "加入拼团", description = "用户参与拼团，支持幂等操作")
    public Result<Boolean> joinGroup(@PathVariable("id") Long groupId,
                                     @RequestBody GroupJoinDTO dto) {
        try {
            Boolean success = groupService.joinGroup(groupId, dto);
            return Result.success(success);
        } catch (RuntimeException e) {
            log.warn("加入拼团失败：{}", e.getMessage());
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("加入拼团异常", e);
            return Result.fail("加入失败，请稍后再试");
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询拼团进度", description = "查询拼团详情，包括成员列表和当前状态")
    public Result<GroupVO> getGroupDetail(@PathVariable("id") Long groupId) {
        try {
            GroupVO result = groupService.getGroupDetail(groupId);
            return Result.success(result);
        } catch (RuntimeException e) {
            log.warn("查询拼团失败：{}", e.getMessage());
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("查询拼团异常", e);
            return Result.fail("查询失败，请稍后再试");
        }
    }

    @PutMapping("/{id}/finish")
    @Operation(summary = "完成拼团", description = "手动结束拼团（成功或失败），可结合定时任务自动处理")
    public Result<Boolean> finishGroup(@PathVariable("id") Long groupId,
                                       @RequestBody GroupFinishDTO dto) {
        try {
            Boolean success = groupService.finishGroup(groupId, dto);
            return Result.success(success);
        } catch (RuntimeException e) {
            log.warn("完成拼团失败：{}", e.getMessage());
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("完成拼团异常", e);
            return Result.fail("操作失败，请稍后再试");
        }
    }
}

