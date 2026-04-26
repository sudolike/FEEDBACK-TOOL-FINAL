-- ============================================================
-- Feedback Tool - 完整数据库 schema
-- 适用于 MySQL 8.0+
-- 包含原有表 + 新增的资料/作业/问答/RAG/教师评分等表
-- ============================================================

CREATE DATABASE IF NOT EXISTS `feedback_1`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE `feedback_1`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- 用户表
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `username` varchar(255) DEFAULT NULL COMMENT '用户名',
  `password` varchar(255) DEFAULT NULL COMMENT '密码',
  `nickname` varchar(255) DEFAULT NULL COMMENT '昵称',
  `avatar_url` longtext COMMENT '头像',
  `email` varchar(64) DEFAULT NULL COMMENT '邮箱',
  `role_id` int DEFAULT NULL COMMENT '角色ID',
  `status` tinyint DEFAULT '1' COMMENT '是否有效 1有效 0无效',
  `role` varchar(64) DEFAULT NULL COMMENT '角色 admin/teacher/student',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ----------------------------
-- 角色表
-- ----------------------------
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(50) DEFAULT NULL COMMENT '名称',
  `description` varchar(255) DEFAULT NULL COMMENT '描述',
  `flag` varchar(64) DEFAULT NULL COMMENT '唯一标识',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- ----------------------------
-- 课程表（含管理员审批字段）
-- ----------------------------
DROP TABLE IF EXISTS `sys_courses`;
CREATE TABLE `sys_courses` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL COMMENT '课程名称',
  `code` varchar(50) NOT NULL COMMENT '课程代码',
  `teacher_id` bigint NOT NULL COMMENT '授课教师ID',
  `description` text COMMENT '课程描述',
  `cover_url` varchar(512) DEFAULT NULL COMMENT '课程封面图',
  `academic_year` varchar(20) DEFAULT NULL COMMENT '学年',
  `semester` tinyint DEFAULT NULL COMMENT '学期 1春/2秋',
  `course_time` varchar(255) DEFAULT NULL COMMENT '上课时间',
  `location` varchar(255) DEFAULT NULL COMMENT '上课地点',
  `status` varchar(16) NOT NULL DEFAULT 'pending' COMMENT 'pending待审批/approved已通过/rejected已驳回',
  `reject_reason` varchar(512) DEFAULT NULL COMMENT '驳回理由',
  `reviewed_by` bigint DEFAULT NULL COMMENT '审批人(管理员ID)',
  `reviewed_at` datetime DEFAULT NULL COMMENT '审批时间',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`),
  KEY `idx_teacher` (`teacher_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程表';

-- ----------------------------
-- 选课关系表
-- ----------------------------
DROP TABLE IF EXISTS `sys_course_students`;
CREATE TABLE `sys_course_students` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `course_id` bigint NOT NULL,
  `student_id` bigint NOT NULL,
  `status` varchar(16) NOT NULL DEFAULT 'approved' COMMENT 'pending/approved/rejected',
  `source` varchar(20) NOT NULL DEFAULT 'student_apply' COMMENT 'student_apply/teacher_invite',
  `apply_message` varchar(512) DEFAULT NULL COMMENT '学生申请留言',
  `reject_reason` varchar(512) DEFAULT NULL COMMENT '驳回原因',
  `reviewed_at` datetime DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_course_student` (`course_id`,`student_id`),
  KEY `idx_student` (`student_id`),
  KEY `idx_cs_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='选课/申请/邀请关系表';

-- ----------------------------
-- 问卷表
-- ----------------------------
DROP TABLE IF EXISTS `sys_questionnaires`;
CREATE TABLE `sys_questionnaires` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(255) NOT NULL COMMENT '问卷标题',
  `description` text COMMENT '问卷描述',
  `created_by` bigint NOT NULL COMMENT '创建者ID',
  `questions` json DEFAULT NULL COMMENT '问题列表(JSON数组)',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_creator` (`created_by`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='问卷表';

-- ----------------------------
-- 课程-问卷关联
-- ----------------------------
DROP TABLE IF EXISTS `sys_course_questionnaire`;
CREATE TABLE `sys_course_questionnaire` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `course_id` bigint NOT NULL,
  `questionnaire_id` bigint NOT NULL,
  `status` int DEFAULT '0' COMMENT '0待发布 1进行中 2已完成',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_course_questionnaire` (`course_id`,`questionnaire_id`),
  KEY `idx_questionnaire` (`questionnaire_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程问卷关联表';

-- ----------------------------
-- 问卷答案表（学生提交）
-- ----------------------------
DROP TABLE IF EXISTS `sys_questionnaire_responses`;
CREATE TABLE `sys_questionnaire_responses` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `course_id` bigint NOT NULL,
  `questionnaire_id` bigint NOT NULL,
  `student_id` bigint NOT NULL COMMENT '学生ID（仅服务端存储用于去重，对外接口匿名化）',
  `answers` text COMMENT '答案(JSON)',
  `submitted_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_course_questionnaire` (`course_id`,`questionnaire_id`),
  KEY `idx_student` (`student_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='问卷答案表';

-- ----------------------------
-- 课程评分（学生对课程评价）
-- ----------------------------
DROP TABLE IF EXISTS `sys_course_feedback`;
CREATE TABLE `sys_course_feedback` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `course_id` bigint NOT NULL,
  `student_id` bigint NOT NULL,
  `content` text NOT NULL COMMENT '评价内容',
  `rating` int DEFAULT NULL COMMENT '评分 1-5',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_course_student` (`course_id`,`student_id`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程评价表';

-- ----------------------------
-- 通用文件表
-- ----------------------------
DROP TABLE IF EXISTS `sys_file`;
CREATE TABLE `sys_file` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(256) DEFAULT NULL COMMENT '文件名',
  `url` varchar(512) DEFAULT NULL COMMENT '下载链接',
  `type` varchar(64) DEFAULT NULL COMMENT '类型',
  `md5` varchar(64) DEFAULT NULL,
  `size` bigint DEFAULT NULL,
  `enable` tinyint DEFAULT '1',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_md5` (`md5`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件表';

-- ============================================================
-- 以下为新增表（v2 功能：资料/作业/问答/教师评分/RAG）
-- ============================================================

-- ----------------------------
-- 课程资料表（PPT/PDF/视频/代码等）
-- ----------------------------
DROP TABLE IF EXISTS `sys_course_resource`;
CREATE TABLE `sys_course_resource` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `course_id` bigint NOT NULL COMMENT '所属课程',
  `uploader_id` bigint NOT NULL COMMENT '上传人',
  `uploader_role` varchar(32) DEFAULT NULL COMMENT 'teacher/student',
  `title` varchar(255) NOT NULL COMMENT '资料标题',
  `description` varchar(1024) DEFAULT NULL,
  `file_id` bigint DEFAULT NULL COMMENT '关联sys_file',
  `file_name` varchar(255) DEFAULT NULL,
  `file_url` varchar(512) DEFAULT NULL,
  `file_type` varchar(32) DEFAULT NULL COMMENT 'ppt/pdf/video/code/image/other',
  `file_size` bigint DEFAULT NULL,
  `category` varchar(32) DEFAULT 'lecture' COMMENT 'lecture课件/recording录播/code代码/other其他',
  `download_count` int DEFAULT '0',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_course` (`course_id`),
  KEY `idx_uploader` (`uploader_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程资料表';

-- ----------------------------
-- 教师评分表（学生对老师打分）
-- ----------------------------
DROP TABLE IF EXISTS `sys_teacher_rating`;
CREATE TABLE `sys_teacher_rating` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `course_id` bigint NOT NULL,
  `teacher_id` bigint NOT NULL,
  `student_id` bigint NOT NULL COMMENT '学生ID（对外匿名化）',
  `rating` int NOT NULL COMMENT '总分 1-5',
  `teaching_score` int DEFAULT NULL COMMENT '授课能力 1-5',
  `attitude_score` int DEFAULT NULL COMMENT '教学态度 1-5',
  `content_score` int DEFAULT NULL COMMENT '内容质量 1-5',
  `comment` text COMMENT '评论',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_course_teacher_student` (`course_id`,`teacher_id`,`student_id`),
  KEY `idx_teacher` (`teacher_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='教师评分表';

-- ----------------------------
-- 课程作业表
-- ----------------------------
DROP TABLE IF EXISTS `sys_assignment`;
CREATE TABLE `sys_assignment` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `course_id` bigint NOT NULL,
  `teacher_id` bigint NOT NULL,
  `title` varchar(255) NOT NULL,
  `description` text,
  `attachment_url` varchar(512) DEFAULT NULL,
  `attachment_name` varchar(255) DEFAULT NULL,
  `deadline` datetime DEFAULT NULL,
  `total_score` int DEFAULT '100',
  `status` tinyint DEFAULT '1' COMMENT '1已发布/0草稿/2已结束',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_course` (`course_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='作业表';

-- ----------------------------
-- 学生作业提交表
-- ----------------------------
DROP TABLE IF EXISTS `sys_assignment_submission`;
CREATE TABLE `sys_assignment_submission` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `assignment_id` bigint NOT NULL,
  `student_id` bigint NOT NULL,
  `content` text COMMENT '文字内容',
  `attachment_url` varchar(512) DEFAULT NULL,
  `attachment_name` varchar(255) DEFAULT NULL,
  `score` int DEFAULT NULL,
  `comment` varchar(1024) DEFAULT NULL COMMENT '老师评语',
  `submitted_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `graded_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_assignment_student` (`assignment_id`,`student_id`),
  KEY `idx_student` (`student_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='作业提交表';

-- ----------------------------
-- 课程问答区帖子
-- ----------------------------
DROP TABLE IF EXISTS `sys_qa_post`;
CREATE TABLE `sys_qa_post` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `course_id` bigint NOT NULL,
  `author_id` bigint NOT NULL,
  `author_role` varchar(32) DEFAULT NULL,
  `title` varchar(255) NOT NULL,
  `content` text NOT NULL,
  `view_count` int DEFAULT '0',
  `reply_count` int DEFAULT '0',
  `is_resolved` tinyint DEFAULT '0',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_course` (`course_id`),
  KEY `idx_author` (`author_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='问答区帖子';

-- ----------------------------
-- 问答区回复
-- ----------------------------
DROP TABLE IF EXISTS `sys_qa_reply`;
CREATE TABLE `sys_qa_reply` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `post_id` bigint NOT NULL,
  `author_id` bigint NOT NULL,
  `author_role` varchar(32) DEFAULT NULL,
  `content` text NOT NULL,
  `parent_id` bigint DEFAULT NULL COMMENT '回复的回复',
  `is_accepted` tinyint DEFAULT '0' COMMENT '是否被采纳',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_post` (`post_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='问答区回复';

-- ----------------------------
-- RAG 知识库分片（用于 AI 浮窗的检索增强）
-- ----------------------------
DROP TABLE IF EXISTS `sys_kb_chunk`;
CREATE TABLE `sys_kb_chunk` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `source_type` varchar(32) NOT NULL COMMENT 'course/feedback/teacher/resource/qa',
  `source_id` bigint DEFAULT NULL COMMENT '关联源id',
  `course_id` bigint DEFAULT NULL COMMENT '冗余 course_id 加速过滤',
  `title` varchar(255) DEFAULT NULL,
  `content` text NOT NULL COMMENT '可被检索的文本',
  `keywords` varchar(512) DEFAULT NULL COMMENT '空格分割的关键词，简易倒排',
  `tokens` int DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_source` (`source_type`,`source_id`),
  KEY `idx_course` (`course_id`),
  FULLTEXT KEY `ft_content` (`title`,`content`,`keywords`) /*!50700 WITH PARSER ngram */
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='RAG 知识库分片';

-- ----------------------------
-- AI 聊天历史（可选）
-- ----------------------------
DROP TABLE IF EXISTS `sys_chat_message`;
CREATE TABLE `sys_chat_message` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `session_id` varchar(64) NOT NULL COMMENT '会话ID',
  `role` varchar(32) NOT NULL COMMENT 'user/assistant/system',
  `content` text NOT NULL,
  `tokens` int DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_session` (`user_id`,`session_id`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 聊天历史';

SET FOREIGN_KEY_CHECKS = 1;
