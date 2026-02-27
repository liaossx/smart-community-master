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
        System.out.println("Received Activity: " + body);
        Long id = activityService.publish(body);
        return Result.success(id);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除活动")
    public Result<Boolean> delete(@PathVariable("id") Long id) {
        boolean ok = activityService.deleteByIdWithCheck(id);
        return ok ? Result.success(true) : Result.fail("删除失败");
    }

    @GetMapping("/signup/list")
    @Operation(summary = "活动报名列表")
    public Result<IPage<com.lsx.core.activity.dto.SignupRecordDTO>> signupList(
            @RequestParam("activityId") Long activityId,
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        IPage<com.lsx.core.activity.dto.SignupRecordDTO> page = activityService.getSignupList(activityId, pageNum, pageSize);
        return Result.success(page);
    }

    @PutMapping
    @Operation(summary = "修改活动")
    public Result<Boolean> update(@RequestBody SysActivity activity) {
        if (activity.getId() == null) {
            return Result.fail("活动ID不能为空");
        }
        // 复用 publish 方法中的编辑逻辑（因为它已经包含了权限检查和增量更新）
        // 或者直接调用 service.updateById(activity) 但要注意这会绕过权限检查
        // 建议：我们在 service 层封装一个安全的 update 方法，或者直接用 publish (因为它兼容了更新)
        // 既然你明确要 update 接口，我们这里简单包装一下
        Long id = activityService.publish(activity); 
        return id != null ? Result.success(true) : Result.fail("更新失败");
    }
}
