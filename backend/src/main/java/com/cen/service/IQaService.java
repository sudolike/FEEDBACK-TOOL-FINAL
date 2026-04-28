package com.cen.service;

import com.cen.entity.QaPost;
import com.cen.entity.QaReply;

import java.util.List;
import java.util.Map;

/**
 * 课程问答区。
 */
public interface IQaService {

    QaPost createPost(QaPost post);

    QaReply createReply(QaReply reply);

    List<QaPost> listPosts(Long courseId);

    Map<String, Object> postDetail(Long postId);

    boolean acceptReply(Long replyId);
}
