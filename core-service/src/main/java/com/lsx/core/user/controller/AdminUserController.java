package com.lsx.core.user.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lsx.core.common.Result.Result;
import com.lsx.core.user.dto.AdminUpdateUserDTO;
import com.lsx.core.user.dto.UserDTO;
import com.lsx.core.user.dto.UserDetailDTO;
import com.lsx.core.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/user")
@Tag(name = "管理员-用户管理接口")
public class AdminUserController {

    @Resource
    private UserService userService;

    @Operation(summary = "分页获取用户列表")
    @GetMapping("/list")
    public Result<Map<String, Object>> getUserList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role
    ) {
        Page<UserDTO> page = userService.getUserList(pageNum, pageSize, keyword, role);
        List<Map<String, Object>> records = new ArrayList<>();
        for (UserDTO dto : page.getRecords()) {
            Map<String, Object> item = new HashMap<>();
            item.put("userId", dto.getId());
            item.put("username", dto.getUsername());
            item.put("realName", dto.getRealName());
            item.put("phone", dto.getPhone());
            item.put("role", "owner".equalsIgnoreCase(dto.getRole()) ? "user" : dto.getRole());
            item.put("status", dto.getStatus());
            records.add(item);
        }
        Map<String, Object> data = new HashMap<>();
        data.put("records", records);
        data.put("total", page.getTotal());
        data.put("pageNum", page.getCurrent());
        data.put("pageSize", page.getSize());
        Result<Map<String, Object>> result = new Result<>();
        result.setCode(0);
        result.setMsg("success");
        result.setData(data);
        return result;
    }

    @Operation(summary = "获取用户详情")
    @GetMapping("/{userId}")
    public Result<UserDetailDTO> getUserDetail(@PathVariable Long userId) {
        return Result.success(userService.getUserDetail(userId));
    }

    @Operation(summary = "管理员修改用户信息")
    @PutMapping("/update")
    public Result<String> updateUser(@RequestBody AdminUpdateUserDTO dto) {
        userService.adminUpdateUser(dto);
        Result<String> result = new Result<>();
        result.setCode(0);
        result.setMsg("success");
        result.setData(null);
        return result;
    }

    @Operation(summary = "修改用户状态(禁用/启用)")
    @PutMapping("/{userId}/status")
    public Result<String> updateUserStatus(
            @PathVariable Long userId,
            @RequestParam Integer status
    ) {
        userService.updateStatus(userId, status);
        Result<String> result = new Result<>();
        result.setCode(0);
        result.setMsg("success");
        result.setData(null);
        return result;
    }

    @Operation(summary = "重置用户密码")
    @PutMapping("/{userId}/reset-password")
    public Result<String> resetPassword(@PathVariable Long userId,
                                        @RequestBody(required = false) Map<String, String> body) {
        String newPassword = body != null ? body.get("newPassword") : null;
        userService.resetPassword(userId, newPassword);
        Result<String> result = new Result<>();
        result.setCode(0);
        result.setMsg("success");
        result.setData(null);
        return result;
    }
}
