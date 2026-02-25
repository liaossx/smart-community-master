package com.lsx.core.property.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lsx.core.property.dto.CurrentFeeDTO;
import com.lsx.core.property.dto.FeeHistoryDTO;
import com.lsx.core.property.dto.GenerateFeeDTO;
import com.lsx.core.property.dto.PayFeeDTO;

import java.time.LocalDateTime;
import java.util.List;

public interface FeeService {

    List<CurrentFeeDTO> getCurrentUnpaid(Long userId);


    Page<FeeHistoryDTO> getPaymentHistory(Long userId, LocalDateTime startTime, LocalDateTime endTime,
                                          Integer pageNum, Integer pageSize);

    Boolean generateBills(GenerateFeeDTO dto, Long adminId);


    String payFee(PayFeeDTO dto, Long userId);

    void payCallback(String orderNo, String tradeNo, String status);

    Page<com.lsx.core.property.entity.SysFee> adminList(String status, String ownerName, Integer pageNum, Integer pageSize);
}
