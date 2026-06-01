package com.suiqu.cloud.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.suiqu.cloud.entity.FileInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface FileMapper extends BaseMapper<FileInfo> {

    /**
     * 根据 MD5 查询文件（用于秒传功能）
     * 即使有多个记录，取最新的一条即可
     */
    @Select("SELECT * FROM file_info WHERE md5 = #{md5} AND is_dir = 0 LIMIT 1")
    FileInfo selectOneByMd5(@Param("md5") String md5);

    /**
     * 查询指定用户、指定目录下的文件列表
     */
    @Select("SELECT * FROM file_info WHERE user_id = #{userId} AND parent_id = #{parentId} ORDER BY is_dir DESC, create_time DESC")
    List<FileInfo> selectByParentId(@Param("userId") Long userId, @Param("parentId") Long parentId);

    /**
     * 物理删除某个路径下的所有文件（如清空文件夹）
     */
    @Select("DELETE FROM file_info WHERE user_id = #{userId} AND parent_id = #{parentId}")
    void deleteByParentId(@Param("userId") Long userId, @Param("parentId") Long parentId);
}
