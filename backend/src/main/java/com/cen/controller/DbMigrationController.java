package com.cen.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/migrate")
public class DbMigrationController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/add-course-time")
    public String addCourseTimeColumn() {
        try {
            jdbcTemplate.execute("ALTER TABLE sys_courses ADD COLUMN course_time VARCHAR(255) DEFAULT NULL");
            return "Migration successful: 'course_time' column added.";
        } catch (Exception e) {
            return "Migration failed (or column already exists): " + e.getMessage();
        }
    }
}
