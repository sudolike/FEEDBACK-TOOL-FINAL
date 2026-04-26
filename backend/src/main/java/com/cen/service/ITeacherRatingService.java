package com.cen.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cen.controller.dto.TeacherRatingStatsDTO;
import com.cen.entity.TeacherRating;

public interface ITeacherRatingService extends IService<TeacherRating> {

    TeacherRatingStatsDTO statsByTeacher(Long teacherId);
}
