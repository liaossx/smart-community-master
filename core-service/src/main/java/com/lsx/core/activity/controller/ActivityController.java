package com.lsx.core.activity.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lsx.core.activity.entity.SysActivity;
import com.lsx.core.activity.service.ActivityService;
import com.lsx.core.common.Result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/activity")
@Tag(name = "社区活动接口")
public class ActivityController {

    @Autowired
    private ActivityService activityService;

    @GetMapping("/list")
    @Operation(summary = "活动列表")
    public Result<IPage<SysActivity>> list(@RequestParam(value = "status", required = false) String status,
                                           @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
                                           @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        IPage<SysActivity> page = activityService.list(status, pageNum, pageSize);
        return Result.success(page);
    }

    @GetMapping("/{id}")
    @Operation(summary = "活动详情")
    public Result<SysActivity> detail(@PathVariable("id") Long id) {
        SysActivity a = activityService.detail(id);
        if (a == null) return Result.fail("活动不存在");
        return Result.success(a);
    }

    @PostMapping("/join")
    @Operation(summary = "报名活动")
    public Result<Boolean> join(@RequestParam("activityId") Long activityId,
                                @RequestParam("userId") Long userId) {
        boolean ok = activityService.join(activityId, userId);
        return ok ? Result.success(true) : Result.fail("报名失败");
    }

    @PostMapping("/publish")
    @Operation(summary = "发布活动")
    public Result<Long> publish(@RequestBody SysActivity body) {
        Long id = activityService.publish(body);
        return Result.success(id);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除活动")
    public Result<Boolean> delete(@PathVariable("id") Long id) {
        boolean ok = activityService.deleteByIdWithCheck(id);
        return ok ? Result.success(true) : Result.fail("删除失败");
    }
}
