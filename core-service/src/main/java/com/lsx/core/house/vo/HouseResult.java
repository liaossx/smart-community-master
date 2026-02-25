package com.lsx.core.house.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class HouseResult {
    private Long id;
    private String communityName;
    private String buildingNo;
    private String houseNo;
    private BigDecimal area;

}
