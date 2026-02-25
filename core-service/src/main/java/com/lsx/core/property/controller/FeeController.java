package com.lsx.core.property.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lsx.core.common.Result.Result;
import com.lsx.core.property.dto.CurrentFeeDTO;
import com.lsx.core.property.dto.FeeHistoryDTO;
import com.lsx.core.property.dto.GenerateFeeDTO;
import com.lsx.core.property.dto.PayFeeDTO;
import com.lsx.core.property.service.FeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

@RestController
@RequestMapping("/api/fee")
@Slf4j
@Tag(name = "物业费接口", description = "物业费账单管理相关接口（查询、生成、缴费等）") // 类级别注解
public class FeeController {

    @Autowired
    private FeeService feeService;

    /**
     * 业主查询当前未缴账单
     */
    @GetMapping("/current")
    @Operation(summary = "查询当前未缴账单", description = "根据业主ID查询其绑定房屋的所有未缴物业费账单")
    public Result<List<CurrentFeeDTO>> getCurrentUnpaid(
            @Parameter(description = "业主ID", required = true) @RequestParam("userId") Long userId) {
        try {
            log.info("业主[{}]查询当前未缴账单", userId);
            List<CurrentFeeDTO> result = feeService.getCurrentUnpaid(userId);
            return Result.success(result);
        } catch (Exception e) {
            log.error("查询未缴账单失败", e);
            return Result.fail("查询失败：" + e.getMessage());
        }
    }

    @GetMapping("/unpaid")
    @Operation(summary = "查询未缴账单别名")
    public Result<List<CurrentFeeDTO>> getUnpaidAlias(@RequestParam("userId") Long userId) {
        try {
            List<CurrentFeeDTO> result = feeService.getCurrentUnpaid(userId);
            return Result.success(result);
        } catch (Exception e) {
            return Result.fail("查询失败：" + e.getMessage());
        }
    }

    /**
     * 业主查询历史缴费记录
     */
    @GetMapping("/history")
    @Operation(summary = "查询历史缴费记录")
    public Result<Page<FeeHistoryDTO>> getPaymentHistory(
            @RequestParam Long userId,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {

        try {
            if (userId == null) {
                return Result.fail("缺少 userId 参数");
            }

            LocalDateTime start = null;
            LocalDateTime end = null;

            if (startTime != null && startTime.length() == 10) {
                start = LocalDate.parse(startTime, DateTimeFormatter.ofPattern("yyyy-MM-dd")).atStartOfDay();
            } else if (startTime != null) {
                start = LocalDateTime.parse(startTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            }

            if (endTime != null && endTime.length() == 10) {
                end = LocalDate.parse(endTime, DateTimeFormatter.ofPattern("yyyy-MM-dd")).atTime(23, 59, 59);
            } else if (endTime != null) {
                end = LocalDateTime.parse(endTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            }

            Page<FeeHistoryDTO> result = feeService.getPaymentHistory(userId, start, end, pageNum, pageSize);
            return Result.success(result);

        } catch (Exception e) {
            log.error("查询历史缴费记录失败 userId={}", userId, e);
            return Result.fail("查询失败：" + e.getMessage());
        }
    }


    /**
     * 管理员批量生成账单
     */
    @PostMapping("/generate")
    @Operation(summary = "批量生成物业费账单", description = "管理员根据小区、楼栋筛选房屋，批量生成指定周期的物业费账单")
    public Result<Boolean> generateBills(
            @Parameter(description = "生成账单参数（小区、楼栋、周期等）", required = true) @RequestBody GenerateFeeDTO dto,
            @Parameter(description = "管理员ID", required = true) @RequestParam("adminId") Long adminId) {
        try {
            log.info("管理员[{}]批量生成账单，参数：{}", adminId, dto);
            Boolean success = feeService.generateBills(dto, adminId);
            return success ? Result.success(true) : Result.fail("生成失败，未找到符合条件的房屋");
        } catch (Exception e) {
            log.error("生成账单失败", e);
            return Result.fail("生成失败：" + e.getMessage());
        }
    }

    /**
     * 业主提交缴费
     */
    @PutMapping("/pay")
    @Operation(summary = "提交物业费缴费", description = "业主根据账单ID提交缴费，支持多种支付方式")
    public Result<String> payFee(
            @Parameter(description = "缴费参数（账单ID、支付方式）", required = true) @RequestBody PayFeeDTO dto,
            @Parameter(description = "业主ID", required = true) @RequestParam("userId") Long userId) {
        try {
            log.info("业主[{}]提交缴费，账单ID：{}", userId, dto.getFeeId());
            String result = feeService.payFee(dto, userId);
            return Result.success(result);
        } catch (RuntimeException e) {
            log.warn("缴费失败：{}", e.getMessage());
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("缴费系统异常", e);
            return Result.fail("缴费失败，请稍后重试");
        }
    }

    @PostMapping("/pay")
    @Operation(summary = "提交物业费缴费别名")
    public Result<String> payFeePost(@RequestBody PayFeeDTO dto, @RequestParam("userId") Long userId) {
        try {
            String result = feeService.payFee(dto, userId);
            return Result.success(result);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            return Result.fail("缴费失败，请稍后重试");
        }
    }

    @GetMapping("/list")
    @Operation(summary = "管理员查询账单")
    public Result<Page<com.lsx.core.property.entity.SysFee>> adminList(@RequestParam(value = "status", required = false) String status,
                                                                       @RequestParam(value = "ownerName", required = false) String ownerName,
                                                                       @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
                                                                       @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        try {
            Page<com.lsx.core.property.entity.SysFee> page = feeService.adminList(status, ownerName, pageNum, pageSize);
            return Result.success(page);
        } catch (Exception e) {
            return Result.fail("查询失败：" + e.getMessage());
        }
    }

    /**
     * 支付回调接口
     */
    @PostMapping("/pay/callback")
    @Operation(summary = "支付回调处理", description = "第三方支付平台调用，更新缴费状态")
    public Result<String> payCallback(
            @Parameter(description = "订单号", required = true) @RequestParam("orderNo") String orderNo,
            @Parameter(description = "第三方交易号", required = true) @RequestParam("tradeNo") String tradeNo,
            @Parameter(description = "支付状态（SUCCESS/FAIL）", required = true) @RequestParam("status") String status) {
        try {
            log.info("支付回调，订单号：{}，状态：{}", orderNo, status);
            feeService.payCallback(orderNo, tradeNo, status);
            return Result.success("回调处理成功");
        } catch (Exception e) {
            log.error("支付回调处理失败", e);
            return Result.fail("回调处理失败");
        }
    }
}
