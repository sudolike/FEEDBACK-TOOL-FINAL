package com.cen.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 启动期数据库初始化 / 兼容性迁移。
 *
 * 职责：
 *   1. 老库无 course_time 列 → 自动 ADD COLUMN
 *   2. 老库无课程审批相关列   → 自动 ADD COLUMN
 *   3. 强制清除 admin/admin 等弱口令管理员账号
 *   4. 若 sys_user 中没有任何 admin 角色账号，则自动注入 2 个内置管理员
 *      （账号信息见 README，默认密码足够长且包含特殊字符）
 *   5. 历史课程 status 为空时回填 approved，避免存量课程被审批策略隐藏
 */
@Component
public class DatabaseInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseInitializer.class);

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        safeExec("ALTER TABLE sys_courses ADD COLUMN course_time VARCHAR(255) DEFAULT NULL",
                "course_time");
        safeExec("ALTER TABLE sys_courses ADD COLUMN status VARCHAR(16) NOT NULL DEFAULT 'pending'",
                "status");
        safeExec("ALTER TABLE sys_courses ADD COLUMN reject_reason VARCHAR(512) DEFAULT NULL",
                "reject_reason");
        safeExec("ALTER TABLE sys_courses ADD COLUMN reviewed_by BIGINT DEFAULT NULL",
                "reviewed_by");
        safeExec("ALTER TABLE sys_courses ADD COLUMN reviewed_at DATETIME DEFAULT NULL",
                "reviewed_at");
        safeExec("CREATE INDEX idx_status ON sys_courses(status)", "idx_status");

        try {
            int filled = jdbcTemplate.update(
                    "UPDATE sys_courses SET status='approved' " +
                            "WHERE status IS NULL OR status=''");
            if (filled > 0) {
                log.info("[DB-Init] backfill {} legacy courses to status=approved", filled);
            }
        } catch (Exception e) {
            log.warn("[DB-Init] backfill course status skipped: {}", e.getMessage());
        }

        try {
            int removed = jdbcTemplate.update(
                    "DELETE FROM sys_user WHERE username='admin' AND password='admin'");
            if (removed > 0) {
                log.warn("[DB-Init] removed {} legacy weak-password admin/admin account(s)", removed);
            }
        } catch (Exception e) {
            log.warn("[DB-Init] cleaning weak-password admin failed: {}", e.getMessage());
        }

        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM sys_user WHERE role='admin' AND status=1",
                    Integer.class);
            if (count == null || count < 1) {
                log.warn("[DB-Init] no admin found, injecting two built-in admin accounts");
                upsertBuiltinAdmin(101L, "admin01", "Admin@Cen2026!Feedback", "系统管理员-A");
                upsertBuiltinAdmin(102L, "admin02", "Cen#Admin2026!Master",   "系统管理员-B");
            }
        } catch (Exception e) {
            log.warn("[DB-Init] ensure built-in admins failed: {}", e.getMessage());
        }
    }

    private void upsertBuiltinAdmin(Long id, String username, String password, String nickname) {
        try {
            jdbcTemplate.update(
                    "INSERT INTO sys_user(id,username,password,nickname,role_id,status,role) " +
                            "VALUES(?,?,?,?,1,1,'admin') " +
                            "ON DUPLICATE KEY UPDATE password=VALUES(password), " +
                            "nickname=VALUES(nickname), role='admin', role_id=1, status=1",
                    id, username, password, nickname);
            log.info("[DB-Init] built-in admin ready: {}", username);
        } catch (Exception e) {
            log.error("[DB-Init] inject admin {} failed: {}", username, e.getMessage());
        }
    }

    private void safeExec(String sql, String label) {
        try {
            jdbcTemplate.execute(sql);
            log.info("[DB-Init] {} migrated", label);
        } catch (Exception e) {
            log.debug("[DB-Init] {} migration skipped: {}", label, e.getMessage());
        }
    }
}
