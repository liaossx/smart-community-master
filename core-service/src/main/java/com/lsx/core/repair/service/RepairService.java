package com.lsx.core.repair.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lsx.core.repair.dto.RepairDto;
import com.lsx.core.repair.entity.Repair;
import com.lsx.core.repair.vo.RepairResult;
import com.lsx.core.repair.vo.RepairStatsResult;

import java.util.List;

import javax.servlet.http.HttpServletResponse;

public interface RepairService extends IService<Repair> {
    // 提交报修（业主用）
    boolean submitRepair(RepairDto repairdto);

    // 更新报修状态（管理员用）
    boolean updateRepairStatus(Long repairId, String status, String remark);

    //业主查询自己的报修记录
    IPage<RepairResult> getMyRepairs(Long userId, Integer pageNum, Integer pageSize);
    //管理员查询所有的报修记录
    IPage<RepairResult> getAllRepairs(Integer pageNum, Integer pageSize, String status, String keyword);
    //查询单个业主的报修记录
    IPage<RepairResult> getUserRepairs(Long userId, Integer pageNum, Integer pageSize);
    
    // 批量更新报修状态
    boolean batchUpdateStatus(List<Long> repairIds, String status, String remark);
    
    // 导出报修数据
    void exportRepairs(String status, String keyword, HttpServletResponse response);
    
    // 获取报修统计数据
    RepairStatsResult getRepairStats();

}