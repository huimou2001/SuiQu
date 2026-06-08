package com.suiqu.cloud.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FileMergeRequest {
    private String md5;
    private String fileName;
    private Integer totalChunks;
    private Long parentId;
    private String description;
}
