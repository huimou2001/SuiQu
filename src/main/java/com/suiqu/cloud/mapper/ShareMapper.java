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
     * 联表查询分享的详细信息
     * 注意：share.file_id 应该存储的是 file_user 的 ID
     */
    @Select("SELECT s.id, s.expire_time, s.status, " +
            "fu.file_name as fileName, fi.size as fileSize, fi.type as fileType, fi.path as filePath " +
            "FROM share s " +
            "JOIN file_user fu ON s.file_id = fu.id " +
            "JOIN file_info fi ON fu.file_id = fi.id " +
            "WHERE s.id = #{shareId}")
    ShareVO selectShareDetail(@Param("shareId") Long shareId);
}