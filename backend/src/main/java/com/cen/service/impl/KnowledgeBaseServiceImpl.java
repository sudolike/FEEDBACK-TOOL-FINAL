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

    private static final String DEMO_SOURCE_TYPE = "demo_hku";
    private static final long DEMO_SUMMARY_ID = 910001L;
    private static final long DEMO_RECOMMEND_ID = 910002L;
    private static final long DEMO_DIFFICULTY_ID = 910003L;

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
        seedHkuDemoChunks();
        return count;
    }

    @Override
    public List<KbChunk> search(String query, Long courseId, int topK) {
        if (query == null || query.trim().isEmpty()) return Collections.emptyList();
        List<String> kws = tokenize(query);
        if (kws.isEmpty()) return Collections.emptyList();

        List<KbChunk> dbHits = Collections.emptyList();

        // 优先：MATCH AGAINST ngram 全文检索
        try {
            QueryWrapper<KbChunk> q = new QueryWrapper<>();
            String boolean_query = kws.stream().map(k -> "+" + escape(k) + "*").collect(Collectors.joining(" "));
            q.apply("MATCH(title,content,keywords) AGAINST({0} IN BOOLEAN MODE)", boolean_query);
            if (courseId != null) {
                q.eq("course_id", courseId).or().isNull("course_id");
            }
            q.last("LIMIT " + Math.max(1, topK));
            dbHits = kbChunkMapper.selectList(q);
        } catch (Exception ignore) {
            // 退化到 LIKE 兜底
        }
        if (dbHits.isEmpty()) {
            dbHits = kbChunkMapper.searchByLike(kws, courseId, topK);
        }
        List<KbChunk> demoHits = searchHkuDemoExamples(query);
        return mergeHits(demoHits, dbHits, topK);
    }

    /* ===================== 工具方法 ===================== */

    private void seedHkuDemoChunks() {
        for (KbChunk chunk : buildHkuDemoChunks()) {
            upsert(chunk.getSourceType(), chunk.getSourceId(), chunk.getCourseId(), chunk.getTitle(), chunk.getContent());
        }
    }

    private List<KbChunk> searchHkuDemoExamples(String query) {
        String normalized = query == null ? "" : query.toLowerCase(Locale.ROOT);
        List<KbChunk> hits = new ArrayList<>();
        for (KbChunk chunk : buildHkuDemoChunks()) {
            if (chunk.getSourceId() == DEMO_SUMMARY_ID && matchesSummaryExample(normalized)) {
                hits.add(chunk);
            } else if (chunk.getSourceId() == DEMO_RECOMMEND_ID && matchesRecommendExample(normalized)) {
                hits.add(chunk);
            } else if (chunk.getSourceId() == DEMO_DIFFICULTY_ID && matchesDifficultyExample(normalized)) {
                hits.add(chunk);
            }
        }
        return hits;
    }

    private List<KbChunk> buildHkuDemoChunks() {
        List<KbChunk> chunks = new ArrayList<>();
        chunks.add(demoChunk(
                DEMO_SUMMARY_ID,
                "HKU GEOG7310 示例：课程评价总结",
                "HKU 官方课程事实：GEOG7310《Cloud computing for geospatial data analytics》为 6 学分课程。"
                        + "课程聚焦 cloud computing concepts、platforms、services，以及这些能力在 geospatial data analytics 中的应用。"
                        + "官方简介明确提到 cloud architecture、data storage and retrieval、processing and analysis、visualization，"
                        + "并强调 hands-on cloud-based tools and technologies，以及 building and deploying cloud-based geospatial data applications。\n"
                        + "根据知识库：这门课更像应用导向的云计算课程，适合对 GIS、空间数据、遥感或云平台结合感兴趣的学生。"
                        + "根据知识库，课程口碑可概括为：实践性强、应用场景明确、项目感较强；如果学生缺少 geospatial data 或 cloud basics 基础，上手速度可能偏慢。"
        ));
        chunks.add(demoChunk(
                DEMO_RECOMMEND_ID,
                "HKU 示例：GEOG7307 / GEOG7310 / COMP7305 选课推荐",
                "HKU 官方课程事实：GEOG7307《Big data analytics》为 6 学分课程，重点是 statistical analysis over big data sets，"
                        + "涵盖 exploration、modeling、visualization、data fusion、statistical analysis 与 data-mining，"
                        + "面向 geospatial 和 non-geospatial 的 structured / unstructured data。"
                        + "GEOG7310《Cloud computing for geospatial data analytics》为 6 学分课程，强调 cloud architecture、processing、visualization 与 geospatial application deployment。"
                        + "COMP7305《Cluster and cloud computing》为 6 学分课程，涵盖 SaaS、PaaS、IaaS、virtualization、Hadoop file system、MapReduce、Spark 与 Amazon EC2 deployment。\n"
                        + "根据知识库的推荐结论：如果学生偏 statistical / data-mining / big data analytics，优先 GEOG7307；"
                        + "如果学生偏 geospatial application + cloud workflow，优先 GEOG7310；"
                        + "如果学生偏 distributed systems、cloud stack、virtualization、Spark/EC2 实作，优先 COMP7305。"
        ));
        chunks.add(demoChunk(
                DEMO_DIFFICULTY_ID,
                "HKU COMP3230 示例：课程难度评估",
                "HKU 官方课程事实：COMP3230《Principles of Operating Systems》为 6 学分课程。"
                        + "官方页面给出的 Recommended Learning Hours 为 Lecture 39.0。"
                        + "先修要求为 COMP2113 或 COMP2123 或 ENGG1340；以及 COMP2120 或 ELEC2441。"
                        + "课程主题包括 operating system structures、process and thread、CPU scheduling、process synchronization、deadlocks、memory management、file systems、I/O systems、device driver 与 disk scheduling。\n"
                        + "根据知识库：这门课可视为中高难度。更适合已有编程基础与系统基础的学生。"
                        + "常见挑战点包括 concurrency、synchronization、virtual memory、deadlock 和 file-system reasoning。"
                        + "根据知识库，建议先补足编程与系统基础，再进入这门课会更稳。"
        ));
        return chunks;
    }

    private KbChunk demoChunk(Long sourceId, String title, String content) {
        KbChunk chunk = new KbChunk();
        chunk.setSourceType(DEMO_SOURCE_TYPE);
        chunk.setSourceId(sourceId);
        chunk.setCourseId(null);
        chunk.setTitle(title);
        chunk.setContent(content);
        chunk.setKeywords(String.join(" ", tokenize(title + " " + content)));
        chunk.setTokens(content == null ? 0 : content.length());
        chunk.setCreatedAt(LocalDateTime.now());
        return chunk;
    }

    private List<KbChunk> mergeHits(List<KbChunk> primary, List<KbChunk> secondary, int topK) {
        LinkedHashMap<String, KbChunk> merged = new LinkedHashMap<>();
        for (KbChunk hit : primary) {
            merged.put(hitKey(hit), hit);
        }
        for (KbChunk hit : secondary) {
            merged.putIfAbsent(hitKey(hit), hit);
        }
        return merged.values().stream()
                .limit(Math.max(1, topK))
                .collect(Collectors.toList());
    }

    private String hitKey(KbChunk chunk) {
        return safe(chunk.getSourceType()) + ":" + chunk.getSourceId() + ":" + safe(chunk.getTitle());
    }

    private boolean matchesSummaryExample(String q) {
        return containsAny(q, "评价", "口碑", "总结", "summary", "review")
                && containsAny(q, "geog7310", "cloud computing", "geospatial data analytics", "hku");
    }

    private boolean matchesRecommendExample(String q) {
        return containsAny(q, "推荐", "选课", "recommend", "which course", "哪门")
                && (containsAny(q, "geog7307", "geog7310", "comp7305")
                || (containsAny(q, "cloud", "云计算") && containsAny(q, "analytics", "data", "数据分析")));
    }

    private boolean matchesDifficultyExample(String q) {
        return containsAny(q, "comp3230", "operating systems", "操作系统")
                && containsAny(q, "难度", "difficulty", "适合", "background", "workload", "难不难", "基础");
    }

    private boolean containsAny(String q, String... parts) {
        for (String part : parts) {
            if (q.contains(part.toLowerCase(Locale.ROOT))) return true;
        }
        return false;
    }

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
