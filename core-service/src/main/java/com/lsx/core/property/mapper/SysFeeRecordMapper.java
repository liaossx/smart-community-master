package com.lsx.core.property.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lsx.core.property.entity.SysFeeRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysFeeRecordMapper extends BaseMapper<SysFeeRecord> {
    // 同样继承BaseMapper，自动获得CRUD能力
}