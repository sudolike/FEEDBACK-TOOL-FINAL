package com.cen.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cen.entity.CourseResource;
import com.cen.mapper.CourseResourceMapper;
import com.cen.service.ICourseResourceService;
import com.cen.service.IKnowledgeBaseService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class CourseResourceServiceImpl extends ServiceImpl<CourseResourceMapper, CourseResource> implements ICourseResourceService {

    @Resource
    private IKnowledgeBaseService knowledgeBaseService;

    @Override
    public List<CourseResource> listByCourse(Long courseId, String category) {
        QueryWrapper<CourseResource> qw = new QueryWrapper<>();
        qw.eq("course_id", courseId).eq("is_deleted", 0);
        if (category != null && !category.isEmpty()) qw.eq("category", category);
        qw.orderByDesc("created_at");
        return list(qw);
    }

    @Override
    public boolean save(CourseResource entity) {
        boolean ok = super.save(entity);
        if (ok) knowledgeBaseService.syncResource(entity.getId());
        return ok;
    }

    @Override
    public void incrementDownload(Long resourceId) {
        UpdateWrapper<CourseResource> uw = new UpdateWrapper<>();
        uw.eq("id", resourceId).setSql("download_count = download_count + 1");
        update(uw);
    }
}
