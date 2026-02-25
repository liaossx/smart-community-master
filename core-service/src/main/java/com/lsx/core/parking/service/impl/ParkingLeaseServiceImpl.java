package com.lsx.core.parking.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lsx.core.parking.dto.ParkingLeaseOrderCreateDTO;
import com.lsx.core.parking.dto.ParkingLeaseOrderPayDTO;
import com.lsx.core.parking.entity.ParkingAccount;
import com.lsx.core.parking.entity.ParkingLeaseOrder;
import com.lsx.core.parking.entity.ParkingOrder;
import com.lsx.core.parking.entity.ParkingSpaceLease;
import com.lsx.core.parking.mapper.ParkingAccountMapper;
import com.lsx.core.parking.mapper.ParkingLeaseOrderMapper;
import com.lsx.core.parking.mapper.ParkingOrderMapper;
import com.lsx.core.parking.mapper.ParkingSpaceLeaseMapper;
import com.lsx.core.parking.service.ParkingLeaseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class ParkingLeaseServiceImpl implements ParkingLeaseService {

    @Autowired
    private ParkingLeaseOrderMapper leaseOrderMapper;

    @Autowired
    private ParkingSpaceLeaseMapper leaseMapper;

    @Autowired
    private ParkingAccountMapper accountMapper;
    @Override

    public Long createLeaseOrder(ParkingLeaseOrderCreateDTO dto) {
        Assert.notNull(dto.getUserId(), "用户不能为空");
        Assert.notNull(dto.getSpaceId(), "车位不能为空");
        Assert.notNull(dto.getLeaseType(), "租赁类型不能为空");

        BigDecimal amount;
        switch (dto.getLeaseType()) {
            case "MONTHLY": amount = BigDecimal.valueOf(200); break;
            case "YEARLY": amount = BigDecimal.valueOf(2000); break;
            case "PERPETUAL": amount = BigDecimal.valueOf(20000); break;
            default: throw new RuntimeException("非法租赁类型");
        }

        ParkingLeaseOrder order = new ParkingLeaseOrder();
        order.setUserId(dto.getUserId());
        order.setSpaceId(dto.getSpaceId());
        order.setLeaseType(dto.getLeaseType());
        order.setAmount(amount);
        order.setStatus("UNPAID");
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());

        leaseOrderMapper.insert(order);
        return order.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void payLeaseOrder(ParkingLeaseOrderPayDTO dto) {

        ParkingLeaseOrder order = leaseOrderMapper.selectById(dto.getOrderId());
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (!"UNPAID".equals(order.getStatus())) {
            throw new RuntimeException("订单已支付");
        }

        Long userId = order.getUserId();

        // 1️⃣ 查询账户余额
        ParkingAccount account = accountMapper.selectOne(
                Wrappers.<ParkingAccount>lambdaQuery()
                        .eq(ParkingAccount::getUserId, userId)
        );
        if (account == null) {
            throw new RuntimeException("账户不存在");
        }

        // 2️⃣ 校验余额
        if (account.getBalance().compareTo(order.getAmount()) < 0) {
            throw new RuntimeException("账户余额不足");
        }

        // 3️⃣ 扣减余额
        account.setBalance(account.getBalance().subtract(order.getAmount()));
        accountMapper.updateById(account);

        // 4️⃣ 更新订单状态
        order.setStatus("PAID");
        order.setPayTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        leaseOrderMapper.updateById(order);

        // 5️⃣ 查询当前生效 lease
        ParkingSpaceLease lease = leaseMapper.selectOne(
                Wrappers.<ParkingSpaceLease>lambdaQuery()
                        .eq(ParkingSpaceLease::getUserId, userId)
                        .eq(ParkingSpaceLease::getSpaceId, order.getSpaceId())
                        .eq(ParkingSpaceLease::getStatus, "ACTIVE")
                        .last("LIMIT 1")
        );

        if (lease == null) {
            throw new RuntimeException("未找到可续费的车位");
        }

        // 永久车位禁止续费
        if ("PERPETUAL".equals(lease.getLeaseType())) {
            throw new RuntimeException("永久车位无需续费");
        }

        // 6️⃣ 计算新的结束时间
        LocalDateTime baseTime =
                lease.getEndTime().isAfter(LocalDateTime.now())
                        ? lease.getEndTime()
                        : LocalDateTime.now();

        LocalDateTime newEnd;
        switch (order.getLeaseType()) {
            case "MONTHLY":
                newEnd = baseTime.plusMonths(1);
                break;
            case "YEARLY":
                newEnd = baseTime.plusYears(1);
                break;
            default:
                throw new RuntimeException("非法续费类型");
        }

        // 7️⃣ 更新 lease（关键点：不是 insert）
        lease.setEndTime(newEnd);
        lease.setUpdateTime(LocalDateTime.now());
        leaseMapper.updateById(lease);
    }
}
