package com.lsx.core.express.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lsx.core.express.dto.ExpressAuthorizeDTO;
import com.lsx.core.express.dto.ExpressCreateDTO;
import com.lsx.core.express.dto.ExpressPickDTO;
import com.lsx.core.express.entity.Express;
import com.lsx.core.express.vo.ExpressVO;

public interface ExpressService extends IService<Express> {

    Long registerExpress(ExpressCreateDTO dto);

    Page<ExpressVO> listMyExpress(Long userId, Integer pageNum, Integer pageSize);

    Boolean pickExpress(Long expressId, ExpressPickDTO dto);

    Boolean authorizeExpress(Long expressId, ExpressAuthorizeDTO dto);
}


