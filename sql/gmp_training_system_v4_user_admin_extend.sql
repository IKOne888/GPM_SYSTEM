-- =====================================================================
-- GMP 培训系统 · 用户端与管理端功能补全 增量脚本（v4）
-- 说明：本脚本为「学员端培训需求申报 + 管理端个人中心/通知公告/待办中心」所需的数据库变更，
--       可重复执行（幂等），不会破坏存量数据。
-- 关联文档：docs/用户端与管理端-功能补全-设计与开发方案.md
-- 变更内容：
--   1) 培训需求表 tr_training_demand 增加 submit_user_id 字段（学员端提交人用户ID）
--   2) 培训需求表 tr_training_demand 增加索引 idx_demand_submit_user（「我的申报」按此过滤）
-- 其余三个功能均无表结构变更：
--   个人中心复用 sys_user；通知公告复用 sys_message；待办中心为纯查询聚合。
-- 兼容性说明：
--   submit_user_id 允许为 NULL，NULL 表示「管理端代录」，不回填存量数据，
--   避免把历史代录记录错误归属到某个学员；「我的申报」仅按 submit_user_id 过滤，故不会显示存量数据。
-- 执行方式：mysql -uroot -p123456 gmp_training_system < gmp_training_system_v4_user_admin_extend.sql
-- 执行日期：2026-10-13
-- =====================================================================

-- ---------------------------------------------------------------------
-- 变更 1：培训需求表 tr_training_demand 增加提交人字段
-- 幂等处理：MySQL 8.0 不支持 ADD COLUMN IF NOT EXISTS，故使用存储过程先判断再执行
-- ---------------------------------------------------------------------
DROP PROCEDURE IF EXISTS `add_demand_submit_user_column`;           -- 若存在同名过程先删除，保证可重复执行
DELIMITER $$                                                        -- 切换语句结束符，便于定义存储过程体
CREATE PROCEDURE `add_demand_submit_user_column`()                  -- 创建临时过程：安全添加提交人字段
BEGIN
  -- 1.1 提交人用户ID：学员端提交需求时写入，用于「我的申报」数据隔离与处理结果通知；
  --     管理端代录时为空（NULL），保持原展示行为不变
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tr_training_demand' AND COLUMN_NAME = 'submit_user_id'
  ) THEN
    ALTER TABLE `tr_training_demand`
      ADD COLUMN `submit_user_id` bigint DEFAULT NULL
      COMMENT '提交人用户ID（学员端提交时写入，用于我的申报隔离与结果通知；NULL-管理端代录）' AFTER `department_id`;
  END IF;
END$$                                                               -- 过程定义结束
DELIMITER ;                                                         -- 恢复默认语句结束符
CALL `add_demand_submit_user_column`();                             -- 执行过程（幂等添加字段）
DROP PROCEDURE IF EXISTS `add_demand_submit_user_column`;           -- 清理临时过程，不遗留数据库对象

-- ---------------------------------------------------------------------
-- 变更 2：为「我的申报」查询补充索引
-- 作用：学员端按 submit_user_id 过滤本人的需求记录，避免全表扫描
-- ---------------------------------------------------------------------
DROP PROCEDURE IF EXISTS `add_demand_submit_user_index`;            -- 幂等：先删除同名过程
DELIMITER $$
CREATE PROCEDURE `add_demand_submit_user_index`()
BEGIN
  -- 仅在索引不存在时创建，避免重复执行报错
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tr_training_demand' AND INDEX_NAME = 'idx_demand_submit_user'
  ) THEN
    ALTER TABLE `tr_training_demand` ADD KEY `idx_demand_submit_user` (`submit_user_id`);
  END IF;
END$$
DELIMITER ;
CALL `add_demand_submit_user_index`();
DROP PROCEDURE IF EXISTS `add_demand_submit_user_index`;

-- =====================================================================
-- 脚本执行完成
-- 校验建议：
--   1) SHOW COLUMNS FROM tr_training_demand LIKE 'submit_user_id';  -- 确认提交人字段已存在
--   2) SHOW INDEX FROM tr_training_demand WHERE Key_name = 'idx_demand_submit_user'; -- 确认索引已创建
--   3) SELECT COUNT(*) FROM tr_training_demand WHERE submit_user_id IS NULL;
--      -- 预期：存量数据 submit_user_id 均为 NULL，不影响原有列表展示
-- =====================================================================