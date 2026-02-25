package com.lsx.core.repair.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ReUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Assert;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lsx.core.common.Util.UserContext;
import com.lsx.core.house.service.HouseService;
import com.lsx.core.repair.dto.RepairDto;
import com.lsx.core.house.entity.House;
import com.lsx.core.repair.entity.Repair;
import com.lsx.core.house.mapper.HouseMapper;
import com.lsx.core.repair.mapper.RepairMapper;
import com.lsx.core.repair.service.RepairService;
import com.lsx.core.repair.vo.RepairResult;
import com.lsx.core.repair.vo.RepairStatsResult;
import lombok.var;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RepairServiceImpl extends ServiceImpl<RepairMapper, Repair> implements RepairService {
    @Autowired
    private HouseMapper houseMapper;  // 注入 HouseMapper
    @Autowired
    private HouseService houseService;
    @Override
    public boolean submitRepair(RepairDto repairdto) {
        String buildingNo = repairdto.getBuildingNo();
        String houseNo = repairdto.getHouseNo();
        // 1. 非空校验（已有的逻辑）
        if (buildingNo == null || buildingNo.trim().isEmpty()) {
            throw new RuntimeException("楼栋号不能为空");
        }
        if (houseNo == null || houseNo.trim().isEmpty()) {
            throw new RuntimeException("房屋号不能为空");
        }
        // 2. 格式校验（新增逻辑）
        // 楼栋号正则：必须是“数字+栋”（如“1栋”“10栋”）
        String buildingPattern = "^\\d+栋$"; // ^表示开头，\\d+表示1个以上数字，栋是固定后缀，$表示结尾
        if (!ReUtil.isMatch(buildingPattern, buildingNo.trim())) {
            throw new RuntimeException("楼栋号格式错误，应为类似'1栋'的格式（数字+栋）");
        }

        // 房屋号正则：必须是纯数字（如“101”“2001”，这里限制3-4位，可根据实际调整）
        String housePattern = "^\\d{3,4}$"; // \\d{3,4}表示3-4位数字
        if (!ReUtil.isMatch(housePattern, houseNo.trim())) {
            throw new RuntimeException("房屋号格式错误，应为3-4位数字（如'101'）");
        }

        //根据楼栋号和房屋编号查询房屋ID
        LambdaQueryWrapper<House> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.select(House::getId)
                .eq(House::getBuildingNo, buildingNo)
                .eq(House::getHouseNo, houseNo);
        House house = houseMapper.selectOne(queryWrapper);
        // 添加空值检查
        if (house == null) {
            throw new RuntimeException("房屋不存在，请检查楼栋号和房屋号");
        }
        Long houseId = house.getId();
        //把值传给repair对象
        Repair repair = new Repair();
        BeanUtil.copyProperties(repairdto, repair);
        repair.setHouseId(houseId);
        // 1. 设初始状态为“待处理”
        repair.setStatus("pending");
        // 设置社区ID
        if (UserContext.getCommunityId() != null) {
            repair.setCommunityId(UserContext.getCommunityId());
        }
        // 2. 插入数据库（baseMapper是ServiceImpl自带的）
        return baseMapper.insert(repair) > 0;
    }

    @Override
    public boolean updateRepairStatus(Long repairId, String status, String remark) {
        // 1. 查报修单是否存在
        Repair repair = baseMapper.selectById(repairId);
        if (repair == null) {
            return false;
        }
        // 2. 更新状态和备注
        repair.setStatus(status);
        repair.setHandleRemark(remark);
        return baseMapper.updateById(repair) > 0;
    }

    //用户查询自己的报修订单
    @Override
    public IPage<RepairResult> getMyRepairs(Long userId, Integer pageNum, Integer pageSize) {
        Assert.notNull(userId, "用户ID不能为空");
        Page<Repair> page = new Page<>(pageNum, pageSize);

        // 分页查询当前用户的报修记录
        IPage<Repair> repairPage = baseMapper.selectPage(page, Wrappers.<Repair>lambdaQuery()
                .eq(Repair::getUserId, userId)
                .orderByDesc(Repair::getCreateTime));

        // 转换为前端展示的VO
        return repairPage.convert(this::convertToRepairResult);
    }

    //管理员查询全部的报修订单
    @Override
    public IPage<RepairResult> getAllRepairs(Integer pageNum, Integer pageSize, String status, String keyword) {
        Page<Repair> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<Repair> queryWrapper = new LambdaQueryWrapper<Repair>()
                .orderByDesc(Repair::getCreateTime);

        // --- 权限过滤逻辑 ---
        String role = UserContext.getRole();
        Long currentCommunityId = UserContext.getCommunityId();
        
        if ("super_admin".equalsIgnoreCase(role)) {
             // 超级管理员：不做限制
        } else {
             if (currentCommunityId == null) {
                  // 普通管理员/业主如果没有社区ID，查不到数据
                  queryWrapper.eq(Repair::getId, -1L);
             } else {
                  queryWrapper.eq(Repair::getCommunityId, currentCommunityId);
             }
        }
        // -----------------

        // 状态筛选
        if (StringUtils.hasText(status)) {
            queryWrapper.eq(Repair::getStatus, status);
        }

        // 关键词搜索
        if (StringUtils.hasText(keyword)) {
            // 处理关键词，支持"1栋101"格式
            List<String> searchTerms = processSearchKeyword(keyword);
            final List<Long> houseIds = getAllMatchingHouseIds(searchTerms);

            queryWrapper.and(wrapper -> {
                // 为每个搜索项添加条件
                for (String term : searchTerms) {
                    wrapper.like(Repair::getFaultType, term)
                            .or()
                            .like(Repair::getFaultDesc, term);
                }

                // 添加房屋ID条件
                if (!houseIds.isEmpty()) {
                    wrapper.or().in(Repair::getHouseId, houseIds);
                }
            });
        }

        IPage<Repair> repairPage = baseMapper.selectPage(page, queryWrapper);
        return repairPage.convert(this::convertToRepairResult);
    }

    // 处理搜索关键词
    private List<String> processSearchKeyword(String keyword) {
        List<String> searchTerms = new ArrayList<>();
        searchTerms.add(keyword); // 原始关键词

        // 处理"1栋101"格式
        if (keyword.contains("栋")) {
            // 提取楼栋部分
            String[] parts = keyword.split("栋");
            if (parts.length > 0) {
                String buildingPart = parts[0] + "栋";
                searchTerms.add(buildingPart);
            }

            // 提取房号部分
            if (parts.length > 1 && !parts[1].isEmpty()) {
                String housePart = parts[1];
                // 移除"室"字（如果有）
                housePart = housePart.replace("室", "");
                if (!housePart.isEmpty()) {
                    searchTerms.add(housePart);
                }
            }
        }

        return searchTerms.stream().distinct().collect(Collectors.toList());
    }

    // 获取所有匹配的房屋ID
    private List<Long> getAllMatchingHouseIds(List<String> searchTerms) {
        Set<Long> houseIds = new HashSet<>();

        for (String term : searchTerms) {
            houseIds.addAll(getHouseIdsByKeyword(term));
        }

        return new ArrayList<>(houseIds);
    }

    
    // 根据单个关键词查询房屋ID（保持不变）
    private List<Long> getHouseIdsByKeyword(String keyword) {
        LambdaQueryWrapper<House> houseQuery = new LambdaQueryWrapper<>();
        houseQuery.select(House::getId)
                .and(wrapper -> wrapper
                        .like(House::getBuildingNo, keyword)
                        .or()
                        .like(House::getHouseNo, keyword)
                        .or()
                        .like(House::getCommunityName, keyword)
                );

        return houseService.list(houseQuery)
                .stream()
                .map(House::getId)
                .collect(Collectors.toList());
    }


    /**
     * 管理员查询单个业主的报修记录（分页）
     */
    @Override
    public IPage<RepairResult> getUserRepairs(Long userId, Integer pageNum, Integer pageSize) {
        Assert.notNull(userId, "用户ID不能为空");
        Page<Repair> page = new Page<>(pageNum, pageSize);

        IPage<Repair> repairPage = baseMapper.selectPage(page, Wrappers.<Repair>lambdaQuery()
                .eq(Repair::getUserId, userId)
                .orderByDesc(Repair::getCreateTime));

        return repairPage.convert(this::convertToRepairResult);
    }


    /**
     * 公共转换方法：Repair → RepairResult（关联房屋信息）
     */
    private RepairResult convertToRepairResult(Repair repair) {
        if (repair == null) return null;

        RepairResult result = new RepairResult();

        // 1. 复制基本报修信息
        result.setId(repair.getId());
        result.setFaultType(repair.getFaultType());
        result.setFaultDesc(repair.getFaultDesc());
        result.setStatus(repair.getStatus());
        result.setHandleRemark(repair.getHandleRemark());
        result.setCreateTime(repair.getCreateTime());
        result.setUpdateTime(repair.getUpdateTime());

        // 2. 处理图片列表
        if (StringUtils.hasText(repair.getFaultImgs())) {
            List<String> imgs = Arrays.asList(repair.getFaultImgs().split(","));
            result.setFaultImgs(imgs);
        } else {
            result.setFaultImgs(new ArrayList<>());
        }

        // 3. 查询并设置房屋信息
        if (repair.getHouseId() != null) {
            House house = houseService.getById(repair.getHouseId());
            if (house != null) {
                result.setCommunityName(house.getCommunityName());
                result.setBuildingNo(house.getBuildingNo());
                result.setHouseNo(house.getHouseNo());
            }
        }

        // 4. 设置状态中文描述（可选）
        result.setStatusDesc(getStatusDesc(repair.getStatus()));

        return result;
    }

    // 获取状态中文描述
    private String getStatusDesc(String status) {
        switch (status) {
            case "pending":
                return "待处理";
            case "processing":
                return "处理中";
            case "completed":
                return "已完成";
            case "cancelled":
                return "已取消";
            default:
                return status;
        }
    }

    /**
     * 状态英文转中文描述
     */
    private String convertStatusToDesc(String status) {
        if (status == null) {
            return "未知状态";
        }
        // JDK 8 支持的传统 switch 语句
        switch (status) {
            case "pending":
                return "待处理";
            case "processing":
                return "处理中";
            case "completed":
                return "已完成";
            case "cancelled":
                return "已取消";
            default:
                return "未知状态";
        }
    }

    @Override
    public boolean batchUpdateStatus(List<Long> repairIds, String status, String remark) {
        if (repairIds == null || repairIds.isEmpty()) {
            return false;
        }
        
        // 验证状态合法性
        List<String> validStatuses = Arrays.asList("pending", "processing", "completed", "cancelled");
        if (!validStatuses.contains(status)) {
            throw new RuntimeException("无效的状态值");
        }
        
        // 批量更新
        List<Repair> repairs = new ArrayList<>();
        for (Long repairId : repairIds) {
            Repair repair = new Repair();
            repair.setId(repairId);
            repair.setStatus(status);
            repair.setHandleRemark(remark);
            repairs.add(repair);
        }
        
        // 使用MyBatis Plus的批量更新方法
        return this.updateBatchById(repairs);
    }

    @Override
    public void exportRepairs(String status, String keyword, HttpServletResponse response) {
        // 查询符合条件的报修数据
        LambdaQueryWrapper<Repair> queryWrapper = new LambdaQueryWrapper<Repair>()
                .orderByDesc(Repair::getCreateTime);

        // --- 权限过滤逻辑 ---
        String role = UserContext.getRole();
        Long currentCommunityId = UserContext.getCommunityId();
        
        if ("super_admin".equalsIgnoreCase(role)) {
             // 超级管理员：不做限制
        } else {
             if (currentCommunityId == null) {
                  queryWrapper.eq(Repair::getId, -1L);
             } else {
                  queryWrapper.eq(Repair::getCommunityId, currentCommunityId);
             }
        }
        // -----------------
        
        // 状态筛选
        if (StringUtils.hasText(status)) {
            queryWrapper.eq(Repair::getStatus, status);
        }
        
        // 关键词搜索
        if (StringUtils.hasText(keyword)) {
            // 处理关键词，支持"1栋101"格式
            List<String> searchTerms = processSearchKeyword(keyword);
            final List<Long> houseIds = getAllMatchingHouseIds(searchTerms);
            
            queryWrapper.and(wrapper -> {
                // 为每个搜索项添加条件
                for (String term : searchTerms) {
                    wrapper.like(Repair::getFaultType, term)
                            .or()
                            .like(Repair::getFaultDesc, term);
                }
                
                // 添加房屋ID条件
                if (!houseIds.isEmpty()) {
                    wrapper.or().in(Repair::getHouseId, houseIds);
                }
            });
        }
        
        List<Repair> repairs = this.list(queryWrapper);
        
        // 转换为RepairResult
        List<RepairResult> repairResults = repairs.stream()
                .map(this::convertToRepairResult)
                .collect(Collectors.toList());
        
        // 这里实现简单的CSV导出，如需Excel导出可添加POI依赖
        try {
            // 设置响应头
            response.setContentType("text/csv;charset=UTF-8");
            response.setHeader("Content-Disposition", "attachment;filename=repairs.csv");
            
            // 写入CSV内容
            try (PrintWriter writer = response.getWriter()) {
                // 写入表头
                writer.println("ID,社区名称,楼栋号,房屋号,故障类型,故障描述,状态,处理备注,创建时间");
                
                // 写入数据行
                for (RepairResult result : repairResults) {
                    writer.printf("%d,%s,%s,%s,%s,%s,%s,%s,%s%n",
                            result.getId(),
                            result.getCommunityName(),
                            result.getBuildingNo(),
                            result.getHouseNo(),
                            result.getFaultType(),
                            result.getFaultDesc(),
                            result.getStatusDesc(),
                            result.getHandleRemark() != null ? result.getHandleRemark() : "",
                            result.getCreateTime());
                }
                
                writer.flush();
            }
        } catch (IOException e) {
            throw new RuntimeException("导出失败：" + e.getMessage());
        }
    }

    @Override
    public RepairStatsResult getRepairStats() {
        RepairStatsResult stats = new RepairStatsResult();
        
        // --- 权限过滤逻辑 ---
        String role = UserContext.getRole();
        Long currentCommunityId = UserContext.getCommunityId();
        
        LambdaQueryWrapper<Repair> baseWrapper = Wrappers.lambdaQuery(Repair.class);
        
        if (!"super_admin".equalsIgnoreCase(role)) {
            if (currentCommunityId != null) {
                baseWrapper.eq(Repair::getCommunityId, currentCommunityId);
            } else {
                baseWrapper.eq(Repair::getId, -1L);
            }
        }

        // 查询总数
        stats.setTotal(Math.toIntExact(this.count(baseWrapper)));
        
        // 查询各状态数量
        LambdaQueryWrapper<Repair> pendingQuery = baseWrapper.clone().eq(Repair::getStatus, "pending");
        stats.setPending(Math.toIntExact(this.count(pendingQuery)));
        
        LambdaQueryWrapper<Repair> processingQuery = baseWrapper.clone().eq(Repair::getStatus, "processing");
        stats.setProcessing(Math.toIntExact(this.count(processingQuery)));
        
        LambdaQueryWrapper<Repair> completedQuery = baseWrapper.clone().eq(Repair::getStatus, "completed");
        stats.setCompleted(Math.toIntExact(this.count(completedQuery)));
        
        LambdaQueryWrapper<Repair> cancelledQuery = baseWrapper.clone().eq(Repair::getStatus, "cancelled");
        stats.setCancelled(Math.toIntExact(this.count(cancelledQuery)));
        
        return stats;
    }
}
