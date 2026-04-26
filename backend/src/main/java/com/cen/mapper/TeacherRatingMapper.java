package com.cen.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cen.entity.TeacherRating;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

public interface TeacherRatingMapper extends BaseMapper<TeacherRating> {

    @Select("SELECT AVG(rating) AS avgRating, " +
            "AVG(teaching_score) AS avgTeaching, " +
            "AVG(attitude_score) AS avgAttitude, " +
            "AVG(content_score) AS avgContent, " +
            "COUNT(*) AS total " +
            "FROM sys_teacher_rating WHERE teacher_id = #{teacherId}")
    Map<String, Object> aggregateByTeacher(@Param("teacherId") Long teacherId);

    @Select("SELECT rating, COUNT(*) AS cnt FROM sys_teacher_rating " +
            "WHERE teacher_id = #{teacherId} GROUP BY rating ORDER BY rating ASC")
    List<Map<String, Object>> distributionByTeacher(@Param("teacherId") Long teacherId);
}
