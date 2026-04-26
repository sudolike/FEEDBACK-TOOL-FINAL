package com.cen.service;

public interface ICalendarService {

    /**
     * 生成学生选课的 iCalendar (RFC 5545) 文本，可被 Android 系统日历直接导入。
     */
    String buildStudentICal(Long studentId);
}
