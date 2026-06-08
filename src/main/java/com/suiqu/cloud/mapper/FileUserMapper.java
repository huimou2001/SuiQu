package com.suiqu.cloud.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.suiqu.cloud.entity.FileIndex;
import com.suiqu.cloud.entity.FileUser;
import com.suiqu.cloud.entity.vo.FileVO; // 自定义VO，用于展示
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface FileUserMapper extends BaseMapper<FileUser> {

    /**
     * 查询指定父目录下的文件列表
     * 逻辑：关联 file_info 表，以获取 size, type, path 等物理信息
     */
    @Select("SELECT fu.*, fi.size, fi.type, fi.path, fi.md5 " +
            "FROM file_user fu " +
            "LEFT JOIN file_info fi ON fu.file_id = fi.id " +
            "WHERE fu.user_id = #{userId} AND fu.parent_id = #{parentId} " +
            "ORDER BY fu.is_dir DESC, fu.create_time DESC")
    List<FileVO> selectFileList(@Param("userId") Long userId, @Param("parentId") Long parentId);

    /**
     * 检查同一目录下是否存在同名文件/文件夹
     */
    @Select("SELECT COUNT(*) FROM file_user WHERE user_id = #{userId} AND parent_id = #{parentId} AND file_name = #{fileName}")
    int countDuplicateName(@Param("userId") Long userId, @Param("parentId") Long parentId, @Param("fileName") String fileName);

    @Select("SELECT " +
            "fu.id AS id, " +
            "fu.file_name AS name, " +
            "fu.description AS description, " + // 显式写出，确保映射
            "fu.user_id AS userId, " +
            "fu.create_time AS createTime " +
            "FROM file_user fu")
    List<FileIndex> selectAllForSync();
}