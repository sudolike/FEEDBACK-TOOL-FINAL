package com.cen.controller;

import com.cen.common.Result;
import com.cen.entity.CourseResource;
import com.cen.service.ICourseResourceService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 课程资料：教师/学生均可上传，按 category 分类（lecture/recording/code/other）。
 * 文件上传仍走 /file/upload 拿到 url 后，再来这里建条目。
 */
@RestController
@RequestMapping("/courseResource")
public class CourseResourceController {

    @Resource private ICourseResourceService courseResourceService;

    @PostMapping("/save")
    public Result save(@RequestBody CourseResource resource) {
        return Result.success(courseResourceService.save(resource));
    }

    @PostMapping("/delete/{id}")
    public Result delete(@PathVariable Long id) {
        return Result.success(courseResourceService.removeById(id));
    }

    @GetMapping("/course/{courseId}")
    public Result listByCourse(@PathVariable Long courseId,
                               @RequestParam(required = false) String category) {
        return Result.success(courseResourceService.listByCourse(courseId, category));
    }

    @GetMapping("/{id}")
    public Result findOne(@PathVariable Long id) {
        return Result.success(courseResourceService.getById(id));
    }

    @PostMapping("/download/{id}")
    public Result onDownload(@PathVariable Long id) {
        courseResourceService.incrementDownload(id);
        return Result.success();
    }
}
