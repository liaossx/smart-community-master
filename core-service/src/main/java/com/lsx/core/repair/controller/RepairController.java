package com.lsx.core.repair.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lsx.core.common.Result.Result;
import com.lsx.core.repair.dto.BatchUpdateStatusDTO;
import com.lsx.core.repair.dto.RepairDto;
import com.lsx.core.repair.entity.Repair;
import com.lsx.core.repair.service.RepairService;
import com.lsx.core.repair.vo.RepairResult;
import com.lsx.core.repair.vo.RepairStatsResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

@RestController
@RequestMapping("/api/repair")
@Tag(name = "报修接口", description = "业主报修提交、状态查询及管理员处理接口")
public class RepairController {

    @Resource
    private RepairService repairService;

    // 1. 业主提交报修
    @Operation(summary = "提交报修（业主）", description = "业主提交房屋故障报修，需传入用户ID、房屋信息、故障详情等")
    @PostMapping("/submit")
    public Result<Boolean> submitRepair(
            @Parameter(description = "报修信息DTO，包含userId、buildingNo、houseNo、faultType等")
            @RequestBody RepairDto repairDto) {
        boolean success = repairService.submitRepair(repairDto);
        if (success) {
            return Result.success(true); // 用 success(T data) 返回成功状态
        } else {
            return Result.fail("提交失败，请检查信息"); // 用 fail 返回失败原因
        }
    }

    // 2. 管理员更新报修状态
    @Operation(summary = "更新报修状态（管理员）", description = "管理员处理报修单，更新状态为处理中/已完成/已取消，可填写处理备注")
    @PostMapping("/admin/updateStatus")
    public Result<Boolean> updateStatus(
            @Parameter(description = "报修记录ID", required = true) @RequestParam Long repairId,
            @Parameter(description = "目标状态：processing（处理中）、completed（已完成）、cancelled（已取消）", required = true) @RequestParam String status,
            @Parameter(description = "处理备注（取消时必填）") @RequestParam(required = false) String remark) {
        try {
            boolean success = repairService.updateRepairStatus(repairId, status, remark);
            return success ? Result.success(true) : Result.fail("更新失败，报修单不存在");
        } catch (RuntimeException e) {
            // 捕获Service层抛出的业务异常（如状态不合法、取消未填备注）
            return Result.fail(e.getMessage());
        }
    }

    // 3. 业主查询自己的报修记录（分页）
    @Operation(summary = "业主查询自己的报修记录", description = "分页查询当前业主的所有报修记录，按提交时间倒序")
    @GetMapping("/user/my")
    public Result<IPage<RepairResult>> getMyRepairs(
            @Parameter(description = "业主用户ID", required = true) @RequestParam Long userId,
            @Parameter(description = "页码（默认1）") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页条数（默认10）") @RequestParam(defaultValue = "10") Integer pageSize) {
        IPage<RepairResult> resultPage = repairService.getMyRepairs(userId, pageNum, pageSize);
        return Result.success(resultPage); // 直接返回分页数据
    }

    // 4. 管理员查询所有报修记录（分页+状态筛选）
    @GetMapping("/admin/all")
    public Result<IPage<RepairResult>> getAllRepairs(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {  // ✅ 添加 keyword

        IPage<RepairResult> resultPage = repairService.getAllRepairs(pageNum, pageSize, status, keyword);
        return Result.success(resultPage);
    }

    // 5. 管理员查询单个业主的报修记录（分页）
    @Operation(summary = "管理员查询单个业主的报修记录", description = "分页查询指定业主的所有报修记录，按提交时间倒序")
    @GetMapping("/admin/user")
    public Result<IPage<RepairResult>> getUserRepairs(
            @Parameter(description = "业主用户ID", required = true) @RequestParam Long userId,
            @Parameter(description = "页码（默认1）") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页条数（默认10）") @RequestParam(defaultValue = "10") Integer pageSize) {
        IPage<RepairResult> resultPage = repairService.getUserRepairs(userId, pageNum, pageSize);
        return Result.success(resultPage); // 直接返回分页数据
    }

    // 6. 管理员批量更新报修状态
    @PostMapping("/admin/batchUpdateStatus")
    @Operation(summary = "批量更新报修状态", description = "管理员批量更新多条报修状态")
    public Result<Boolean> batchUpdateStatus(@RequestBody BatchUpdateStatusDTO dto) {
        boolean success = repairService.batchUpdateStatus(dto.getRepairIds(), dto.getStatus(), dto.getRemark());
        return success ? Result.success(true) : Result.fail("批量更新失败");
    }

    // 7. 导出报修数据
    @GetMapping("/admin/export")
    @Operation(summary = "导出报修数据", description = "导出符合条件的报修数据为Excel或CSV")
    public void exportRepairs(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            HttpServletResponse response) {
        
        repairService.exportRepairs(status, keyword, response);
    }

    // 8. 获取报修统计数据
    @GetMapping("/admin/stats")
    @Operation(summary = "获取报修统计数据", description = "获取各种状态报修的统计数量")
    public Result<RepairStatsResult> getRepairStats() {
        RepairStatsResult stats = repairService.getRepairStats();
        return Result.success(stats);
    }

}