package com.cen.service.impl;

import com.cen.entity.Courses;
import com.cen.mapper.CoursesMapper;
import com.cen.service.ICalendarService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 把 sys_courses.course_time 字符串解析成 iCal VEVENT。
 * 支持的常见格式：
 *   "Tuesday 14:00-16:00"
 *   "周二 14:00-16:00"
 *   "Mon 09:00-10:50"
 *   "Wednesday 9:00 AM - 10:30 AM"  （兜底）
 * 解析失败的课程将被跳过，不会让整个导出失败。
 */
@Service
public class CalendarServiceImpl implements ICalendarService {

    @Resource private CoursesMapper coursesMapper;

    private static final DateTimeFormatter DT_UTC = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'", Locale.ROOT);
    private static final DateTimeFormatter DT_LOCAL = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss", Locale.ROOT);

    private static final Map<String, DayOfWeek> WEEK_MAP = new HashMap<>();
    static {
        WEEK_MAP.put("monday", DayOfWeek.MONDAY);
        WEEK_MAP.put("mon", DayOfWeek.MONDAY);
        WEEK_MAP.put("tuesday", DayOfWeek.TUESDAY);
        WEEK_MAP.put("tue", DayOfWeek.TUESDAY);
        WEEK_MAP.put("wednesday", DayOfWeek.WEDNESDAY);
        WEEK_MAP.put("wed", DayOfWeek.WEDNESDAY);
        WEEK_MAP.put("thursday", DayOfWeek.THURSDAY);
        WEEK_MAP.put("thu", DayOfWeek.THURSDAY);
        WEEK_MAP.put("friday", DayOfWeek.FRIDAY);
        WEEK_MAP.put("fri", DayOfWeek.FRIDAY);
        WEEK_MAP.put("saturday", DayOfWeek.SATURDAY);
        WEEK_MAP.put("sat", DayOfWeek.SATURDAY);
        WEEK_MAP.put("sunday", DayOfWeek.SUNDAY);
        WEEK_MAP.put("sun", DayOfWeek.SUNDAY);
        WEEK_MAP.put("周一", DayOfWeek.MONDAY);
        WEEK_MAP.put("周二", DayOfWeek.TUESDAY);
        WEEK_MAP.put("周三", DayOfWeek.WEDNESDAY);
        WEEK_MAP.put("周四", DayOfWeek.THURSDAY);
        WEEK_MAP.put("周五", DayOfWeek.FRIDAY);
        WEEK_MAP.put("周六", DayOfWeek.SATURDAY);
        WEEK_MAP.put("周日", DayOfWeek.SUNDAY);
        WEEK_MAP.put("星期一", DayOfWeek.MONDAY);
        WEEK_MAP.put("星期二", DayOfWeek.TUESDAY);
        WEEK_MAP.put("星期三", DayOfWeek.WEDNESDAY);
        WEEK_MAP.put("星期四", DayOfWeek.THURSDAY);
        WEEK_MAP.put("星期五", DayOfWeek.FRIDAY);
        WEEK_MAP.put("星期六", DayOfWeek.SATURDAY);
        WEEK_MAP.put("星期日", DayOfWeek.SUNDAY);
        WEEK_MAP.put("星期天", DayOfWeek.SUNDAY);
    }

    private static final Pattern TIME_RANGE = Pattern.compile("(\\d{1,2}):(\\d{2})\\s*[-~–至到]\\s*(\\d{1,2}):(\\d{2})");

    @Override
    public String buildStudentICal(Long studentId) {
        List<Courses> courses = coursesMapper.getCoursesByStudentId(studentId);

        StringBuilder sb = new StringBuilder();
        sb.append("BEGIN:VCALENDAR\r\n");
        sb.append("VERSION:2.0\r\n");
        sb.append("PRODID:-//FeedbackTool//Course Schedule//EN\r\n");
        sb.append("CALSCALE:GREGORIAN\r\n");
        sb.append("METHOD:PUBLISH\r\n");

        ZonedDateTime nowUtc = ZonedDateTime.now(java.time.ZoneOffset.UTC);
        for (Courses c : courses) {
            try {
                buildEvent(sb, c, nowUtc);
            } catch (Exception ignore) {}
        }

        sb.append("END:VCALENDAR\r\n");
        return sb.toString();
    }

    private void buildEvent(StringBuilder sb, Courses c, ZonedDateTime nowUtc) {
        if (c.getCourseTime() == null || c.getCourseTime().trim().isEmpty()) return;
        String text = c.getCourseTime().trim().toLowerCase(Locale.ROOT);

        DayOfWeek dow = null;
        for (Map.Entry<String, DayOfWeek> e : WEEK_MAP.entrySet()) {
            if (text.contains(e.getKey())) { dow = e.getValue(); break; }
        }
        if (dow == null) return;

        Matcher m = TIME_RANGE.matcher(text);
        if (!m.find()) return;
        int sh = Integer.parseInt(m.group(1));
        int sm = Integer.parseInt(m.group(2));
        int eh = Integer.parseInt(m.group(3));
        int em = Integer.parseInt(m.group(4));

        LocalDate today = LocalDate.now();
        int diff = (dow.getValue() - today.getDayOfWeek().getValue() + 7) % 7;
        LocalDate firstClass = today.plusDays(diff);

        String startLocal = firstClass.atTime(LocalTime.of(sh, sm)).format(DT_LOCAL);
        String endLocal = firstClass.atTime(LocalTime.of(eh, em)).format(DT_LOCAL);

        String uid = "course-" + c.getId() + "@feedback-tool";

        sb.append("BEGIN:VEVENT\r\n");
        sb.append("UID:").append(uid).append("\r\n");
        sb.append("DTSTAMP:").append(nowUtc.format(DT_UTC)).append("\r\n");
        sb.append("DTSTART;TZID=Asia/Shanghai:").append(startLocal).append("\r\n");
        sb.append("DTEND;TZID=Asia/Shanghai:").append(endLocal).append("\r\n");
        sb.append("RRULE:FREQ=WEEKLY;COUNT=16").append("\r\n");
        sb.append("SUMMARY:").append(escape(c.getName() == null ? c.getCode() : c.getName())).append("\r\n");
        if (c.getLocation() != null) {
            sb.append("LOCATION:").append(escape(c.getLocation())).append("\r\n");
        }
        if (c.getDescription() != null) {
            sb.append("DESCRIPTION:").append(escape(c.getDescription())).append("\r\n");
        }
        sb.append("END:VEVENT\r\n");
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace(",", "\\,").replace(";", "\\;").replace("\n", "\\n");
    }
}
