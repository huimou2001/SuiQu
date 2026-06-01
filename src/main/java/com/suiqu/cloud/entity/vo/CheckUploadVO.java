package com.suiqu.cloud.entity.vo;


import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;

@Data
@AllArgsConstructor
public class CheckUploadVO {
    // 是否已存在（秒传标志）
    private Boolean isExist;
    // 已上传的分片索引列表（断点续传用）
    private List<Integer> uploadedChunks;
}
