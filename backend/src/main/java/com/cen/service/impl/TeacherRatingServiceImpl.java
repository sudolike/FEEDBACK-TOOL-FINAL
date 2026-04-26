package com.cen.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cen.controller.dto.TeacherRatingStatsDTO;
import com.cen.entity.TeacherRating;
import com.cen.mapper.TeacherRatingMapper;
import com.cen.service.IKnowledgeBaseService;
import com.cen.service.ITeacherRatingService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@Service
public class TeacherRatingServiceImpl extends ServiceImpl<TeacherRatingMapper, TeacherRating> implements ITeacherRatingService {

    @Resource private TeacherRatingMapper teacherRatingMapper;
    @Resource private IKnowledgeBaseService knowledgeBaseService;

    @Override
    public boolean saveOrUpdate(TeacherRating entity) {
        boolean ok = super.saveOrUpdate(entity);
        if (ok && entity.getTeacherId() != null) {
            try { knowledgeBaseService.syncTeacherRating(entity.getTeacherId()); } catch (Exception ignored) {}
        }
        return ok;
    }

    @Override
    public TeacherRatingStatsDTO statsByTeacher(Long teacherId) {
        TeacherRatingStatsDTO dto = new TeacherRatingStatsDTO();
        dto.setTeacherId(teacherId);

        Map<String, Object> agg = teacherRatingMapper.aggregateByTeacher(teacherId);
        if (agg != null) {
            dto.setAvgRating(toDouble(agg.get("avgRating")));
            dto.setAvgTeaching(toDouble(agg.get("avgTeaching")));
            dto.setAvgAttitude(toDouble(agg.get("avgAttitude")));
            dto.setAvgContent(toDouble(agg.get("avgContent")));
            Object total = agg.get("total");
            dto.setTotal(total == null ? 0L : ((Number) total).longValue());
        }
        List<Map<String, Object>> dist = teacherRatingMapper.distributionByTeacher(teacherId);
        dto.setDistribution(dist);
        return dto;
    }

    private Double toDouble(Object v) {
        if (v == null) return 0.0;
        if (v instanceof Number) return ((Number) v).doubleValue();
        try { return Double.parseDouble(v.toString()); } catch (Exception e) { return 0.0; }
    }
}
