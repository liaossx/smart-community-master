package com.lsx.core.complaint.dto;

import com.lsx.core.complaint.entity.SysComplaint;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class ComplaintDTO extends SysComplaint {
    private String userPhone;
    private String userName;
    private String buildingNo;
    private String houseNo;
}