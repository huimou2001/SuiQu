package com.suiqu.cloud.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("file_user")
public class FileUser {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String fileName;
    private Long parentId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Long fileId;      // 只有文件才有这个ID，目录为null
    private String description;
    private Integer isDir;    // 0-文件, 1-目录
}
