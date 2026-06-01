package com.suiqu.cloud.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ShareCreateRequest {
    private Long fileId;
    private Integer expireHours; // 有效时长（小时）
}
