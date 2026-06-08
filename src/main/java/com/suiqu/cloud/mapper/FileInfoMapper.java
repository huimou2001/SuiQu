package com.suiqu.cloud.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.suiqu.cloud.entity.FileInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface FileInfoMapper extends BaseMapper<FileInfo> {

    /**
     * 根据 MD5 查询物理文件记录 (秒传核心)
     */
    @Select("SELECT * FROM file_info WHERE md5 = #{md5} LIMIT 1")
    FileInfo selectByMd5(@Param("md5") String md5);

    /**
     * 原子增加引用计数
     */
    @Update("UPDATE file_info SET user_count = user_count + 1 WHERE id = #{id}")
    int incrementUserCount(@Param("id") Long id);

    /**
     * 原子减少引用计数
     */
    @Update("UPDATE file_info SET user_count = user_count - 1 WHERE id = #{id} AND user_count > 0")
    int decrementUserCount(@Param("id") Long id);
}
