package com.cen.controller;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cen.entity.Questionnaires;
import org.apache.logging.log4j.util.Strings;
import org.springframework.web.bind.annotation.*;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cen.common.Result;
import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import com.cen.service.ICoursesService;
import com.cen.entity.Courses;

import org.springframework.web.bind.annotation.RestController;
import lombok.Data;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author wyt
 * @since 2025-03-14
 */
@RestController
@RequestMapping("/courses")
public class CoursesController {

    @Resource
    private ICoursesService coursesService;

    // 查询所有
    @GetMapping("/list")
    public Result list(@RequestParam(required = false) Long teacherId){
        QueryWrapper<Courses> queryWrapper = new QueryWrapper<>();
        if (teacherId != null) {
            queryWrapper.eq("teacher_id", teacherId);
        }
        return Result.success(coursesService.list(queryWrapper));
    }

    // 查询学生课程列表
    @GetMapping("/studentList")
    public Result studentList(@RequestParam Long studentId){
        return Result.success(coursesService.getCoursesByStudentId(studentId));
    }
    //新增或修改
    @PostMapping("/save")
    public Result save(@RequestBody Courses courses) {
        return Result.success(coursesService.saveOrUpdate(courses));
    }
    //删除
    @PostMapping("/delete")
    public Result delete(@RequestBody Courses courses){ //@RequestBody把前台的json对象转成java的对象
        return Result.success(coursesService.removeById(courses.getId()));
    }
    //批量删除
    @PostMapping("/del/batch")
    public Result Batch(@RequestBody List<Integer> ids){
        return Result.success(coursesService.removeBatchByIds(ids));
    }
    //根据id获取
    @GetMapping("/getById")
    public Result findOne(@PathVariable Courses courses) {
        return Result.success(coursesService.getById(courses.getId()));
    }

    /**
     * 获取全部课程列表并按学年学期分组 - 教师端My Courses页面的主要数据源
     * @param teacherId 教师ID，可选参数
     * @return 按学年和学期分组后的课程列表
     */
    @GetMapping("/allList")
    public Result allList(@RequestParam(required = false) Long teacherId) {
        // 创建查询条件
        QueryWrapper<Courses> queryWrapper = new QueryWrapper<>();
        // 如果提供了教师ID，则只查询该教师的课程
        if (teacherId != null) {
            queryWrapper.eq("teacher_id", teacherId);
        }
        // 按学年降序、学期升序、ID降序排序
        queryWrapper.orderByDesc("academic_year")
                   .orderByAsc("semester")
                   .orderByDesc("id");
        
        // 执行查询获取所有符合条件的课程
        List<Courses> coursesList = coursesService.list(queryWrapper);
        
        // 使用Java 8 Stream API按学年和学期进行多级分组
        // 第一级：按学年分组，并使用自定义比较器实现年份降序排列
        // 第二级：按学期分组
        Map<String, Map<Integer, List<Courses>>> groupedMap = coursesList.stream()
                .filter(c -> c.getAcademicYear() != null && c.getSemester() != null) // 过滤掉学年或学期为空的课程
                .collect(Collectors.groupingBy(
                        Courses::getAcademicYear, // 按学年分组
                        () -> new TreeMap<String, Map<Integer, List<Courses>>>((a, b) -> b.compareTo(a)), // 学年降序排序
                        Collectors.groupingBy(
                                Courses::getSemester, // 按学期分组
                                TreeMap::new, // 使用默认排序（升序）
                                Collectors.toList() // 收集到List中
                        )
                ));

        // 将嵌套Map转换为前端需要的数据结构（YearData和SemesterData对象的列表）
        List<YearData> resultList = groupedMap.entrySet().stream()
                .map(yearEntry -> {
                    // 创建学年数据对象
                    YearData yearData = new YearData();
                    yearData.setYear(yearEntry.getKey());
                    // 处理该学年下的所有学期
                    yearData.setSemesters(yearEntry.getValue().entrySet().stream()
                            .map(semesterEntry -> {
                                // 创建学期数据对象
                                SemesterData semesterData = new SemesterData();
                                semesterData.setSemester(semesterEntry.getKey());
                                semesterData.setCourses(semesterEntry.getValue());
                                return semesterData;
                            })
                            .collect(Collectors.toList()));
                    return yearData;
                })
                .collect(Collectors.toList());

        // 返回处理后的数据
        return Result.success(resultList);
    }

    @Data
    static class YearData {
        private String year;
        private List<SemesterData> semesters;
    }

    @Data
    static class SemesterData {
        private Integer semester;
        private List<Courses> courses;
    }

    //分页查询
    @GetMapping("/page")
    public Result findPage(@RequestParam(defaultValue = "1") Integer pageNum,
                           @RequestParam(defaultValue = "10") Integer pageSize,
                           @RequestParam(defaultValue = "") String name
                           ) {
        QueryWrapper<Courses> queryWrapper = new QueryWrapper<>();
        queryWrapper.like(Strings.isNotEmpty(name),"name",name);
        queryWrapper.orderByDesc("id"); //设置id倒序
        return Result.success(coursesService.page(new Page<>(pageNum, pageSize),queryWrapper));
    }

    // 查询学生关联的课程列表
    @GetMapping("/student/{studentId}")
    public Result getCoursesByStudentId(@PathVariable Long studentId) {
        return Result.success(coursesService.getCoursesByStudentId(studentId));
    }
}

