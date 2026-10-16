# GMP 培训系统 · RAG 知识库与智能问答 设计与开发方案

> 版本：v1.0
> 编写日期：2026-10-15
> 关联文档：《系统设计与接口文档.md》《用户端与管理端-功能补全-设计与开发方案.md》《功能设计报告.md》
> 适用范围：`backend`（8080）、`frontend-pc`（管理端 3000）、`frontend-student`（学员端 3002）、`miniprogram`（微信小程序）

---

## 1. 背景与目标

### 1.1 背景

系统已交付培训管理、考试管理、审计追踪等 8 大业务模块，沉淀了大量**结构化程度高、文本密集、且越准确越重要**的资料：

- 培训课件（PDF / 视频、`tr_course_pdf`）；
- 考试题库（`tr_question`）；
- 制度类文档（由管理端上传维护，当前无承载表）。

学员在学习过程中遇到「洁净区更衣有哪些 GMP 要求」「偏差处理时限是多久」等问题时，只能自行翻阅课件，检索效率低、易遗漏。本方案引入 **RAG（Retrieval-Augmented Generation，检索增强生成）**，让系统基于**内部知识库**回答问题，并给出可溯源的引用出处。

### 1.2 目标

1. 管理端提供**知识库管理**：上传文档 → 自动解析分块 → 向量化入库 → 可查看分块、重建索引、删除；
2. 学员端提供**智能问答**：自然语言提问 → 检索内部资料 → 生成答案 → **返回引用出处**；
3. 问答链路具备**降级能力**：模型或向量服务不可用时，退化为关键词资料检索，保证「可用」；
4. **零重型中间件**：不引入 Milvus / Elasticsearch 等独立服务，向量数据复用既有 MySQL 存储。

### 1.3 边界（本次不做）

- 不做多模态（图片 / 视频内容理解），仅处理**文本类文档**；
- 不做 OCR，扫描件 PDF 直接判定解析失败并给出提示；
- 不做多路召回 / 重排（Rerank）/ 知识图谱，仅**单路向量召回 + 阈值过滤**；
- 不做流式输出（SSE），答案一次性返回；
- 不动任何既有接口的路径与返回结构（只新增）。

---

## 2. 总体架构

### 2.1 技术选型

| 组件 | 选型 | 理由 |
| --- | --- | --- |
| 向量存储 | **MySQL**（`tr_knowledge_chunk.embedding`）+ Java 内存余弦检索 | 语料量级为「千级分块」，无需向量库；避免新增中间件与部署负担 |
| Embedding | 可插拔接口 `EmbeddingClient`，默认**智谱 embedding-3 / 通义 text-embedding-v3** | 中文效果好、国内可直连；离线场景可切换本地 BGE 模型 |
| LLM | 可插拔接口 `LlmClient`，默认**DeepSeek / 智谱 GLM-4-Flash**，可切换 **Ollama 本地模型** | 成本极低；本地模型作为断网演示兜底 |
| 文档解析 | Apache **PDFBox**（新增依赖）+ 纯文本直读 | 与既有 MinIO 存储链路对接，实现简单 |
| 文件存储 | 复用既有 **MinIO** | 上传文档直接落既有对象存储 |
| 会话存储 | MySQL（`tr_chat_session` / `tr_chat_message`） | 复用现有持久层，便于「我的历史问答」 |

> **关键约束**：API Key / 模型地址必须通过**环境变量**注入（`AI_EMBEDDING_API_KEY`、`AI_LLM_API_KEY`、`AI_LLM_BASE_URL`），与项目既有敏感配置规范一致，**禁止写死在 `application.yml`**。

### 2.2 数据流

```
上传文档 → MinIO 存储 → 解析抽文本(PDFBox) → 分块(定长+overlap)
        → 向量化(EmbeddingClient) → 写入 tr_knowledge_chunk

学员提问 → 问题向量化 → 内存余弦 Top-K → 阈值过滤
        → 拼装 Prompt(系统提示+检索片段+问题) → LLM 生成
        → 返回 {answer, sources[]} → 落库 tr_chat_session/message
```

### 2.3 降级策略（必须实现）

| 层级 | 触发条件 | 降级行为 |
| --- | --- | --- |
| L1 | LLM 不可用（超时/报错/未配置） | 仅返回**检索片段列表**，不生成答案，提示「以下为知识库相关内容」 |
| L2 | Embedding 不可用 | 退化为**关键词匹配**（`content LIKE` / 分词命中），返回候选片段 |
| L3 | 知识库为空 | 明确提示「知识库暂无可用资料」 |

---

## 3. 功能一：管理端「知识库管理」

### 3.1 功能点

| 功能 | 说明 |
| --- | --- |
| 文档上传 | 支持 `pdf / txt / md`，单文件 ≤ 20MB；上传即触发解析流水线 |
| 解析状态 | `0-解析中` `1-已就绪` `2-解析失败`（含失败原因） |
| 分块预览 | 查看某文档的分块列表（块序号 + 文本 + 字数），用于验证解析质量 |
| 重建索引 | 重新解析并向量化（用于切换模型 / 修复失败文档） |
| 删除文档 | 逻辑删除文档，并**级联删除**其分块 |
| 检索测试 | 管理员输入问题，返回 Top-K 分块与相似度，用于**调试召回效果**（不调用 LLM） |

### 3.2 解析与分块规则

- 按**段落**优先切分，段落超长时按定长切；
- 单块目标长度 **500 字符**，相邻块重叠 **50 字符**（避免语义断裂）；
- 分块时保留 `chunk_index`，检索结果可据此回溯源文档位置；
- 解析结果为空（如扫描件）→ 状态置 `2`，`fail_reason` 记「未提取到文本，可能为扫描件」。

---

## 4. 功能二：学员端「智能问答」

### 4.1 功能点

| 功能 | 说明 |
| --- | --- |
| 提问 | 输入问题（≤500 字符）→ 返回答案 + 引用出处列表 |
| 引用出处 | 每条含 `docTitle`、`chunkContent`、`score`，前端可折叠展开 |
| 历史会话 | 会话列表（左）+ 消息流（右）；支持新建会话、删除会话 |
| 免责提示 | 答案固定展示「本回答由 AI 基于内部培训资料生成，仅供参考，正式执行请以现行文件为准」 |

### 4.2 检索与生成链路（核心）

**步骤一：召回**

1. 将问题向量化得到 `q`（维度 `d`）；
2. 遍历**已就绪**文档的全部分块，计算余弦相似度：
   `cos(q, c) = (q · c) / (|q| · |c|)`；
3. 取 `Top-K`（默认 `K = 5`）；
4. **阈值过滤**：丢弃 `score < minScore`（默认 `0.35`）的分块；
5. 过滤后为空 → 直接返回「知识库中未找到相关内容」，**不调用 LLM**（防幻觉）。

**步骤二：生成**

系统提示词（System Prompt）约束：

```
你是 GMP 培训系统的知识助手。仅依据下列【参考资料】回答用户问题：
1. 不得编造资料中不存在的内容；
2. 若资料不足以回答，明确说明"资料中未涉及"；
3. 引用时用 [1][2] 标注对应资料序号。
【参考资料】
[1] {chunkContent} （来源：{docTitle}）
...
【用户问题】{question}
```

**步骤三：落库**

- 新建会话（首次提问）或复用当前会话；
- 分别写入 `tr_chat_message`：`role=1`（用户问题）、`role=2`（助手答案，`sources` 存引用 JSON）。

### 4.3 会话数据隔离

由 **Service 层**通过 `StpUtil.getLoginIdAsLong()` 强制过滤，不依赖前端传参：

- 会话列表：`.eq(TrChatSession::getUserId, currentUserId)`；
- 会话消息：先校验会话归属，非本人抛 `ErrorCode.FORBIDDEN`。

---

## 5. 数据模型

新增增量脚本：`sql/gmp_training_system_v6_rag.sql`（幂等，可重复执行）。

### 5.1 `tr_knowledge_doc`（知识库文档）

| 字段 | 类型 | 允许空 | 说明 |
| --- | --- | --- | --- |
| `id` | bigint | 否 | 主键 |
| `title` | varchar(200) | 否 | 文档标题 |
| `source_type` | tinyint | 否 | 来源：`1-上传文档` `2-课程PDF` `3-题库` |
| `file_url` | varchar(500) | 是 | MinIO 对象地址 |
| `file_name` | varchar(255) | 是 | 原始文件名 |
| `file_size` | bigint | 是 | 文件字节数 |
| `chunk_count` | int | 否 | 分块总数（默认 0） |
| `status` | tinyint | 否 | `0-解析中` `1-已就绪` `2-解析失败` |
| `fail_reason` | varchar(500) | 是 | 失败原因 |
| `embed_model` | varchar(100) | 是 | 向量化所用模型标识（用于判断是否需重建索引） |
| `create_by` | varchar(50) | 是 | 创建人（MyBatis-Plus 自动填充） |
| `create_time` / `update_time` | datetime | 是 | 自动填充 |
| `deleted` | tinyint | 否 | 逻辑删除（`@TableLogic`） |

### 5.2 `tr_knowledge_chunk`（知识分块）

| 字段 | 类型 | 允许空 | 说明 |
| --- | --- | --- | --- |
| `id` | bigint | 否 | 主键 |
| `doc_id` | bigint | 否 | 所属文档ID |
| `chunk_index` | int | 否 | 块序号（从 0 开始） |
| `content` | text | 否 | 分块文本 |
| `embedding` | blob | 是 | 向量（**float32 小端字节序列**，长度 = `dim × 4`） |
| `dim` | int | 是 | 向量维度 |
| `char_count` | int | 是 | 字符数 |

> 索引：`idx_chunk_doc (doc_id)`。
> 存储约定：`embedding` 采用 `float[]` → `ByteBuffer`（LITTLE_ENDIAN）序列化。**读取与写入必须使用同一字节序**，且 `dim` 需与解析结果一致，否则拒绝写入。

### 5.3 `tr_chat_session`（问答会话）

| 字段 | 类型 | 允许空 | 说明 |
| --- | --- | --- | --- |
| `id` | bigint | 否 | 主键 |
| `user_id` | bigint | 否 | 归属学员（数据隔离依据） |
| `title` | varchar(100) | 是 | 会话标题（取首个问题前 30 字） |
| `message_count` | int | 否 | 消息数 |
| `create_time` / `update_time` | datetime | 是 | 自动填充 |
| `deleted` | tinyint | 否 | 逻辑删除 |

> 索引：`idx_chat_session_user (user_id)`。

### 5.4 `tr_chat_message`（问答消息）

| 字段 | 类型 | 允许空 | 说明 |
| --- | --- | --- | --- |
| `id` | bigint | 否 | 主键 |
| `session_id` | bigint | 否 | 所属会话 |
| `role` | tinyint | 否 | `1-用户` `2-助手` |
| `content` | text | 否 | 消息正文 |
| `sources` | text | 是 | 引用出处 JSON（仅 `role=2` 有值） |
| `create_time` | datetime | 是 | 自动填充 |

> 索引：`idx_chat_msg_session (session_id)`。

---

## 6. 接口契约

### 6.1 管理端知识库（`/system/knowledge/**`）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/system/knowledge/docs` | 上传文档（`multipart/form-data`，字段 `file`、`title`、`sourceType`） |
| GET | `/system/knowledge/docs` | 分页查询文档（筛选：关键字 / 状态 / 来源） |
| GET | `/system/knowledge/docs/{id}/chunks` | 查询文档分块列表（分页） |
| DELETE | `/system/knowledge/docs/{id}` | 删除文档（级联删分块） |
| POST | `/system/knowledge/docs/{id}/reindex` | 重建索引（重新解析 + 向量化） |
| POST | `/system/knowledge/search` | 检索测试（`{query, topK}` → 返回分块 + 相似度，**不调 LLM**） |

**上传校验（服务端强制）**

- 后缀白名单：`.pdf / .txt / .md`；
- **Magic Bytes 校验**：PDF 必须为 `%PDF-` 开头，其余按文本 UTF-8 校验（与既有文件上传规范一致）；
- 单文件 ≤ 20MB，超限返回 `400`。

### 6.2 学员端问答（`/study/knowledge`、`/study/chat`）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/study/knowledge/ask` | 提问（`{question, sessionId?}`）→ `{sessionId, answer, sources[]}` |
| GET | `/study/chat/sessions` | 我的会话列表（分页，按更新时间倒序） |
| GET | `/study/chat/sessions/{id}/messages` | 会话消息列表（非本人 `403`） |
| DELETE | `/study/chat/sessions/{id}` | 删除会话（非本人 `403`） |

**POST `/study/knowledge/ask` 请求体**

```json
{ "question": "洁净区更衣有哪些GMP要求？", "sessionId": 12 }
```

**返回结构**

```json
{
  "sessionId": 12,
  "answer": "根据《无菌区操作规范》，洁净区更衣要求包括：……[1][2]",
  "sources": [
    { "docTitle": "无菌操作规范.pdf", "chunkIndex": 4, "chunkContent": "……", "score": 0.83 },
    { "docTitle": "GMP培训课件.md", "chunkIndex": 17, "chunkContent": "……", "score": 0.71 }
  ],
  "degraded": false
}
```

`degraded` 语义：`true` 表示走了降级链路（未调用 LLM，仅返回检索片段）。

---

## 7. 权限规则变更（`SaTokenConfig`）

| 新增规则 | 允许角色 | 备注 |
| --- | --- | --- |
| `/system/knowledge`、`/system/knowledge/**` | `SUPER_ADMIN` `QA_MANAGER` `TRAINING_ADMIN` | 在 `/system/**` 通用规则**之后**追加，排除 `DEPT_LEADER` |
| `/study/knowledge/**`、`/study/chat/**` | 登录即可 | 已有 `/study/**` 兜底，无需新增规则 |

> 与既有 `/system/notices` 的处理方式保持一致：Sa-Token 的多条 `checkRoleOr` 为**累加校验**，后追加的规则用于收紧权限。

---

## 8. 配置项（`application.yml`）

```yaml
gmp:
  ai:
    # 是否启用 AI 问答（关闭时全链路走关键词降级）
    enabled: true
    embedding:
      # 向量化服务地址与模型名（Key 由环境变量 AI_EMBEDDING_API_KEY 注入）
      base-url: https://open.bigmodel.cn/api/paas/v4/embeddings
      model: embedding-3
      dim: 1024
    llm:
      # 大模型服务地址与模型名（Key 由环境变量 AI_LLM_API_KEY 注入）
      base-url: https://api.deepseek.com/v1
      model: deepseek-chat
      timeout: 30000
    rag:
      # 单块字符数 / 重叠字符数 / 召回条数 / 相似度阈值
      chunk-size: 500
      chunk-overlap: 50
      top-k: 5
      min-score: 0.35
```

---

## 9. 测试要点

| 编号 | 场景 | 预期 |
| --- | --- | --- |
| T1 | 上传文本 PDF | `chunk_count > 0`、`status = 1`、分块可预览 |
| T2 | 上传扫描件 PDF | `status = 2`，`fail_reason` 含「未提取到文本」 |
| T3 | 上传非白名单后缀 / 伪造 PDF | `400`，Magic Bytes 校验拦截 |
| T4 | 删除文档 | 文档逻辑删除，其分块被级联删除 |
| T5 | 管理员检索测试 | 返回 Top-K 分块与相似度，**不调用 LLM** |
| T6 | 学员提问（有命中） | 返回 `answer` + ≥1 条 `sources`，`degraded=false` |
| T7 | 学员提问（无命中） | 返回「知识库中未找到相关内容」，**不调用 LLM** |
| T8 | LLM 不可用 | `degraded=true`，返回检索片段列表而非报错 |
| T9 | Embedding 不可用 | 退化为关键词检索，返回候选片段 |
| T10 | 学员A 查学员B 会话 | `403` |
| T11 | 部门负责人访问 `/system/knowledge` | `403` |
| T12 | 会话列表 | 仅本人数据；删除会话后不再出现 |
| T13 | 维度不一致的向量写入 | `400`，拒绝入库 |
| T14 | 问题超长（>500 字符） | `400` |

---

## 10. 开发顺序

1. 增量 SQL 脚本 `gmp_training_system_v6_rag.sql`（四张表 + 索引）并执行；
2. 后端：AI 抽象层（`EmbeddingClient` / `LlmClient` 接口 + 默认实现 + 配置开关 + 环境变量注入）；
3. 后端：文档解析与分块（PDFBox）+ 向量化入库流水线；
4. 后端：检索服务（内存余弦 + 阈值 + Top-K + 关键词降级）；
5. 后端：问答链路（Prompts + 生成 + 会话落库 + 降级）；
6. 后端：管理端知识库 6 接口 + 学员端问答 4 接口 + `SaTokenConfig` 规则；
7. 前端 PC：知识库管理页（上传 / 列表 / 分块预览 / 检索测试）；
8. 前端学员端：智能问答页（含 API、路由、侧栏菜单）；
9. 小程序端：问答页（可选，复用 `/study/knowledge/ask`）；
10. 编译构建（后端 / PC / 学员端）+ 接口实测（对照第 9 章用例）。

---

## 11. 风险与注意事项

1. **幻觉风险（最高优先级）**：GMP 属合规领域，答错后果严重。必须做到「**无依据则拒答**」+「**强制引用出处**」+「仅供参考提示」三件套；阈值 `minScore` 不宜过低。
2. **数据合规**：GMP 内部资料可能涉密。**优先本地化部署**（本地 Embedding + Ollama）；若调用云端 API，须评估数据出境与脱敏要求。
3. **网络依赖与演示可靠性**：外部 API 断网会导致演示失败。必须实现第 2.3 节降级策略，并提供 `gmp.ai.enabled=false` 一键关闭 AI、走关键词降级的开关。
4. **成本控制**：Embedding 与 LLM 均按量计费。建议在 `sys_user` 维度做**每日提问次数限制**（可通过 Redis 计数实现，后续增强）。
5. **向量序列化一致性**：`embedding` 的字节序（LITTLE_ENDIAN）与 `dim` 必须读写成对；更换 Embedding 模型后，**旧分块需重建索引**（依据 `embed_model` 判断）。
6. **检索性能**：全量内存余弦随分块数量线性变慢。分块 ≤ 5 万时性能可接受；超过时需改为**分批检索 + 缓存**，或引入专用向量库（`tr_knowledge_chunk` 结构可平滑迁移）。
7. **不引入重型中间件**：本方案刻意不引入 Milvus / ES，避免增加部署与演示负担——这是该方案的**设计取舍**，答辩时需说明理由。
8. **新增依赖**：`pom.xml` 需新增 `org.apache.pdfbox:pdfbox`（解析 PDF）与 JSON 处理（可复用 Hutool）。
9. **敏感配置**：`AI_EMBEDDING_API_KEY` / `AI_LLM_API_KEY` 一律环境变量注入；`prod` 环境可配置为 `enabled=false` 以彻底关闭外呼。
10. **与主流程解耦**：知识库问答为**独立模块**，不得影响培训 / 考试 / 审核等既有链路；AI 全链路异常必须被捕获并降级，不得向上抛出导致主流程 500。

---

## 12. 附：答辩创新点提炼

| 创新点 | 说明 |
| --- | --- |
| 领域知识增强 | 不直接调 API，而是「**检索内部 GMP 资料 + 约束生成**」，答案有据可依 |
| 来源可追溯 | 每条答案附引用片段与文档出处，符合合规场景「可审计」要求 |
| 工程取舍 | 以 MySQL 存储向量、零新增中间件完成 RAG，体现成本与复杂度权衡 |
| 可靠降级 | 三级降级链路保证断网 / 未配置模型时系统仍可用，体现工程严谨性 |
| 数据合规 | 优先本地模型部署，规避敏感资料外流 |
