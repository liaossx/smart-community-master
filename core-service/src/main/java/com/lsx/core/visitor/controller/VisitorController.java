package com.lsx.core.visitor.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lsx.core.common.Result.Result;
import com.lsx.core.visitor.entity.SysVisitor;
import com.lsx.core.visitor.service.VisitorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/visitor")
@Tag(name = "访客管理接口")
public class VisitorController {

    @Autowired
    private VisitorService visitorService;

    @PostMapping("/apply")
    @Operation(summary = "提交预约")
    public Result<Long> apply(@RequestBody SysVisitor body) {
        Long id = visitorService.apply(body);
        return Result.success(id);
    }

    @GetMapping("/my")
    @Operation(summary = "我的访客预约")
    public Result<IPage<SysVisitor>> my(@RequestParam("userId") Long userId,
                                        @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
                                        @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        IPage<SysVisitor> page = visitorService.myList(userId, pageNum, pageSize);
        return Result.success(page);
    }

    @GetMapping("/list")
    @Operation(summary = "管理员审核列表")
    public Result<IPage<SysVisitor>> adminList(@RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
                                               @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
                                               @RequestParam(value = "status", required = false) String status,
                                               @RequestParam(value = "keyword", required = false) String keyword) {
        IPage<SysVisitor> page = visitorService.adminList(pageNum, pageSize, status, keyword);
        return Result.success(page);
    }

    @PutMapping("/audit")
    @Operation(summary = "管理员审核")
    public Result<Boolean> audit(@RequestParam("id") Long id,
                                 @RequestParam("status") String status,
                                 @RequestParam(value = "remark", required = false) String remark) {
        boolean ok = visitorService.audit(id, status, remark);
        return ok ? Result.success(true) : Result.fail("审核失败");
    }
}
