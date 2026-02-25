package com.lsx.core.community.controller;

import com.lsx.core.common.Result.Result;
import com.lsx.core.community.entity.Community;
import com.lsx.core.community.service.CommunityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/api/community")
@Tag(name = "社区接口", description = "社区管理接口")
public class CommunityController {

    @Resource
    private CommunityService communityService;

    @GetMapping("/list")
    @Operation(summary = "获取社区列表", description = "根据当前登录用户的角色返回不同的社区列表")
    public Result<List<Community>> list() {
        List<Community> list = communityService.getListByRole();
        return Result.success(list);
    }
}
