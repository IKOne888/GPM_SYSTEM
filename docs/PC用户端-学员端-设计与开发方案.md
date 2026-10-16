# GMP 内部培训系统 —— PC 用户端（学员端）设计与开发方案

> 编写日期：2026-10-12
> 关联文档：《后端模块补全开发方案》《系统设计与接口文档》
> 目标：在 PC 管理端（`frontend-pc`）已全部打通的基础上，新建独立的 **PC 用户端（学员端）** 工程 `frontend-student`，
> 面向普通员工/学员，提供「我的任务 → 在线学习 → 在线考试 → 个人中心与培训档案」的完整闭环。

---

## 一、背景与目标

### 1.1 现状

| 端 | 工程 | 面向角色 | 状态 |
| --- | --- | --- | --- |
| PC 管理端 | `frontend-pc` | 系统管理员 / QA负责人 / 培训管理员 / 部门主管 | ✅ 已完成 |
| PC 用户端（学员端） | `frontend-student` | 普通员工（EMPLOYEE） | ⏳ 本文档规划 |
| 微信小程序 | `miniprogram` | 普通员工（移动端） | ⏸ 后续阶段，本方案预留接口对齐 |

后端数据表已具备学员端所需的全部结构（`tr_training_task`、`tr_course_video`、`tr_video_study_record`、
`tr_exam_paper`、`tr_question`、`tr_exam_record`、`tr_exam_answer`、`sys_message`），
但**学员端对应的 Java 实体 / Service / Controller 均未实现**，需要按本方案补齐。

### 1.2 目标

1. 新建独立前端工程 `frontend-student`，与管理端**完全分离**（独立路由、布局、主题、构建产物）；
2. 覆盖学员核心业务闭环：**领取任务 → 观看视频学习 → 参加考试 → 生成培训档案**；
3. 视觉风格参考**药企内部培训系统**：洁净、专业、蓝白主色、卡片化、数据可视；
4. 延续企业级规范：统一 `Result` 返回、`BusinessException` 错误处理、`@Valid` 校验、
   `@Transactional`、Sa-Token 鉴权、详细中文注释；
5. 接口设计兼顾后续小程序复用（统一 `/study/**` 学员接口，小程序按同一契约适配）。

---

## 二、角色与使用场景

### 2.1 目标角色

- 主角色：`EMPLOYEE`（普通员工/学员）
- 登录后仅可访问 `frontend-student`，不可进入管理端菜单（两端各自独立工程 + 独立 token 存储键名）。

### 2.2 典型场景

1. 员工登录 → 首页看到「待学习 / 进行中 / 待考试 / 已完成 / 已逾期」统计与任务列表；
2. 进入任务详情 → 查看课程目标、SOP 编号、视频课件、学习要求与截止时间；
3. 点击「开始学习」→ 视频播放器断点续播，进度实时上报，学完标记完成；
4. 学习达标后点击「去考试」→ 在线答题、倒计时、交卷自动判分；
5. 考试通过 → 任务完成，培训档案中生成成绩单与有效期；
6. 个人中心查看学习记录、考试成绩单、证书有效期，修改密码。

---

## 三、技术栈与工程结构

### 3.1 技术栈（与管理端一致，降低学习成本）

| 层 | 选型 |
| --- | --- |
| 框架 | Vue 3.4 + `<script setup>` |
| 构建 | Vite 5 |
| UI 库 | Element Plus 2.x |
| 状态 | Pinia（用户信息、任务统计缓存） |
| 路由 | Vue Router 4（history 模式 + 登录守卫） |
| 请求 | Axios 封装（`@/utils/request`，`baseURL=/api`，拦截器注入 token） |
| 可视化 | ECharts 5（学习进度环、统计图） |
| 富文本/编辑器 | 无（考试答题为表单化） |

### 3.2 工程结构

```
frontend-student/
├── index.html
├── package.json
├── vite.config.js              # 端口 3002，/api 代理到 http://localhost:8080
├── .env.development
├── .env.production
└── src/
    ├── main.js                 # 挂载 Element Plus / Pinia / Router
    ├── App.vue
    ├── assets/                 # 品牌 logo、登录背景图、图标
    ├── layout/
    │   ├── index.vue           # 主框架：顶部品牌栏 + 左侧菜单 + 内容区
    │   └── components/
    │       ├── SideMenu.vue    # 学员菜单
    │       ├── HeaderBar.vue   # 品牌区 + 消息铃铛 + 用户下拉
    │       └── MessageBell.vue # 未读消息下拉
    ├── router/
    │   └── index.js            # 路由 + 登录/角色守卫
    ├── store/
    │   ├── user.js             # 用户信息、token
    │   └── task.js             # 任务统计缓存
    ├── utils/
    │   ├── request.js          # axios 封装（Authorization 无前缀）
    │   ├── format.js           # 时长/日期/百分比格式化
    │   └── status.js           # 任务/考试状态字典
    ├── api/
    │   ├── study.js            # 学员任务/学习接口
    │   ├── exam.js             # 在线考试接口
    │   └── profile.js          # 个人中心接口
    └── views/
        ├── login/index.vue
        ├── dashboard/index.vue         # 我的任务（首页工作台）
        ├── task/detail.vue             # 任务详情
        ├── learn/play.vue              # 在线学习（视频播放）
        ├── exam/enter.vue              # 考试须知/进入
        ├── exam/test.vue               # 在线答题
        ├── exam/result.vue             # 成绩与解析
        ├── profile/index.vue           # 个人资料
        ├── profile/record.vue          # 学习记录 + 成绩单
        └── profile/certificate.vue     # 培训档案/证书
```

> `vite.config.js` 中 `server.port = 3002`，`/api` 代理到后端 `8080`（与管理端共用同一后端）。

---

## 四、视觉设计规范（药企内部培训风格）

### 4.1 设计基调

- 关键词：**洁净（Clean）、专业（Professional）、可信（Trustworthy）、数据化（Data-driven）**；
- 联想意象：GMP 洁净车间、蓝色无菌服、质量盾牌、检验报告、SOP 文档；
- 大量留白 + 白色卡片 + 柔和蓝调阴影 + 圆角，避免高饱和撞色。

### 4.2 设计令牌（Design Token）

| 令牌 | 值 | 用途 |
| --- | --- | --- |
| `--brand-primary` | `#1677FF` | 主色（按钮、链接、选中态） |
| `--brand-primary-hover` | `#3C8CFF` | 主色悬浮 |
| `--brand-primary-light` | `#E8F1FF` | 主色浅底（选中背景、标签底） |
| `--brand-teal` | `#13C2C2` | 辅助色（进度、科技感点缀） |
| `--brand-gradient` | `linear-gradient(135deg,#1677FF 0%,#13C2C2 100%)` | 品牌栏、统计卡、登录按钮 |
| `--success` | `#52C41A` | 已完成/通过 |
| `--warning` | `#FAAD14` | 待考试/临期 |
| `--danger` | `#FF4D4F` | 已逾期/未通过 |
| `--info` | `#8C9AAE` | 中性信息 |
| `--text-primary` | `#1F2D3D` | 标题 |
| `--text-regular` | `#5A6B7C` | 正文 |
| `--text-secondary` | `#8A94A6` | 辅助说明 |
| `--bg-page` | `#F0F5FA` | 页面底色（浅蓝灰） |
| `--bg-card` | `#FFFFFF` | 卡片底色 |
| `--border-color` | `#E4EAF2` | 分割线/边框 |
| `--radius-sm / md / lg` | `6px / 8px / 12px` | 圆角层级 |
| `--shadow-card` | `0 4px 16px rgba(22,119,255,.08)` | 卡片阴影 |
| `--shadow-hover` | `0 8px 24px rgba(22,119,255,.16)` | 悬浮阴影 |

字体：`-apple-system, "PingFang SC", "Microsoft YaHei", "Segoe UI", sans-serif`；
正文 14px，标题 16/20/24px，统计数字 28px 加粗。

### 4.3 关键界面风格

1. **登录页**：沿用管理端「药企洁净车间」背景图（`assets/login-bg.png`），右侧居中卡片，品牌渐变按钮；
2. **顶部品牌栏**：左侧 GMP 盾牌 Logo + 「GMP 培训学习中心」，右侧消息铃铛（未读红点）+ 用户头像下拉；
3. **左侧菜单**：浅色底，选中项 `--brand-primary-light` 底 + 主色文字 + 左侧主色竖条；
4. **首页工作台**：顶部 4~5 个渐变统计卡（待学习/学习中/待考试/已完成/已逾期）+ 任务卡片列表（卡片内含进度环、截止倒计时、状态标签）；
5. **学习页**：左侧视频播放器（黑底）、右侧课件目录与学习记录，底部醒目进度条与「标记完成」；
6. **考试页**：顶部固定倒计时条 + 题号导航面板，主区单题作答，底部「上一题/下一题/交卷」；
7. **培训档案**：证书卡片（盾牌 + 主色渐变边框）、成绩单表格、有效期进度条。

---

## 五、信息架构与路由设计

### 5.1 路由表

| 路径 | 页面 | 说明 | 需登录 |
| --- | --- | --- | --- |
| `/login` | `login/index.vue` | 登录（账号+密码+图形验证码） | 否 |
| `/` → `/dashboard` | `dashboard/index.vue` | 我的任务（工作台） | 是 |
| `/task/:id` | `task/detail.vue` | 任务详情 | 是 |
| `/learn/:taskId/:videoId?` | `learn/play.vue` | 在线学习（视频播放） | 是 |
| `/exam/enter/:taskId` | `exam/enter.vue` | 考试须知/进入考试 | 是 |
| `/exam/test/:recordId` | `exam/test.vue` | 在线答题 | 是 |
| `/exam/result/:recordId` | `exam/result.vue` | 成绩与解析 | 是 |
| `/profile` | `profile/index.vue` | 个人资料 | 是 |
| `/profile/record` | `profile/record.vue` | 学习记录 + 成绩单 | 是 |
| `/profile/certificate` | `profile/certificate.vue` | 培训档案/证书 | 是 |

### 5.2 路由守卫

1. 未登录访问受保护路由 → 重定向 `/login`；
2. 已登录访问 `/login` → 重定向 `/dashboard`；
3. token 失效（401）→ 清除本地信息并跳登录。

### 5.3 左侧菜单

```
我的任务        /dashboard
在线学习        /learn（由任务进入，菜单作快捷入口提示）
在线考试        /exam（由任务进入，菜单作快捷入口提示）
个人中心        /profile
 ├─ 个人资料     /profile
 ├─ 学习记录     /profile/record
 └─ 培训档案     /profile/certificate
```

---

## 六、功能模块详细设计

### 6.1 模块一：我的任务

**页面**：`dashboard/index.vue`、`task/detail.vue`

- 统计卡：调用 `GET /study/tasks/stats`，展示 待学习 / 学习中 / 待考试 / 已完成 / 已逾期；
- 任务列表：`GET /study/tasks`（分页 + `status` 筛选 + `keyword` 模糊任务名），
  卡片展示 任务名、课程名、截止时间（倒计时）、状态标签、学习进度环；
- 任务详情：`GET /study/tasks/{id}` 返回任务 + 课程信息 + 视频列表 + 学习记录 + 考试信息；
- 操作：
  - 「开始/继续学习」→ 跳 `/learn/:taskId/:videoId`；
  - 「去考试」→ 跳 `/exam/enter/:taskId`（未完成学习时禁用并提示）；
  - 「查看成绩」→ 跳 `/exam/result/:recordId`。

**状态字典**（后端 tinyint ↔ 前端文案，集中定义于 `utils/status.js`）：

| 值 | 文案 | 标签色 |
| --- | --- | --- |
| 0 | 待学习 | info |
| 1 | 学习中 | primary |
| 2 | 待考试 | warning |
| 3 | 已完成 | success |
| 4 | 已逾期 | danger |

### 6.2 模块二：在线学习

**页面**：`learn/play.vue`

- 视频源：`GET /study/videos/{videoId}/stream`（支持 Range 206，`<video>` 直接播放，无需 token）；
- 断点续播：进入页面 `GET /study/videos/{videoId}/record?taskId=` 获取 `lastPosition`，播放器 `currentTime` 跳到该位置；
- 进度上报：`POST /study/videos/progress`，节流（每 15 秒 / 暂停 / 切页 / 播放结束时上报），
  参数 `{taskId, videoId, progress, lastPosition, studyDuration}`；
  后端写 `tr_video_study_record`，并按所有视频加权回填 `tr_training_task.video_progress` 与 `study_duration`；
- 学完判定：单视频 `progress ≥ 95%` 视为该视频完成，全部视频完成 → 任务进入「待考试」（status=2）；
- 课程资料：`GET /study/courses/{courseId}/files` 列出资料，`GET /study/courses/files/{fileId}/url` 取下载地址；
- 可播放格式：与后端一致（MP4）

### 6.3 模块三：在线考试

**页面**：`exam/enter.vue`、`exam/test.vue`、`exam/result.vue`

- 考试须知：`GET /study/tasks/{taskId}/exam` 返回试卷基本信息（`name`、`duration` 分钟、`passScore`、
  `questionCount`、`totalScore`）、剩余考试次数（`maxRetry - retryCount`）、上次成绩；
- 开始考试：`POST /study/exams/{taskId}/start` → 创建 `tr_exam_record`（status=0 考试中），
  返回 `recordId` + 试卷题目（**不含答案与解析**）+ 交卷截止时间；
- 在线答题：`GET /study/exams/records/{recordId}` 取回题目与已作答内容；前端状态机管理作答，
  题型支持 单选 / 多选 / 判断 / 填空 / 简答；
- 交卷：`POST /study/exams/records/{recordId}/submit`，提交 `answers:{questionId, answer}`；
  后端自动判分（客观题自动判分，主观题按关键词/待人工批阅策略，本轮客观题为主）：
  1. 写 `tr_exam_answer`（每题：答案、是否正确、得分）；
  2. 更新 `tr_exam_record`（score、pass、submitTime、duration、status=1/2）；
  3. 回填 `tr_training_task`（examScore、examPass、retryCount）；
  4. 通过则任务 `status=3 已完成`，并写 `sys_message` 通知；
- 成绩与解析：`GET /study/exams/records/{recordId}/result` 返回得分、是否通过、逐题对错与解析；
- 超时保护：前端倒计时归零自动交卷；后端校验提交时间不得晚于 `startTime + duration × 1.2`（容错）。

**考试状态字典**：

| 值 | 文案 |
| --- | --- |
| 0 | 考试中 |
| 1 | 已提交 |
| 2 | 已批阅 |

### 6.4 模块四：个人中心与培训档案

**页面**：`profile/index.vue`、`profile/record.vue`、`profile/certificate.vue`

- 个人资料：`GET /study/profile`（脱敏：不含密码/token）、`PUT /study/profile`（姓名/手机等可改字段）、
  `PUT /study/profile/password`（旧密码 + 新密码，强度：6–20 位含字母和数字）；
- 学习记录：`GET /study/records/study` 分页（课程、视频、学习时长、进度、完成时间）；
- 成绩单：`GET /study/records/exam` 分页（试卷名、得分、是否通过、第几次、提交时间）；
- 培训档案/证书：`GET /study/records/certificate`
  按课程 `tr_course.retrain_period`（再培训周期，天）计算**有效期至**：
  `有效期至 = 最近一次通过时间 + retrain_period 天`，并标记 有效 / 临期（≤30 天）/ 已过期；
- 消息通知：`GET /study/messages`（分页）、`PUT /study/messages/{id}/read`、
  `PUT /study/messages/read-all`，顶部铃铛未读数 `GET /study/messages/unread-count`。

---

## 七、后端接口契约（学员端新增，统一前缀 `/study`）

> 说明：学员端使用独立前缀 `/study/**`，与管理员端 `/training/**`、`/exam/**` 分离，避免与管理员鉴权规则冲突；
> 全部接口默认 `SaRouter.match("/study/**")` 仅需登录（不做管理角色限制），并在服务端强制「仅能操作本人数据」。

### 7.1 我的任务

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/study/tasks` | 分页：`page/size/status/keyword`，仅当前登录用户 |
| GET | `/study/tasks/stats` | 各状态任务数量统计 |
| GET | `/study/tasks/{id}` | 任务详情（含课程、视频列表、学习记录、考试信息） |

### 7.2 在线学习

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/study/tasks/{id}/videos` | 任务视频列表（含本人学习进度） |
| GET | `/study/videos/{videoId}/stream` | 视频流（Range 206，登录链白名单放行） |
| GET | `/study/videos/{videoId}/record` | 单视频学习记录：`taskId` 参数 |
| POST | `/study/videos/progress` | 上报学习进度（节流） |
| GET | `/study/courses/{courseId}/files` | 课程资料列表 |
| GET | `/study/courses/files/{fileId}/url` | 资料下载地址 |

### 7.3 在线考试

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/study/tasks/{taskId}/exam` | 试卷信息 + 剩余次数 + 上次成绩 |
| POST | `/study/exams/{taskId}/start` | 开始考试，创建记录，返回题目（无答案） |
| GET | `/study/exams/records/{recordId}` | 取回考试中题目与已作答 |
| POST | `/study/exams/records/{recordId}/submit` | 交卷自动判分 |
| GET | `/study/exams/records/{recordId}/result` | 成绩与逐题解析 |

### 7.4 个人中心与档案

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/study/profile` | 个人信息（脱敏） |
| PUT | `/study/profile` | 修改个人资料 |
| PUT | `/study/profile/password` | 修改密码 |
| GET | `/study/records/study` | 学习记录分页 |
| GET | `/study/records/exam` | 考试成绩单分页 |
| GET | `/study/records/certificate` | 培训档案/证书有效期 |
| GET | `/study/messages` | 消息分页 |
| GET | `/study/messages/unread-count` | 未读数 |
| PUT | `/study/messages/{id}/read` | 单条已读 |
| PUT | `/study/messages/read-all` | 全部已读 |

### 7.5 返回与字段约定

- 统一 `Result<T>`；分页统一 `Page<VO>`（`records` + `total`）；
- 任务 VO 字段：`id`、`name`、`courseName`、`planName`、`deadline`、`status`（字符串）、
  `videoProgress`、`studyDuration`、`examScore`、`examPass`、`description`、`createTime`；
- 学习记录 VO：`videoId`、`videoName`、`studyDuration`、`videoProgress`、`lastPosition`、`isCompleted`；
- 考试 VO：题目含 `id`、`type`、`content`、`options`、`score`（**不含 answer/analysis**，判分后结果接口再返回）。

---

## 八、数据模型（复用现有表）

| 表 | 用途 | 学员端操作 |
| --- | --- | --- |
| `tr_training_task` | 我的任务 | 读 + 更新进度/成绩/状态 |
| `tr_course` / `tr_course_file` | 课程与资料 | 读 |
| `tr_course_video` | 视频课件 | 读 |
| `tr_video_study_record` | 视频学习记录 | 写（进度）+ 读 |
| `tr_exam_paper` / `tr_question` / `tr_paper_question` | 试卷与题目 | 读 |
| `tr_exam_record` | 考试记录 | 写（开始/交卷）+ 读 |
| `tr_exam_answer` | 答题明细 | 写（交卷）+ 读 |
| `sys_message` | 消息通知 | 读写 |
| `sys_user` | 个人信息 | 读 + 改（脱敏） |

**需新增的后端实体/Mapper**（当前缺失）：
`TrVideoStudyRecord`、`TrExamAnswer`、`SysMessage`（其余实体已存在）。

---

## 九、权限与安全

1. Sa-Token 拦截：`/study/**` 仅校验登录（`StpUtil.checkLogin()`），不做管理角色限制；
2. 数据隔离：所有 `/study/**` 接口在 Service 层强制以 `StpUtil.getLoginIdAsLong()` 过滤，
   禁止越权访问他人任务/记录；
3. 白名单：`/study/videos/*/stream`、`/study/courses/files/*` 加入登录链白名单（供 `<video>`、直链下载）；
4. 密码安全：修改密码校验旧密码、强度校验（6–20 位含字母和数字）、BCrypt 存储；
5. 防刷：考试交卷校验记录归属与状态，禁止重复交卷；
6. token：`Authorization` 头 **无** `Bearer ` 前缀（与后端 Sa-Token 配置一致）。

---

## 十、实施计划与验收标准

### 10.1 实施步骤

| 阶段 | 内容 | 产物 |
| --- | --- | --- |
| P1 | 后端学员接口：实体 `TrVideoStudyRecord/TrExamAnswer/SysMessage` + Mapper + `StudyService` + `StudyController` + 权限补充 | 编译通过、接口自测 200 |
| P2 | 前端工程初始化：`frontend-student` 脚手架、请求封装、路由、布局、主题令牌 | `npm run dev` 可跑空壳 |
| P3 | 模块一「我的任务」+ 登录 | 首页工作台可用 |
| P4 | 模块二「在线学习」 | 视频播放、进度上报、断点续播可用 |
| P5 | 模块三「在线考试」 | 答题、倒计时、交卷判分、成绩解析可用 |
| P6 | 模块四「个人中心与培训档案」 | 资料、记录、证书、消息可用 |
| P7 | 联调收尾 | `npm run build` 通过、端到端演示 |

### 10.2 验收标准

1. 学员登录后仅见学员端页面，无法访问管理端功能；
2. 任务列表/详情/统计正确，状态与进度实时更新；
3. 视频可播放、可续播，进度上报后任务进度同步变化；
4. 考试全流程可用：进入 → 答题 → 交卷 → 判分 → 成绩与解析；
5. 个人中心展示学习记录、成绩单、证书有效期与消息通知；
6. 所有页面视觉统一（蓝白主色、卡片化、药企风格），无「请求资源不存在」。

---

## 十一、后续阶段（非本次范围）

- **微信小程序**：复用 `/study/**` 学员接口；修正 `miniprogram/utils/request.js` 的 `Bearer ` 前缀
  与登录路径（`/system/user/login` → `/auth/login`），补齐缺失页面（`exam/list`、`exam/test`、
  `exam/result`、`profile/index`、`profile/record`）。
