package com.lsx.core.property.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lsx.core.property.entity.SysFee;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysFeeMapper extends BaseMapper<SysFee> {
    // 继承BaseMapper后，无需手动写CRUD方法，MyBatis-Plus自动生成
    // 支持：selectById、selectList、insert、updateById等
}