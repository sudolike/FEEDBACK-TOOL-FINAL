package com.cen.controller;

import com.cen.entity.Courses;
import com.cen.mapper.CoursesMapper;
import com.cen.service.ICalendarService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 学生课程日历
 *  - GET /calendar/student/{id}/json    返回 JSON 列表（前端自己渲染）
 *  - GET /calendar/student/{id}/ics     返回标准 iCal 文本（可被系统日历直接导入）
 */
@RestController
@RequestMapping("/calendar")
public class CalendarController {

    @Resource private ICalendarService calendarService;
    @Resource private CoursesMapper coursesMapper;

    @GetMapping("/student/{id}/json")
    public List<Courses> json(@PathVariable("id") Long studentId) {
        return coursesMapper.getCoursesByStudentId(studentId);
    }

    @GetMapping(value = "/student/{id}/ics", produces = "text/calendar;charset=UTF-8")
    public ResponseEntity<String> ics(@PathVariable("id") Long studentId) {
        String body = calendarService.buildStudentICal(studentId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/calendar;charset=UTF-8"));
        headers.setContentDispositionFormData("attachment", "courses-" + studentId + ".ics");
        return new ResponseEntity<>(body, headers, 200);
    }
}
