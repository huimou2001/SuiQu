package com.suiqu.cloud.entity.vo;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder // 引入建造者模式
public class ShareVO {
    private Long id;            // 分享ID
    private String shareUrl;    // 分享链接
    private LocalDateTime expireTime; // 过期时间
    private Integer status;     // 状态
    private String fileName;    // 文件名
    private Long fileSize;      // 文件大小
    private String fileType;    // 文件类型
}