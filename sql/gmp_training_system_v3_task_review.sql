-- =====================================================================
-- GMP 培训系统 · 培训任务两级审核闭环 增量脚本（v3）
-- 说明：本脚本为「培训任务两级审核（主管审核 + QA 复核）」所需的数据库变更，
--       可重复执行（幂等），不会破坏存量数据。
-- 关联文档：docs/培训任务两级审核闭环-设计与开发方案.md
-- 变更内容：
--   1) 培训任务表 tr_training_task 增加 7 个审核字段：
--      review_status、leader_review_user_id / time / remark、qa_review_user_id / time / remark
--   2) 培训计划表 tr_training_plan 增加 submit_user_id 字段（用于「自提自批」校验）
-- 兼容性说明：
--   review_status 允许为 NULL，NULL 表示「尚未进入审核链」；
--   存量已完成任务（status=3）review_status 保持 NULL，页面按「已完成」展示，无需回填。
-- 执行方式：mysql -uroot -p123456 gmp_training_system < gmp_training_system_v3_task_review.sql
-- 执行日期：2026-10-13
-- =====================================================================

-- ---------------------------------------------------------------------
-- 变更 1：培训任务表 tr_training_task 增加审核相关字段
-- 幂等处理：MySQL 8.0 不支持 ADD COLUMN IF NOT EXISTS，故使用存储过程先判断再执行
-- ---------------------------------------------------------------------
DROP PROCEDURE IF EXISTS `add_task_review_columns`;                 -- 若存在同名过程先删除，保证可重复执行
DELIMITER $$                                                        -- 切换语句结束符，便于定义存储过程体
CREATE PROCEDURE `add_task_review_columns`()                        -- 创建临时过程：安全添加审核字段
BEGIN
  -- 1.1 审核状态：0-待主管审核 1-待QA复核 2-已复核通过 3-已退回；NULL-未进入审核链
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tr_training_task' AND COLUMN_NAME = 'review_status'
  ) THEN
    ALTER TABLE `tr_training_task`
      ADD COLUMN `review_status` tinyint DEFAULT NULL
      COMMENT '审核状态：0-待主管审核 1-待QA复核 2-已复核通过 3-已退回；NULL-未进入审核链' AFTER `status`;
  END IF;

  -- 1.2 一级审核人（部门主管）用户ID
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tr_training_task' AND COLUMN_NAME = 'leader_review_user_id'
  ) THEN
    ALTER TABLE `tr_training_task`
      ADD COLUMN `leader_review_user_id` bigint DEFAULT NULL
      COMMENT '一级审核人（部门主管）用户ID' AFTER `review_status`;
  END IF;

  -- 1.3 一级审核时间
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tr_training_task' AND COLUMN_NAME = 'leader_review_time'
  ) THEN
    ALTER TABLE `tr_training_task`
      ADD COLUMN `leader_review_time` datetime DEFAULT NULL
      COMMENT '一级审核时间' AFTER `leader_review_user_id`;
  END IF;

  -- 1.4 一级审核意见（主管通过/退回说明）
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tr_training_task' AND COLUMN_NAME = 'leader_review_remark'
  ) THEN
    ALTER TABLE `tr_training_task`
      ADD COLUMN `leader_review_remark` varchar(255) DEFAULT NULL
      COMMENT '一级审核意见（主管审核说明）' AFTER `leader_review_time`;
  END IF;

  -- 1.5 二级复核人（QA）用户ID
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tr_training_task' AND COLUMN_NAME = 'qa_review_user_id'
  ) THEN
    ALTER TABLE `tr_training_task`
      ADD COLUMN `qa_review_user_id` bigint DEFAULT NULL
      COMMENT '二级复核人（QA）用户ID' AFTER `leader_review_remark`;
  END IF;

  -- 1.6 二级复核时间
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tr_training_task' AND COLUMN_NAME = 'qa_review_time'
  ) THEN
    ALTER TABLE `tr_training_task`
      ADD COLUMN `qa_review_time` datetime DEFAULT NULL
      COMMENT '二级复核时间' AFTER `qa_review_user_id`;
  END IF;

  -- 1.7 二级复核意见（QA 复核说明 / 免考确认理由）
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tr_training_task' AND COLUMN_NAME = 'qa_review_remark'
  ) THEN
    ALTER TABLE `tr_training_task`
      ADD COLUMN `qa_review_remark` varchar(255) DEFAULT NULL
      COMMENT '二级复核意见（QA 复核说明 / 免考确认理由）' AFTER `qa_review_time`;
  END IF;
END$$                                                               -- 过程定义结束
DELIMITER ;                                                         -- 恢复默认语句结束符
CALL `add_task_review_columns`();                                   -- 执行过程（幂等添加字段）
DROP PROCEDURE IF EXISTS `add_task_review_columns`;                 -- 清理临时过程，不遗留数据库对象

-- ---------------------------------------------------------------------
-- 变更 2：为审核查询补充索引
-- 作用：后台「待我审核」列表按 review_status 过滤，学员端任务列表按审核状态展示
-- ---------------------------------------------------------------------
DROP PROCEDURE IF EXISTS `add_task_review_index`;                   -- 幂等：先删除同名过程
DELIMITER $$
CREATE PROCEDURE `add_task_review_index`()
BEGIN
  -- 仅在索引不存在时创建，避免重复执行报错
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tr_training_task' AND INDEX_NAME = 'idx_review_status'
  ) THEN
    ALTER TABLE `tr_training_task` ADD KEY `idx_review_status` (`review_status`);
  END IF;
END$$
DELIMITER ;
CALL `add_task_review_index`();
DROP PROCEDURE IF EXISTS `add_task_review_index`;

-- ---------------------------------------------------------------------
-- 变更 3：培训计划表 tr_training_plan 增加 submit_user_id 字段
-- 作用：记录「提交审批人」的用户ID，用于校验「审批人 ≠ 提交人」，修复自提自批缺陷
-- 说明：原 create_by 存的是登录账号 username，无法与 StpUtil.getLoginIdAsLong() 直接比对，故显式新增
-- ---------------------------------------------------------------------
DROP PROCEDURE IF EXISTS `add_plan_submit_user_column`;             -- 幂等：先删除同名过程
DELIMITER $$
CREATE PROCEDURE `add_plan_submit_user_column`()
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tr_training_plan' AND COLUMN_NAME = 'submit_user_id'
  ) THEN
    ALTER TABLE `tr_training_plan`
      ADD COLUMN `submit_user_id` bigint DEFAULT NULL
      COMMENT '提交审批人用户ID（用于「自提自批」校验）' AFTER `submit_time`;
  END IF;
END$$
DELIMITER ;
CALL `add_plan_submit_user_column`();
DROP PROCEDURE IF EXISTS `add_plan_submit_user_column`;

-- =====================================================================
-- 脚本执行完成
-- 校验建议：
--   1) SHOW COLUMNS FROM tr_training_task LIKE 'review%';       -- 确认审核状态字段已存在
--   2) SHOW COLUMNS FROM tr_training_task LIKE '%review_user_id%';
--   3) SHOW COLUMNS FROM tr_training_plan LIKE 'submit_user_id';-- 确认计划表字段已存在
--   4) SELECT review_status, COUNT(*) FROM tr_training_task GROUP BY review_status;
--      -- 预期：存量数据 review_status 均为 NULL，不影响原有展示
-- =====================================================================
