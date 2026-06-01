package com.suiqu.cloud.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("file_info")
public class FileInfo {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;        // 文件名
    private String md5;         // 文件MD5
    private String path;        // MinIO中的存储路径
    private Long size;          // 文件大小
    private String type;        // 文件后缀
    private Long parentId;      // 父文件夹ID (0为根目录)
    private Integer isDir;      // 是否为文件夹 0-否 1-是
    private Long userId;        // 所属用户ID
    private String description; // 文件描述（用于ES检索）
    private LocalDateTime createTime;
}
