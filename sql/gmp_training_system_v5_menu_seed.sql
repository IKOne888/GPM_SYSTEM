-- ======================================================================
-- GMP 培训系统 - sys_menu 菜单种子数据 + sys_role_menu 角色菜单绑定
-- 来源：frontend-pc/src/router/index.js 路由定义
-- ======================================================================

SET NAMES utf8mb4;

-- 清空菜单数据（表已建好，不走 DROP/CREATE）
DELETE FROM sys_role_menu;
DELETE FROM sys_menu;
ALTER TABLE sys_menu AUTO_INCREMENT = 1;
ALTER TABLE sys_role_menu AUTO_INCREMENT = 1;

-- ----------------------------------------------------------------------
-- 顶级菜单（type = 1，目录）
-- ----------------------------------------------------------------------

-- 1. 首页
INSERT INTO sys_menu(name, parent_id, path, component, icon, sort, type, permission, visible, status)
VALUES('首页', 0, '/dashboard', NULL, 'HomeFilled', 0, 1, NULL, 1, 1);
SET @pid_dashboard = LAST_INSERT_ID();

INSERT INTO sys_menu(name, parent_id, path, component, icon, sort, type, permission, visible, status)
VALUES('首页', @pid_dashboard, '/dashboard', 'views/dashboard/index', NULL, 0, 2, NULL, 1, 1);

-- 2. 待办中心
INSERT INTO sys_menu(name, parent_id, path, component, icon, sort, type, permission, visible, status)
VALUES('待办中心', 0, '/todo', NULL, 'Bell', 1, 1, NULL, 1, 1);
SET @pid_todo = LAST_INSERT_ID();

INSERT INTO sys_menu(name, parent_id, path, component, icon, sort, type, permission, visible, status)
VALUES('待办中心', @pid_todo, '/todo/index', 'views/todo/index', NULL, 0, 2, NULL, 1, 1);

-- 3. 系统管理
INSERT INTO sys_menu(name, parent_id, path, component, icon, sort, type, permission, visible, status)
VALUES('系统管理', 0, '/system', NULL, 'Setting', 2, 1, NULL, 1, 1);
SET @pid_system = LAST_INSERT_ID();

INSERT INTO sys_menu(name, parent_id, path, component, icon, sort, type, permission, visible, status)
VALUES('用户管理',   @pid_system, '/system/user',       'views/system/user/index',       'User',            0, 2, NULL, 1, 1);
INSERT INTO sys_menu(name, parent_id, path, component, icon, sort, type, permission, visible, status)
VALUES('部门管理',   @pid_system, '/system/department', 'views/system/department/index', 'OfficeBuilding',  1, 2, NULL, 1, 1);
INSERT INTO sys_menu(name, parent_id, path, component, icon, sort, type, permission, visible, status)
VALUES('岗位管理',   @pid_system, '/system/position',   'views/system/position/index',   'Postcard',        2, 2, NULL, 1, 1);
INSERT INTO sys_menu(name, parent_id, path, component, icon, sort, type, permission, visible, status)
VALUES('通知公告',   @pid_system, '/system/notice',     'views/system/notice/index',     'ChatDotSquare',   3, 2, NULL, 1, 1);

-- 4. 培训管理
INSERT INTO sys_menu(name, parent_id, path, component, icon, sort, type, permission, visible, status)
VALUES('培训管理', 0, '/training', NULL, 'Reading', 3, 1, NULL, 1, 1);
SET @pid_training = LAST_INSERT_ID();

INSERT INTO sys_menu(name, parent_id, path, component, icon, sort, type, permission, visible, status)
VALUES('课程管理',   @pid_training, '/training/course', 'views/training/course/index', 'Notebook',     0, 2, NULL, 1, 1);
INSERT INTO sys_menu(name, parent_id, path, component, icon, sort, type, permission, visible, status)
VALUES('视频课件',   @pid_training, '/training/video',  'views/training/video/index',  'VideoCamera',  1, 2, NULL, 1, 1);
INSERT INTO sys_menu(name, parent_id, path, component, icon, sort, type, permission, visible, status)
VALUES('PDF课件',    @pid_training, '/training/pdf',    'views/training/pdf/index',    'Document',     2, 2, NULL, 1, 1);
INSERT INTO sys_menu(name, parent_id, path, component, icon, sort, type, permission, visible, status)
VALUES('培训需求',   @pid_training, '/training/demand', 'views/training/demand/index', 'Notification', 3, 2, NULL, 1, 1);
INSERT INTO sys_menu(name, parent_id, path, component, icon, sort, type, permission, visible, status)
VALUES('培训计划',   @pid_training, '/training/plan',   'views/training/plan/index',   'Calendar',     4, 2, NULL, 1, 1);
INSERT INTO sys_menu(name, parent_id, path, component, icon, sort, type, permission, visible, status)
VALUES('培训任务',   @pid_training, '/training/task',   'views/training/task/index',   'List',         5, 2, NULL, 1, 1);

-- 5. 考试管理
INSERT INTO sys_menu(name, parent_id, path, component, icon, sort, type, permission, visible, status)
VALUES('考试管理', 0, '/exam', NULL, 'EditPen', 4, 1, NULL, 1, 1);
SET @pid_exam = LAST_INSERT_ID();

INSERT INTO sys_menu(name, parent_id, path, component, icon, sort, type, permission, visible, status)
VALUES('题库管理',   @pid_exam, '/exam/question', 'views/exam/question/index', 'Collection', 0, 2, NULL, 1, 1);
INSERT INTO sys_menu(name, parent_id, path, component, icon, sort, type, permission, visible, status)
VALUES('试卷管理',   @pid_exam, '/exam/paper',    'views/exam/paper/index',    'Document',   1, 2, NULL, 1, 1);

-- 6. 审计追踪
INSERT INTO sys_menu(name, parent_id, path, component, icon, sort, type, permission, visible, status)
VALUES('审计追踪', 0, '/audit', NULL, 'View', 5, 1, NULL, 1, 1);
SET @pid_audit = LAST_INSERT_ID();

INSERT INTO sys_menu(name, parent_id, path, component, icon, sort, type, permission, visible, status)
VALUES('操作日志',   @pid_audit, '/audit/log', 'views/audit/log/index', 'Tickets', 0, 2, NULL, 1, 1);

-- 7. 统计报表
INSERT INTO sys_menu(name, parent_id, path, component, icon, sort, type, permission, visible, status)
VALUES('统计报表', 0, '/report', NULL, 'DataAnalysis', 6, 1, NULL, 1, 1);
SET @pid_report = LAST_INSERT_ID();

INSERT INTO sys_menu(name, parent_id, path, component, icon, sort, type, permission, visible, status)
VALUES('报表概览',   @pid_report, '/report/index', 'views/report/index', 'TrendCharts', 0, 2, NULL, 1, 1);

-- 8. 个人中心（路由 hidden=true，visible 设为 0，不出现在侧栏菜单，但仍可按路径访问）
INSERT INTO sys_menu(name, parent_id, path, component, icon, sort, type, permission, visible, status)
VALUES('个人中心', 0, '/profile', NULL, 'User', 7, 1, NULL, 0, 1);
SET @pid_profile = LAST_INSERT_ID();

INSERT INTO sys_menu(name, parent_id, path, component, icon, sort, type, permission, visible, status)
VALUES('个人中心',   @pid_profile, '/profile/index', 'views/profile/index', NULL, 0, 2, NULL, 0, 1);

-- ----------------------------------------------------------------------
-- 读取所有菜单 id 到变量，后面按角色批量绑定
-- ----------------------------------------------------------------------
SELECT id INTO @m_dashboard     FROM sys_menu WHERE path = '/dashboard'   AND parent_id = @pid_dashboard LIMIT 1;
SELECT id INTO @m_todo         FROM sys_menu WHERE path = '/todo/index'   AND parent_id = @pid_todo      LIMIT 1;
SELECT id INTO @m_sys_user     FROM sys_menu WHERE path = '/system/user'         LIMIT 1;
SELECT id INTO @m_sys_dept     FROM sys_menu WHERE path = '/system/department'   LIMIT 1;
SELECT id INTO @m_sys_pos      FROM sys_menu WHERE path = '/system/position'     LIMIT 1;
SELECT id INTO @m_sys_notice   FROM sys_menu WHERE path = '/system/notice'       LIMIT 1;
SELECT id INTO @m_tr_course    FROM sys_menu WHERE path = '/training/course'     LIMIT 1;
SELECT id INTO @m_tr_video     FROM sys_menu WHERE path = '/training/video'      LIMIT 1;
SELECT id INTO @m_tr_pdf       FROM sys_menu WHERE path = '/training/pdf'        LIMIT 1;
SELECT id INTO @m_tr_demand    FROM sys_menu WHERE path = '/training/demand'     LIMIT 1;
SELECT id INTO @m_tr_plan      FROM sys_menu WHERE path = '/training/plan'       LIMIT 1;
SELECT id INTO @m_tr_task      FROM sys_menu WHERE path = '/training/task'       LIMIT 1;
SELECT id INTO @m_ex_question  FROM sys_menu WHERE path = '/exam/question'       LIMIT 1;
SELECT id INTO @m_ex_paper     FROM sys_menu WHERE path = '/exam/paper'          LIMIT 1;
SELECT id INTO @m_audit_log    FROM sys_menu WHERE path = '/audit/log'           LIMIT 1;
SELECT id INTO @m_report       FROM sys_menu WHERE path = '/report/index'        LIMIT 1;
SELECT id INTO @m_profile      FROM sys_menu WHERE path = '/profile/index'      LIMIT 1;

-- ----------------------------------------------------------------------
-- sys_role_menu 初始绑定
-- 角色 id（来自 sys_role 种子）：
--   1 = SUPER_ADMIN   2 = QA_MANAGER   3 = TRAINING_ADMIN
--   4 = DEPT_LEADER   5 = EMPLOYEE（不进管理端菜单，跳过）
-- ----------------------------------------------------------------------

-- ---------- SUPER_ADMIN(1)：所有菜单 ----------
INSERT INTO sys_role_menu(role_id, menu_id) VALUES
(1, @pid_dashboard), (1, @m_dashboard),
(1, @pid_todo),      (1, @m_todo),
(1, @pid_system),    (1, @m_sys_user), (1, @m_sys_dept), (1, @m_sys_pos), (1, @m_sys_notice),
(1, @pid_training),  (1, @m_tr_course), (1, @m_tr_video), (1, @m_tr_pdf), (1, @m_tr_demand), (1, @m_tr_plan), (1, @m_tr_task),
(1, @pid_exam),      (1, @m_ex_question), (1, @m_ex_paper),
(1, @pid_audit),     (1, @m_audit_log),
(1, @pid_report),    (1, @m_report),
(1, @pid_profile),   (1, @m_profile);

-- ---------- QA_MANAGER(2)：系统管理(除部门/岗位) + 培训管理(除视频/PDF) + 考试 + 审计 + 报表 + 待办 ----------
INSERT INTO sys_role_menu(role_id, menu_id) VALUES
(2, @pid_dashboard), (2, @m_dashboard),
(2, @pid_todo),      (2, @m_todo),
(2, @pid_system),    (2, @m_sys_user), (2, @m_sys_notice),
(2, @pid_training),  (2, @m_tr_course), (2, @m_tr_demand), (2, @m_tr_plan), (2, @m_tr_task),
(2, @pid_exam),      (2, @m_ex_question), (2, @m_ex_paper),
(2, @pid_audit),     (2, @m_audit_log),
(2, @pid_report),    (2, @m_report),
(2, @pid_profile),   (2, @m_profile);

-- ---------- TRAINING_ADMIN(3)：培训管理全部 + 考试管理 + 统计报表 + 待办中心 ----------
INSERT INTO sys_role_menu(role_id, menu_id) VALUES
(3, @pid_dashboard), (3, @m_dashboard),
(3, @pid_todo),      (3, @m_todo),
(3, @pid_system),    (3, @m_sys_user), (3, @m_sys_notice),
(3, @pid_training),  (3, @m_tr_course), (3, @m_tr_video), (3, @m_tr_pdf), (3, @m_tr_demand), (3, @m_tr_plan), (3, @m_tr_task),
(3, @pid_exam),      (3, @m_ex_question), (3, @m_ex_paper),
(3, @pid_report),    (3, @m_report),
(3, @pid_profile),   (3, @m_profile);

-- ---------- DEPT_LEADER(4)：培训任务 + 培训需求 + 待办中心 + 个人中心 ----------
INSERT INTO sys_role_menu(role_id, menu_id) VALUES
(4, @pid_dashboard), (4, @m_dashboard),
(4, @pid_todo),      (4, @m_todo),
(4, @pid_training),  (4, @m_tr_demand), (4, @m_tr_task),
(4, @pid_profile),   (4, @m_profile);

-- EMPLOYEE(5) 不进管理端菜单，跳过
