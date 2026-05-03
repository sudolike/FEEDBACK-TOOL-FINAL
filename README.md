# Feedback Tool — 高校课程反馈与 AI 教学助理平台

> 面向高校的三端（学生 / 教师 / 管理员）课程反馈与 AI 教学助理平台。
> 一个 Android 客户端 + 一个 Spring Boot 后端，支持课程审批、双向选课、多格式
> 课程资料（PDF / PPT / 视频 / 图片）、匿名问卷、AI 教学反馈分析与 RAG 知识检索，
> 全栈 Docker 一键部署。

仓库地址：<https://github.com/sudolike/FEEDBACK-TOOL-FINAL>

---

## 1. 项目概览

### 1.1 三种角色与核心能力

| 角色 | 主要能力 |
|---|---|
| **学生** | 发现 / 申请课程，查看课件 PDF·PPT·视频·图片，匿名填写问卷，对课程评分评价，浮窗 AI 助理（学习问答 / 选课推荐 / 课程难度评估） |
| **教师** | 提交课程待审批，发布资料 / 作业 / 问卷，邀请学生 / 审批申请，查看每份匿名问卷答案与统计图表，AI 教师助手（一键总结问卷反馈 / 改进建议） |
| **管理员** | 课程审批工作流（pending / approved / rejected），用户管理，强制重建 RAG 知识库 |

### 1.2 关键亮点

- **双 AI Provider**：抽象 `AiClient` 接口，可在 **阿里云通义千问 DashScope** 与 **阿里云 PAI EAS（自部署 vLLM）** 之间一键切换，配置仅需改 `AI_PROVIDER` 一个环境变量。
- **生产级 RAG**：基于 MySQL 8 FULLTEXT (ngram) + 256 维本地哈希向量召回，支持课程 / 教师 / 资料 / 问答 / 教师反馈五种语料类型；教师反馈意图下自动屏蔽噪声引用，避免小模型答非所问。
- **匿名问卷 + 数据分析**：题型支持 rating / single / multiple / text；每份答案匿名落库，分析页含评分直方图、单题统计、AI 综合摘要（实测 5 秒返回 1200+ 字结构化分析）。
- **完整文件管线**：从 `MultipartFile` 上传到 MD5 去重、内容嵌入式预览（image/png · application/pdf · video/mp4 都用 `inline` Content-Disposition），Android 端原生 VideoView 全屏播放。
- **双向选课工作流**：`student_apply` / `teacher_invite` 两种来源 + `pending` / `approved` / `rejected` 三态，避免课程被自动加入或被静默驳回。
- **Android UI**：Jetpack Compose + Material 3 全套品牌主题（明 / 暗 / 渐变 hero / 胶囊 Tab / 打字机文本 / Markdown 渲染 / 媒体播放器）。
- **Docker 一键部署**：MySQL 8 + Redis 7 + Spring Boot 后端三容器编排，启动脚本含 `wslrelay.exe` 端口劫持自动清理，启动后会打印所有 Mapping 方便排查。
- **演示数据脚本**：`deploy/seed_demo/` 一条命令生成 5 老师 / 30 学生 / 10 课程 / 34 文件 / 229 问卷答案 / 99 课程评价的完整自洽数据集，并自动重建 RAG 索引。

---

## 2. 系统架构

```
┌────────────────────────────────┐         ┌──────────────────────────────────┐
│  Android App (Kotlin Compose)  │         │   Web Browser (admin tools)      │
│  Hilt · Retrofit · DataStore   │         │  Optional: 直接调 REST           │
└──────────────┬─────────────────┘         └──────────────────────────────────┘
               │  HTTPS / HTTP (JWT)                              REST
               ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│                       Spring Boot 2.7 Backend  (port 9091)                   │
│  Controllers ── Service ── MyBatis-Plus ── MySQL 8                           │
│      │                                                                       │
│      ├── AiClient ─┬─ DashScopeAiClient (阿里云通义千问 SDK)                 │
│      │             └─ PaiEasAiClient   (PAI EAS · vLLM · OpenAI 兼容)        │
│      │                                                                       │
│      ├── KnowledgeBaseService (RAG)  ← MySQL FULLTEXT + 256-d hash embedding │
│      ├── AssistantService            ← 教师反馈意图 query routing            │
│      └── FileController              ← /file/upload + 内联预览              │
└──────────────┬───────────────────────────────────────────┬───────────────────┘
               ▼                                           ▼
        ┌──────────────┐                            ┌──────────────┐
        │  MySQL 8.0   │                            │   Redis 7    │
        │  + ngram FT  │                            │  会话/缓存    │
        └──────────────┘                            └──────────────┘
```

### 2.1 技术栈

**后端**：Java 8 · Spring Boot 2.7 · MyBatis-Plus · MySQL 8 · Redis 7 · Hutool ·
JWT · Fastjson · Maven · Lombok · Alibaba DashScope SDK

**Android**：Kotlin 1.9 · Jetpack Compose (BOM 2024.02) · Material 3 ·
Hilt 2.52 · Retrofit2 · OkHttp4 · Moshi (kotlin-codegen + KSP) · Coroutines/Flow ·
DataStore Preferences · Coil · SplashScreen · Navigation Compose

**部署**：Docker / Docker Compose · MySQL 8.0.36 · Redis 7.2-alpine ·
eclipse-temurin:8-jre-jammy

**AI**：阿里云 DashScope（qwen-plus / qwen-turbo）或阿里云 PAI EAS（自部署 vLLM 模型）

---

## 3. 快速开始

### 3.1 前置条件

| 工具 | 版本 |
|---|---|
| Docker Desktop | 最新稳定版（Windows 需开启 WSL2 后端） |
| Android Studio | Hedgehog 2023.1+，自带 JBR 21（用于 Android 端编译） |
| Python | 3.8+（仅运行演示数据脚本时需要） |
| 端口 | 9091（后端）、3307（MySQL host port）、6380（Redis host port） |

### 3.2 后端 + 数据库（Docker 一键起）

```powershell
cd deploy
copy .env.example .env       # Linux/macOS: cp .env.example .env
# 按需修改 .env 中的 AI_PROVIDER / AI_API_KEY / AI_EAS_* 等

# Windows:
.\start.bat

# Linux / macOS:
./start.sh
```

启动脚本会：

1. 自动清理 `wslrelay.exe` 在 9091 上的残留监听（Docker Desktop / WSL2 老 bug）。
2. 拉镜像、构建后端 jar、启动 mysql / redis / backend 三容器。
3. 阻塞等待 `http://localhost:9091/actuator/health/live` 返回 200。
4. 打印 AI Provider 状态横幅，方便核对。

排查时：

```powershell
# 一键全量重建（用于本地代码刚改完）
.\rebuild.bat

# 查日志
docker compose logs -f backend

# 健康 / AI 自检
curl http://localhost:9091/actuator/health/live
curl http://localhost:9091/ai/status
curl "http://localhost:9091/ai/ping?msg=hi"
```

### 3.3 Android 客户端

```bash
# 命令行
cd Android
./gradlew :app:assembleDebug

# 或：Android Studio → File → Open → 选 Android/ → Sync → Run ▶
```

`Android/app/src/main/java/com/cen/feedback/data/api/NetworkModule.kt` 中
`BASE_URL` 默认指向 `http://10.0.2.2:9091/`（模拟器内访问宿主机）。
真机请改成 `http://<电脑局域网 IP>:9091/`。

> 安卓项目最低 minSdk = 24，编译 SDK 35；不依赖 Google Services，可在国内
> 离线 Gradle 构建。

### 3.4 注入演示数据（可选但强烈推荐用于答辩 / demo）

```powershell
cd deploy\seed_demo
python -m pip install requests pymysql      # 仅首次
python seed.py
```

详见 [`deploy/seed_demo/README.md`](deploy/seed_demo/README.md)，会得到：

- 5 教师 / 30 学生 / 10 HKU 课程
- 34 个真实课程资料（PDF / PPTX / PNG / MP4）已落到 `/data/files`
- 3 套问卷模板，229 份匿名学生答案，99 条课程评价
- RAG 知识库自动重建，AI 助理立刻可用

**演示推荐主线**：

| 角色 | 账号 | 密码 |
|---|---|---|
| 管理员 | `admin01` | `Admin@Cen2026!Feedback` |
| 教师（demo 主线） | `t_li` | `Teacher@123` |
| 学生（demo 主线） | `student` | `student` |

`t_li` 教 **COMP7506 Smart Phone Apps Development**——刻意调高的 demo 主课，
4 种文件全齐、30 学生选课、84 份问卷答案、24 条评价，所有页面都"满满当当"。

完整账号清单见 [`deploy/seed_demo/README.md`](deploy/seed_demo/README.md)。

---

## 4. 主要功能模块

### 4.1 学生端

- **课程发现**：分页 + 搜索全部已审批课程；申请加入需附留言。
- **我的课程**：仅展示 `status=approved` 的关系；进入课程见资料 / 作业 / 问卷 / 问答 4 个 Tab。
- **资料浏览**：图片 / PDF 内嵌预览，PPTX 跳系统应用打开，视频走自带 `MediaPlayerScreen`（VideoView + MediaController 全屏 + 进度条）。
- **匿名问卷**：进度条 + sticky 提交栏，单选行可点 + 评分语义化提示；提交后只可查看。
- **课程评价**：1-5 星 + 文字评价；存 `sys_course_feedback`，对教师匿名展示。
- **AI 浮窗**：主页右下 FAB 弹出助手；支持打字机效果 + Markdown 渲染（粗体 / 列表 / 引用）；学生场景下集成「选课推荐」「课程难度评估」两个固定 prompt。

### 4.2 教师端

- **提交课程审批**：教师创建课程 → `status=pending` → 等管理员批准 / 驳回；驳回后可改后再提交。
- **学生管理**：查看课程已加入学生列表（昵称 + 加入时间），主动邀请未选课学生加入；处理学生申请（同意 / 驳回）。
- **资料发布**：上传 PDF / PPT / 视频 / 图片，自动按 category（lecture / recording / code / other）分类。
- **问卷模板库**：自定义题型组合（rating / single / multiple / text），保存后可一键绑定到任意课程并发布。
- **数据分析**：评分直方图、每题分布、AI 综合摘要（基于真实匿名数据，绝不返回 RAG 噪声）。
- **AI 教师助手**：意图识别命中"问卷反馈 / 学生评价 / 改进建议"等关键词时，自动注入该教师近期课程的真实问卷与课程评价数据；AI 失败 / 返回空时回退到本地数据驱动汇总，不会返回风马牛不相及的内容。

### 4.3 管理员端

- **课程审批面板**：列出 pending 课程；批准 / 驳回；驳回需附原因。
- **用户管理**：查看 / 禁用账号；管理员账号注册入口在前后端都被禁掉，只能用预置的两个 admin 账号。
- **系统维护**：手动触发 `/assistant/kb/rebuild` 重建知识库索引。

---

## 5. AI 与 RAG 设计

### 5.1 双 Provider 切换

```yaml
# backend/src/main/resources/application.yml
ai:
  provider: ${AI_PROVIDER:dashscope}        # dashscope | pai-eas
  model:    ${AI_MODEL:qwen-plus}
  eas:
    endpoint: ${AI_EAS_ENDPOINT:}
    token:    ${AI_EAS_TOKEN:}
    model:    ${AI_EAS_MODEL:Qwen3.5-0.8B}
    mode:     ${AI_EAS_MODE:openai}         # openai | predict
    thinking: ${AI_EAS_THINKING:false}
    maxTokens:${AI_EAS_MAX_TOKENS:1024}
```

`PaiEasAiClient` 内置：

- OpenAI-compatible (`/v1/chat/completions`) 与原生 `predict` 双模式。
- vLLM `enable_thinking=false` 加速参数自动注入。
- **双层 JSON 解析**：先严格 fastjson，失败时回退到正则提取 `content` 字段，
  兼容小模型偶发"unclosed string / 末尾噪声"等异常响应。
- 启动期 banner 日志、每次调用 `latency / reqLen / respLen / replyLen` 全链路埋点。

### 5.2 RAG 检索

- 主索引表：`sys_kb_chunk(source_type, source_id, course_id, title, content, keywords, embedding)`。
- 召回路径：MySQL FULLTEXT (BOOLEAN MODE) + 256-d hash embedding 余弦相似度 + LIKE 兜底，三路结果合并去重。
- 教师反馈意图下自动屏蔽 RAG 引用注入，避免 `teacher_rating` 自动索引的占位文本污染 LLM 输入。

---

## 6. 目录结构

```
Feedback-tool-final/
├── Android/                            # Android Compose 客户端（Kotlin）
│   └── app/src/main/java/com/cen/feedback/
│       ├── data/                       # api · model · repo · token
│       ├── nav/                        # AppRoot · Routes
│       ├── ui/
│       │   ├── auth/                   # 登录 · 注册
│       │   ├── student/                # 学生端各页面
│       │   ├── teacher/                # 教师端各页面
│       │   ├── admin/                  # 管理员端各页面
│       │   ├── common/                 # MediaPlayerScreen 等通用页
│       │   ├── components/             # 设计组件 · AiAssistantBubble · MarkdownText
│       │   └── theme/                  # AppSpacing · BrandGradients · Theme
│       └── res/                        # 多语言 strings · 颜色 · 主题
│
├── backend/                            # Spring Boot 服务端（Java 8）
│   ├── src/main/java/com/cen/
│   │   ├── ai/                         # AiClient 抽象 + DashScope/PAI-EAS 两个实现
│   │   ├── config/                     # 拦截器 / CORS / WebSocket / DatabaseInitializer
│   │   ├── controller/                 # 各功能 REST 入口
│   │   ├── entity/                     # 实体类（课程 / 用户 / 问卷 / 资料 / KbChunk …）
│   │   ├── mapper/                     # MyBatis Mapper 接口
│   │   ├── service/                    # IService 接口
│   │   └── service/impl/               # AssistantServiceImpl · KnowledgeBaseServiceImpl …
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   ├── mapper/                     # MyBatis XML
│   │   └── templates/                  # 代码生成模板
│   ├── Dockerfile                      # 多阶段构建 + BuildKit Maven 缓存
│   ├── maven-settings.xml              # 阿里云镜像，加速容器构建
│   └── pom.xml
│
├── deploy/                             # 一键部署
│   ├── docker-compose.yml              # mysql + redis + backend
│   ├── .env.example                    # 部署环境变量模板（拷贝为 .env 后修改）
│   ├── start.bat / start.sh            # 启动脚本（含 wslrelay 端口清理）
│   ├── stop.bat  / stop.sh
│   ├── rebuild.bat                     # 强制重建后端镜像 + 健康自检 + AI 自检
│   ├── db/                             # 容器初始化 SQL（卷挂载到 /docker-entrypoint-initdb.d）
│   └── seed_demo/                      # 演示数据生成器
│       ├── catalog.py                  # 教师/学生/课程/问卷的声明式数据
│       ├── answers.py                  # 学生答案 + 课程评价随机生成
│       ├── seed.py                     # 主入口：清理 + 全量生成
│       └── README.md
│
├── feedback.sql                        # 早期单文件 schema 备份（容器初始化由 db/ 接管）
├── README.md                           # 你正在看的这份
└── .gitignore
```

---

## 7. 关键 API（节选）

> 全部走 `Authorization: <jwt-token>`，登录响应里直接有 `data.token`。
> `/login` `/register` `/file/**` `/ai/status` `/ai/ping` `/actuator/**` 不需要鉴权。

| 模块 | 方法 + 路径 | 说明 |
|---|---|---|
| 鉴权 | `POST /login` `POST /register` | 登录 / 注册（admin 不允许注册） |
| 课程 | `GET /courses/teacher/my` `GET /courses/student/{id}` `GET /courses/discover` `POST /courses/teacher/submit` `POST /courses/admin/approve/{id}` `POST /courses/admin/reject/{id}` | 教师 / 学生 / 管理员视角课程接口 |
| 选课 | `POST /enrollments/apply` `POST /enrollments/invite` `POST /enrollments/review/{id}` | 双向选课工作流 |
| 资料 | `POST /file/upload` `GET /file/{uuid}` `POST /courseResource/save` `GET /courseResource/course/{cid}` | 文件上传（带 MD5 去重） + 课程资料关联 |
| 问卷 | `POST /questionnaires/save` `GET /questionnaires/page` `POST /courseQuestionnaire/bind` `POST /response/save` `GET /questionnaireResponses/{cid}/analysis` | 模板库 + 课程绑定 + 答案 + 数据分析 |
| 评价 | `POST /courseFeedback/save` `GET /courseFeedback/course/{cid}` | 课程文字评价 + 评分 |
| AI | `POST /assistant/chat` `POST /assistant/recommend` `GET /assistant/summarize` `POST /assistant/kb/rebuild` `GET /ai/status` `GET /ai/ping?msg=` | 通用对话 / 选课推荐 / 问卷摘要 / 重建 RAG / 自检 |

---

## 8. 配置项速查

`deploy/.env`（参考 `.env.example`）：

| 变量 | 默认值 | 说明 |
|---|---|---|
| `BACKEND_PORT_HOST` | `9091` | 暴露给宿主机的后端端口 |
| `DB_PORT_HOST` | `3307` | MySQL 暴露给宿主机的端口 |
| `DB_PASSWORD` | `feedback123` | MySQL root 密码（容器内） |
| `AI_PROVIDER` | `dashscope` | `dashscope` 或 `pai-eas` |
| `AI_API_KEY` | （空） | DashScope API Key（`AI_PROVIDER=dashscope` 时必填） |
| `AI_MODEL` | `qwen-plus` | DashScope 调用的模型 |
| `AI_EAS_ENDPOINT` | （空） | PAI EAS 公网 endpoint |
| `AI_EAS_TOKEN` | （空） | PAI EAS Token |
| `AI_EAS_MODEL` | `Qwen3.5-0.8B` | PAI EAS 部署的模型名 |
| `AI_EAS_MODE` | `openai` | `openai` 或 `predict` |
| `AI_EAS_THINKING` | `false` | Qwen3 thinking 模式 |
| `AI_EAS_MAX_TOKENS` | `1024` | 单次响应最大 token |
| `MAIL_USERNAME` / `MAIL_PASSWORD` | （空） | 邮件通知；空则禁用 |

---

## 9. 团队功能分工（参考）

> 详见 [`deploy/seed_demo/README.md`](deploy/seed_demo/README.md) 与提交记录。
> 项目按四人均分实现：

| 成员 | 主要负责 |
|---|---|
| 成员 A | Android 学生端（首页 / 课程 / 资料 / 媒体播放器） |
| 成员 B | Android 教师端（课程管理 / 问卷编辑器 / 数据分析） |
| 成员 C | Android 管理员端 + 鉴权 + UI 设计系统 |
| 成员 D | Spring Boot 后端 + AI/RAG + 部署脚本 + Demo 数据 |

---

## 10. 致谢

- HKU STAT8307 课程 demo 数据沿用真实课程编号（COMP7104 / COMP7404 / COMP7409 /
  COMP7506 / DASC7011 / DASC7102 / STAT7008 / STAT8003 / STAT8017 / STAT8307）。
- 后端 AI 集成参考阿里云 DashScope 与 PAI EAS 官方 SDK / API。
- Android 客户端基于 Jetpack Compose Material 3 设计语言。

如需进一步定制（如接入企业 IDP、扩展到 iOS / Web 客户端），可基于现有
`AiClient` / `IAssistantService` 抽象与 REST 接口直接平滑扩展。
