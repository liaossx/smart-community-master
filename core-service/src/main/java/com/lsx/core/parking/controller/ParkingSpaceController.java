package com.lsx.core.parking.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lsx.core.common.Result.Result;
import com.lsx.core.common.Util.UserContext;
import com.lsx.core.parking.dto.ParkingAuthorizeDTO;
import com.lsx.core.parking.dto.ParkingSpaceBindDTO;
import com.lsx.core.parking.dto.ParkingSpaceQueryDTO;
import com.lsx.core.parking.service.ParkingSpaceService;
import com.lsx.core.parking.vo.ParkingAuthorizeVO;
import com.lsx.core.parking.vo.ParkingSpaceRemainVO;
import com.lsx.core.parking.vo.ParkingSpaceVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/parking")
@Tag(name = "车位管理接口")
@Slf4j
public class ParkingSpaceController {

    @Autowired
    private ParkingSpaceService parkingSpaceService;

    @GetMapping("/space/remaining")
    @Operation(summary = "查询可用车位数量", description = "支持按小区ID查询，未传则统计全部")
    public Result<ParkingSpaceRemainVO> getRemaining(@RequestParam(value = "communityId", required = false) Long communityId) {
        String role = UserContext.getRole();
        Long currentCommunityId = UserContext.getCommunityId();
        if (!"super_admin".equalsIgnoreCase(role)) {
            communityId = currentCommunityId;
        }
        ParkingSpaceRemainVO vo = parkingSpaceService.getRemaining(communityId);
        return Result.success(vo);
    }

    @PostMapping("/space/bind")
    @Operation(summary = "业主绑定固定车位", description = "绑定后车位状态变为占用")
    public Result<Boolean> bindSpace(@RequestBody ParkingSpaceBindDTO dto) {
        try {
            Boolean success = parkingSpaceService.bindSpace(dto);
            return success ? Result.success(true) : Result.fail("绑定失败");
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("绑定车位异常", e);
            return Result.fail("绑定失败，请稍后再试");
        }
    }

    @GetMapping("/space/my")
    @Operation(summary = "查询我绑定的车位")
    public Result<List<ParkingSpaceVO>> listMySpaces() {
        try {
            Long userId = UserContext.getCurrentUserId();
            if (userId == null) {
                return Result.fail("未登录");
            }
            List<ParkingSpaceVO> spaces = parkingSpaceService.listMySpaces(userId);
            return Result.success(spaces);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("查询车位失败", e);
            return Result.fail("查询失败，请稍后再试");
        }
    }

    @PostMapping("/space/{id}/authorize")
    @Operation(summary = "固定车位授权访客使用")
    public Result<Boolean> authorize(@PathVariable("id") Long spaceId, @RequestBody ParkingAuthorizeDTO dto) {
        try {
            Boolean success = parkingSpaceService.authorizeSpace(spaceId, dto);
            return success ? Result.success(true) : Result.fail("授权失败");
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("车位授权异常", e);
            return Result.fail("授权失败，请稍后再试");
        }
    }

    @GetMapping("/authorize/my")
    @Operation(summary = "业主查询自己的授权记录")
    public Result<IPage<ParkingAuthorizeVO>> listMyAuthorize(@RequestParam("userId") Long userId,
                                                            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
                                                            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        try {
            IPage<ParkingAuthorizeVO> page = parkingSpaceService.listMyAuthorizes(userId, pageNum, pageSize);
            return Result.success(page);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("查询授权记录异常", e);
            return Result.fail("查询失败，请稍后再试");
        }
    }

    @GetMapping("/space/admin/list")
    @Operation(summary = "管理员获取车位列表", description = "支持按编号查询，返回是否占用及业主信息")
    public Result<Map<String, Object>> getAdminSpaceList(ParkingSpaceQueryDTO dto) {
        try {
            IPage<ParkingSpaceVO> page = parkingSpaceService.adminListSpaces(dto);
            
            Map<String, Object> data = new HashMap<>();
            data.put("records", page.getRecords());
            data.put("total", page.getTotal());
            data.put("pageNum", page.getCurrent());
            data.put("pageSize", page.getSize());
            
            return Result.success(data);
        } catch (Exception e) {
            log.error("管理员查询车位列表异常", e);
            return Result.fail("查询失败");
        }
    }
}


