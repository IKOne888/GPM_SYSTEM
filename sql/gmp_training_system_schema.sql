-- =====================================================
-- GMP内部培训系统 数据库结构脚本（纯净版，不含业务数据）
-- 数据库：gmp_training_system
-- 说明：仅包含建库与建表语句，共 24 张表
-- =====================================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS `gmp_training_system` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE `gmp_training_system`;

-- =====================================================
-- 1. 部门表
-- =====================================================
DROP TABLE IF EXISTS `sys_department`;
CREATE TABLE `sys_department` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(100) NOT NULL COMMENT '部门名称',
  `code` varchar(50) NOT NULL COMMENT '部门编码',
  `leader` varchar(50) DEFAULT NULL COMMENT '负责人',
  `phone` varchar(20) DEFAULT NULL COMMENT '联系电话',
  `parent_id` bigint DEFAULT 0 COMMENT '父部门ID',
  `sort` int DEFAULT 0 COMMENT '排序',
  `status` tinyint DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` varchar(50) DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(50) DEFAULT NULL COMMENT '更新人',
  `deleted` tinyint DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门表';

-- =====================================================
-- 2. 岗位表
-- =====================================================
DROP TABLE IF EXISTS `sys_position`;
CREATE TABLE `sys_position` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(100) NOT NULL COMMENT '岗位名称',
  `code` varchar(50) NOT NULL COMMENT '岗位编码',
  `department_id` bigint DEFAULT NULL COMMENT '所属部门ID（岗位归属部门）',
  `level` varchar(20) DEFAULT NULL COMMENT '岗位级别：初级/中级/高级/专家',
  `description` varchar(500) DEFAULT NULL COMMENT '岗位描述',
  `status` tinyint DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` varchar(50) DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(50) DEFAULT NULL COMMENT '更新人',
  `deleted` tinyint DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='岗位表';

-- =====================================================
-- 3. 角色表
-- =====================================================
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(100) NOT NULL COMMENT '角色名称',
  `code` varchar(50) NOT NULL COMMENT '角色编码',
  `description` varchar(500) DEFAULT NULL COMMENT '角色描述',
  `status` tinyint DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` varchar(50) DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(50) DEFAULT NULL COMMENT '更新人',
  `deleted` tinyint DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- =====================================================
-- 4. 用户表
-- =====================================================
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` varchar(50) NOT NULL COMMENT '用户名',
  `password` varchar(200) NOT NULL COMMENT '密码（加密）',
  `real_name` varchar(50) NOT NULL COMMENT '真实姓名',
  `phone` varchar(20) DEFAULT NULL COMMENT '手机号',
  `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
  `department_id` bigint DEFAULT NULL COMMENT '部门ID',
  `position_id` bigint DEFAULT NULL COMMENT '岗位ID',
  `is_department_leader` tinyint DEFAULT 0 COMMENT '是否部门负责人：0-否 1-是',
  `status` tinyint DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
  `openid` varchar(100) DEFAULT NULL COMMENT '微信openid',
  `last_login_time` datetime DEFAULT NULL COMMENT '最后登录时间',
  `last_login_ip` varchar(50) DEFAULT NULL COMMENT '最后登录IP',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` varchar(50) DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(50) DEFAULT NULL COMMENT '更新人',
  `deleted` tinyint DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_department_id` (`department_id`),
  KEY `idx_position_id` (`position_id`),
  KEY `idx_openid` (`openid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- =====================================================
-- 5. 用户角色关联表
-- =====================================================
DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_role` (`user_id`, `role_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

-- =====================================================
-- 6. 菜单权限表
-- =====================================================
DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `parent_id` bigint DEFAULT 0 COMMENT '父菜单ID',
  `name` varchar(100) NOT NULL COMMENT '菜单名称',
  `path` varchar(200) DEFAULT NULL COMMENT '路由路径',
  `component` varchar(200) DEFAULT NULL COMMENT '组件路径',
  `icon` varchar(100) DEFAULT NULL COMMENT '图标',
  `sort` int DEFAULT 0 COMMENT '排序',
  `type` tinyint DEFAULT 1 COMMENT '类型：1-菜单 2-按钮',
  `permission` varchar(100) DEFAULT NULL COMMENT '权限标识',
  `visible` tinyint DEFAULT 1 COMMENT '是否可见：0-隐藏 1-显示',
  `status` tinyint DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单权限表';

-- =====================================================
-- 7. 角色菜单关联表
-- =====================================================
DROP TABLE IF EXISTS `sys_role_menu`;
CREATE TABLE `sys_role_menu` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `menu_id` bigint NOT NULL COMMENT '菜单ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_menu` (`role_id`, `menu_id`),
  KEY `idx_role_id` (`role_id`),
  KEY `idx_menu_id` (`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色菜单关联表';

-- =====================================================
-- 8. 课程分类表
-- =====================================================
DROP TABLE IF EXISTS `tr_course_category`;
CREATE TABLE `tr_course_category` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(100) NOT NULL COMMENT '分类名称',
  `code` varchar(50) NOT NULL COMMENT '分类编码',
  `sort` int DEFAULT 0 COMMENT '排序',
  `status` tinyint DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` varchar(50) DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(50) DEFAULT NULL COMMENT '更新人',
  `deleted` tinyint DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程分类表';

-- =====================================================
-- 9. 课程表
-- =====================================================
DROP TABLE IF EXISTS `tr_course`;
CREATE TABLE `tr_course` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(200) NOT NULL COMMENT '课程名称',
  `code` varchar(50) NOT NULL COMMENT '课程编码',
  `category_id` bigint DEFAULT NULL COMMENT '分类ID',
  `department_id` bigint DEFAULT NULL COMMENT '所属部门ID（课程归属部门，实现"部门-岗位"两级分类）',
  `position_id` bigint DEFAULT NULL COMMENT '适用岗位ID（课程面向的岗位，可为空表示全岗位通用）',
  `sop_code` varchar(50) DEFAULT NULL COMMENT 'SOP编号',
  `sop_version` varchar(20) DEFAULT NULL COMMENT 'SOP版本号',
  `training_objective` varchar(1000) DEFAULT NULL COMMENT '培训目标',
  `training_hours` decimal(5,2) DEFAULT NULL COMMENT '培训学时',
  `pass_score` int DEFAULT 60 COMMENT '及格分数',
  `retrain_period` int DEFAULT NULL COMMENT '复训周期（天）',
  `status` tinyint DEFAULT 1 COMMENT '状态：0-停用 1-启用',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` varchar(50) DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(50) DEFAULT NULL COMMENT '更新人',
  `deleted` tinyint DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`),
  KEY `idx_category_id` (`category_id`),
  KEY `idx_department_id` (`department_id`),
  KEY `idx_position_id` (`position_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程表';

-- =====================================================
-- 10. 视频课件表
-- =====================================================
DROP TABLE IF EXISTS `tr_course_video`;
CREATE TABLE `tr_course_video` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `course_id` bigint NOT NULL COMMENT '课程ID',
  `name` varchar(200) NOT NULL COMMENT '视频名称',
  `file_name` varchar(200) NOT NULL COMMENT '文件名',
  `file_path` varchar(500) NOT NULL COMMENT '文件路径',
  `file_size` bigint DEFAULT NULL COMMENT '文件大小（字节）',
  `file_type` varchar(20) DEFAULT 'MP4' COMMENT '文件类型',
  `duration` int DEFAULT NULL COMMENT '视频时长（秒）',
  `sort` int DEFAULT 0 COMMENT '排序',
  `upload_user_id` bigint NOT NULL COMMENT '上传人ID',
  `upload_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  `status` tinyint DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
  `deleted` tinyint DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_course_id` (`course_id`),
  KEY `idx_upload_user_id` (`upload_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='视频课件表';

-- =====================================================
-- 10.1 课程资料附件表（支持 PDF/Word/Excel 等课程资料，管理员上传、学员只读）
-- =====================================================
DROP TABLE IF EXISTS `tr_course_file`;
CREATE TABLE `tr_course_file` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `course_id` bigint NOT NULL COMMENT '课程ID',
  `name` varchar(200) NOT NULL COMMENT '资料名称',
  `file_name` varchar(255) NOT NULL COMMENT '原始文件名',
  `file_path` varchar(500) NOT NULL COMMENT 'MinIO对象路径',
  `file_size` bigint DEFAULT NULL COMMENT '文件大小（字节）',
  `file_type` varchar(20) DEFAULT NULL COMMENT '文件类型：PDF/DOC/DOCX/XLS/XLSX',
  `upload_user_id` bigint NOT NULL COMMENT '上传人ID',
  `upload_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  `status` tinyint DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
  `deleted` tinyint DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_course_id` (`course_id`),
  KEY `idx_upload_user_id` (`upload_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程资料附件表';

-- =====================================================
-- 11. PDF课件表
-- =====================================================
DROP TABLE IF EXISTS `tr_course_pdf`;
CREATE TABLE `tr_course_pdf` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `course_id` bigint NOT NULL COMMENT '课程ID',
  `name` varchar(200) NOT NULL COMMENT '课件名称',
  `file_name` varchar(200) NOT NULL COMMENT '文件名',
  `file_path` varchar(500) NOT NULL COMMENT '文件路径',
  `file_size` bigint DEFAULT NULL COMMENT '文件大小（字节）',
  `upload_user_id` bigint NOT NULL COMMENT '上传人ID',
  `upload_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  `status` tinyint DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
  `deleted` tinyint DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_course_id` (`course_id`),
  KEY `idx_upload_user_id` (`upload_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='PDF课件表';

-- =====================================================
-- 12. 岗位培训矩阵表
-- =====================================================
DROP TABLE IF EXISTS `tr_position_course_matrix`;
CREATE TABLE `tr_position_course_matrix` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `position_id` bigint NOT NULL COMMENT '岗位ID',
  `course_id` bigint NOT NULL COMMENT '课程ID',
  `is_required` tinyint DEFAULT 1 COMMENT '是否必修：0-选修 1-必修',
  `frequency` varchar(20) DEFAULT NULL COMMENT '培训频次：onboard-入职时 yearly-每年 half_year-每半年 quarterly-每季度 once-一次性',
  `retrain_period` int DEFAULT NULL COMMENT '复训周期（天）',
  `pass_score` int DEFAULT 60 COMMENT '及格分数',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` varchar(50) DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(50) DEFAULT NULL COMMENT '更新人',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_position_course` (`position_id`, `course_id`),
  KEY `idx_position_id` (`position_id`),
  KEY `idx_course_id` (`course_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='岗位培训矩阵表';

-- =====================================================
-- 13. 培训需求表
-- =====================================================
DROP TABLE IF EXISTS `tr_training_demand`;
CREATE TABLE `tr_training_demand` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `title` varchar(200) NOT NULL COMMENT '需求标题',
  `content` text COMMENT '需求内容',
  `training_topic` varchar(200) DEFAULT NULL COMMENT '培训主题',
  `training_target` varchar(500) DEFAULT NULL COMMENT '培训对象',
  `reason` varchar(1000) DEFAULT NULL COMMENT '需求原因',
  `department_id` bigint DEFAULT NULL COMMENT '申请部门ID',
  `status` tinyint DEFAULT 0 COMMENT '状态：0-待处理 1-已采纳 2-已拒绝',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` varchar(50) DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(50) DEFAULT NULL COMMENT '更新人',
  `deleted` tinyint DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_department_id` (`department_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='培训需求表';

-- =====================================================
-- 14. 培训计划表
-- =====================================================
DROP TABLE IF EXISTS `tr_training_plan`;
CREATE TABLE `tr_training_plan` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `code` varchar(64) DEFAULT NULL COMMENT '计划编号（唯一，如 PLAN-2026-001）',
  `name` varchar(200) NOT NULL COMMENT '计划名称',
  `year` int DEFAULT NULL COMMENT '计划年度',
  `plan_type` tinyint DEFAULT 1 COMMENT '计划类型：1-年度计划 2-专项计划',
  `description` varchar(1000) DEFAULT NULL COMMENT '计划描述',
  `target_departments` varchar(500) DEFAULT NULL COMMENT '培训对象部门ID列表（JSON，如 [1,2]）',
  `start_date` date DEFAULT NULL COMMENT '开始日期',
  `end_date` date DEFAULT NULL COMMENT '结束日期',
  `status` tinyint DEFAULT 0 COMMENT '状态：0-草稿 1-待审批 2-已通过 3-已驳回 4-已发布',
  `submit_time` datetime DEFAULT NULL COMMENT '提交时间',
  `approve_time` datetime DEFAULT NULL COMMENT '审批时间',
  `approve_user_id` bigint DEFAULT NULL COMMENT '审批人ID',
  `approve_remark` varchar(500) DEFAULT NULL COMMENT '审批意见',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` varchar(50) DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(50) DEFAULT NULL COMMENT '更新人',
  `deleted` tinyint DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`),
  KEY `idx_status` (`status`),
  KEY `idx_year` (`year`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='培训计划表';

-- =====================================================
-- 15. 培训计划课程关联表
-- =====================================================
DROP TABLE IF EXISTS `tr_plan_course`;
CREATE TABLE `tr_plan_course` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `plan_id` bigint NOT NULL COMMENT '计划ID',
  `course_id` bigint NOT NULL COMMENT '课程ID',
  `trainer` varchar(50) DEFAULT NULL COMMENT '讲师',
  `planned_date` date DEFAULT NULL COMMENT '计划培训日期',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_plan_course` (`plan_id`, `course_id`),
  KEY `idx_plan_id` (`plan_id`),
  KEY `idx_course_id` (`course_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='培训计划课程关联表';

-- =====================================================
-- 16. 培训任务表
-- =====================================================
DROP TABLE IF EXISTS `tr_training_task`;
CREATE TABLE `tr_training_task` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(200) DEFAULT NULL COMMENT '任务名称',
  `plan_id` bigint DEFAULT NULL COMMENT '计划ID',
  `course_id` bigint NOT NULL COMMENT '课程ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `task_type` tinyint DEFAULT 1 COMMENT '任务类型：1-正常培训 2-复训 3-补训',
  `description` varchar(500) DEFAULT NULL COMMENT '任务说明',
  `status` tinyint DEFAULT 0 COMMENT '状态：0-待学习 1-学习中 2-待考试 3-已完成 4-已逾期',
  `deadline` datetime DEFAULT NULL COMMENT '截止时间',
  `start_time` datetime DEFAULT NULL COMMENT '开始学习时间',
  `complete_time` datetime DEFAULT NULL COMMENT '完成时间',
  `study_duration` int DEFAULT 0 COMMENT '学习时长（秒）',
  `video_progress` decimal(5,2) DEFAULT 0 COMMENT '视频学习进度（百分比）',
  `exam_score` int DEFAULT NULL COMMENT '考试成绩',
  `exam_pass` tinyint DEFAULT 0 COMMENT '是否通过考试：0-未通过 1-已通过',
  `retry_count` int DEFAULT 0 COMMENT '补考次数',
  `max_retry` int DEFAULT 3 COMMENT '最大补考次数',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_plan_id` (`plan_id`),
  KEY `idx_course_id` (`course_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`),
  KEY `idx_deadline` (`deadline`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='培训任务表';

-- =====================================================
-- 17. 题库表
-- =====================================================
DROP TABLE IF EXISTS `tr_question`;
CREATE TABLE `tr_question` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `course_id` bigint DEFAULT NULL COMMENT '课程ID（可为空，题库同时支持按课程分类维护）',
  `question_type` tinyint NOT NULL COMMENT '题目类型：1-单选 2-多选 3-判断 4-填空 5-简答',
  `content` text NOT NULL COMMENT '题目内容',
  `options` text COMMENT '选项（JSON格式，如 [{"text":"A选项","isAnswer":true}]）',
  `answer` varchar(500) NOT NULL COMMENT '正确答案（单/多选存选项字母如 A 或 AB；判断存 正确/错误；填空/简答存文本）',
  `analysis` text COMMENT '答案解析',
  `score` int DEFAULT 5 COMMENT '题目分数',
  `difficulty` tinyint DEFAULT 1 COMMENT '难度：1-简单 2-中等 3-困难',
  `category` varchar(50) DEFAULT NULL COMMENT '课程分类编码：gmp_basic-基础 skill-岗位技能 regulation-法规标准 quality-质量安全',
  `status` tinyint DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` varchar(50) DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(50) DEFAULT NULL COMMENT '更新人',
  `deleted` tinyint DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_course_id` (`course_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题库表';

-- =====================================================
-- 18. 试卷表
-- =====================================================
DROP TABLE IF EXISTS `tr_exam_paper`;
CREATE TABLE `tr_exam_paper` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `code` varchar(64) DEFAULT NULL COMMENT '试卷编号（唯一，如 PAPER-2026-001）',
  `name` varchar(200) NOT NULL COMMENT '试卷名称',
  `course_id` bigint DEFAULT NULL COMMENT '课程ID（可为空，试卷可不绑定具体课程）',
  `total_score` int DEFAULT 100 COMMENT '总分',
  `pass_score` int DEFAULT 60 COMMENT '及格分数',
  `exam_duration` int DEFAULT 60 COMMENT '考试时长（分钟）',
  `max_retry` int DEFAULT 3 COMMENT '最大补考次数',
  `question_count` int DEFAULT 0 COMMENT '题目数量',
  `composition_type` varchar(20) DEFAULT 'random' COMMENT '组卷方式：manual-手动选题 random-随机组卷',
  `rules` text COMMENT '组卷规则（JSON，如 [{"type":"single","difficulty":"medium","count":20,"score":2}]）',
  `status` tinyint DEFAULT 0 COMMENT '状态：0-草稿 1-已发布 2-已归档',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` varchar(50) DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(50) DEFAULT NULL COMMENT '更新人',
  `deleted` tinyint DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`),
  KEY `idx_course_id` (`course_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='试卷表';

-- =====================================================
-- 19. 试卷题目关联表
-- =====================================================
DROP TABLE IF EXISTS `tr_paper_question`;
CREATE TABLE `tr_paper_question` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `paper_id` bigint NOT NULL COMMENT '试卷ID',
  `question_id` bigint NOT NULL COMMENT '题目ID',
  `sort` int DEFAULT 0 COMMENT '排序',
  `score` int DEFAULT 5 COMMENT '题目分数',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_paper_question` (`paper_id`, `question_id`),
  KEY `idx_paper_id` (`paper_id`),
  KEY `idx_question_id` (`question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='试卷题目关联表';

-- =====================================================
-- 20. 考试记录表
-- =====================================================
DROP TABLE IF EXISTS `tr_exam_record`;
CREATE TABLE `tr_exam_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `task_id` bigint NOT NULL COMMENT '任务ID',
  `paper_id` bigint NOT NULL COMMENT '试卷ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `retry_count` int DEFAULT 1 COMMENT '第几次考试',
  `score` int DEFAULT NULL COMMENT '得分',
  `pass` tinyint DEFAULT 0 COMMENT '是否通过：0-未通过 1-已通过',
  `start_time` datetime DEFAULT NULL COMMENT '开始时间',
  `submit_time` datetime DEFAULT NULL COMMENT '提交时间',
  `duration` int DEFAULT NULL COMMENT '答题时长（秒）',
  `status` tinyint DEFAULT 0 COMMENT '状态：0-考试中 1-已提交 2-已批阅',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_task_id` (`task_id`),
  KEY `idx_paper_id` (`paper_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考试记录表';

-- =====================================================
-- 21. 考试答题记录表
-- =====================================================
DROP TABLE IF EXISTS `tr_exam_answer`;
CREATE TABLE `tr_exam_answer` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `record_id` bigint NOT NULL COMMENT '考试记录ID',
  `question_id` bigint NOT NULL COMMENT '题目ID',
  `answer` varchar(500) DEFAULT NULL COMMENT '考生答案',
  `is_correct` tinyint DEFAULT 0 COMMENT '是否正确：0-错误 1-正确',
  `score` int DEFAULT 0 COMMENT '得分',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_record_id` (`record_id`),
  KEY `idx_question_id` (`question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考试答题记录表';

-- =====================================================
-- 22. 审计追踪表
-- =====================================================
DROP TABLE IF EXISTS `sys_audit_log`;
CREATE TABLE `sys_audit_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint DEFAULT NULL COMMENT '操作人ID',
  `username` varchar(50) DEFAULT NULL COMMENT '操作账号',
  `real_name` varchar(50) DEFAULT NULL COMMENT '真实姓名',
  `operation_type` varchar(50) NOT NULL COMMENT '操作类型：INSERT/UPDATE/DELETE',
  `module` varchar(100) DEFAULT NULL COMMENT '操作模块',
  `description` varchar(500) DEFAULT NULL COMMENT '操作描述',
  `request_method` varchar(10) DEFAULT NULL COMMENT '请求方法',
  `request_url` varchar(500) DEFAULT NULL COMMENT '请求URL',
  `request_params` text COMMENT '请求参数',
  `before_data` text COMMENT '变更前数据',
  `after_data` text COMMENT '变更后数据',
  `ip` varchar(50) DEFAULT NULL COMMENT '操作IP',
  `user_agent` varchar(500) DEFAULT NULL COMMENT '用户代理',
  `execute_time` int DEFAULT NULL COMMENT '执行时长（毫秒）',
  `status` tinyint DEFAULT 1 COMMENT '状态：0-失败 1-成功',
  `error_msg` text COMMENT '错误信息',
  `operation_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_operation_time` (`operation_time`),
  KEY `idx_operation_type` (`operation_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审计追踪表';

-- =====================================================
-- 23. 系统配置表
-- =====================================================
DROP TABLE IF EXISTS `sys_config`;
CREATE TABLE `sys_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `config_key` varchar(100) NOT NULL COMMENT '配置键',
  `config_value` varchar(500) DEFAULT NULL COMMENT '配置值',
  `config_name` varchar(200) DEFAULT NULL COMMENT '配置名称',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` varchar(50) DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(50) DEFAULT NULL COMMENT '更新人',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';

-- =====================================================
-- 24. 消息通知表
-- =====================================================
DROP TABLE IF EXISTS `sys_message`;
CREATE TABLE `sys_message` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '接收人ID',
  `title` varchar(200) NOT NULL COMMENT '消息标题',
  `content` text COMMENT '消息内容',
  `message_type` tinyint DEFAULT 1 COMMENT '消息类型：1-任务通知 2-到期提醒 3-补考提醒 4-系统通知',
  `is_read` tinyint DEFAULT 0 COMMENT '是否已读：0-未读 1-已读',
  `related_id` bigint DEFAULT NULL COMMENT '关联业务ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `read_time` datetime DEFAULT NULL COMMENT '阅读时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_is_read` (`is_read`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息通知表';

-- =====================================================
-- 25. 视频学习记录表
-- =====================================================
DROP TABLE IF EXISTS `tr_video_study_record`;
CREATE TABLE `tr_video_study_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `task_id` bigint NOT NULL COMMENT '任务ID',
  `video_id` bigint NOT NULL COMMENT '视频ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `study_duration` int DEFAULT 0 COMMENT '学习时长（秒）',
  `video_progress` decimal(5,2) DEFAULT 0 COMMENT '视频播放进度（百分比）',
  `last_position` int DEFAULT 0 COMMENT '上次播放位置（秒）',
  `is_completed` tinyint DEFAULT 0 COMMENT '是否完成：0-未完成 1-已完成',
  `last_study_time` datetime DEFAULT NULL COMMENT '最后学习时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_task_id` (`task_id`),
  KEY `idx_video_id` (`video_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='视频学习记录表';

-- =====================================================
-- 增量变更脚本（针对已存在的旧库执行，全新安装可忽略）
-- 说明：以下语句用于将早期版本的表结构升级到当前版本，
--       已执行过的语句再次执行会因列已存在而报错，可忽略对应错误。
-- =====================================================

-- 1）题库表：course_id 允许为空，新增 category 分类字段，扩展题型取值
ALTER TABLE `tr_question` MODIFY COLUMN `course_id` bigint DEFAULT NULL COMMENT '课程ID（可为空）';
ALTER TABLE `tr_question` ADD COLUMN `category` varchar(50) DEFAULT NULL COMMENT '课程分类编码' AFTER `difficulty`;
ALTER TABLE `tr_question` MODIFY COLUMN `question_type` tinyint NOT NULL COMMENT '题目类型：1-单选 2-多选 3-判断 4-填空 5-简答';

-- 2）试卷表：course_id 允许为空，新增 code/composition_type/rules 字段，调整 status 语义
ALTER TABLE `tr_exam_paper` MODIFY COLUMN `course_id` bigint DEFAULT NULL COMMENT '课程ID（可为空）';
ALTER TABLE `tr_exam_paper` ADD COLUMN `code` varchar(64) DEFAULT NULL COMMENT '试卷编号' AFTER `id`;
ALTER TABLE `tr_exam_paper` ADD COLUMN `composition_type` varchar(20) DEFAULT 'random' COMMENT '组卷方式' AFTER `question_count`;
ALTER TABLE `tr_exam_paper` ADD COLUMN `rules` text COMMENT '组卷规则（JSON）' AFTER `composition_type`;
ALTER TABLE `tr_exam_paper` MODIFY COLUMN `status` tinyint DEFAULT 0 COMMENT '状态：0-草稿 1-已发布 2-已归档';
ALTER TABLE `tr_exam_paper` ADD UNIQUE KEY `uk_code` (`code`);

-- 3）培训计划表：新增 code/year/target_departments 字段
ALTER TABLE `tr_training_plan` ADD COLUMN `code` varchar(64) DEFAULT NULL COMMENT '计划编号' AFTER `id`;
ALTER TABLE `tr_training_plan` ADD COLUMN `year` int DEFAULT NULL COMMENT '计划年度' AFTER `name`;
ALTER TABLE `tr_training_plan` ADD COLUMN `target_departments` varchar(500) DEFAULT NULL COMMENT '培训对象部门ID列表（JSON）' AFTER `description`;
ALTER TABLE `tr_training_plan` ADD UNIQUE KEY `uk_code` (`code`);

-- 4）培训计划课程关联表：新增 trainer/planned_date 字段
ALTER TABLE `tr_plan_course` ADD COLUMN `trainer` varchar(50) DEFAULT NULL COMMENT '讲师' AFTER `course_id`;
ALTER TABLE `tr_plan_course` ADD COLUMN `planned_date` date DEFAULT NULL COMMENT '计划培训日期' AFTER `trainer`;

-- 5）培训任务表：新增 name/description 字段
ALTER TABLE `tr_training_task` ADD COLUMN `name` varchar(200) DEFAULT NULL COMMENT '任务名称' AFTER `id`;
ALTER TABLE `tr_training_task` ADD COLUMN `description` varchar(500) DEFAULT NULL COMMENT '任务说明' AFTER `task_type`;
