package com.lsx.core.parking.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Assert;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lsx.core.parking.dto.ParkingAuthorizeDTO;
import com.lsx.core.parking.dto.ParkingSpaceBindDTO;
import com.lsx.core.parking.dto.ParkingSpaceQueryDTO;
import com.lsx.core.parking.entity.ParkingAuthorize;
import com.lsx.core.parking.entity.ParkingSpace;
import com.lsx.core.parking.entity.ParkingSpaceLease;
import com.lsx.core.parking.entity.ParkingSpacePlate;
import com.lsx.core.parking.mapper.ParkingAuthorizeMapper;
import com.lsx.core.parking.mapper.ParkingPlateMapper;
import com.lsx.core.parking.mapper.ParkingSpaceLeaseMapper;
import com.lsx.core.parking.mapper.ParkingSpaceMapper;
import com.lsx.core.parking.service.ParkingSpaceService;
import com.lsx.core.parking.vo.ParkingAuthorizeVO;
import com.lsx.core.parking.vo.ParkingSpaceRemainVO;
import com.lsx.core.parking.vo.ParkingSpaceVO;
import com.lsx.core.user.entity.User;
import com.lsx.core.user.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class ParkingSpaceServiceImpl
        extends ServiceImpl<ParkingSpaceMapper, ParkingSpace>
        implements ParkingSpaceService {

    private static final String SPACE_AVAILABLE = "AVAILABLE";
    private static final String SPACE_FIXED = "FIXED";

    @Autowired
    private ParkingSpaceLeaseMapper leaseMapper;

    @Autowired
    private ParkingAuthorizeMapper parkingAuthorizeMapper;

    @Autowired
    private ParkingSpaceMapper parkingSpaceMapper;

    @Autowired
    private ParkingPlateMapper plateMapper;

    @Autowired
    private UserMapper userMapper;

    /**
     * 查询剩余车位
     */
    @Override
    public ParkingSpaceRemainVO getRemaining(Long communityId) {

        long tempRemain = this.count(Wrappers.<ParkingSpace>lambdaQuery()
                .eq(communityId != null, ParkingSpace::getCommunityId, communityId)
                .eq(ParkingSpace::getSpaceType, "TEMP")
                .eq(ParkingSpace::getStatus, SPACE_AVAILABLE));

        long fixedRemain = this.count(Wrappers.<ParkingSpace>lambdaQuery()
                .eq(communityId != null, ParkingSpace::getCommunityId, communityId)
                .eq(ParkingSpace::getSpaceType, SPACE_FIXED)
                .eq(ParkingSpace::getStatus, SPACE_AVAILABLE));

        ParkingSpaceRemainVO vo = new ParkingSpaceRemainVO();
        vo.setCommunityId(communityId);
        vo.setTempRemaining(tempRemain);
        vo.setFixedRemaining(fixedRemain);

        if (communityId != null) {
            ParkingSpace space = this.getOne(
                    Wrappers.<ParkingSpace>lambdaQuery()
                            .eq(ParkingSpace::getCommunityId, communityId)
                            .last("limit 1")
            );
            if (space != null) {
                vo.setCommunityName(space.getCommunityName());
            }
        }
        return vo;
    }

    /**
     * 绑定固定车位（只做“占位”，不产生使用权）
     */
    @Override
    public Boolean bindSpace(ParkingSpaceBindDTO dto) {

        Assert.notNull(dto.getSpaceId(), "车位ID不能为空");

        ParkingSpace space = this.getById(dto.getSpaceId());
        if (space == null) {
            throw new RuntimeException("车位不存在");
        }
        if (!SPACE_AVAILABLE.equals(space.getStatus())) {
            throw new RuntimeException("车位不可绑定");
        }
        if (!SPACE_FIXED.equals(space.getSpaceType())) {
            throw new RuntimeException("仅固定车位可绑定");
        }

        // 仅改变车位状态
        space.setStatus("DISABLED");
        space.setUpdateTime(LocalDateTime.now());

        return this.updateById(space);
    }

    /**
     * 查询我拥有的车位（基于使用权）
     */
    @Override
    public List<ParkingSpaceVO> listMySpaces(Long userId) {
        Assert.notNull(userId, "用户ID不能为空");

        // 查询用户 ACTIVE 的租赁记录
        List<ParkingSpaceLease> leases = leaseMapper.selectList(
                Wrappers.<ParkingSpaceLease>lambdaQuery()
                        .eq(ParkingSpaceLease::getUserId, userId)
                        .eq(ParkingSpaceLease::getStatus, "ACTIVE")
                        .orderByDesc(ParkingSpaceLease::getCreateTime)
        );

        LocalDateTime now = LocalDateTime.now();

        return leases.stream().map(lease -> {
            ParkingSpace space = parkingSpaceMapper.selectById(lease.getSpaceId());
            if (space == null) return null;

            ParkingSpaceVO vo = new ParkingSpaceVO();
            vo.setId(space.getId());
            vo.setSlot(space.getSpaceNo());
            vo.setCommunityName(space.getCommunityName());

            // 租赁信息
            vo.setLeaseType(lease.getLeaseType());
            vo.setLeaseStartTime(lease.getStartTime());
            vo.setLeaseEndTime(lease.getEndTime());
            vo.setLeaseStatus(lease.getStatus());

            // 状态判断
            boolean active = lease.getEndTime() == null || lease.getEndTime().isAfter(now);
            vo.setActive(active);
            vo.setStatusText(active ? "使用中" : "已过期");

            // ===== 新增：获取车牌号 =====
            List<ParkingSpacePlate> plates = plateMapper.selectList(
                    Wrappers.<ParkingSpacePlate>lambdaQuery()
                            .eq(ParkingSpacePlate::getSpaceId, lease.getSpaceId())
                            .eq(ParkingSpacePlate::getStatus, "ACTIVE")
                            .eq(ParkingSpacePlate::getUserId, userId)
            );
            if (!plates.isEmpty()) {
                // 这里示例只取第一个车牌
                vo.setPlateNo(plates.get(0).getPlateNo());
                // 如果想拼接多个车牌：
                // vo.setPlateNo(plates.stream().map(ParkingSpacePlate::getPlateNo).collect(Collectors.joining(",")));
            }

            return vo;
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }
    /**
     * 固定车位授权访客
     */
    @Override
    public Boolean authorizeSpace(Long spaceId, ParkingAuthorizeDTO dto) {

        Assert.notNull(spaceId, "车位ID不能为空");
        Assert.notNull(dto.getUserId(), "用户ID不能为空");

        // 校验是否有有效使用权
        ParkingSpaceLease lease = leaseMapper.selectOne(
                Wrappers.<ParkingSpaceLease>lambdaQuery()
                        .eq(ParkingSpaceLease::getSpaceId, spaceId)
                        .eq(ParkingSpaceLease::getUserId, dto.getUserId())
                        .eq(ParkingSpaceLease::getStatus, "ACTIVE")
                        .last("limit 1")
        );

        if (lease == null) {
            throw new RuntimeException("无该车位的有效使用权");
        }

        ParkingAuthorize authorize = new ParkingAuthorize();
        authorize.setSpaceId(spaceId);
        authorize.setUserId(dto.getUserId());
        authorize.setAuthorizedName(dto.getAuthorizedName());
        authorize.setAuthorizedPhone(dto.getAuthorizedPhone());
        authorize.setPlateNo(dto.getPlateNo());
        authorize.setStartTime(LocalDateTime.now());
        authorize.setEndTime(dto.getEndTime());
        authorize.setStatus("ACTIVE");
        authorize.setCreateTime(LocalDateTime.now());
        authorize.setUpdateTime(LocalDateTime.now());

        parkingAuthorizeMapper.insert(authorize);
        return true;
    }

    /**
     * 查询我的授权记录
     */
    @Override
    public IPage<ParkingAuthorizeVO> listMyAuthorizes(
            Long userId, Integer pageNum, Integer pageSize) {

        Page<ParkingAuthorize> page = new Page<>(pageNum, pageSize);

        IPage<ParkingAuthorize> authorizePage =
                parkingAuthorizeMapper.selectPage(page,
                        Wrappers.<ParkingAuthorize>lambdaQuery()
                                .eq(ParkingAuthorize::getUserId, userId)
                                .orderByDesc(ParkingAuthorize::getCreateTime));

        return authorizePage.convert(record -> {
            ParkingAuthorizeVO vo = new ParkingAuthorizeVO();
            BeanUtil.copyProperties(record, vo);

            ParkingSpace space = this.getById(record.getSpaceId());
            if (space != null) {
                vo.setSpaceNo(space.getSpaceNo());
            }

            if (record.getEndTime() != null &&
                    record.getEndTime().isBefore(LocalDateTime.now())) {
                vo.setStatus("EXPIRED");
            } else {
                vo.setStatus(record.getStatus());
            }
            return vo;
        });
    }

    @Override
    public IPage<ParkingSpaceVO> adminListSpaces(ParkingSpaceQueryDTO dto) {
        String role = com.lsx.core.common.Util.UserContext.getRole();
        Long currentCommunityId = com.lsx.core.common.Util.UserContext.getCommunityId();

        if (!"super_admin".equalsIgnoreCase(role)) {
            if (currentCommunityId == null) {
                // 无社区则查不到任何数据
                dto.setCommunityId(-1L);
            } else {
                // 强制限定为当前社区
                dto.setCommunityId(currentCommunityId);
            }
        }
        Page<ParkingSpaceVO> page = new Page<>(dto.getPageNum(), dto.getPageSize());
        return parkingSpaceMapper.selectAdminPage(page, dto);
    }

    @Override
    public IPage<ParkingSpaceVO> listAvailableFixedSpaces(Long communityId, Integer pageNum, Integer pageSize) {
        Page<ParkingSpace> page = new Page<>(pageNum, pageSize);
        
        LambdaQueryWrapper<ParkingSpace> query = Wrappers.lambdaQuery();
        query.eq(ParkingSpace::getStatus, SPACE_AVAILABLE)
             .eq(ParkingSpace::getSpaceType, SPACE_FIXED);
             
        if (communityId != null) {
            query.eq(ParkingSpace::getCommunityId, communityId);
        }
        
        IPage<ParkingSpace> spacePage = parkingSpaceMapper.selectPage(page, query);
        
        return spacePage.convert(space -> {
            ParkingSpaceVO vo = new ParkingSpaceVO();
            BeanUtil.copyProperties(space, vo);
            vo.setSlot(space.getSpaceNo());
            return vo;
        });
    }
}

