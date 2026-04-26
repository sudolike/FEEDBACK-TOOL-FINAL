package com.cen.service;

import com.cen.entity.KbChunk;

import java.util.List;

/**
 * RAG 检索能力（基于 MySQL 全文索引/LIKE 兜底）。
 */
public interface IKnowledgeBaseService {

    /** 同步课程到知识库（创建/更新课程时调用，可幂等） */
    void syncCourse(Long courseId);

    /** 同步学生评价到知识库 */
    void syncFeedback(Long courseId);

    /** 同步教师评分到知识库 */
    void syncTeacherRating(Long teacherId);

    /** 同步课程资料元信息（不读内容） */
    void syncResource(Long resourceId);

    /** 同步问答区帖子 */
    void syncQa(Long postId);

    /** 全量重建（一次性）。生产环境慎用。 */
    int rebuildAll();

    /** 关键词检索 */
    List<KbChunk> search(String query, Long courseId, int topK);
}
