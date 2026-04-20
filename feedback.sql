/*
 Navicat Premium Dump SQL

 Source Server         : feedback
 Source Server Type    : MySQL
 Source Server Version : 80041 (8.0.41)
 Source Host           : localhost:3306
 Source Schema         : feedback_1

 Target Server Type    : MySQL
 Target Server Version : 80041 (8.0.41)
 File Encoding         : 65001

 Date: 26/02/2026 23:59:49
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for sys_course_feedback
-- ----------------------------
DROP TABLE IF EXISTS `sys_course_feedback`;
CREATE TABLE `sys_course_feedback`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `course_id` bigint NOT NULL COMMENT '课程ID',
  `student_id` bigint NOT NULL COMMENT '学生ID',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '反馈内容',
  `rating` int NULL DEFAULT NULL COMMENT '课程评分(1-5)',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_course_student`(`course_id` ASC, `student_id` ASC) USING BTREE,
  INDEX `idx_created_at`(`created_at` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '课程反馈表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_course_feedback
-- ----------------------------
INSERT INTO `sys_course_feedback` VALUES (1, 2, 2, '这课程吧，怎么说呢，好像就还行', 3, '2025-02-26 15:15:55', NULL);
INSERT INTO `sys_course_feedback` VALUES (2, 5, 24, '12345678', 5, '2025-03-01 22:11:36', NULL);
INSERT INTO `sys_course_feedback` VALUES (3, 6, 24, 'very Good', 5, '2025-04-09 20:34:24', NULL);
INSERT INTO `sys_course_feedback` VALUES (4, 6, 24, 'I don not understand the konwledge in the PowerPoint.', 5, '2025-04-09 20:36:05', NULL);
INSERT INTO `sys_course_feedback` VALUES (5, 6, 24, 'bad', 1, '2025-04-09 20:44:28', NULL);
INSERT INTO `sys_course_feedback` VALUES (6, 9, 26, 'just so so', 4, '2025-04-09 21:16:35', NULL);
INSERT INTO `sys_course_feedback` VALUES (7, 6, 28, '.....', 3, '2025-04-17 17:05:36', NULL);
INSERT INTO `sys_course_feedback` VALUES (8, 22, 29, 'It\'s OK', 4, '2026-02-26 16:58:00', NULL);

-- ----------------------------
-- Table structure for sys_course_questionnaire
-- ----------------------------
DROP TABLE IF EXISTS `sys_course_questionnaire`;
CREATE TABLE `sys_course_questionnaire`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `course_id` bigint NOT NULL COMMENT '课程ID',
  `questionnaire_id` bigint NOT NULL COMMENT '问卷ID',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '关联时间',
  `status` int NULL DEFAULT 0 COMMENT '问卷状态：0-待发布，1-进行中，2-已完成',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `unique_course_questionnaire`(`course_id` ASC, `questionnaire_id` ASC) USING BTREE,
  INDEX `questionnaire_id`(`questionnaire_id` ASC) USING BTREE,
  CONSTRAINT `sys_course_questionnaire_ibfk_1` FOREIGN KEY (`course_id`) REFERENCES `sys_courses` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `sys_course_questionnaire_ibfk_2` FOREIGN KEY (`questionnaire_id`) REFERENCES `sys_questionnaires` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 33 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_course_questionnaire
-- ----------------------------
INSERT INTO `sys_course_questionnaire` VALUES (7, 2, 1, '2025-02-25 14:59:53', 1);
INSERT INTO `sys_course_questionnaire` VALUES (9, 3, 1, '2025-02-25 16:40:33', 0);
INSERT INTO `sys_course_questionnaire` VALUES (12, 9, 4, '2025-03-28 20:33:56', 1);
INSERT INTO `sys_course_questionnaire` VALUES (13, 11, 4, '2025-03-28 20:36:46', 1);
INSERT INTO `sys_course_questionnaire` VALUES (17, 6, 6, '2025-04-17 12:58:10', 2);
INSERT INTO `sys_course_questionnaire` VALUES (23, 8, 3, '2025-05-08 17:33:16', 0);
INSERT INTO `sys_course_questionnaire` VALUES (24, 8, 5, '2025-05-08 17:33:22', 0);
INSERT INTO `sys_course_questionnaire` VALUES (26, 6, 5, '2025-05-17 21:30:12', 0);
INSERT INTO `sys_course_questionnaire` VALUES (27, 7, 5, '2025-05-18 14:00:55', 0);
INSERT INTO `sys_course_questionnaire` VALUES (28, 6, 7, '2025-05-18 14:35:42', 1);
INSERT INTO `sys_course_questionnaire` VALUES (29, 5, 7, '2025-05-18 14:52:05', 1);
INSERT INTO `sys_course_questionnaire` VALUES (30, 6, 3, '2025-05-18 15:07:51', 1);
INSERT INTO `sys_course_questionnaire` VALUES (31, 5, 5, '2026-02-26 22:15:41', 2);
INSERT INTO `sys_course_questionnaire` VALUES (32, 5, 6, '2026-02-26 22:59:46', 1);

-- ----------------------------
-- Table structure for sys_course_students
-- ----------------------------
DROP TABLE IF EXISTS `sys_course_students`;
CREATE TABLE `sys_course_students`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `course_id` bigint NOT NULL COMMENT '课程ID',
  `student_id` bigint NOT NULL COMMENT '学生ID',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '选课时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `unique_course_student`(`course_id` ASC, `student_id` ASC) USING BTREE,
  INDEX `student_id`(`student_id` ASC) USING BTREE,
  CONSTRAINT `sys_course_students_ibfk_1` FOREIGN KEY (`course_id`) REFERENCES `sys_courses` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `sys_course_students_ibfk_2` FOREIGN KEY (`student_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 41 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_course_students
-- ----------------------------
INSERT INTO `sys_course_students` VALUES (3, 2, 2, '2025-02-25 15:31:04');
INSERT INTO `sys_course_students` VALUES (7, 9, 26, '2025-03-28 20:31:21');
INSERT INTO `sys_course_students` VALUES (8, 9, 27, '2025-03-28 20:31:21');
INSERT INTO `sys_course_students` VALUES (9, 6, 28, '2025-04-17 12:59:33');
INSERT INTO `sys_course_students` VALUES (10, 6, 29, '2025-04-17 12:59:33');
INSERT INTO `sys_course_students` VALUES (11, 6, 30, '2025-04-17 12:59:33');
INSERT INTO `sys_course_students` VALUES (12, 6, 31, '2025-04-17 12:59:33');
INSERT INTO `sys_course_students` VALUES (14, 5, 29, '2025-04-17 12:59:45');
INSERT INTO `sys_course_students` VALUES (15, 5, 30, '2025-04-17 12:59:45');
INSERT INTO `sys_course_students` VALUES (16, 5, 31, '2025-04-17 12:59:45');
INSERT INTO `sys_course_students` VALUES (20, 5, 32, '2025-05-18 14:52:29');
INSERT INTO `sys_course_students` VALUES (21, 5, 33, '2025-05-18 14:52:29');
INSERT INTO `sys_course_students` VALUES (22, 5, 34, '2025-05-18 14:52:29');
INSERT INTO `sys_course_students` VALUES (23, 5, 35, '2025-05-18 14:52:29');
INSERT INTO `sys_course_students` VALUES (24, 5, 36, '2025-05-18 14:52:29');
INSERT INTO `sys_course_students` VALUES (25, 5, 37, '2025-05-18 14:52:29');
INSERT INTO `sys_course_students` VALUES (26, 6, 32, '2025-05-18 14:52:47');
INSERT INTO `sys_course_students` VALUES (27, 6, 33, '2025-05-18 14:52:47');
INSERT INTO `sys_course_students` VALUES (28, 6, 34, '2025-05-18 14:52:47');
INSERT INTO `sys_course_students` VALUES (29, 6, 35, '2025-05-18 14:52:47');
INSERT INTO `sys_course_students` VALUES (30, 6, 36, '2025-05-18 14:52:47');
INSERT INTO `sys_course_students` VALUES (31, 6, 37, '2025-05-18 14:52:47');
INSERT INTO `sys_course_students` VALUES (32, 5, 24, '2025-05-18 15:35:21');
INSERT INTO `sys_course_students` VALUES (33, 5, 2, '2025-05-18 15:35:21');
INSERT INTO `sys_course_students` VALUES (34, 5, 27, '2025-05-18 15:38:36');
INSERT INTO `sys_course_students` VALUES (35, 5, 26, '2025-05-18 15:38:36');
INSERT INTO `sys_course_students` VALUES (36, 6, 26, '2025-05-19 10:34:27');
INSERT INTO `sys_course_students` VALUES (38, 22, 29, '2026-02-26 16:23:55');
INSERT INTO `sys_course_students` VALUES (39, 7, 29, '2026-02-26 17:00:50');
INSERT INTO `sys_course_students` VALUES (40, 8, 29, '2026-02-26 17:01:53');

-- ----------------------------
-- Table structure for sys_courses
-- ----------------------------
DROP TABLE IF EXISTS `sys_courses`;
CREATE TABLE `sys_courses`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '课程名称',
  `code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '课程代码',
  `teacher_id` bigint NOT NULL COMMENT '授课教师ID',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `academic_year` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '学年(如2024-2025)',
  `semester` tinyint NULL DEFAULT NULL COMMENT '学期(1-春季学期,2-秋季学期)',
  `course_time` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `code`(`code` ASC) USING BTREE,
  INDEX `teacher_id`(`teacher_id` ASC) USING BTREE,
  CONSTRAINT `sys_courses_ibfk_1` FOREIGN KEY (`teacher_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 23 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_courses
-- ----------------------------
INSERT INTO `sys_courses` VALUES (2, '数学', 'MATH101', 21, '2025-02-24 11:21:04', '2025-02-24 11:21:04', '2025', 2, NULL);
INSERT INTO `sys_courses` VALUES (3, '英语', 'ENG102', 21, '2025-02-24 11:21:15', '2025-02-24 11:21:15', '2024', 1, NULL);
INSERT INTO `sys_courses` VALUES (4, '测试', '啊哈哈', 21, '2025-02-27 11:09:01', '2025-02-27 11:09:01', '2025', 2, NULL);
INSERT INTO `sys_courses` VALUES (5, 'Cloud Computing', '111', 23, '2025-03-01 22:06:53', '2026-02-26 16:59:51', '2025', 1, 'Tuesday 12:00-03:00');
INSERT INTO `sys_courses` VALUES (6, 'Smart Infrastructure and Data Analytics', 'SIDA2025-1', 23, '2025-03-17 16:25:51', '2026-02-26 17:00:33', '2025', 1, 'Saturday 15:00-18:00');
INSERT INTO `sys_courses` VALUES (7, '123', '2', 23, '2025-03-26 23:37:22', '2026-02-26 17:01:13', '2020', 2, 'Wednesday 11:10-14:01');
INSERT INTO `sys_courses` VALUES (8, 'Operation System', '1111111', 23, '2025-03-28 19:52:10', '2026-02-26 17:01:44', '2025', 2, 'Thursday 19:35-22:00');
INSERT INTO `sys_courses` VALUES (9, 'Java', 'J01', 25, '2025-03-28 20:18:09', '2025-03-28 20:18:09', '2025', 1, NULL);
INSERT INTO `sys_courses` VALUES (10, 'Python', 'P01', 25, '2025-03-28 20:26:04', '2025-03-28 20:26:04', '2025', 2, NULL);
INSERT INTO `sys_courses` VALUES (11, 'Java', 'J02', 25, '2025-03-28 20:26:48', '2025-03-28 20:26:48', '2024', 1, NULL);
INSERT INTO `sys_courses` VALUES (16, 'app', 'test', 25, '2026-02-26 15:07:55', '2026-02-26 15:07:55', NULL, NULL, NULL);
INSERT INTO `sys_courses` VALUES (17, 'what', '1', 39, '2026-02-26 15:23:33', '2026-02-26 15:23:33', NULL, NULL, NULL);
INSERT INTO `sys_courses` VALUES (22, 'sos', 'aaaa', 23, '2026-02-26 15:56:52', '2026-02-26 15:56:52', NULL, NULL, 'Monday 15:56-18:56');

-- ----------------------------
-- Table structure for sys_file
-- ----------------------------
DROP TABLE IF EXISTS `sys_file`;
CREATE TABLE `sys_file`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(256) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '文件名称',
  `url` varchar(256) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '下载链接',
  `type` varchar(256) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '文件类型',
  `md5` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '文件md5',
  `size` bigint NULL DEFAULT NULL COMMENT '文件大小',
  `enable` tinyint NULL DEFAULT 1 COMMENT '是否禁用(1-启用, 1-禁用)',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '是否删除(0-未删, 1-已删)',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 150 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci COMMENT = '文件上传的列表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_file
-- ----------------------------

-- ----------------------------
-- Table structure for sys_questionnaire_responses
-- ----------------------------
DROP TABLE IF EXISTS `sys_questionnaire_responses`;
CREATE TABLE `sys_questionnaire_responses`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `course_id` bigint NOT NULL COMMENT '课程ID',
  `questionnaire_id` bigint NOT NULL COMMENT '问卷ID',
  `student_id` bigint NOT NULL COMMENT '学生ID',
  `answers` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '答案内容（JSON格式存储）',
  `submitted_at` datetime NOT NULL COMMENT '提交时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `index_course_questionnaire`(`course_id` ASC, `questionnaire_id` ASC) USING BTREE,
  INDEX `index_student`(`student_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 26 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '问卷答案表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_questionnaire_responses
-- ----------------------------
INSERT INTO `sys_questionnaire_responses` VALUES (2, 2, 1, 2, '{\"1740451938362\":\"123\",\"1740452251865\":\"4564\",\"1740452674400\":[\"12\",\"123\"],\"1740452696578\":[\"2\",\"6\"],\"1740453079670\":[\"12\"],\"1740453107627\":\"1123\",\"1740453118485\":\"123123132131\",\"1740453133408\":\"123\",\"1740453147118\":\"2025-02-08\",\"1740453158724\":\"00:00:04\",\"1740453169569\":100,\"1740463523979\":\"123\"}', '2025-02-26 06:34:28');
INSERT INTO `sys_questionnaire_responses` VALUES (3, 5, 3, 24, '{\"1740838082695\":\"1\",\"1740838106260\":[\"23\"],\"1740838122211\":\"11\",\"1740838141074\":\"干货间\"}', '2025-03-01 14:11:23');
INSERT INTO `sys_questionnaire_responses` VALUES (4, 6, 3, 24, '{\"1740838082695\":\"1\",\"1740838106260\":[\"12\",\"23\"],\"1740838122211\":\"22\",\"1740838141074\":\"123456\",\"1744206352289\":\"hh\"}', '2025-04-09 16:04:30');
INSERT INTO `sys_questionnaire_responses` VALUES (5, 6, 6, 29, '{\"1744865646487\":\" Very enjoyable\",\"1744865705755\":[\" The course content\"],\"1744865751186\":\"Speak loudly\",\"1744865785372\":\"Yes, definitely\"}', '2025-04-17 05:01:40');
INSERT INTO `sys_questionnaire_responses` VALUES (6, 6, 6, 30, '{\"1744865646487\":\"Somewhat enjoyable\",\"1744865705755\":[\"The teaching style\"],\"1744865751186\":\"too difficult\",\"1744865785372\":\"Yes, definitely\"}', '2025-04-17 05:02:49');
INSERT INTO `sys_questionnaire_responses` VALUES (7, 6, 6, 31, '{\"1744865646487\":\"Neutral\",\"1744865705755\":[\"The interactive activities\",\"The course structure and organization\"],\"1744865751186\":\"add some activity\",\"1744865785372\":\"Maybe, depending on the student\'s interests\"}', '2025-04-17 05:04:05');
INSERT INTO `sys_questionnaire_responses` VALUES (8, 6, 6, 28, '{\"1744865646487\":\" Very enjoyable\",\"1744865705755\":[\" The course content\",\"The teaching style\",\"The course materials (e.g., textbook, slides)\",\"The interactive activities\",\"The course structure and organization\"],\"1744865751186\":\"good enough\",\"1744865785372\":\"Yes, definitely\"}', '2025-04-17 05:04:44');
INSERT INTO `sys_questionnaire_responses` VALUES (9, 6, 7, 28, '{\"1747549074561\":\"Satisfied\",\"1747549580038\":\"Moderate\",\"1747549627641\":\"Good\",\"1747549683343\":\"4-6 hours\",\"1747549735373\":\"Probably would recommend\",\"1747549811994\":[\"Textbooks and reading materials\",\"Assignments and projects\",\"Classroom lectures\"],\"1747549954310\":[\"Workload too heavy\",\"Teaching pace too fast\"],\"1747550027442\":[\"More practical opportunities\",\"More instructor-student interaction\",\"More flexible learning schedule\"],\"1747550052615\":\"The case studies in the course were most helpful to me because they connected theoretical knowledge with practical applications, helping me better understand the concepts\",\"1747550076470\":\" I hope to increase more group activities to give students more opportunities to learn from each other and exchange ideas.\"}', '2025-05-18 07:15:48');
INSERT INTO `sys_questionnaire_responses` VALUES (10, 6, 7, 29, '{\"1747549074561\":\"Neutral\",\"1747549580038\":\"Challenging but appropriate\",\"1747549627641\":\"Average\",\"1747549683343\":\"2-4 hours\",\"1747549735373\":\"Not sure\",\"1747549811994\":[\"Classroom lectures\",\"Assignments and projects\"],\"1747549954310\":[\"Course content too difficult\",\"Lack of adequate learning resources\"],\"1747550027442\":[\"More diverse teaching materials\",\"Clearer learning objectives\",\"More reasonable grading criteria\"],\"1747550052615\":\" The classroom lectures were very helpful, as the teacher was able to explain complex concepts in simple language, making it easier for me to understand the course content.\",\"1747550076470\":\" I hope more supplementary learning materials can be provided, especially detailed analyses of difficult points.\"}', '2025-05-18 07:17:53');
INSERT INTO `sys_questionnaire_responses` VALUES (11, 5, 7, 29, '{\"1747549074561\":\"Satisfied\",\"1747549580038\":\"Moderate\",\"1747549627641\":\"Good\",\"1747549683343\":\"4-6 hours\",\"1747549735373\":\"Probably would recommend\",\"1747549811994\":[\"Textbooks and reading materials\",\"Group discussions\",\"Assignments and projects\"],\"1747549954310\":[\"Workload too heavy\",\"Lack of interaction with peers\"],\"1747550027442\":[\"More practical opportunities\",\"More instructor-student interaction\"],\"1747550052615\":\"The group discussion sessions were very valuable, allowing me to hear different viewpoints and broadening my perspective.\",\"1747550076470\":\" I suggest increasing more opportunities for communication with industry experts to understand how the material is applied in actual work situations.\"}', '2025-05-18 07:19:56');
INSERT INTO `sys_questionnaire_responses` VALUES (12, 6, 7, 30, '{\"1747549074561\":\"Dissatisfied\",\"1747549580038\":\"Too difficult\",\"1747549627641\":\"Average\",\"1747549683343\":\"6-8 hours\",\"1747549735373\":\"Probably would not recommend\",\"1747549811994\":[\"Textbooks and reading materials\"],\"1747549954310\":[\"Course content too difficult\",\"Workload too heavy\",\"Teaching pace too fast\"],\"1747550027442\":[\"Clearer learning objectives\",\"More instructor-student interaction\",\"More reasonable grading criteria\"],\"1747550052615\":\"The textbook materials were comprehensive, providing rich background knowledge that helped me build a systematic understanding framework.\"}', '2025-05-18 07:23:21');
INSERT INTO `sys_questionnaire_responses` VALUES (13, 5, 7, 30, '{\"1747549074561\":\"Neutral\",\"1747549580038\":\"Challenging but appropriate\",\"1747549627641\":\"Good\",\"1747549683343\":\"4-6 hours\",\"1747549735373\":\"Not sure\",\"1747549811994\":[\"Classroom lectures\",\"Practical activities\"],\"1747549954310\":[\"Lack of adequate learning resources\",\"Lack of effective instructor guidance\"],\"1747550027442\":[\"More diverse teaching materials\",\"Clearer learning objectives\",\"More reasonable grading criteria\"],\"1747550052615\":\"The practical activities gave me the opportunity to operate hands-on, which is more effective than just reading theoretical knowledge.\"}', '2025-05-18 07:24:05');
INSERT INTO `sys_questionnaire_responses` VALUES (14, 6, 7, 31, '{\"1747549074561\":\"Satisfied\",\"1747549580038\":\"Moderate\",\"1747549627641\":\"Good\",\"1747549683343\":\"2-4 hours\",\"1747549735373\":\"Probably would recommend\",\"1747549811994\":[\"Classroom lectures\",\"Group discussions\",\"Instructor feedback\"],\"1747549954310\":[\"Teaching pace too fast\",\"Lack of interaction with peers\"],\"1747550027442\":[\"More practical opportunities\",\"More instructor-student interaction\"],\"1747550052615\":\"The instructor\'s detailed feedback was very helpful to me, pointing out my weaknesses and providing suggestions for improvement, helping me continuously improve.\",\"1747550076470\":\"I suggest the course pace could be slowed down appropriately to give students more time to digest and think.\"}', '2025-05-18 07:25:23');
INSERT INTO `sys_questionnaire_responses` VALUES (15, 5, 7, 31, '{\"1747549074561\":\"Very satisfied\",\"1747549580038\":\"Challenging but appropriate\",\"1747549627641\":\"Excellent\",\"1747549683343\":\"4-6 hours\",\"1747549735373\":\"Definitely would recommend\",\"1747549811994\":[\"Practical activities\",\"Assignments and projects\",\"Instructor feedback\"],\"1747549954310\":[\"Workload too heavy\"],\"1747550027442\":[\"More flexible learning schedule\"],\"1747550052615\":\"The course projects were well-designed, allowing me to comprehensively apply what I learned and cultivate my problem-solving abilities.\",\"1747550076470\":\"I hope some optional challenging tasks could be provided for students interested in deeper learning to have more development opportunities.\"}', '2025-05-18 07:26:11');
INSERT INTO `sys_questionnaire_responses` VALUES (16, 6, 7, 32, '{\"1747549074561\":\"Neutral\",\"1747549580038\":\"Easy\",\"1747549627641\":\"Average\",\"1747549683343\":\"Less than 2 hours\",\"1747549735373\":\"Not sure\",\"1747549811994\":[\"Textbooks and reading materials\",\"Group discussions\"],\"1747549954310\":[],\"1747550027442\":[],\"1747550052615\":\"The group discussion sessions allowed me to hear different perspectives, broadened my thinking, and also exercised my expression skills.\"}', '2025-05-18 07:27:14');
INSERT INTO `sys_questionnaire_responses` VALUES (17, 5, 7, 32, '{\"1747549074561\":\"Satisfied\",\"1747549580038\":\"Moderate\",\"1747549627641\":\"Good\",\"1747549683343\":\"2-4 hours\",\"1747549735373\":\"Probably would recommend\",\"1747549811994\":[\"Classroom lectures\",\"Textbooks and reading materials\",\"Assignments and projects\"],\"1747549954310\":[],\"1747550027442\":[],\"1747550052615\":\"The assignments were well-targeted, allowing me to consolidate what I learned and test my learning outcomes.\"}', '2025-05-18 07:27:56');
INSERT INTO `sys_questionnaire_responses` VALUES (18, 6, 7, 33, '{\"1747549074561\":\"Very satisfied\",\"1747549580038\":\"Challenging but appropriate\",\"1747549735373\":\"Definitely would recommend\",\"1747549811994\":[\"Classroom lectures\",\"Practical activities\",\"Instructor feedback\"],\"1747549954310\":[\"Workload too heavy\"],\"1747550027442\":[\"More flexible learning schedule\"],\"1747550052615\":\"The instructor\'s explanations were very clear, capable of explaining complex concepts in a simple way, making it easy for me to understand difficult points.\"}', '2025-05-18 07:28:58');
INSERT INTO `sys_questionnaire_responses` VALUES (19, 5, 7, 33, '{\"1747549074561\":\"Satisfied\",\"1747549580038\":\"Challenging but appropriate\",\"1747549735373\":\"Probably would recommend\",\"1747549811994\":[\"Practical activities\",\"Assignments and projects\"],\"1747549954310\":[],\"1747550027442\":[],\"1747550052615\":\"The practical activities were well-designed, allowing me to personally experience the application of theoretical knowledge in practice.\"}', '2025-05-18 07:29:28');
INSERT INTO `sys_questionnaire_responses` VALUES (20, 6, 7, 34, '{\"1747549074561\":\"Satisfied\",\"1747549580038\":\"Moderate\",\"1747549735373\":\"Probably would recommend\",\"1747549811994\":[\"Classroom lectures\",\"Textbooks and reading materials\",\"Assignments and projects\"],\"1747549954310\":[],\"1747550027442\":[],\"1747550052615\":\"The assignments were well-targeted, allowing me to consolidate what I learned in class and deepen my understanding.\"}', '2025-05-18 07:31:00');
INSERT INTO `sys_questionnaire_responses` VALUES (21, 5, 7, 34, '{\"1747549074561\":\"Neutral\",\"1747549580038\":\"Too difficult\",\"1747549627641\":\"Average\",\"1747549735373\":\"Not sure\",\"1747549811994\":[\"Textbooks and reading materials\",\"Assignments and projects\"],\"1747549954310\":[\"Course content too difficult\",\"Workload too heavy\",\"Teaching pace too fast\"],\"1747550027442\":[],\"1747550052615\":\"The textbooks were well-chosen, comprehensive and easy to understand, serving as good learning references.\"}', '2025-05-18 07:32:08');
INSERT INTO `sys_questionnaire_responses` VALUES (22, 6, 7, 35, '{\"1747549074561\":\"Very satisfied\",\"1747549580038\":\"Easy\",\"1747549627641\":\"Excellent\",\"1747549683343\":\"6-8 hours\",\"1747549735373\":\"Definitely would recommend\",\"1747549811994\":[\"Classroom lectures\"],\"1747549954310\":[],\"1747550027442\":[],\"1747550052615\":\"Very good\"}', '2025-05-18 07:33:41');
INSERT INTO `sys_questionnaire_responses` VALUES (23, 5, 7, 35, '{\"1747549074561\":\"Dissatisfied\",\"1747549580038\":\"Too difficult\",\"1747549627641\":\"Below average\",\"1747549683343\":\"Less than 2 hours\",\"1747549735373\":\"Probably would not recommend\",\"1747549811994\":[\"Group discussions\"],\"1747549954310\":[],\"1747550027442\":[],\"1747550052615\":\"not Very good\"}', '2025-05-18 07:34:39');
INSERT INTO `sys_questionnaire_responses` VALUES (24, 6, 3, 28, '{\"1740838122211\":\"11\",\"1740838106260\":[\"23\",\"34\"],\"1740838082695\":\"1\"}', '2026-02-26 23:14:51');
INSERT INTO `sys_questionnaire_responses` VALUES (25, 5, 6, 29, '{\"1744865751186\":\"ss\",\"1744865785372\":\"Maybe, depending on the student\\u0027s interests\",\"1744865646487\":\" Very enjoyable\",\"1744865705755\":[\"The teaching style\",\"The interactive activities\"]}', '2026-02-26 23:35:45');

-- ----------------------------
-- Table structure for sys_questionnaires
-- ----------------------------
DROP TABLE IF EXISTS `sys_questionnaires`;
CREATE TABLE `sys_questionnaires`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '问卷标题',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '问卷描述',
  `created_by` bigint NOT NULL COMMENT '创建者ID（通常是教师）',
  `questions` json NULL COMMENT '问题列表（JSON数组）',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `created_by`(`created_by` ASC) USING BTREE,
  CONSTRAINT `sys_questionnaires_ibfk_1` FOREIGN KEY (`created_by`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 10 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_questionnaires
-- ----------------------------
INSERT INTO `sys_questionnaires` VALUES (1, '测试问卷', '测试问卷', 21, '[{\"id\": 1740451938362, \"max\": 100, \"min\": 0, \"step\": 1, \"type\": \"single\", \"index\": 1, \"title\": \"测试\", \"options\": [\"12\", \"123\"], \"required\": true, \"dateFormat\": \"YYYY-MM-DD\", \"timeFormat\": \"HH:mm:ss\", \"placeholder\": \"\"}, {\"id\": 1740452251865, \"max\": 100, \"min\": 0, \"step\": 1, \"type\": \"radio\", \"index\": 2, \"label\": \"\", \"title\": \"测试萨达\", \"options\": [\"4567\", \"45647\", \"4564\"], \"required\": false, \"dateFormat\": \"YYYY-MM-DD\", \"timeFormat\": \"HH:mm:ss\", \"placeholder\": \"\"}, {\"id\": 1740452674400, \"max\": 100, \"min\": 0, \"step\": 1, \"type\": \"multiple\", \"index\": 3, \"label\": \"\", \"title\": \"测试阿萨德\", \"options\": [\"123\", \"12\", \"445\"], \"required\": false, \"dateFormat\": \"YYYY-MM-DD\", \"timeFormat\": \"HH:mm:ss\", \"placeholder\": \"\"}, {\"id\": 1740452696578, \"max\": 100, \"min\": 0, \"step\": 1, \"type\": \"checkbox\", \"index\": 4, \"label\": \"\", \"title\": \"测试多选框\", \"options\": [\"1\", \"2\", \"6\", \"4\"], \"required\": true, \"dateFormat\": \"YYYY-MM-DD\", \"timeFormat\": \"HH:mm:ss\", \"placeholder\": \"\"}, {\"id\": 1740453079670, \"max\": 100, \"min\": 0, \"step\": 1, \"type\": \"checkbox\", \"index\": 5, \"label\": \"\", \"title\": \"测试多选题\", \"options\": [\"123\", \"12\"], \"required\": false, \"dateFormat\": \"YYYY-MM-DD\", \"timeFormat\": \"HH:mm:ss\", \"placeholder\": \"\"}, {\"id\": 1740453107627, \"max\": 100, \"min\": 0, \"step\": 1, \"type\": \"text\", \"index\": 6, \"label\": \"\", \"title\": \"测试填空题\", \"options\": [\"\", \"\"], \"required\": false, \"dateFormat\": \"YYYY-MM-DD\", \"timeFormat\": \"HH:mm:ss\", \"placeholder\": \"请输入\"}, {\"id\": 1740453118485, \"max\": 100, \"min\": 0, \"step\": 1, \"type\": \"textarea\", \"index\": 7, \"label\": \"\", \"title\": \"测试文本域\", \"options\": [\"\", \"\"], \"required\": false, \"dateFormat\": \"YYYY-MM-DD\", \"timeFormat\": \"HH:mm:ss\", \"placeholder\": \"123\"}, {\"id\": 1740453133408, \"max\": 100, \"min\": 0, \"step\": 1, \"type\": \"select\", \"index\": 8, \"label\": \"\", \"title\": \"测试下拉选择\", \"options\": [\"1\", \"123\"], \"required\": false, \"dateFormat\": \"YYYY-MM-DD\", \"timeFormat\": \"HH:mm:ss\", \"placeholder\": \"\"}, {\"id\": 1740453147118, \"max\": 100, \"min\": 0, \"step\": 1, \"type\": \"date\", \"index\": 9, \"label\": \"\", \"title\": \"测试日期选择\", \"options\": [\"\", \"\"], \"required\": false, \"dateFormat\": \"YYYY-MM-DD\", \"timeFormat\": \"HH:mm:ss\", \"placeholder\": \"\"}, {\"id\": 1740453158724, \"max\": 100, \"min\": 0, \"step\": 1, \"type\": \"time\", \"index\": 10, \"label\": \"\", \"title\": \"测试时间选择\", \"options\": [\"\", \"\"], \"required\": false, \"dateFormat\": \"YYYY-MM-DD\", \"timeFormat\": \"HH:mm:ss\", \"placeholder\": \"\"}, {\"id\": 1740453169569, \"max\": 100, \"min\": 0, \"step\": 1, \"type\": \"number\", \"index\": 11, \"label\": \"\", \"title\": \"测试数字输入\", \"options\": [\"\", \"\"], \"required\": false, \"dateFormat\": \"YYYY-MM-DD\", \"timeFormat\": \"HH:mm:ss\", \"placeholder\": \"\"}, {\"id\": 1740463523979, \"max\": 100, \"min\": 0, \"step\": 1, \"type\": \"single\", \"index\": 12, \"label\": \"\", \"title\": \"123\", \"options\": [\"123\", \"1231\"], \"required\": false, \"dateFormat\": \"YYYY-MM-DD\", \"timeFormat\": \"HH:mm:ss\", \"placeholder\": \"\"}]', '2025-02-24 11:53:00', '2025-02-24 11:53:00');
INSERT INTO `sys_questionnaires` VALUES (2, '问卷2', '22', 1, '[{\"id\": 1740557906792, \"max\": 100, \"min\": 0, \"step\": 1, \"type\": \"single\", \"index\": 1, \"label\": \"\", \"title\": \"123\", \"options\": [\"456\", \"456\"], \"required\": false, \"dateFormat\": \"YYYY-MM-DD\", \"timeFormat\": \"HH:mm:ss\", \"placeholder\": \"\"}, {\"id\": 1740557920148, \"max\": 100, \"min\": 0, \"step\": 1, \"type\": \"text\", \"index\": 2, \"label\": \"\", \"title\": \"北京在哪里\", \"options\": [\"\", \"\"], \"required\": true, \"dateFormat\": \"YYYY-MM-DD\", \"timeFormat\": \"HH:mm:ss\", \"placeholder\": \"\"}]', '2025-02-25 11:10:31', '2025-02-25 11:10:31');
INSERT INTO `sys_questionnaires` VALUES (3, 'Survey01', 'A survey on whether students like this course', 23, '[{\"id\": 1740838082695, \"max\": 100, \"min\": 0, \"step\": 1, \"type\": \"single\", \"index\": 1, \"label\": \"\", \"title\": \"13454\", \"options\": [\"1\", \"2\", \"3\", \"4\"], \"required\": true, \"dateFormat\": \"YYYY-MM-DD\", \"timeFormat\": \"HH:mm:ss\", \"placeholder\": \"\"}, {\"id\": 1740838106260, \"max\": 100, \"min\": 0, \"step\": 1, \"type\": \"multiple\", \"index\": 2, \"label\": \"\", \"title\": \"3333\", \"options\": [\"12\", \"23\", \"34\", \"45\"], \"required\": true, \"dateFormat\": \"YYYY-MM-DD\", \"timeFormat\": \"HH:mm:ss\", \"placeholder\": \"\"}, {\"id\": 1740838122211, \"max\": 100, \"min\": 0, \"step\": 1, \"type\": \"radio\", \"index\": 3, \"label\": \"\", \"title\": \"43\", \"options\": [\"22\", \"11\"], \"required\": false, \"dateFormat\": \"YYYY-MM-DD\", \"timeFormat\": \"HH:mm:ss\", \"placeholder\": \"\"}, {\"id\": 1740838141074, \"max\": 100, \"min\": 0, \"step\": 1, \"type\": \"text\", \"index\": 4, \"label\": \"\", \"title\": \"11\", \"options\": [\"\", \"\"], \"required\": true, \"dateFormat\": \"YYYY-MM-DD\", \"timeFormat\": \"HH:mm:ss\", \"placeholder\": \"呃呃\"}, {\"id\": 1744206352289, \"max\": 100, \"min\": 0, \"step\": 1, \"type\": \"text\", \"index\": 5, \"label\": \"\", \"title\": \"why\", \"options\": [\"\", \"\"], \"required\": true, \"dateFormat\": \"YYYY-MM-DD\", \"timeFormat\": \"HH:mm:ss\", \"placeholder\": \"please write\"}]', '2025-03-01 22:07:34', '2025-03-01 22:07:34');
INSERT INTO `sys_questionnaires` VALUES (4, 'survey1', '1234567io7654ew', 25, '[{\"id\": 1743165171990, \"max\": 100, \"min\": 0, \"step\": 1, \"type\": \"single\", \"index\": 1, \"label\": \"\", \"title\": \"which 12345678\", \"options\": [\"good\", \"not good\", \"bad\"], \"required\": true, \"dateFormat\": \"YYYY-MM-DD\", \"timeFormat\": \"HH:mm:ss\", \"placeholder\": \"\"}, {\"id\": 1743165213336, \"max\": 100, \"min\": 0, \"step\": 1, \"type\": \"multiple\", \"index\": 2, \"label\": \"\", \"title\": \"when\", \"options\": [\"11\", \"12\", \"13\", \"14\"], \"required\": false, \"dateFormat\": \"YYYY-MM-DD\", \"timeFormat\": \"HH:mm:ss\", \"placeholder\": \"\"}]', '2025-03-28 20:31:51', '2025-03-28 20:31:51');
INSERT INTO `sys_questionnaires` VALUES (5, '12345', '23456787665', 23, '[{\"id\": 1747401904135, \"max\": 100, \"min\": 0, \"step\": 1, \"type\": \"multiple\", \"index\": 1, \"label\": \"\", \"title\": \"是否大吉大利\", \"options\": [\"是\", \"一般\", \"否\"], \"required\": false, \"dateFormat\": \"YYYY-MM-DD\", \"timeFormat\": \"HH:mm:ss\", \"placeholder\": \"\"}, {\"id\": 1747548030326, \"max\": 100, \"min\": 0, \"step\": 1, \"type\": \"radio\", \"index\": 2, \"label\": \"\", \"title\": \"1234\", \"options\": [\"sm \", \"ds\"], \"required\": false, \"dateFormat\": \"YYYY-MM-DD\", \"timeFormat\": \"HH:mm:ss\", \"placeholder\": \"\"}]', '2025-03-31 20:27:09', '2025-03-31 20:27:09');
INSERT INTO `sys_questionnaires` VALUES (6, 'vivaSurvey', 'A survey on whether students like this course', 23, '[{\"id\": 1744865646487, \"max\": 100, \"min\": 0, \"step\": 1, \"type\": \"single\", \"index\": 1, \"label\": \"\", \"title\": \"How would you rate your overall enjoyment of this course?\", \"options\": [\" Very enjoyable\", \"Somewhat enjoyable\", \"Neutral\", \"Somewhat unenjoyable\", \"Very unenjoyable\"], \"required\": true, \"dateFormat\": \"YYYY-MM-DD\", \"timeFormat\": \"HH:mm:ss\", \"placeholder\": \"\"}, {\"id\": 1744865705755, \"max\": 100, \"min\": 0, \"step\": 1, \"type\": \"multiple\", \"index\": 2, \"label\": \"\", \"title\": \"What aspects of the course did you find most enjoyable? (You can select more than one)\", \"options\": [\" The course content\", \"The teaching style\", \"The course materials (e.g., textbook, slides)\", \"The interactive activities\", \"The course structure and organization\"], \"required\": true, \"dateFormat\": \"YYYY-MM-DD\", \"timeFormat\": \"HH:mm:ss\", \"placeholder\": \"\"}, {\"id\": 1744865751186, \"max\": 100, \"min\": 0, \"step\": 1, \"type\": \"text\", \"index\": 3, \"label\": \"\", \"title\": \"What suggestions do you have for improving this course in the future?\", \"options\": [\"\", \"\"], \"required\": false, \"dateFormat\": \"YYYY-MM-DD\", \"timeFormat\": \"HH:mm:ss\", \"placeholder\": \"please write\"}, {\"id\": 1744865785372, \"max\": 100, \"min\": 0, \"step\": 1, \"type\": \"single\", \"index\": 4, \"label\": \"\", \"title\": \"Would you recommend this course to other students?\", \"options\": [\"Yes, definitely\", \"Maybe, depending on the student\'s interests\", \"No, I would not recommend it\"], \"required\": true, \"dateFormat\": \"YYYY-MM-DD\", \"timeFormat\": \"HH:mm:ss\", \"placeholder\": \"\"}]', '2025-04-17 12:53:00', '2025-04-17 12:53:00');
INSERT INTO `sys_questionnaires` VALUES (7, 'Final Survey!!!', 'A survey on whether students like this course', 23, '[{\"id\": \"1747549074561\", \"type\": \"single\", \"title\": \"Overall, how satisfied are you with this course?\", \"options\": [\"Very satisfied\", \"Satisfied\", \"Neutral\", \"Dissatisfied\"], \"required\": true, \"placeholder\": \"\"}, {\"id\": \"1747549580038\", \"type\": \"single\", \"title\": \"How would you rate the difficulty level of this course?\", \"options\": [\"Too difficult\", \"Challenging but appropriate\", \"Moderate\", \"Easy\", \"Too easy\"], \"required\": true, \"placeholder\": \"\"}, {\"id\": \"1747549627641\", \"type\": \"single\", \"title\": \"How would you rate the teaching quality of the instructor?\", \"options\": [\"Excellent\", \"Good\", \"Average\", \"Below average\", \"Poor\"], \"required\": false, \"placeholder\": \"\"}, {\"id\": \"1747549683343\", \"type\": \"single\", \"title\": \"How much time do you spend on this course per week?\", \"options\": [\"Less than 2 hours\", \"2-4 hours\", \"4-6 hours\", \"6-8 hours\", \"More than 8 hours\"], \"required\": false, \"placeholder\": \"\"}, {\"id\": \"1747549735373\", \"type\": \"single\", \"title\": \"Would you recommend this course to other students?\", \"options\": [\"Definitely would recommend\", \"Probably would recommend\", \"Not sure\", \"Probably would not recommend\", \"Definitely would not recommend\"], \"required\": true, \"placeholder\": \"\"}, {\"id\": \"1747549811994\", \"type\": \"multiple\", \"title\": \"Which aspects of the course did you find most valuable?\", \"options\": [\"Classroom lectures\", \"Practical activities\", \"Textbooks and reading materials\", \"Group discussions\", \"Assignments and projects\", \"Instructor feedback\"], \"required\": true, \"placeholder\": \"\"}, {\"id\": \"1747549954310\", \"type\": \"multiple\", \"title\": \"What were the main challenges you faced in this course?\", \"options\": [\"Course content too difficult\", \"Workload too heavy\", \"Teaching pace too fast\", \"Lack of adequate learning resources\", \"Lack of effective instructor guidance\", \"Lack of interaction with peers\"], \"required\": false, \"placeholder\": \"\"}, {\"id\": \"1747550027442\", \"type\": \"multiple\", \"title\": \"What improvements would you like to see in future courses?\", \"options\": [\"More practical opportunities\", \"More diverse teaching materials\", \"Clearer learning objectives\", \"More instructor-student interaction\", \"More reasonable grading criteria\", \"More flexible learning schedule\"], \"required\": false, \"placeholder\": \"\"}, {\"id\": \"1747550052615\", \"type\": \"text\", \"title\": \"Please describe one aspect of the course that you found most helpful and explain why.\", \"options\": [\"\", \"\"], \"required\": true, \"placeholder\": \"please write\"}, {\"id\": \"1747550076470\", \"type\": \"text\", \"title\": \"Do you have any specific suggestions for improving this course?\", \"options\": [\"\", \"\"], \"required\": false, \"placeholder\": \"please write\"}, {\"id\": \"40381d9e-512c-4940-8ef0-4da5d01edde9\", \"type\": \"single\", \"title\": \"testdanxuan\", \"options\": [\"1\", \"2\", \"3\"], \"required\": true}, {\"id\": \"4616cc50-1fcd-4fec-aa2a-69bc4b0320d2\", \"type\": \"text\", \"title\": \"testtext\", \"required\": true}]', '2025-05-18 14:16:44', '2025-05-18 14:16:44');
INSERT INTO `sys_questionnaires` VALUES (8, 'rrfg', 'ert', 23, NULL, '2025-05-19 10:34:57', '2025-05-19 10:34:57');

-- ----------------------------
-- Table structure for sys_responses
-- ----------------------------
DROP TABLE IF EXISTS `sys_responses`;
CREATE TABLE `sys_responses`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `questionnaire_id` bigint NOT NULL COMMENT '问卷ID',
  `student_id` bigint NOT NULL COMMENT '提交学生ID',
  `answers` json NOT NULL COMMENT '学生回答（JSON数组）',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '回答时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `questionnaire_id`(`questionnaire_id` ASC) USING BTREE,
  INDEX `student_id`(`student_id` ASC) USING BTREE,
  CONSTRAINT `sys_responses_ibfk_1` FOREIGN KEY (`questionnaire_id`) REFERENCES `sys_questionnaires` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `sys_responses_ibfk_2` FOREIGN KEY (`student_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_responses
-- ----------------------------

-- ----------------------------
-- Table structure for sys_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `name` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '名称',
  `description` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '描述',
  `flag` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '唯一标识',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_role
-- ----------------------------
INSERT INTO `sys_role` VALUES (1, '管理员', '管理员', 'admin');
INSERT INTO `sys_role` VALUES (2, '学生', '查看问卷和提交反馈', 'student');
INSERT INTO `sys_role` VALUES (3, '教师', '发布问卷和查看结果分析\r\n', 'teacher');

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `username` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '用户名',
  `password` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '密码',
  `nickname` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '昵称',
  `avatar_url` longtext CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL COMMENT '头像',
  `email` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '邮箱',
  `role_id` int NULL DEFAULT NULL COMMENT '角色  0超级管理员  1管理员 2普通账号',
  `status` tinyint NULL DEFAULT 1 COMMENT '是否有效 1有效 0无效',
  `role` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '角色',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `username`(`username` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 40 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_user
-- ----------------------------
INSERT INTO `sys_user` VALUES (1, 'admin', 'admin', '管理员', NULL, '', 1, 1, 'admin');
INSERT INTO `sys_user` VALUES (2, 'user', 'user', '学生', NULL, '', 2, 1, 'student');
INSERT INTO `sys_user` VALUES (21, '123', '123', '李老师', NULL, NULL, 3, 1, 'teacher');
INSERT INTO `sys_user` VALUES (22, '456', '456', '456', NULL, NULL, 3, 1, 'teacher');
INSERT INTO `sys_user` VALUES (23, 'www', '123456', 'TeacherABC', NULL, NULL, 3, 1, 'teacher');
INSERT INTO `sys_user` VALUES (24, '2021', '111111', '神', NULL, NULL, 2, 1, 'student');
INSERT INTO `sys_user` VALUES (25, 'teacher11', '123456', 'Teacher11', NULL, NULL, 3, 1, 'teacher');
INSERT INTO `sys_user` VALUES (26, 'wyt1', '123456', '111', NULL, NULL, 2, 1, 'student');
INSERT INTO `sys_user` VALUES (27, 'wyt2', '123456', '222', NULL, NULL, 2, 1, 'student');
INSERT INTO `sys_user` VALUES (28, 'S1', '1', 'S1', NULL, NULL, 2, 1, 'student');
INSERT INTO `sys_user` VALUES (29, 'S2', '1', 'S2', NULL, NULL, 2, 1, 'student');
INSERT INTO `sys_user` VALUES (30, 'S3', '1', 'S3', NULL, NULL, 2, 1, 'student');
INSERT INTO `sys_user` VALUES (31, 'S4', '1', 'S4', NULL, NULL, 2, 1, 'student');
INSERT INTO `sys_user` VALUES (32, 'S5', '1', 'S5', NULL, NULL, 2, 1, 'student');
INSERT INTO `sys_user` VALUES (33, 'S6', '1', 'S6', NULL, NULL, 2, 1, 'student');
INSERT INTO `sys_user` VALUES (34, 'S7', '1', 'S7', NULL, NULL, 2, 1, 'student');
INSERT INTO `sys_user` VALUES (35, 'S8', '1', 'S8', NULL, NULL, 2, 1, 'student');
INSERT INTO `sys_user` VALUES (36, 'S9', '1', 'S9', NULL, NULL, 2, 1, 'student');
INSERT INTO `sys_user` VALUES (37, 'S10', '1', 'S10', NULL, NULL, 2, 1, 'student');
INSERT INTO `sys_user` VALUES (38, 'test', '1', NULL, NULL, '', 1, 1, 'Teacher');
INSERT INTO `sys_user` VALUES (39, 't', '1', NULL, NULL, '', 1, 1, 'Teacher');

SET FOREIGN_KEY_CHECKS = 1;
