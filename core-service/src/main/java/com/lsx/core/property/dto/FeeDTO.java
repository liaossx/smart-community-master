package com.lsx.core.property.dto;

import com.lsx.core.property.entity.SysFee;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class FeeDTO extends SysFee {
    private String ownerName;
    private String buildingNo;
    private String houseNo;
}