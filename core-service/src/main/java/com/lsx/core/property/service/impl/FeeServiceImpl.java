package com.lsx.core.property.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lsx.core.common.Util.UserContext;
import com.lsx.core.community.entity.Community;
import com.lsx.core.community.mapper.CommunityMapper;
import com.lsx.core.house.entity.UserHouse;
import com.lsx.core.house.mapper.UserHouseMapper;
import com.lsx.core.house.service.HouseService;
import com.lsx.core.house.vo.HouseResult;
import com.lsx.core.notice.dto.NoticeCreateDTO;
import com.lsx.core.notice.service.NoticeService;
import com.lsx.core.property.dto.*;
import com.lsx.core.property.entity.SysFee;
import com.lsx.core.property.entity.SysFeeRecord;
import com.lsx.core.property.mapper.SysFeeMapper;
import com.lsx.core.property.mapper.SysFeeRecordMapper;
import com.lsx.core.property.service.FeeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class FeeServiceImpl implements FeeService {

    @Autowired
    private SysFeeMapper feeMapper;
    @Autowired
    private SysFeeRecordMapper feeRecordMapper;
    @Autowired
    private HouseService houseService;
    @Autowired
    private UserHouseMapper userHouseMapper;
    @Autowired
    private CommunityMapper communityMapper;
    @Autowired
    private NoticeService noticeService;


    @Override
    @Transactional
    public boolean remind(List<Long> feeIds) {
        if (feeIds == null || feeIds.isEmpty()) {
            return false;
        }

        // 1. Query unpaid fees
        List<SysFee> fees = feeMapper.selectBatchIds(feeIds);
        if (fees.isEmpty()) {
            return false;
        }

        int count = 0;
        for (SysFee fee : fees) {
            if (!"UNPAID".equals(fee.getStatus())) {
                continue;
            }

            // 2. Find owner
            QueryWrapper<UserHouse> query = new QueryWrapper<>();
            query.eq("house_id", fee.getHouseId()).eq("status", "审核通过");
            List<UserHouse> userHouses = userHouseMapper.selectList(query);

            if (userHouses.isEmpty()) {
                continue;
            }

            // Send to all owners
            boolean sent = false;
            for (UserHouse uh : userHouses) {
                NoticeCreateDTO noticeDTO = new NoticeCreateDTO();
                noticeDTO.setTitle("缴费提醒");
                noticeDTO.setContent("尊敬的业主，您有一笔物业费（" + fee.getFeeCycle() + "）尚未缴纳，请及时处理。");
                noticeDTO.setTargetType("USER");
                noticeDTO.setTargetUserId(uh.getUserId());
                noticeDTO.setPublishStatus("PUBLISHED");

                // Set community info if available
                if (fee.getCommunityId() != null) {
                    noticeDTO.setCommunityId(fee.getCommunityId());
                    Community c = communityMapper.selectById(fee.getCommunityId());
                    if (c != null) {
                        noticeDTO.setCommunityName(c.getName());
                    }
                }

                try {
                    // Assuming current user is admin/operator
                    // 修改这里：把 .getUserId() 改为 .getCurrentUserId()
                    Long currentUserId = UserContext.getCurrentUserId();
                    if (currentUserId == null) currentUserId = 1L; // Fallback to system admin if not logged in (e.g. scheduled task)
                    noticeService.createNotice(noticeDTO, currentUserId);
                    sent = true;
                } catch (Exception e) {
                    log.error("Failed to send reminder to user {}", uh.getUserId(), e);
                }
            }

            // 3. Update remind count
            if (sent) {
                if (fee.getRemindCount() == null) {
                    fee.setRemindCount(0);
                }
                fee.setRemindCount(fee.getRemindCount() + 1);
                feeMapper.updateById(fee);
                count++;
            }
        }

        return count > 0;
    }


    @Override
    public List<CurrentFeeDTO> getCurrentUnpaid(Long userId) {
        // 1. 查询用户绑定的房屋ID
        QueryWrapper<UserHouse> userHouseQuery = new QueryWrapper<>();
        userHouseQuery.eq("user_id", userId).eq("status", "审核通过");
        List<UserHouse> userHouses = userHouseMapper.selectList(userHouseQuery);

        if (userHouses.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<Long> houseIds = userHouses.stream()
                .map(UserHouse::getHouseId)
                .collect(Collectors.toList());
        // ... (省略后续代码)

        // 2. 查询未缴账单
        QueryWrapper<SysFee> feeQuery = new QueryWrapper<>();
        feeQuery.in("house_id", houseIds).eq("status", "UNPAID").orderByDesc("due_date");
        List<SysFee> fees = feeMapper.selectList(feeQuery);

        // 3. 关联房屋信息
        return fees.stream().map(fee -> {
            CurrentFeeDTO dto = new CurrentFeeDTO();
            BeanUtils.copyProperties(fee, dto);
            dto.setFeeId(fee.getId());
            HouseResult houseResult = houseService.getHouseInfoById(fee.getHouseId());
            if (houseResult != null) {
                BeanUtils.copyProperties(houseResult, dto);
            }
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    public Page<FeeHistoryDTO> getPaymentHistory(Long userId,
                                                 LocalDateTime startTime,
                                                 LocalDateTime endTime,
                                                 Integer pageNum,
                                                 Integer pageSize) {

        Page<SysFeeRecord> page = new Page<>(pageNum, pageSize);

        QueryWrapper<SysFeeRecord> query = new QueryWrapper<>();
        query.eq("user_id", userId)
                .eq("status", "SUCCESS");

        if (startTime != null) query.ge("pay_time", startTime);
        if (endTime != null) query.le("pay_time", endTime);

        query.orderByDesc("pay_time");

        // 分页查询缴费记录
        Page<SysFeeRecord> recordPage = feeRecordMapper.selectPage(page, query);

        // 批量查询对应 Fee
        List<Long> feeIds = recordPage.getRecords()
                .stream()
                .map(SysFeeRecord::getFeeId)
                .collect(Collectors.toList());

        Map<Long, SysFee> feeMap = new HashMap<>();

        if (!feeIds.isEmpty()) {
            List<SysFee> feeList = feeMapper.selectBatchIds(feeIds);
            // 使用 putAll 而不是重新赋值
            feeMap.putAll(
                    feeList.stream().collect(
                            Collectors.toMap(SysFee::getId, f -> f)
                    )
            );
        }

        // 封装 DTO
        List<FeeHistoryDTO> dtoList = recordPage.getRecords().stream().map(record -> {
            FeeHistoryDTO dto = new FeeHistoryDTO();
            dto.setRecordId(record.getId());
            dto.setPayAmount(record.getPayAmount());
            dto.setPayType(record.getPayType());
            dto.setPayTime(record.getPayTime());
            dto.setStatus(record.getStatus());

            SysFee fee = feeMap.get(record.getFeeId());
            if (fee != null) {
                dto.setFeeCycle(fee.getFeeCycle());
            }

            return dto;
        }).collect(Collectors.toList());

        // 构造返回分页对象
        Page<FeeHistoryDTO> result = new Page<>(pageNum, pageSize);
        result.setTotal(recordPage.getTotal());
        result.setRecords(dtoList);

        return result;
    }


    @Override
    @Transactional
    public Boolean generateBills(GenerateFeeDTO dto, Long adminId) {
        // 先解析日期
        LocalDateTime dueDateTime = parseDueDate(dto.getDueDate());

        // 校验必要参数
        if (dto.getUnitPrice() == null || dto.getUnitPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("单价必须大于0");
        }
        if (dto.getFeeCycle() == null || dto.getFeeCycle().trim().isEmpty()) {
            throw new RuntimeException("费用周期不能为空");
        }

        // 1. 查询已绑定且审核通过的房屋
        List<Long> boundHouseIds = getBoundAndApprovedHouseIds(dto.getCommunityName(), dto.getBuildingNo());
        if (boundHouseIds.isEmpty()) {
            log.warn("生成账单失败：未找到已绑定用户的房屋");
            return false;
        }

        // 2. 查询房屋详细信息
        List<HouseResult> targetHouses = houseService.getHouseInfoByIds(boundHouseIds);
        if (targetHouses.isEmpty()) {
            log.warn("生成账单失败：未找到房屋详细信息");
            return false;
        }

        // 3. 检查是否已存在相同周期的账单
        checkDuplicateBills(boundHouseIds, dto.getFeeCycle());

        // 4. 生成账单
        int successCount = 0;
        for (HouseResult house : targetHouses) {
            try {
                SysFee fee = new SysFee();
                fee.setHouseId(house.getId());
                fee.setBuildingNo(house.getBuildingNo());
                if (house.getCommunityName() != null) {
                    Community c = communityMapper.selectOne(new QueryWrapper<Community>().eq("name", house.getCommunityName()));
                    if (c != null) {
                        fee.setCommunityId(c.getId());
                    }
                }

                fee.setFeeCycle(dto.getFeeCycle());
                fee.setFeeAmount(house.getArea().multiply(dto.getUnitPrice()).setScale(2, RoundingMode.HALF_UP));
                fee.setFeeType("物业费");
                fee.setStatus("UNPAID");
                fee.setDueDate(dueDateTime);
                fee.setCreateTime(LocalDateTime.now());
                fee.setUpdateTime(LocalDateTime.now());
                fee.setRemark("自动生成：" + dto.getFeeCycle() + "物业费");

                feeMapper.insert(fee);
                successCount++;

            } catch (Exception e) {
                log.error("生成房屋{}的账单失败：{}", house.getId(), e.getMessage());
            }
        }

        log.info("管理员[{}]生成账单成功，共{}户，成功{}户", adminId, targetHouses.size(), successCount);
        return successCount > 0;
    }

    /**
     * 获取已绑定且审核通过的房屋ID列表
     */
    private List<Long> getBoundAndApprovedHouseIds(String communityName, String buildingNo) {
        QueryWrapper<UserHouse> query = new QueryWrapper<>();
        query.eq("status", "审核通过");

        // 如果需要根据小区/楼栋筛选，需要关联房屋表
        if (StringUtils.isNotBlank(communityName) || StringUtils.isNotBlank(buildingNo)) {
            query.inSql("house_id",
                    "SELECT id FROM sys_house WHERE 1=1" +
                            (StringUtils.isNotBlank(communityName) ? " AND community_name = '" + communityName + "'" : "") +
                            (StringUtils.isNotBlank(buildingNo) ? " AND building_no = '" + buildingNo + "'" : "")
            );
        }

        List<UserHouse> userHouses = userHouseMapper.selectList(query);
        return userHouses.stream()
                .map(UserHouse::getHouseId)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * 检查重复账单
     */
    private void checkDuplicateBills(List<Long> houseIds, String feeCycle) {
        QueryWrapper<SysFee> query = new QueryWrapper<>();
        query.in("house_id", houseIds)
                .eq("fee_cycle", feeCycle)
                .eq("fee_type", "物业费")
                .in("status", Arrays.asList("UNPAID", "PAID")); // 未缴和已缴的都算重复

        long duplicateCount = feeMapper.selectCount(query);
        if (duplicateCount > 0) {
            throw new RuntimeException("存在重复账单，费用周期 " + feeCycle + " 已生成过账单");
        }
    }


    @Override
    @Transactional
    public String payFee(PayFeeDTO dto, Long userId) {
        // 校验账单
        SysFee fee = feeMapper.selectById(dto.getFeeId());
        if (fee == null || !"UNPAID".equals(fee.getStatus())) {
            throw new RuntimeException("账单不存在或已缴费");
        }

        // 校验房屋绑定权限
        QueryWrapper<UserHouse> query = new QueryWrapper<>();
        query.eq("user_id", userId).eq("house_id", fee.getHouseId()).eq("status", "审核通过");
        if (userHouseMapper.selectOne(query) == null) {
            throw new RuntimeException("未绑定该房屋，无法缴费");
        }

        // 生成支付记录（使用copyProperties）
        String orderNo = "FEE_" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8);
        SysFeeRecord record = new SysFeeRecord();
        // 复制账单关联信息（houseId、feeId等）
        BeanUtils.copyProperties(fee, record);
        record.setFeeId(fee.getId());
        // 补充支付特有字段
        record.setUserId(userId);
        record.setPayType(dto.getPayType());
        record.setPayAmount(fee.getFeeAmount());
        record.setOrderNo(orderNo);
        record.setTradeNo("TRADE_" + orderNo);
        record.setPayTime(LocalDateTime.now());
        record.setStatus("SUCCESS");
        feeRecordMapper.insert(record);

        // 更新账单状态
        fee.setStatus("PAID");
        fee.setUpdateTime(LocalDateTime.now());
        feeMapper.updateById(fee);

        return "支付成功，订单号：" + orderNo;
    }


    @Override
    @Transactional
    public void payCallback(String orderNo, String tradeNo, String status) {
        QueryWrapper<SysFeeRecord> query = new QueryWrapper<>();
        query.eq("order_no", orderNo);
        SysFeeRecord record = feeRecordMapper.selectOne(query);
        if (record == null) {
            log.error("支付回调失败：订单号{}不存在", orderNo);
            return;
        }

        // 更新支付记录状态
        record.setTradeNo(tradeNo);
        record.setPayTime(LocalDateTime.now());
        record.setStatus(status);
        feeRecordMapper.updateById(record);

        // 更新账单状态
        if ("SUCCESS".equals(status)) {
            SysFee fee = feeMapper.selectById(record.getFeeId());
            fee.setStatus("PAID");
            fee.setUpdateTime(LocalDateTime.now());
            feeMapper.updateById(fee);
        }
    }

    @Override
    public Page<FeeDTO> adminList(String status, String ownerName, Integer pageNum, Integer pageSize) {
        Page<FeeDTO> page = new Page<>(pageNum, pageSize);
        String role = UserContext.getRole();
        Long communityId = UserContext.getCommunityId();
        
        Long filterCid = null;
        if (!"super_admin".equalsIgnoreCase(role)) {
            if (communityId != null) filterCid = communityId;
            else filterCid = -1L;
        }
        
        return (Page<FeeDTO>) feeMapper.selectAdminList(page, status, ownerName, filterCid);
    }


    private LocalDateTime parseDueDate(String dueDateStr) {
        if (dueDateStr == null || dueDateStr.trim().isEmpty()) {
            throw new RuntimeException("缴费截止日期不能为空");
        }

        try {
            // 尝试解析 "yyyy-MM-dd HH:mm:ss" 格式
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            return LocalDateTime.parse(dueDateStr, formatter);
        } catch (DateTimeParseException e1) {
            try {
                // 尝试解析 "yyyy-MM-dd" 格式，自动加上时间
                DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                LocalDate date = LocalDate.parse(dueDateStr, dateFormatter);
                return date.atTime(23, 59, 59);
            } catch (DateTimeParseException e2) {
                throw new RuntimeException("日期格式错误，请使用 yyyy-MM-dd HH:mm:ss 或 yyyy-MM-dd 格式");
            }
        }
    }
}
