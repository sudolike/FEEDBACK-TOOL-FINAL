package com.cen.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cen.entity.*;
import com.cen.mapper.*;
import com.cen.service.IKnowledgeBaseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 简易 RAG 知识库实现：
 *   - 不依赖外部向量库；
 *   - 基于 MySQL ngram 全文索引 + LIKE 兜底；
 *   - 关键词从用户提问中通过简单的中英文分词得到。
 *
 * 优点：零运维、可在容器中一键起飞；
 * 缺点：相关性弱于向量检索，但对教学场景（评价/课程描述/资料标题）已足够。
 */
@Slf4j
@Service
public class KnowledgeBaseServiceImpl implements IKnowledgeBaseService {

    @Resource private KbChunkMapper kbChunkMapper;
    @Resource private CoursesMapper coursesMapper;
    @Resource private CourseFeedbackMapper courseFeedbackMapper;
    @Resource private TeacherRatingMapper teacherRatingMapper;
    @Resource private CourseResourceMapper courseResourceMapper;
    @Resource private QaPostMapper qaPostMapper;
    @Resource private UserMapper userMapper;

    private static final Pattern SPLIT = Pattern.compile("[\\s,，.。;；:：!！?？/\\\\\"'`()（）\\[\\]{}<>|]+");

    @Override
    public void syncCourse(Long courseId) {
        Courses c = coursesMapper.selectById(courseId);
        if (c == null) return;
        upsert("course", courseId, courseId,
                "课程：" + c.getName() + " (" + c.getCode() + ")",
                buildCourseContent(c));
    }

    private String buildCourseContent(Courses c) {
        StringBuilder sb = new StringBuilder();
        sb.append("课程名称：").append(safe(c.getName())).append('\n');
        sb.append("课程代码：").append(safe(c.getCode())).append('\n');
        sb.append("学年：").append(safe(c.getAcademicYear())).append('\n');
        if (c.getSemester() != null) sb.append("学期：").append(c.getSemester()).append('\n');
        sb.append("上课时间：").append(safe(c.getCourseTime())).append('\n');
        if (c.getTeacherId() != null) {
            User teacher = userMapper.selectById(c.getTeacherId());
            if (teacher != null) {
                sb.append("授课教师：").append(safe(teacher.getNickname() == null ? teacher.getUsername() : teacher.getNickname())).append('\n');
            }
        }
        return sb.toString();
    }

    @Override
    public void syncFeedback(Long courseId) {
        QueryWrapper<CourseFeedback> qw = new QueryWrapper<>();
        qw.eq("course_id", courseId);
        List<CourseFeedback> list = courseFeedbackMapper.selectList(qw);
        if (list.isEmpty()) return;

        Courses c = coursesMapper.selectById(courseId);
        String courseName = c == null ? ("课程#" + courseId) : c.getName();

        StringBuilder sb = new StringBuilder();
        double avg = list.stream().filter(f -> f.getRating() != null).mapToInt(CourseFeedback::getRating).average().orElse(0);
        sb.append("课程《").append(courseName).append("》共有 ").append(list.size())
          .append(" 条学生评价，平均评分 ").append(String.format("%.2f", avg)).append("/5。\n");
        int kept = 0;
        for (CourseFeedback f : list) {
            if (kept++ > 200) break;
            sb.append("- ").append(f.getRating() == null ? "" : ("[" + f.getRating() + "★] "))
              .append(safe(f.getContent())).append('\n');
        }
        upsert("feedback", courseId.longValue(), courseId,
                "学生对《" + courseName + "》的评价汇总", sb.toString());
    }

    @Override
    public void syncTeacherRating(Long teacherId) {
        Map<String, Object> agg = teacherRatingMapper.aggregateByTeacher(teacherId);
        if (agg == null) return;
        User u = userMapper.selectById(teacherId);
        String teacherName = u == null ? ("教师#" + teacherId) : (u.getNickname() == null ? u.getUsername() : u.getNickname());
        StringBuilder sb = new StringBuilder();
        sb.append("教师 ").append(safe(teacherName)).append(" 的综合评分：\n");
        sb.append("总平均：").append(agg.get("avgRating")).append("/5，").append("评分总人次：").append(agg.get("total")).append('\n');
        sb.append("授课能力：").append(agg.get("avgTeaching")).append("，态度：").append(agg.get("avgAttitude"))
          .append("，内容质量：").append(agg.get("avgContent")).append('\n');
        upsert("teacher", teacherId, null,
                "教师评分：" + teacherName, sb.toString());
    }

    @Override
    public void syncResource(Long resourceId) {
        CourseResource r = courseResourceMapper.selectById(resourceId);
        if (r == null) return;
        StringBuilder sb = new StringBuilder();
        sb.append("课程资料：").append(safe(r.getTitle())).append("（类型：").append(safe(r.getCategory())).append("）\n");
        if (r.getDescription() != null) sb.append(safe(r.getDescription())).append('\n');
        upsert("resource", resourceId, r.getCourseId(),
                "资料：" + r.getTitle(), sb.toString());
    }

    @Override
    public void syncQa(Long postId) {
        QaPost p = qaPostMapper.selectById(postId);
        if (p == null) return;
        upsert("qa", postId, p.getCourseId(),
                "问答：" + p.getTitle(),
                "课程问答：" + safe(p.getTitle()) + "\n问题描述：" + safe(p.getContent()));
    }

    @Override
    public int rebuildAll() {
        kbChunkMapper.delete(new QueryWrapper<>());
        int count = 0;
        for (Courses c : coursesMapper.selectList(new QueryWrapper<>())) {
            syncCourse(c.getId());
            syncFeedback(c.getId());
            count++;
        }
        QueryWrapper<User> tq = new QueryWrapper<>();
        tq.eq("role", "teacher");
        for (User t : userMapper.selectList(tq)) {
            syncTeacherRating(t.getId());
        }
        for (CourseResource r : courseResourceMapper.selectList(new QueryWrapper<>())) {
            syncResource(r.getId());
        }
        for (QaPost p : qaPostMapper.selectList(new QueryWrapper<>())) {
            syncQa(p.getId());
        }
        return count;
    }

    @Override
    public List<KbChunk> search(String query, Long courseId, int topK) {
        if (query == null || query.trim().isEmpty()) return Collections.emptyList();
        List<String> kws = tokenize(query);
        if (kws.isEmpty()) return Collections.emptyList();

        // 优先：MATCH AGAINST ngram 全文检索
        try {
            QueryWrapper<KbChunk> q = new QueryWrapper<>();
            String boolean_query = kws.stream().map(k -> "+" + escape(k) + "*").collect(Collectors.joining(" "));
            q.apply("MATCH(title,content,keywords) AGAINST({0} IN BOOLEAN MODE)", boolean_query);
            if (courseId != null) {
                q.eq("course_id", courseId).or().isNull("course_id");
            }
            q.last("LIMIT " + Math.max(1, topK));
            List<KbChunk> hits = kbChunkMapper.selectList(q);
            if (!hits.isEmpty()) return hits;
        } catch (Exception ignore) {
            // 退化到 LIKE 兜底
        }
        return kbChunkMapper.searchByLike(kws, courseId, topK);
    }

    /* ===================== 工具方法 ===================== */

    private void upsert(String sourceType, Long sourceId, Long courseId, String title, String content) {
        QueryWrapper<KbChunk> qw = new QueryWrapper<>();
        qw.eq("source_type", sourceType).eq("source_id", sourceId);
        KbChunk exists = kbChunkMapper.selectOne(qw);
        String keywords = String.join(" ", tokenize(title + " " + content));
        if (exists == null) {
            KbChunk c = new KbChunk();
            c.setSourceType(sourceType);
            c.setSourceId(sourceId);
            c.setCourseId(courseId);
            c.setTitle(title);
            c.setContent(content);
            c.setKeywords(keywords);
            c.setTokens(content == null ? 0 : content.length());
            c.setCreatedAt(LocalDateTime.now());
            kbChunkMapper.insert(c);
        } else {
            exists.setCourseId(courseId);
            exists.setTitle(title);
            exists.setContent(content);
            exists.setKeywords(keywords);
            exists.setTokens(content == null ? 0 : content.length());
            kbChunkMapper.updateById(exists);
        }
    }

    private List<String> tokenize(String text) {
        if (text == null) return Collections.emptyList();
        Set<String> set = new LinkedHashSet<>();
        // 英文/数字按非字母数字切；中文按二元 ngram 切（粒度小，召回率高）。
        Matcher m = SPLIT.matcher(text);
        for (String token : m.replaceAll(" ").split("\\s+")) {
            if (token.length() < 2) continue;
            if (token.matches("[A-Za-z0-9]+")) {
                set.add(token.toLowerCase(Locale.ROOT));
            } else {
                for (int i = 0; i + 2 <= token.length(); i++) {
                    set.add(token.substring(i, i + 2));
                }
            }
            if (set.size() > 32) break;
        }
        return new ArrayList<>(set);
    }

    private String escape(String s) {
        return s.replaceAll("[+\\-><()~*\"@]", " ");
    }

    private static String safe(String s) { return s == null ? "" : s; }
}
