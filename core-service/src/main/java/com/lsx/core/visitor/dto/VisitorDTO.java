package com.lsx.core.visitor.dto;

import com.lsx.core.visitor.entity.SysVisitor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class VisitorDTO extends SysVisitor {
    private String ownerName;
    private String buildingNo;
    private String houseNo;
}