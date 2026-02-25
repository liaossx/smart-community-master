package com.lsx.core.group.enums;

public enum GroupStatusEnum {
    ONGOING,    // 进行中
    SUCCESS,    // 成功
    FAILED,     // 失败
    CANCELLED;  // 已取消

    public static GroupStatusEnum fromString(String status) {
        if (status == null) {
            return null;
        }
        try {
            return valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}

