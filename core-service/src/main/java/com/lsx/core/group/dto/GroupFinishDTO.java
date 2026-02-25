package com.lsx.core.group.dto;

import lombok.Data;

@Data
public class GroupFinishDTO {
    private Long operatorId;
    private Boolean success;  // true=成功，false=失败
    private String remark;
}

