package com.cen.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cen.common.Result;
import com.cen.entity.CourseFeedback;
import com.cen.entity.QuestionnaireResponses;
import com.cen.mapper.CourseFeedbackMapper;
import com.cen.mapper.CoursesMapper;
import com.cen.mapper.QuestionnaireResponsesMapper;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据分析接口
 *  - 课程反馈整体面板（评分分布 / 平均分 / 关键词）
 *  - 问卷提交率走势
 */
@RestController
@RequestMapping("/analytics")
public class AnalyticsController {

    @Resource private CourseFeedbackMapper courseFeedbackMapper;
    @Resource private CoursesMapper coursesMapper;
    @Resource private QuestionnaireResponsesMapper questionnaireResponsesMapper;

    @GetMapping("/course/{courseId}/dashboard")
    public Result dashboard(@PathVariable Long courseId) {
        Map<String, Object> ret = new HashMap<>();

        QueryWrapper<CourseFeedback> qw = new QueryWrapper<>();
        qw.eq("course_id", courseId);
        List<CourseFeedback> fbList = courseFeedbackMapper.selectList(qw);

        int total = fbList.size();
        double avg = 0;
        // 5 个星级 1..5，下标 0 对应 1 星、下标 4 对应 5 星
        int[] hist = new int[5];
        if (!fbList.isEmpty()) {
            avg = fbList.stream()
                    .filter(f -> f.getRating() != null)
                    .mapToInt(CourseFeedback::getRating)
                    .average()
                    .orElse(0);
            for (CourseFeedback f : fbList) {
                Integer r = f.getRating();
                if (r != null && r >= 1 && r <= 5) {
                    hist[r - 1]++;
                }
            }
        }

        Long studentTotal = coursesMapper.getStudentCountByCourseId(courseId);

        Long submissions = questionnaireResponsesMapper.selectCount(
                new QueryWrapper<QuestionnaireResponses>().eq("course_id", courseId));

        ret.put("totalFeedback", total);
        ret.put("avgRating", Math.round(avg * 100.0) / 100.0);
        ret.put("ratingHistogram", hist);
        ret.put("totalStudents", studentTotal);
        ret.put("totalQuestionnaireSubmissions", submissions);

        return Result.success(ret);
    }
}
