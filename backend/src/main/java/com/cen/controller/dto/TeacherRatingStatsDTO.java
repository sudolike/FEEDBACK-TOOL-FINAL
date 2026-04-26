package com.cen.controller.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class TeacherRatingStatsDTO {
    private Long teacherId;
    private Double avgRating;
    private Double avgTeaching;
    private Double avgAttitude;
    private Double avgContent;
    private Long total;
    private List<Map<String, Object>> distribution;
}
