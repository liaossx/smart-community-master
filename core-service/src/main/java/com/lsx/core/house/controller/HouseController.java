package com.lsx.core.house.controller;


import com.lsx.core.common.Result.Result;
import com.lsx.core.common.Util.UserContext;
import com.lsx.core.house.dto.UserHouseBindDTO;
import com.lsx.core.house.entity.House;
import com.lsx.core.house.service.HouseService;
import com.lsx.core.house.service.UserHouseService;
import com.lsx.core.user.service.UserService;
import com.lsx.core.house.vo.HouseResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

import java.util.List;
@Slf4j
@RestController
@RequestMapping("/api/house")
@Tag(name = "房屋接口")
public class HouseController {
    @Resource
    private HouseService houseService;
    @Resource
    private UserHouseService userhouseService;
    @Resource
    private UserService userService;

    //管理员查房屋信息

    //1,根据id查询房屋信息
    @Operation(summary = "查询房屋信息")
    @GetMapping("/getHouseInfoById")
    public Result<HouseResult> getHouseInfoById(Long houseId) {
        HouseResult houseResult = houseService.getHouseInfoById(houseId);
        return Result.success(houseResult);
    }
    
    @Operation(summary = "提交房屋绑定申请")
    @PostMapping("/bind")
    public Result<String> bindHouse(@RequestBody UserHouseBindDTO bindDTO) {
        Long userId = bindDTO.getUserId();
        // 如果 DTO 中没传 userId，尝试从 UserContext 获取（适配业主端直接调用）
        if (userId == null) {
            userId = UserContext.getCurrentUserId();
        }
        if (userId == null) {
            return Result.fail("未登录或参数错误");
        }
        
        Long houseId = bindDTO.getHouseId();
        try {
            userService.bindUserToHouse(userId, houseId);
            return Result.success("绑定申请提交成功");
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    //2，查询全部房屋信息
    @Operation(summary = "查询全部房屋信息")
    @GetMapping("/getAllHouseInfo")
    public Result<List<HouseResult>> getAllHouseInfo() {
        List<HouseResult> houseResult = houseService.getAllHouseInfo();
        return Result.success(houseResult);
    }

    //3,更改用户绑定房屋的状态
    @Operation(summary = "更改用户绑定房屋的状态")
    @PostMapping("/updateUserHouseStatus")
    public Result<Boolean> updateHouseStatus(
            @RequestParam Long id,  // 加上@RequestParam，明确是请求参数（避免参数绑定失败）
            @RequestParam String status) {
        try {
            houseService.updateHouseStatus(id, status);
            // 成功：返回true + 成功提示
            return Result.success();
        } catch (RuntimeException e) {
            // 失败：返回false + 具体错误信息（如“非法状态”“房屋不存在”）
            return Result.fail(e.getMessage());
        }
    }
    //4，根据用户id查询用户绑定房屋信息
    @Operation(summary = "根据用户id查询用户绑定房屋信息")
    @GetMapping("/getHouseInfoByUserId")
    public Result<List<House>> getHouseInfoByUserId() {
        Long userId = UserContext.getCurrentUserId(); // 从你的JWT解析器取
        try {

            if (userId == null) {
                return Result.fail("未登录");
            }
            List<House> list = userhouseService.getHouseByUserId(userId);
            return Result.success(list);
        } catch (Exception e) {
            log.error("查询房屋信息失败 userId={}", userId, e);
            return Result.fail("查询房屋信息失败：" + e.getMessage());
        }
    }
}