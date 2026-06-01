package com.suiqu.cloud.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件分享实体类
 */
@Data
@TableName("share")
public class Share {

    /**
     * 分享唯一ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 被分享的文件ID (关联 file_info 表的 id)
     */
    private Long fileId;

    /**
     * 分享者用户ID
     */
    private Long userId;

    /**
     * 分享到期时间
     */
    private LocalDateTime expireTime;

    /**
     * 分享状态：0-正常，1-已失效
     */
    private Integer status;

    /**
     * 创建分享的时间
     */
    private LocalDateTime createTime;
}
