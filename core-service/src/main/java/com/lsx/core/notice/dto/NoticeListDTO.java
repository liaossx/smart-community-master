package com.lsx.core.notice.dto;

import lombok.Data;

@Data
public class NoticeListDTO {
    private Long id;
    private String title;
    private Integer readFlag; // 0=未读，1=已读
    private String publishTime; // yyyy-MM-dd HH:mm:ss格式
    private String targetType;
    // 可选：保留content用于详情页
    private String content;
}