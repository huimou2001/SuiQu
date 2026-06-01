package com.suiqu.cloud.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.suiqu.cloud.entity.Share;
import com.suiqu.cloud.entity.vo.ShareVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ShareMapper extends BaseMapper<Share> {

    /**
     * 关联查询分享详情及文件信息
     * 用户访问分享链接时使用
     */
    @Select("SELECT s.id, s.expire_time, s.status, f.name as fileName, f.size as fileSize, f.type as fileType " +
            "FROM share s " +
            "LEFT JOIN file_info f ON s.file_id = f.id " +
            "WHERE s.id = #{shareId}")
    ShareVO selectShareWithFile(@Param("shareId") Long shareId);
}