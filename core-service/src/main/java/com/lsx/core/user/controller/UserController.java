package com.lsx.core.user.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lsx.core.common.Util.JwtUtil;
import com.lsx.core.common.Result.Result;
import com.lsx.core.common.Util.UserContext;
import com.lsx.core.house.entity.House;
import com.lsx.core.house.entity.UserHouse;
import com.lsx.core.house.service.HouseService;
import com.lsx.core.house.service.UserHouseService;
import com.lsx.core.user.dto.LoginDto;
import com.lsx.core.user.dto.RegisterDto;
import com.lsx.core.house.dto.UserHouseBindDTO;
import com.lsx.core.user.dto.UserInfoDTO;
import com.lsx.core.user.vo.LoginResult;
import com.lsx.core.user.entity.User;
import com.lsx.core.user.service.UserService;

import com.lsx.core.user.vo.RegisterResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;


@RestController
@RequestMapping("/api/user")
@Tag(name = "用户接口")
public class UserController {

    @Resource
    private UserService userService;
    @Resource
    private HouseService houseService;
    @Resource
    private UserHouseService userHouseService;
    @Resource  // 添加这个注入
    private JwtUtil jwtUtil;  // 确保JwtUtil类有@Component注解

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<LoginResult> login(
            @RequestBody LoginDto loginDto

    ) {
        try {
            LoginResult loginResult = userService.login(loginDto);
            return Result.success(loginResult);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    @Operation(summary = "获取当前用户信息")
    @GetMapping("/info")
    public Result<UserInfoDTO> getCurrentUser() {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            return Result.fail("Token无效或已过期");
        }

        User user = userService.getById(userId);
        if (user == null) {
            return Result.fail("用户不存在");
        }

        // 1) 优先找默认绑定的房屋
        LambdaQueryWrapper<UserHouse> defaultQ = new LambdaQueryWrapper<>();
        defaultQ.eq(UserHouse::getUserId, userId)
                .eq(UserHouse::getStatus, "审核通过");
        UserHouse userHouse = userHouseService.getOne(defaultQ, false);

        // 2) 若无默认，取最近绑定的一条（或第一条）
        if (userHouse == null) {
            LambdaQueryWrapper<UserHouse> recentQ = new LambdaQueryWrapper<>();
            recentQ.eq(UserHouse::getUserId, userId)
                    .orderByDesc(UserHouse::getId) // 或者按绑定时间字段
                    .last("LIMIT 1");
            userHouse = userHouseService.getOne(recentQ, false);
        }

        String community = "未绑定社区";
        String room = "未绑定房屋";
        if (userHouse != null) {
            House house = houseService.getById(userHouse.getHouseId());
            if (house != null) {
                community = house.getCommunityName();
                room = house.getBuildingNo() + "栋" + house.getHouseNo();
            }
        }

        UserInfoDTO dto = new UserInfoDTO();
        dto.setUserId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setName(user.getRealName());
        dto.setCommunity(community);
        dto.setRoom(room);
        dto.setRole(user.getRole());

        return Result.success(dto);
    }

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public Result<RegisterResult> register(@RequestBody RegisterDto registerDto) {
        try {
            RegisterResult registerResult = userService.register(registerDto);
            return Result.success(registerResult);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    @Operation(summary = "绑定用户与房屋")
    @PostMapping("/bindUserToHouse")
    public Result<String> bindUserToHouse(@RequestBody UserHouseBindDTO bindDTO) {
        Long userId = bindDTO.getUserId();
        Long houseId = bindDTO.getHouseId();
        try {
            userService.bindUserToHouse(userId, houseId);
            return Result.success("绑定成功");
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }


}