package com.cen.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cen.entity.QaPost;
import com.cen.entity.QaReply;
import com.cen.entity.User;
import com.cen.mapper.QaPostMapper;
import com.cen.mapper.QaReplyMapper;
import com.cen.mapper.UserMapper;
import com.cen.service.IKnowledgeBaseService;
import com.cen.service.IQaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class QaServiceImpl implements IQaService {

    @Resource private QaPostMapper qaPostMapper;
    @Resource private QaReplyMapper qaReplyMapper;
    @Resource private UserMapper userMapper;
    @Resource private IKnowledgeBaseService knowledgeBaseService;

    @Override
    @Transactional
    public QaPost createPost(QaPost post) {
        post.setViewCount(0);
        post.setReplyCount(0);
        post.setIsResolved(0);
        post.setCreatedAt(LocalDateTime.now());
        post.setUpdatedAt(LocalDateTime.now());
        qaPostMapper.insert(post);
        try { knowledgeBaseService.syncQa(post.getId()); } catch (Exception ignore) {}
        return post;
    }

    @Override
    @Transactional
    public QaReply createReply(QaReply reply) {
        reply.setIsAccepted(0);
        reply.setCreatedAt(LocalDateTime.now());
        qaReplyMapper.insert(reply);
        qaPostMapper.incrReplyCount(reply.getPostId());
        return reply;
    }

    @Override
    public List<QaPost> listPosts(Long courseId) {
        QueryWrapper<QaPost> qw = new QueryWrapper<>();
        qw.eq("course_id", courseId).orderByDesc("created_at");
        List<QaPost> posts = qaPostMapper.selectList(qw);
        if (posts.isEmpty()) return Collections.emptyList();

        Set<Long> ids = posts.stream()
                .map(QaPost::getAuthorId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, User> userMap = ids.isEmpty() ? Collections.emptyMap()
                : userMapper.selectBatchIds(ids).stream()
                        .collect(Collectors.toMap(User::getId, u -> u));

        for (QaPost p : posts) {
            User u = userMap.get(p.getAuthorId());
            if (u != null) {
                p.setAuthorName(u.getNickname() == null ? u.getUsername() : u.getNickname());
                p.setAuthorAvatar(u.getAvatarUrl());
            }
        }
        return posts;
    }

    @Override
    public Map<String, Object> postDetail(Long postId) {
        QaPost post = qaPostMapper.selectById(postId);
        if (post == null) throw new IllegalArgumentException("帖子不存在");
        qaPostMapper.incrViewCount(postId);

        QueryWrapper<QaReply> qw = new QueryWrapper<>();
        qw.eq("post_id", postId).orderByAsc("created_at");
        List<QaReply> replies = qaReplyMapper.selectList(qw);

        Set<Long> uids = new HashSet<>();
        if (post.getAuthorId() != null) uids.add(post.getAuthorId());
        replies.forEach(r -> { if (r.getAuthorId() != null) uids.add(r.getAuthorId()); });
        Map<Long, User> userMap = uids.isEmpty() ? Collections.emptyMap()
                : userMapper.selectBatchIds(uids).stream()
                        .collect(Collectors.toMap(User::getId, u -> u));

        // 注入作者信息到实体（@TableField(exist=false) 字段，仅序列化）
        User pa = userMap.get(post.getAuthorId());
        if (pa != null) {
            post.setAuthorName(pa.getNickname() == null ? pa.getUsername() : pa.getNickname());
            post.setAuthorAvatar(pa.getAvatarUrl());
        }
        for (QaReply r : replies) {
            User u = userMap.get(r.getAuthorId());
            if (u != null) {
                r.setAuthorName(u.getNickname() == null ? u.getUsername() : u.getNickname());
                r.setAuthorAvatar(u.getAvatarUrl());
            }
        }

        // 返回结构与 Android QaPostDetail 一致：{post, replies}
        Map<String, Object> ret = new LinkedHashMap<>();
        ret.put("post", post);
        ret.put("replies", replies);
        return ret;
    }

    @Override
    @Transactional
    public boolean acceptReply(Long replyId) {
        QaReply reply = qaReplyMapper.selectById(replyId);
        if (reply == null) return false;
        reply.setIsAccepted(1);
        qaReplyMapper.updateById(reply);

        QaPost post = qaPostMapper.selectById(reply.getPostId());
        if (post != null && (post.getIsResolved() == null || post.getIsResolved() == 0)) {
            post.setIsResolved(1);
            qaPostMapper.updateById(post);
        }
        return true;
    }
}
