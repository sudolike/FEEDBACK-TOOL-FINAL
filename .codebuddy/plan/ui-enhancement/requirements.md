# 课评云 Feedback Android App UI 优化需求文档

## 引言

本需求文档针对 `FEEDBACK-TOOL-FINAL/Android/` 子工程的 UI 层进行优化规划。当前 App 使用 **Kotlin 1.9 + Jetpack Compose + Material 3 + Hilt + Navigation Compose** 技术栈，已实现学生端、教师端、管理员端三套 `MainScaffold` + 若干业务页面，并抽取了 `components/Components.kt`、`components/AiAssistantBubble.kt` 等通用组件，整体风格以「紫色渐变 + 玻璃卡片 + 圆角」为主。

通过对下列关键文件的审阅：

- 主题：[Theme.kt](Android/app/src/main/java/com/cen/feedback/ui/theme/Theme.kt)、[Color.kt](Android/app/src/main/java/com/cen/feedback/ui/theme/Color.kt)
- 组件：[Components.kt](Android/app/src/main/java/com/cen/feedback/ui/components/Components.kt)、[AiAssistantBubble.kt](Android/app/src/main/java/com/cen/feedback/ui/components/AiAssistantBubble.kt)
- 三端 Scaffold：[StudentMainScaffold.kt](Android/app/src/main/java/com/cen/feedback/ui/student/StudentMainScaffold.kt)、[TeacherMainScaffold.kt](Android/app/src/main/java/com/cen/feedback/ui/teacher/TeacherMainScaffold.kt)、[AdminMainScaffold.kt](Android/app/src/main/java/com/cen/feedback/ui/admin/AdminMainScaffold.kt)
- 典型页面：`LoginScreen` / `StudentHomeScreen` / `StudentCourseListScreen` / `QuestionnaireFillScreen` / `CourseDetailScreen` / `TeacherHomeScreen` / `AdminDashboardScreen`

发现了以下可改进的主要方向：**视觉一致性**、**导航交互**、**关键页面 UX（登录/首页/问卷/课程详情）**、**加载与空态**、**深浅色适配**、**动效与触感**、**无障碍**。

本次优化的硬性约束：

- **必须保持现有业务功能、数据模型、网络层、ViewModel 不变**；只调整/新增 UI 层 Composable、Theme、Resource。
- **优化完成后必须能用 Android Studio Hedgehog 直接打开 `Android/` 目录，Gradle Sync 后一键 Run 到模拟器或真机**，不增加新的三方 AAR 或需要额外后端配合的改动。
- 若新增依赖，必须是已在 `libs.versions.toml` 或 Compose BOM 2024.02 中存在的官方库（如 `accompanist-systemuicontroller`、`material3-pulltorefresh`），不引入实验性破坏源兼容的版本。
- 不改动后端接口、数据库结构、AndroidManifest 权限声明（除非为视觉补丁，如 `windowSoftInputMode`）。

---

## 需求

### 需求 1 —— 统一设计令牌与主题层

**用户故事：** 作为一名使用者，我希望无论在哪个端、哪个页面，品牌色、字号、圆角、间距、阴影都保持一致，以便获得专业可信的视觉体验。

#### 验收标准

1. WHEN 打开 App 任何一个界面 THEN `MaterialTheme.colorScheme` 的 `primary / secondary / tertiary / surface / background` SHALL 与 `theme/Color.kt` 中定义的紫蓝品牌色 token 完全对应，任何页面中 SHALL NOT 再出现直接 `Color(0xFFxxxxxx)` 形式的一次性硬编码颜色。
2. IF 系统处于深色模式 THEN `DarkColors` 下 `StatusChip` / `InlineError` / Chip 类背景的 `alpha` SHALL 被调整为 ≥ 0.2，保证文字对比度 ≥ 4.5:1（WCAG AA）。
3. WHEN 新增「间距 token」（Spacing.xs=4dp / sm=8dp / md=12dp / lg=16dp / xl=24dp）和「圆角 token」（已有 Shapes，可补 `pill=50`）到 `theme/` 目录 THEN 所有页面 SHALL 通过对象调用而非再写 `8.dp / 12.dp` 散落数字。
4. WHEN 引入 `BrandGradients`（主渐变 / 教师端渐变 / 管理员端渐变） THEN 登录页 / 三端 Hero / 顶栏 / AI FAB SHALL 统一从该对象取色，消除当前 `Primary700-Pink500`、`Accent600-Primary600-Primary400`、`Primary800-Primary600-Pink500` 三套并存的差异。

---

### 需求 2 —— 底部导航 & 顶部栏体验升级

**用户故事：** 作为使用 App 的学生 / 教师 / 管理员，我希望底部 Tab 切换有清晰的选中反馈，顶部栏在滚动时有层次变化，以便快速感知当前位置。

#### 验收标准

1. WHEN 用户点击底部 `NavigationBarItem` THEN 被选中项 SHALL 展示图标填充态（`Outlined → Filled`）+ 底部指示条 + 轻微缩放动画（0.92→1.0，spring），并触发一次 `HapticFeedback.TextHandleMove`。
2. IF 当前 Tab 内页面内容被滑动至 `firstVisibleItemScrollOffset > 0` THEN `GradientTopBar` SHALL 降低渐变饱和度并增加 4dp 阴影，形成"悬停"观感。
3. WHEN 进入支持返回的子页（课程详情、问卷填写等） THEN 系统状态栏文字色 SHALL 在渐变头部时为白色，滚动后自动切换为深色，使用 `WindowCompat.setDecorFitsSystemWindows(false)` + `rememberSystemUiController` 实现。
4. WHEN 在 `StudentMainScaffold` / `TeacherMainScaffold` / `AdminMainScaffold` 之间切换 Tab THEN 动画 SHALL 从「纯 fade」升级为「水平滑入 12dp + fade」，并限制 220ms 内完成。
5. IF 学生端 AI 浮窗 FAB 与底部 `NavigationBar` 同时可见 THEN FAB SHALL 使用 `navigationBarsPadding()` 正确避让系统手势区，并在 LazyColumn 末尾留出 ≥ 88dp 的 `contentPadding.bottom` 避免遮挡最后一条内容。

---

### 需求 3 —— 登录 / 注册页视觉与可用性增强

**用户故事：** 作为第一次打开 App 的用户，我希望登录页有品牌感但又不过度花哨，表单操作流畅且有清晰错误提示，以便快速完成登录。

#### 验收标准

1. WHEN 进入 `LoginScreen` THEN 背景 `AnimatedBackdrop` SHALL 将当前"光晕越界越跑越远"的单向位移改为 lissajous 循环轨迹（8 秒周期），并降低透明度至 ≤ 0.15，避免与前景卡片抢焦点。
2. WHEN 用户在用户名 / 密码输入框获得焦点 THEN 对应 `leadingIcon` SHALL 从灰度 tint 过渡为 `Primary600`，输入框边框 SHALL 使用 2dp 主色高亮，形成 focus ring。
3. IF 登录失败且 `state.error` 非空 THEN `InlineError` SHALL 以"抖动(shake) + 透明度进入"的复合动画展示，并在 3 秒后自动淡出；用户再次输入内容时错误即刻清除。
4. WHEN 用户选择"管理员"角色 THEN 页面底部 SHALL 显示一条 `StatusChip`「管理员账号不开放注册」，并且"立即注册"链接自动灰掉不可点。
5. WHEN 打开 `RegisterScreen` THEN 所有表单字段 SHALL 支持 `ImeAction.Next` 串联跳转，最后一个字段触发 `ImeAction.Done` 即提交；且页面顶部 SHALL 复用 `GradientTopBar`（带返回）保持与登录页同一视觉语言。

---

### 需求 4 —— 三端首页 Hero 与卡片一致化

**用户故事：** 作为学生 / 教师 / 管理员，我希望自己端的首页在布局结构上保持家族感，但颜色和问候语能体现身份差异，以便一眼认出所处环境。

#### 验收标准

1. WHEN 抽取共用 `HomeHero(title, subtitle, gradient, stats: List<Pair<String,String>>, tipIcon, tipText, onTipClick?)` 组件到 `components/` 目录 THEN `StudentHomeScreen.HeaderHero` / `TeacherHomeScreen.HeaderHero` / `AdminDashboardScreen.HeaderHero` SHALL 全部迁移为调用该组件，移除重复的 Box+Brush+Column+Avatar 模板。
2. WHEN 显示问候语 THEN 文案 SHALL 基于当前时间动态返回"早安/午安/晚安"而非写死"早安，$nickname"（见 `StudentHomeScreen.HeaderHero`）。
3. WHEN 用户点击 Hero 中的 AI 提示条 THEN SHALL 直接展开 AI 助手面板（学生/教师端）或跳转管理员账号安全提醒，取代当前"仅装饰无点击"的现状。
4. WHEN 首页所有 `MetricCard` 排版 THEN SHALL 改为每行最多 2 张的均分 Row；单卡内 `headlineSmall` → `displaySmall` 让关键数字更突出，数值左侧图标背景圆 SHALL 统一为 `accent.copy(alpha = 0.14f)`。
5. IF 学生端首页的"进行中问卷"横向列表为空 THEN `EmptyHint` SHALL 被替换为统一的 `EmptyState` 组件（带插画图标），不再出现与课程空态不同风格的两种空态。

---

### 需求 5 —— 课程详情 Tab 样式与内容骨架

**用户故事：** 作为学生，我希望课程详情的 Tab 切换有清晰的胶囊选中指示；作为教师，我希望长内容 Tab（资源/作业）在加载时有骨架屏，以便感知进度。

#### 验收标准

1. WHEN 进入 [CourseDetailScreen.kt](Android/app/src/main/java/com/cen/feedback/ui/student/course/CourseDetailScreen.kt) 的 `ScrollableTabRow` THEN 选中 Tab SHALL 渲染为"文字 + 下方药丸底 (`Primary100` 底色 + `Primary700` 文字)"，未选中 Tab 文字色为 `onSurfaceVariant`，替代当前默认 `indicator` 线。
2. WHEN Tab 切换 THEN 底部内容 SHALL 使用 `slideInHorizontally + fadeIn` 和反向 `slideOutHorizontally + fadeOut`，动画区段 220ms。
3. IF `CourseDetailViewModel.state.loading == true` THEN 当前 Tab 的内容区 SHALL 显示 3 条 `ShimmerBlock` 骨架卡，禁止再出现"先空白再突然刷出"的画面。
4. WHEN 课程头部 `CourseHeader` 展示课程名过长（> 20 字符） THEN SHALL 启用 `marqueeRepeatForever()` 跑马灯或 `maxLines=2 + ellipsis`，确保不破坏头部高度。
5. WHEN 教师端 [TeacherCourseDetailScreen.kt](Android/app/src/main/java/com/cen/feedback/ui/teacher/course/TeacherCourseDetailScreen.kt)（41KB）拆分 THEN SHALL 至少将「学生」「资源」「作业」「问卷」「问答」Tab 的 Composable 拆到同包下独立文件（每个 ≤ 300 行），以便后续单独迭代，且拆分 SHALL NOT 改变外部调用入口。

---

### 需求 6 —— 问卷填写页体验优化

**用户故事：** 作为学生，我希望填写问卷时能清楚看到进度、一次只聚焦一题、提交按钮始终可达，以便高效完成答题。

#### 验收标准

1. WHEN 进入 [QuestionnaireFillScreen.kt](Android/app/src/main/java/com/cen/feedback/ui/student/questionnaire/QuestionnaireFillScreen.kt) 且 `readonly == false` THEN 页面顶部 SHALL 显示「已答 X / 共 Y 题」的 `LinearProgressIndicator`，随答题状态实时变化。
2. WHEN 用户点击单选题（`single`）的某一行 THEN 整行 Row SHALL 作为可点击区域（`clickable + selectableGroup`），而不仅 `RadioButton` 图标；选中态 SHALL 显示 `Primary100` 底色 + `Primary600` 描边。
3. WHEN 用户作答评分题（`rating`） THEN 星星下方 SHALL 展示对应分值的文字提示（1=很差 / 2=较差 / 3=一般 / 4=不错 / 5=很好）。
4. WHEN 未作答题数 > 0 THEN 底部 SHALL 常驻一个 `BottomAppBar` 高度 64dp 的 sticky 操作栏（含进度文字 + 提交按钮），替代当前需要滚到列表末尾才能点击 `PrimaryButton` 的交互。
5. IF 用户触发提交但有必答题未填（`required == true` 且 `answers[qId]` 为空） THEN `InlineError` SHALL 明确列出"第 X 题未作答"，并自动 `LazyListState.animateScrollToItem` 滚动到第一个缺失题。

---

### 需求 7 —— 列表加载、刷新与空态统一

**用户故事：** 作为所有用户，我希望列表页都支持下拉刷新、首次加载有骨架屏、空数据有引导，以便遇到异常时知道怎么做。

#### 验收标准

1. WHEN 进入 `StudentCourseListScreen` / `StudentDiscoverCoursesScreen` / `StudentQuestionnaireListScreen` / `TeacherCourseListScreen` / `AdminCourseApprovalScreen` / `AdminCourseListScreen` / `AdminUserListScreen` THEN 最外层 LazyColumn SHALL 包裹 Material3 `PullToRefreshBox`（或 `accompanist-swiperefresh`），下拉触发对应 VM 的 `refresh()`。
2. WHEN `state.loading == true && 列表为空` THEN 该列表 SHALL 使用 `shimmerCards(count = 4)` 而非空白或居中 `CircularProgressIndicator`。
3. WHEN 列表真实数据为空 THEN SHALL 使用统一的 `EmptyState` 组件（icon + title + subtitle + 可选 action 按钮），并为每个场景提供一个"去做什么"的行动号召（如"发现课程" / "刷新" / "等待教师审批"）。
4. WHEN 网络错误发生 THEN SHALL 出现 `ErrorState` 组件（红色图标 + 提示文字 + 重试按钮），替代当前散落的 `InlineError` + `SecondaryButton("重试")` 模式。
5. WHEN 分页列表（如 `StudentDiscoverCoursesScreen`、`AdminUserListScreen`）滚到底部 THEN SHALL 自动触发 `loadMore()`，并在末尾展示一个 24dp 的进度圆圈，替代"手动点击下一页按钮"。

---

### 需求 8 —— AI 浮窗 & 对话面板打磨

**用户故事：** 作为学生 / 教师，我希望 AI 助手面板打开更自然、消息气泡可复制、引用来源清晰可见，以便放心使用。

#### 验收标准

1. WHEN 用户点击 [AiAssistantBubble.kt](Android/app/src/main/java/com/cen/feedback/ui/components/AiAssistantBubble.kt) 中的 `PulsingFab` THEN 面板 SHALL 以"从 FAB 位置放大展开"的 scale + fade 动画弹出（使用 `animateFloatAsState` + `transformOrigin`），而不是当前的纯 `slideInVertically`。
2. WHEN AI 消息中携带 RAG 引用（`sources: List<String>`） THEN 每条 `MsgBubble` 下方 SHALL 展示可横向滑动的来源 chip 列表（示例文件 / 课程名），点击 chip 可展开详情。
3. WHEN 用户长按任意消息气泡 THEN SHALL 出现操作菜单（复制 / 重新生成 / 举报不佳回答），取代当前无上下文交互。
4. WHEN 输入框处于发送中（`sending == true`） THEN `TypingIndicator` SHALL 居左显示在最后一条 AI 消息下方，发送按钮 SHALL 变为可点击的"停止生成"图标（此阶段仅 UI，业务取消逻辑后续迭代）。
5. IF 面板处于最大高度且键盘弹出 THEN 内容区 SHALL 通过 `Modifier.imePadding() + bringIntoViewRequester` 自动把输入行顶到键盘之上，禁止出现输入框被遮挡的问题。

---

### 需求 9 —— 深色模式与无障碍支持

**用户故事：** 作为夜间使用者 / 视障用户，我希望 App 在深色模式下依然清晰可读，关键控件可被 TalkBack 朗读，以便长期使用不感到不适。

#### 验收标准

1. WHEN 切换系统深色模式 THEN 三端所有 Hero 渐变 SHALL 自动降饱和度 20%（通过 `isSystemInDarkTheme()` 条件选 `BrandGradients.darkPrimary`），否则 `Pink500 → Accent500` 在暗色下刺眼。
2. WHEN `Icon(..., null)` 当前在代码中出现 THEN 若 Icon 具有语义（如返回、发送、关闭、评分星），SHALL 全部补上 `contentDescription = stringResource(...)`；纯装饰性 Icon 维持 `null` 但需在注释中标注 "decorative"。
3. WHEN `StarRatingBar` 被 TalkBack 读取 THEN SHALL 使用 `Modifier.semantics { role = Role.Button; stateDescription = "$rating 星，共 5 星" }`，允许用户通过手势改变评分。
4. IF 设备字体缩放（system font scale）≥ 1.3 THEN `MetricCard` / `CourseRow` / `QuestionRow` 的布局 SHALL 通过 `IntrinsicSize.Max` 自适应，文字 SHALL NOT 被裁剪或挤出边界。
5. WHEN 应用启动 THEN `androidx.core:core-splashscreen` 启动画面 SHALL 在白天/黑夜模式下呈现不同背景色（对应 `Slate50` / `Slate900`），避免闪白。

---

### 需求 10 —— 动效、触感与微交互

**用户故事：** 作为用户，我希望关键操作（提交问卷 / 审批通过 / 加入课程成功）有确认动效 + 触觉反馈，以便感知操作已被系统接收。

#### 验收标准

1. WHEN 学生端提交问卷成功返回 THEN SHALL 播放一次轻量成功动画（Lottie 或 Compose `AnimatedVisibility` + 对勾缩放），并触发 `HapticFeedbackConstants.CONFIRM` 触感。
2. WHEN 教师 / 管理员执行「通过 / 驳回」操作 THEN 对应 Row SHALL 在淡出列表前先播放 150ms 的高亮闪烁（绿色 / 红色），替代当前直接消失。
3. WHEN `PrimaryButton` 按下 THEN 已有的 0.97 缩放 SHALL 叠加 `rippleColor = Color.White.copy(alpha=0.24f)`，并且 `loading == true` 时按钮宽度 SHALL 压缩到 `IntrinsicSize.Min` 形成圆形 loading，更显精致。
4. WHEN 列表 items 首次进场 THEN SHALL 使用 `animateItemPlacement()` + 渐入 delay（index * 40ms），移除"一整屏同时出现"的机械感。
5. IF 用户从课程列表点进课程详情 THEN SHALL 使用 Navigation Compose 的 `enterTransition / exitTransition` 配置"右侧滑入 + fade"，保持过渡感一致。

---

### 需求 11 —— AI 对话"打字机"动画（README §7 · AI Streaming 前端部分）

**用户故事：** 作为使用 AI 助手的学生 / 教师，我希望 AI 回答能像真实输入一样逐字显现，以便感受到对话的实时感，即便后端目前仍是一次性返回。

#### 验收标准

1. WHEN `AiViewModel` / `TeacherAssistantViewModel` 收到后端 `POST /assistant/chat` 的完整回复 THEN UI 层 SHALL NOT 直接把整段文本塞入 `MsgBubble`，而是通过 `flow { ... emit(substring) delay(24ms) }` 在 Composable 内分片发射，形成打字机效果。
2. WHEN 打字动画进行中 THEN 该气泡末尾 SHALL 显示一个闪烁的光标占位符（`▍`），动画结束后自动移除。
3. WHEN 用户在打字过程中点击新增的「跳过动画」按钮或再次发送新消息 THEN 当前打字机 SHALL 立即完成剩余文本并结束，不阻塞新消息发送。
4. WHEN 打字机动画过程中用户滚动对话列表 THEN 列表 SHALL NOT 被强制"粘底"，仅当用户已停留在最底部时才自动保持跟随。
5. WHEN 后端未来切换为真正的 SSE/WebSocket 流式接口 THEN 当前实现的 `TypewriterText` Composable SHALL 能零改造复用（以 `Flow<String>` 作为输入），保留向未来平滑升级的能力。

---

### 需求 12 —— 多语言国际化（README §7 · i18n）

**用户故事：** 作为海外交换生或英文用户，我希望把系统语言设置为英文后 App 界面能自动切换为英文，以便无障碍使用课程系统。

#### 验收标准

1. WHEN 遍历当前 `ui/` 目录 THEN 所有硬编码的中文字符串（如 `"首页"`、`"课程"`、`"提交"`、`"发现课程"`、`"驳回理由"` 等）SHALL 被抽取到 `res/values-zh/strings.xml`，并通过 `stringResource(R.string.xxx)` 引用，Composable 中 SHALL NOT 再直接书写中文字面量（错误提示等动态拼接除外）。
2. WHEN 新建 `res/values-en/strings.xml` THEN SHALL 为每一个 key 提供对应的英文翻译；键名采用分层命名，如 `tab_home` / `btn_submit` / `course_discover` / `error_network`。
3. WHEN 用户在 Android 系统「设置 → 语言」切换至英文 THEN App 启动后 SHALL 自动显示英文界面，且 `GradientTopBar` 标题、底部 Tab、按钮、空态文案全部生效。
4. WHEN `res/values/strings.xml` 保持作为默认（英文回退） THEN 未翻译到的 key SHALL 回退到英文而非崩溃；构建脚本 `lintOptions` SHALL 启用 `MissingTranslation` 警告检查。
5. WHEN 涉及日期 / 数字 / 复数格式 THEN 使用 `androidx.compose.ui.res.pluralStringResource` 与 `java.text.NumberFormat.getInstance(Locale)`，例如"3 门课程 / 3 courses"通过 plurals 资源实现。

---

### 需求 13 —— 课程资源 · 音视频预览入口（README §7 · ExoPlayer，UI 占位）

**用户故事：** 作为学生，我希望在课程资料列表中看到视频/音频类资源时能直接在 App 内播放，而不是只能下载，以便快速预览教学内容。

#### 验收标准

1. WHEN [CourseResourceList](Android/app/src/main/java/com/cen/feedback/ui/student/course) 渲染一条资源项 THEN SHALL 根据文件扩展名（`.mp4 / .mov / .mkv / .m4a / .mp3 / .wav`）自动识别媒体类型，并在右侧操作区追加一枚 `PlayCircleFilled` 图标按钮。
2. WHEN 用户点击该播放按钮 THEN SHALL 导航到新增路由 `Routes.MEDIA_PLAYER?url={encodedUrl}&title={title}`，进入 `MediaPlayerScreen` 全屏页面。
3. WHEN 进入 `MediaPlayerScreen` THEN SHALL 使用 `androidx.media3:media3-exoplayer + media3-ui`（Compose BOM 内置稳定版）构建带播放控制条的播放器；视频 URL 直接使用现有 `CourseResourceController` 返回的下载直链。
4. IF 后端返回的 URL 为 HTTP 明文 THEN `network_security_config.xml` 已放开 cleartext，`MediaPlayerScreen` SHALL 正常播放；如发生播放错误（格式不支持 / 网络失败） THEN SHALL 显示"无法播放，点击下载"回退按钮。
5. WHEN 用户离开播放器页面 THEN SHALL 在 `DisposableEffect` 中释放 ExoPlayer 实例，避免内存泄漏；旋屏时 SHALL 使用 `requestedOrientation = SCREEN_ORIENTATION_SENSOR` 支持自动横屏全屏。
6. NOTE：本需求**仅实现 UI 入口与播放器壳**，不改动后端接口；推送（Firebase / 极光）因涉及第三方注册与 google-services 插件，**不纳入本次 UI 优化范围**。

---

## 非功能性与实现约束

- 所有 Composable 函数保持 `@Composable` 无状态倾向，新增组件优先放到 [Components.kt](Android/app/src/main/java/com/cen/feedback/ui/components/Components.kt) 或按职能拆新的同包文件，避免单文件超过 500 行。
- 颜色 / 间距 / 形状 / 字体的取值一律通过 `MaterialTheme.colorScheme / typography / shapes` 或新加的 `LocalAppSpacing`、`LocalBrandGradients` 获取，禁止页面中散写 `dp` 与 `Color(...)`。
- 改动不得引入新的 `minSdk` 抬升；当前 `minSdk = 24`（Android 7.0），所有动画 API 需兼容 API 24。
- 如需引入 `accompanist-systemuicontroller` 或 `material3 PullToRefresh`，统一写入 [libs.versions.toml](Android/gradle/libs.versions.toml) 并在 [app/build.gradle.kts](Android/app/build.gradle.kts) 中引用。
- 完成后必须保证：`./gradlew :app:assembleDebug` 成功；Android Studio 打开 `Android/` → Sync → Run app 能直接安装到模拟器/真机，且三端登录主流程不报崩溃。

## 交付物

1. 修改或新增的 Kotlin 源码（`ui/theme/` `ui/components/` 及三端页面）。
2. 必要时新增的 `drawable` / `values-night` 资源。
3. 在Android目录下生成一个 `README.md` 进行 UI界面优化的说明。

