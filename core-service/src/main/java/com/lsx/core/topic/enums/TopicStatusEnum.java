package com.lsx.core.topic.enums;

public enum TopicStatusEnum {
    PENDING,    // 待审核
    APPROVED,   // 已通过
    REJECTED;   // 已拒绝

    public static TopicStatusEnum fromString(String status) {
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

