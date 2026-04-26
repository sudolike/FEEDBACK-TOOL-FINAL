# Feedback Tool · 课程问卷与评价管理系统

> **Spring Boot 后端 + 原生 Android App（Kotlin + Jetpack Compose + Material 3）**。
> 同一登录入口按角色自动跳转到不同界面：**学生端 / 教师端 / 管理员端**。
> 内置：问卷、课程评价、资料上传、作业、问答区、教师评分、AI 助手（含 RAG）、课程日历同步、**课程审批流**等模块。

---

## 0. 三端职责矩阵

| 场景 | 学生端 | 教师端 | 管理员端 |
|---|---|---|---|
| 注册入口 | ✅ 公开（默认 student） | ✅ 公开（teacher） | ❌ **不开放注册** |
| 登录入口 | ✅ | ✅ | ✅（角色切换器选「管理员」） |
| 课程查看 | 仅可见**已审批通过**的课程 | 自己提交的全部课程（含 pending/rejected） | 全平台课程，按状态筛选 |
| 课程新建 | — | ✅「申请新课程」→ 进入待审批 | ✅ 直接创建（绕过审批） |
| 课程审批 | — | — | ✅ 通过 / 驳回（带理由） |
| 删除课程 | — | 仅删自己未通过的草稿 | ✅ 删除任意课程 |
| 用户管理 | — | — | ✅ 启停 / 重置密码 / 删除 |
| 仪表盘统计 | — | 仅自己课程的数据 | ✅ 全局：用户/课程/反馈/问卷 |
| AI 助手 | ✅（评价总结/选课推荐/难度） | ✅（教学改进/起草问卷） | ✅（接入相同接口） |
| 数据隐私 | 答卷匿名 | 收到的反馈/答卷匿名 | 仅查看聚合统计 |

### 内置管理员账号（仅供项目组使用，**首次登录后请立即修改密码**）
| 用户名 | 默认密码 | 说明 |
|---|---|---|
| `admin01` | `Admin@Cen2026!Feedback` | 管理员账号 A |
| `admin02` | `Cen#Admin2026!Master`   | 管理员账号 B |

> **安全策略**：注册接口与 Android 注册页都已禁止 `role=admin`；
> 任何 `admin/admin` 弱口令账号会在每次启动时被 `DatabaseInitializer` 自动清除；
> 拦截器 `JwtInterceptor` 只允许 `admin` 角色访问 `/admin/**` 与 `/courses/admin/**` 路径。

---

## 1. 总体架构

```
┌────────────────────────────────────────────────────────────┐
│      Android App (Kotlin + Jetpack Compose + Material 3)  │
│                                                            │
│   LoginScreen ─► role=student ─► StudentMainScaffold       │
│                ├► role=teacher ─► TeacherMainScaffold      │
│                └► role=admin   ─► AdminMainScaffold        │
│                                                            │
│   学生端：首页 / 课程 / 问卷 / 日历 / 我的 + AI 浮窗(FAB) │
│   教师端：看板 / 课程(申请/重提) / 问卷 / 分析 / 我的     │
│   管理员：看板 / 课程审批 / 全部课程 / 用户管理 / 我的    │
└────────────────────────────────────────────────────────────┘
                          │ HTTPS (Retrofit + OkHttp)
                          ▼
┌────────────────────────────────────────────────────────────┐
│                  Spring Boot 2.7 后端 (9091)              │
│  Controllers ── Services ── Mappers ── MyBatis-Plus        │
│      │           │              │                          │
│      │       AI / RAG       JWT 拦截                       │
│      ▼           ▼              ▼                          │
│   MySQL 8 (FULLTEXT ngram)   Redis 7 (登录态/缓存)         │
│   /data/files (静态资源)     通义千问 API（外网）          │
└────────────────────────────────────────────────────────────┘
```

### 关键模块

| 模块 | 后端 Controller | 关键能力 |
|---|---|---|
| 用户/登录/注册 | `LoginController` `UserController` `WebuserController` | JWT 登录，按 role 区分；**禁止 role=admin 注册**；启动时自动注入两个内置管理员 |
| 课程 | `CoursesController` `CourseStudentsController` | 学生只看 approved；教师走 `/courses/teacher/submit` 申请，**必经管理员审批** |
| **管理员** | `AdminController` `CoursesController#admin/*` | 全局看板 / 用户分页/启停/重置密码/删除；课程审批通过/驳回（带理由） |
| 问卷 | `QuestionnairesController` `CourseQuestionnaireController` `QuestionnaireResponsesController` | 问卷发布/回收，**学生答案匿名化** (`/FillinAnonymous`) |
| 课程反馈 | `CourseFeedbackController` | 5 星评分 + 文本 |
| 课程资料 | `CourseResourceController` | PPT/PDF/视频/代码上传，按分类筛选 |
| 教师评分 | `TeacherRatingController` | 学生匿名评分老师 + 统计分布 |
| 作业 | `AssignmentController` | 发布/提交/批改 |
| 问答区 | `QaController` | 帖子 + 回复 + 采纳 |
| AI 助手 | `AssistantController` | 通义千问 + RAG (基于课程数据) |
| 日历 | `CalendarController` | iCalendar `.ics` 导出 |
| 数据分析 | `AnalyticsController` | 评分分布 / 提交率 |

### 隐私合规

- 学生答案默认走 **匿名接口**：`GET /questionnaireResponses/FillinAnonymous`，前端只能看到 `anonymousId` 而非姓名/邮箱。
- 教师评分使用 `AnonymizeUtils.anonymize(studentId, scopeId)` 做稳定哈希。
- 老接口 `/FillinDetails` 仅限管理员排查使用。

### RAG 实现

- 使用 **MySQL 8 FULLTEXT + ngram parser**，无需向量数据库即可上线。
- `KnowledgeBaseService.rebuildAll()` 会把课程信息、课程反馈、教师评分、资料元数据、问答帖子写入 `sys_kb_chunk`。
- 检索：FULLTEXT 命中失败时退化为多关键词 LIKE；前端展示 RAG 引用 chip。
- AI 模型：通义千问 `qwen-plus`（可通过 `AI_MODEL` 环境变量切换）。

---

## 2. 一键部署（推荐）

### 2.1 准备
1. 安装 [Docker Desktop](https://www.docker.com/products/docker-desktop)。
2. 进入 `deploy/` 目录，复制环境变量样例：

   ```bash
   cd deploy
   cp .env.example .env
   # 按需修改 .env：DB 密码 / AI Key / 端口映射
   ```

### 2.2 启动
- **Linux / macOS**：
  ```bash
  bash start.sh
  ```
- **Windows**（双击或 cmd）：
  ```bat
  start.bat
  ```

启动后默认会拉起：
- `feedback-mysql`：MySQL 8（自动加载 `deploy/db/01-schema.sql` 和 `02-seed.sql`）
- `feedback-redis`：Redis 7
- `feedback-backend`：Spring Boot 后端（健康检查 `/actuator/health`）

启动完成后访问：`http://localhost:9091/actuator/health` 应返回 `{"status":"UP"}`。

### 2.3 停止
```bash
bash stop.sh        # 或 stop.bat
```

### 2.4 端口映射
| 容器服务 | 容器内部 | 默认主机端口 | 备注 |
|---|---|---|---|
| MySQL | 3306 | 3307 | 避免与本机 MySQL 冲突 |
| Redis | 6379 | 6380 | 避免与本机 Redis 冲突 |
| Backend | 9091 | 9091 | Android App 直连 |

---

## 3. 本地开发模式（不使用 Docker）

### 3.1 后端
```bash
cd backend
mvn -DskipTests spring-boot:run
```
依赖：JDK 8+、Maven 3.6+、本机 MySQL 8 / Redis 7。  
可通过环境变量覆盖：`DB_HOST DB_USER DB_PASSWORD REDIS_HOST AI_API_KEY` 等。

### 3.2 数据库初始化
执行 `deploy/db/01-schema.sql` + `deploy/db/02-seed.sql`，或参考根目录的旧 `feedback.sql`。

---

## 4. Android App 说明（Kotlin + Jetpack Compose）

### 4.1 技术栈
| 维度 | 选型 |
|---|---|
| 语言 | Kotlin 1.9 |
| UI | Jetpack Compose + Material 3（Navigation Compose、动画、ModalBottomSheet） |
| DI | Hilt 2.50 |
| 网络 | Retrofit2 + OkHttp4 + Moshi（kotlin-codegen） |
| 异步 | Coroutines + Flow + StateFlow |
| 本地持久化 | DataStore Preferences（Token / 角色 / 昵称） |
| 图片 | Coil-Compose |
| 启动 | androidx.core:core-splashscreen |

### 4.2 工程结构
```
Android/
├── settings.gradle.kts        阿里云镜像 + Hilt / Compose / KSP 插件
├── build.gradle.kts            顶层构建脚本
└── app/
    ├── build.gradle.kts        Compose BOM 2024.02 + Material3 + Retrofit + Hilt
    ├── proguard-rules.pro
    └── src/main/
        ├── AndroidManifest.xml
        ├── res/
        │   ├── values/strings.xml / themes.xml
        │   ├── xml/network_security_config.xml   允许调试明文 HTTP
        │   ├── drawable/ic_logo.xml
        │   └── mipmap-anydpi-v26/ic_launcher*.xml
        └── java/com/cen/feedback/
            ├── FeedbackApp.kt                Hilt @HiltAndroidApp
            ├── data/
            │   ├── local/TokenStore.kt        DataStore 包装
            │   ├── api/ApiService.kt          Retrofit 接口（覆盖所有后端端点）
            │   ├── model/Models.kt            全部 DTO/Entity 数据类
            │   └── repo/Repository.kt         统一 unwrap + 文件上传辅助
            ├── di/NetworkModule.kt            Hilt @Module（Moshi/OkHttp/Retrofit）
            └── ui/
                ├── MainActivity.kt
                ├── theme/                     Material 3 配色 + 字体
                ├── components/                
                │   ├── Components.kt          GradientBackground/GlassCard/PrimaryButton/
                │   │                          MetricCard/StarRatingBar/EmptyState/...
                │   └── AiAssistantBubble.kt   全局 AI 浮窗 + ModalBottomSheet 对话
                ├── nav/AppRoot.kt + Routes.kt 路由总线
                ├── session/SessionViewModel   全局会话
                ├── auth/                      登录 / 注册
                ├── student/
                │   ├── StudentMainScaffold.kt 5 Tab 导航 + AI FAB
                │   ├── home/                  首页 hero + 指标 + 热门课程/进行中问卷
                │   ├── course/                课程列表 / 详情（资料/作业/问卷/问答/评价）
                │   ├── questionnaire/         问卷列表 + 填写（自动支持单选/多选/文本/打分）
                │   ├── rate/                  评教（多维度星级 + 匿名提示）
                │   ├── qa/                    课程问答 + 帖子详情 + 老师标记
                │   ├── assignment/            作业详情 + 文件上传 + 重交
                │   ├── calendar/              选课日历 + 一键写入系统日历
                │   ├── ai/AiViewModel         RAG 对话 ViewModel
                │   └── profile/               学生个人中心
                └── teacher/
                    ├── TeacherMainScaffold.kt 5 Tab 导航 + 教师专用 AI 快捷指令
                    ├── dashboard/             看板首页（课程 + 问卷模板）
                    ├── course/                教师课程列表 / 详情（资源/作业/问卷/问答 + 状态切换）
                    ├── questionnaire/         模板列表 + 在线编辑器（增删改、多题型）
                    ├── analysis/              数据分析 Hub + 单卷分析（AI 摘要 + 直方图 + 逐题）
                    ├── assignment/            作业编辑器 + 提交批改
                    └── profile/               教师个人中心
```

### 4.3 运行方式
1. 用 Android Studio Hedgehog 及以上打开 `Android/` 目录。
2. 默认 `BASE_URL` 已写入 `app/build.gradle.kts`：
   - 模拟器：`http://10.0.2.2:9091/`
   - 真机：编辑 `defaultConfig.buildConfigField` 改成 `http://<电脑LAN IP>:9091/`
3. 同步 Gradle → 选择 `app` Run。Compose 预览支持 `M3 + Light/Dark`。

### 4.4 默认账号（来自 `deploy/db/02-seed.sql`）

**管理员账号（不开放注册，仅供项目组使用）**
| 角色 | 账号 | 密码 |
|---|---|---|
| 管理员 A | `admin01` | `Admin@Cen2026!Feedback` |
| 管理员 B | `admin02` | `Cen#Admin2026!Master` |

**演示账号（学生 / 教师，可在登录页或注册页使用）**
| 角色 | 账号 | 密码 |
|---|---|---|
| 教师 | `teacher` | `teacher` |
| 学生 | `student` | `student` |

> - 注册接口（`/register`）和 Android 注册页都已禁止 `role=admin`，并拒绝以 `admin` 开头的用户名。
> - `DatabaseInitializer` 启动时会强制清除 `admin/admin` 弱口令账号；若两个管理员账号都不存在则会自动重新插入上表中的两条。
> - **首次登录后请立即修改默认密码**（管理员端「我的」页有提示）。

### 4.5 学生端核心交互
- **登录**：渐变 Hero + 玻璃卡片 + 圆角按钮，错误信息以 `InlineError` 抖入。
- **首页**：欢迎 Hero + `MetricCard` + 进行中问卷 + 我的课程横向滑动。
- **课程详情**：标签页（概览/资料/作业/问卷/问答/评价），资料/作业上传内置文件选择器。
- **问卷填写**：动态渲染单选 / 多选 / 文本 / 评分；必答题校验；提交后变成只读。
- **评教**：多维度（教学水平 / 态度 / 内容）星级评分 + 匿名说明。
- **日历**：列表展示已选课程 + 「同步全部到系统日历」批量写入 CalendarContract。
- **AI 浮窗**：任意页面可呼出 ModalBottomSheet，预置「课程评价 / 选课推荐 / 课程难度」快捷指令，展示 RAG 引用。

### 4.6 教师端核心交互
- **看板**：课程数 / 问卷数 / 平均评分等指标卡片 + 课程横向滑动 + 模板列表。
- **课程列表**：四类切换 全部 / 已通过 / 待审批 / 已驳回；右下角 FAB「申请新课程」一键打开提交表单。
- **申请新课程**：表单含课程名 / 代码 / 介绍 / 学年 / 学期 / 时间 / 地点；管理员驳回后回到此页可看到驳回理由并直接「重新提交审批」。
- **课程详情**：资料 / 作业 / 问卷 / 问答四大管理 Tab，支持发布、关闭、撤回、解绑、删除。
- **问卷编辑器**：在线增删改单题，支持单选 / 多选 / 文本 / 评分四种题型。
- **作业编辑器**：截止日期 / 满分 / 状态切换 + 批改面板（每个学生提交都可单独打分）。
- **数据分析**：综合 AI 摘要 + 评分直方图 + 「逐题分析」（多选项分布、平均分等）。
- **AI 助教**：默认快捷指令包含「总结这次问卷反馈 / 教学改进建议 / 起草问卷题目」。

### 4.7 管理员端核心交互（`AdminMainScaffold` 5 Tab）
- **看板**：用户构成（学生/教师/管理员/已停用）+ 课程审批（待审批/已通过/已驳回）+ 反馈/问卷/今日活跃。
- **审批**：列出所有 `pending` 课程，附带申请教师，可一键「通过」或「驳回（带理由）」。
- **课程**：全平台课程，按状态筛选；可看到驳回理由与申请人；支持删除。
- **用户**：分页 + 用户名搜索 + 角色筛选；点击行可启停账号、重置密码（系统会随机生成强密码并显示）、删除（不允许删自己 / 删另一个管理员）。
- **我的**：显示账号信息、账号安全提示、内置管理员账号备忘、退出登录。

---

## 5. 主要 REST 接口速查

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/login` | 登录，返回 JWT |
| POST | `/register` | 学生注册 |
| GET | `/courses/list?teacherId=` | 教师课程列表 |
| GET | `/courses/student/{studentId}` | 学生课程列表 |
| GET | `/courseQuestionnaire/student/questionnaires` | 学生收到的问卷 |
| GET | `/questionnaireResponses/FillinAnonymous` | **匿名版** 问卷答案 |
| GET | `/resources/course/{courseId}?category=` | 课程资料列表 |
| POST | `/resources/upload` | 上传资料（multipart） |
| POST | `/teacherRating/save` | 学生评教（匿名） |
| GET | `/teacherRating/teacher/{id}/stats` | 教师评分统计 |
| POST | `/assignment/save` | 教师发布作业 |
| POST | `/assignment/submit` | 学生提交作业 |
| POST | `/qa/post` `/qa/reply` | 问答帖子/回复 |
| POST | `/assistant/chat` | AI 对话（含 RAG） |
| POST | `/assistant/recommend` | AI 选课推荐 |
| GET | `/assistant/summarize?courseId=&questionnaireId=` | AI 问卷总结 |
| GET | `/calendar/student/{id}/json` | 学生课程日程 JSON |
| GET | `/calendar/student/{id}/ics` | 课程 iCalendar 文件 |
| GET | `/analytics/course/{id}/dashboard` | 课程数据看板 |
| POST | `/courses/teacher/submit` | **教师**提交课程申请（必走审批） |
| GET | `/courses/teacher/my` | **教师**自己的全部课程（含 pending/rejected） |
| GET | `/courses/admin/pending` | **管理员**查询待审批课程列表 |
| POST | `/courses/admin/approve/{id}` | **管理员**审批通过 |
| POST | `/courses/admin/reject/{id}` | **管理员**审批驳回（body: `{reason}`） |
| GET | `/admin/dashboard` | **管理员**全局看板统计 |
| GET | `/admin/users/page` | **管理员**用户分页 + 关键字 / 角色 / 状态过滤 |
| POST | `/admin/users/{id}/disable` `/enable` `/reset-password` `/delete` | **管理员**对单个用户的操作 |

---

## 6. 常见问题

1. **Android 登录提示连接失败**  
   修改 `Android/app/build.gradle.kts` 内的 `buildConfigField "BASE_URL"`；模拟器使用 `10.0.2.2`，真机请用电脑局域网 IP；后端容器已绑定 `0.0.0.0`。

2. **AI 没有返回引用**  
   先调用 `POST /assistant/kb/rebuild` 让知识库写入 `sys_kb_chunk`；确认 MySQL 已经启用 `ngram` parser（`docker-compose.yml` 已配置）。

3. **iCalendar 文件无法导入手机日历**  
   ICS 文件保存在 App 私有 `Documents` 目录，已通过 FileProvider 共享；如系统未安装日历应用，请改用第三方 Calendar 类 App。

4. **问卷统计要拿真实姓名怎么办？**  
   切换到 `/questionnaireResponses/FillinDetails`（仅管理员）。前端默认调用匿名接口。

---

## 7. 后续可扩展

- AI Streaming：把 `aliTyqw` 改为流式 SSE/WebSocket，前端实现“打字机”动画。
- 多语言：值已经使用 `Material3.DayNight`，可加 `values-en` `values-zh` strings。
- 推送：Firebase / 极光，新问卷/作业到来时推送提醒。
- 课程音视频回放：可与 `CourseResource` 联动，加入 ExoPlayer。

