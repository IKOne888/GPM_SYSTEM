-- =====================================================================
-- GMP 培训系统 · 学员端功能扩展 增量脚本（v2）
-- 说明：本脚本为「学员端功能扩展」所需的数据库变更，可重复执行（幂等）
-- 关联文档：docs/学员端-功能扩展-设计与开发方案.md
-- 变更内容：
--   1) 新建「错题本」表 tr_wrong_question（学员答错题目自动收集、重做与掌握跟踪）
--   2) 培训任务表 tr_training_task 增加 source 字段（区分系统分配 / 自主报名）
-- 执行方式：mysql -uroot -p123456 gmp_training_system < gmp_training_system_v2_learner_ext.sql
-- 执行日期：2026-10-12
-- =====================================================================

-- ---------------------------------------------------------------------
-- 变更 1：新建错题本表 tr_wrong_question
-- 作用：记录学员在「在线考试」中答错的题目，支撑「错题本」与「错题重做」功能
-- 唯一键 uk_user_question 保证同一学员同一题只有一条记录，重复答错时累加 wrong_count
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `tr_wrong_question`;                        -- 若已存在则先删除，保证脚本可重复执行
CREATE TABLE `tr_wrong_question` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',            -- 主键，自增
  `user_id` bigint NOT NULL COMMENT '学员ID（sys_user.id）',        -- 归属学员，数据隔离依据
  `question_id` bigint NOT NULL COMMENT '题目ID（tr_question.id）', -- 关联题库题目
  `course_id` bigint DEFAULT NULL COMMENT '课程ID（冗余字段，便于按课程筛选错题）', -- 冗余课程，避免多次关联
  `wrong_count` int DEFAULT 1 COMMENT '累计答错次数',               -- 同一题重复答错时累加
  `last_wrong_time` datetime DEFAULT NULL COMMENT '最近答错时间',   -- 用于错题列表排序
  `mastered` tinyint DEFAULT 0 COMMENT '是否已掌握：0-未掌握 1-已掌握', -- 重做答对或手动移除后置 1
  `last_practice_correct` tinyint DEFAULT 0 COMMENT '最近一次练习是否答对：0-否 1-是', -- 重做即时反馈留痕
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间', -- 记录首次收集时间
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间', -- 每次变更自动更新
  PRIMARY KEY (`id`),                                              -- 主键索引
  UNIQUE KEY `uk_user_question` (`user_id`, `question_id`),        -- 唯一键：同一学员同一题仅一条记录（upsert 依据）
  KEY `idx_user_id` (`user_id`),                                   -- 按学员查询错题（列表/统计主查询）
  KEY `idx_course_id` (`course_id`),                               -- 按课程筛选错题
  KEY `idx_mastered` (`mastered`)                                  -- 按掌握状态筛选（默认只看未掌握）
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='错题本表';          -- 使用 utf8mb4 支持中文与 emoji

-- ---------------------------------------------------------------------
-- 变更 2：培训任务表 tr_training_task 增加 source 字段
-- 作用：区分任务来源，便于「课程中心自主报名」与「计划/管理员分配」的统计与筛选
-- 兼容性：存量数据默认值为 1（系统分配），不影响原有业务逻辑
-- 幂等处理：MySQL 8.0 不支持 ADD COLUMN IF NOT EXISTS，故使用存储过程判断后再执行
-- ---------------------------------------------------------------------
DROP PROCEDURE IF EXISTS `add_task_source_column`;                 -- 若存在同名过程先删除，保证可重复执行
DELIMITER $$                                                      -- 切换语句结束符，便于定义存储过程体
CREATE PROCEDURE `add_task_source_column`()                        -- 创建临时过程：安全添加字段
BEGIN
  -- 查询 information_schema，判断 tr_training_task 是否已存在 source 字段
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()                                -- 当前库
      AND TABLE_NAME = 'tr_training_task'                          -- 目标表
      AND COLUMN_NAME = 'source'                                   -- 目标字段
  ) THEN
    -- 不存在时新增字段：1-系统分配（计划/管理员） 2-自主报名，默认 1
    ALTER TABLE `tr_training_task`
      ADD COLUMN `source` tinyint DEFAULT 1 COMMENT '任务来源：1-系统分配 2-自主报名' AFTER `task_type`;
  END IF;
END$$                                                             -- 过程定义结束
DELIMITER ;                                                       -- 恢复默认语句结束符
CALL `add_task_source_column`();                                  -- 执行过程（幂等添加字段）
DROP PROCEDURE IF EXISTS `add_task_source_column`;                 -- 清理临时过程，不遗留数据库对象

-- =====================================================================
-- 脚本执行完成
-- 校验建议：
--   1) SHOW CREATE TABLE tr_wrong_question;   -- 确认表结构与唯一键 uk_user_question
--   2) SHOW COLUMNS FROM tr_training_task LIKE 'source';  -- 确认 source 字段已存在
--   3) UPDATE tr_training_task SET source = 1 WHERE source IS NULL;  -- 兜底：存量数据回填（可选）
-- =====================================================================
