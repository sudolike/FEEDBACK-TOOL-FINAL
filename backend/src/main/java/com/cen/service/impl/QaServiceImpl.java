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
    public List<Map<String, Object>> listPosts(Long courseId) {
        QueryWrapper<QaPost> qw = new QueryWrapper<>();
        qw.eq("course_id", courseId).orderByDesc("created_at");
        List<QaPost> posts = qaPostMapper.selectList(qw);
        if (posts.isEmpty()) return Collections.emptyList();

        Set<Long> ids = posts.stream().map(QaPost::getAuthorId).collect(Collectors.toSet());
        Map<Long, User> userMap = userMapper.selectBatchIds(ids).stream().collect(Collectors.toMap(User::getId, u -> u));

        return posts.stream().map(p -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("post", p);
            User u = userMap.get(p.getAuthorId());
            m.put("authorName", u == null ? null : (u.getNickname() == null ? u.getUsername() : u.getNickname()));
            m.put("authorAvatar", u == null ? null : u.getAvatarUrl());
            return m;
        }).collect(Collectors.toList());
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
        uids.add(post.getAuthorId());
        replies.forEach(r -> uids.add(r.getAuthorId()));
        Map<Long, User> userMap = uids.isEmpty() ? Collections.emptyMap()
                : userMapper.selectBatchIds(uids).stream().collect(Collectors.toMap(User::getId, u -> u));

        Map<String, Object> ret = new LinkedHashMap<>();
        ret.put("post", post);
        User pa = userMap.get(post.getAuthorId());
        ret.put("authorName", pa == null ? null : (pa.getNickname() == null ? pa.getUsername() : pa.getNickname()));
        ret.put("authorAvatar", pa == null ? null : pa.getAvatarUrl());

        List<Map<String, Object>> rls = new ArrayList<>();
        for (QaReply r : replies) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("reply", r);
            User u = userMap.get(r.getAuthorId());
            m.put("authorName", u == null ? null : (u.getNickname() == null ? u.getUsername() : u.getNickname()));
            m.put("authorAvatar", u == null ? null : u.getAvatarUrl());
            rls.add(m);
        }
        ret.put("replies", rls);
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
