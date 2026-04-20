package com.cen.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseInitializer implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        try {
            // Check if column exists, if not add it
            jdbcTemplate.execute("ALTER TABLE sys_courses ADD COLUMN course_time VARCHAR(255) DEFAULT NULL");
            System.out.println("Database migration: 'course_time' column added successfully.");
        } catch (Exception e) {
            // Column likely already exists or table doesn't exist
            System.out.println("Database migration skipped (column might already exist): " + e.getMessage());
        }
    }
}
