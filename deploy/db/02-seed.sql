-- ============================================================
-- Feedback Tool - 种子数据（首次启动自动注入）
--
-- 安全策略：
--   1. 不再注入 admin/admin 弱口令账号
--   2. 仅内置 2 个长复杂密码的管理员账号供项目组成员使用
--   3. 仅保留可对外暴露的演示用 teacher / student 账号
--   4. 应用启动时由 DatabaseInitializer 反向校验：
--      - 若 admin 角色账号缺失，自动补齐这 2 条
--      - 若 admin 弱口令账号存在(admin/admin)，强制清除
-- ============================================================

USE `feedback_1`;

INSERT INTO `sys_role` (`id`,`name`,`description`,`flag`) VALUES
  (1, '管理员', '系统管理员（不开放注册，仅内置）', 'admin'),
  (2, '学生',   '查看问卷和提交反馈',                'student'),
  (3, '教师',   '发布问卷与申请课程，等待管理员审批', 'teacher')
ON DUPLICATE KEY UPDATE name=VALUES(name), description=VALUES(description);

-- 内置管理员账号（默认密码必须强制使用，并在 README 中提示首次登录后修改）
-- admin01 / Admin@Cen2026!Feedback
-- admin02 / Cen#Admin2026!Master
INSERT INTO `sys_user` (`id`,`username`,`password`,`nickname`,`email`,`role_id`,`status`,`role`) VALUES
  (101, 'admin01', 'Admin@Cen2026!Feedback', '系统管理员-A', NULL, 1, 1, 'admin'),
  (102, 'admin02', 'Cen#Admin2026!Master',   '系统管理员-B', NULL, 1, 1, 'admin')
ON DUPLICATE KEY UPDATE
  password=VALUES(password),
  nickname=VALUES(nickname),
  role=VALUES(role),
  role_id=VALUES(role_id),
  status=VALUES(status);

-- 示范用 teacher / student 账号（公开演示，可随时注册新账号）
INSERT INTO `sys_user` (`id`,`username`,`password`,`nickname`,`email`,`role_id`,`status`,`role`) VALUES
  (2, 'teacher', 'teacher', 'Demo Teacher',  NULL, 3, 1, 'teacher'),
  (3, 'student', 'student', 'Demo Student',  NULL, 2, 1, 'student')
ON DUPLICATE KEY UPDATE nickname=VALUES(nickname), role=VALUES(role);

-- 强制清除历史 admin/admin 弱口令账号（如存在）
DELETE FROM `sys_user`
 WHERE `username` = 'admin'
   AND `password` = 'admin';

-- 示例课程（默认 status=approved，让演示账号即开即用）
INSERT INTO `sys_courses`
  (`id`,`name`,`code`,`teacher_id`,`description`,`academic_year`,`semester`,`course_time`,`location`,`status`,`reviewed_by`,`reviewed_at`)
VALUES
  (1, 'Cloud Computing',                       'CS501',   2, '云计算原理与实践，包含 IaaS / PaaS / SaaS 三层架构、虚拟化、容器化与 Serverless。', '2025-2026', 1, 'Tuesday 14:00-16:00', 'Building A 301', 'approved', 101, NOW()),
  (2, 'Smart Infrastructure & Data Analytics', 'SIDA101', 2, '面向智慧基础设施的数据分析方法。',                                                           '2025-2026', 1, 'Saturday 15:00-17:00', 'Building B 102', 'approved', 101, NOW())
ON DUPLICATE KEY UPDATE name=VALUES(name), status=VALUES(status);

INSERT INTO `sys_course_students` (`course_id`,`student_id`) VALUES
  (1, 3), (2, 3)
ON DUPLICATE KEY UPDATE student_id=VALUES(student_id);
