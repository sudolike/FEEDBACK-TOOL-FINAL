package com.cen.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cen.entity.KbChunk;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface KbChunkMapper extends BaseMapper<KbChunk> {

    /**
     * 关键词检索（兜底实现：LIKE）
     */
    @Select("<script>" +
            "SELECT * FROM sys_kb_chunk " +
            "<where>" +
            "  <if test='courseId != null'> AND course_id = #{courseId} </if>" +
            "  <if test='keywords != null and keywords.size() > 0'>" +
            "    AND (" +
            "      <foreach collection='keywords' item='kw' separator='OR'>" +
            "        content LIKE CONCAT('%', #{kw}, '%') OR title LIKE CONCAT('%', #{kw}, '%')" +
            "      </foreach>" +
            "    )" +
            "  </if>" +
            "</where>" +
            " ORDER BY id DESC " +
            " LIMIT #{limit}" +
            "</script>")
    List<KbChunk> searchByLike(@Param("keywords") List<String> keywords,
                               @Param("courseId") Long courseId,
                               @Param("limit") int limit);
}
