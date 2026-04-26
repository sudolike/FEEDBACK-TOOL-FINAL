package com.cen.controller;

import com.cen.common.Result;
import com.cen.entity.QaPost;
import com.cen.entity.QaReply;
import com.cen.service.IQaService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 课程问答区
 */
@RestController
@RequestMapping("/qa")
public class QaController {

    @Resource private IQaService qaService;

    @PostMapping("/post")
    public Result createPost(@RequestBody QaPost post) {
        return Result.success(qaService.createPost(post));
    }

    @PostMapping("/reply")
    public Result reply(@RequestBody QaReply reply) {
        return Result.success(qaService.createReply(reply));
    }

    @GetMapping("/posts/{courseId}")
    public Result list(@PathVariable Long courseId) {
        return Result.success(qaService.listPosts(courseId));
    }

    @GetMapping("/post/{postId}")
    public Result detail(@PathVariable Long postId) {
        return Result.success(qaService.postDetail(postId));
    }

    @PostMapping("/accept/{replyId}")
    public Result accept(@PathVariable Long replyId) {
        return Result.success(qaService.acceptReply(replyId));
    }
}
