package com.suiqu.cloud.entity.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class FileVO {
    // 来自 file_user 的逻辑信息
    private Long id;
    private String fileName;
    private Long parentId;
    private Integer isDir;
    private String description;
    private LocalDateTime createTime;

    // 来自 file_info 的物理信息
    private Long size;
    private String type;
    private String path;
    private String md5;
}
