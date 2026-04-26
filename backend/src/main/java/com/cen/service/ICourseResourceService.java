package com.cen.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cen.entity.CourseResource;

import java.util.List;

public interface ICourseResourceService extends IService<CourseResource> {

    List<CourseResource> listByCourse(Long courseId, String category);

    void incrementDownload(Long resourceId);
}
