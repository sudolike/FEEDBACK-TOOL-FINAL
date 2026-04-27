# Android UI 优化说明


## 适用范围

- 平台：Android（Jetpack Compose）
- 重点：界面一致性、组件复用、关键页面布局优化、国际化基础
- 说明：仅写已落地改动

## 已完成的 UI 优化

### 1）可复用 UI 基础能力

- 新增统一间距与尺寸 token：`AppDimens`
- 新增角色主界面通用壳层抽象：
  - `AppBottomTab`
  - `RoleMainScaffold`
- 新增通用头图区组件：
  - `DashboardHero`（首页/看板顶部）
  - `CourseDetailHero`（课程详情顶部）

已更新文件：
- `app/src/main/java/com/cen/feedback/ui/components/Components.kt`

### 2）三端主壳层统一（学生/教师/管理员）

- 通过 `RoleMainScaffold` 统一底部导航结构
- Tab 定义统一为字符串资源驱动（`@StringRes`）

已更新文件：
- `app/src/main/java/com/cen/feedback/ui/student/StudentMainScaffold.kt`
- `app/src/main/java/com/cen/feedback/ui/teacher/TeacherMainScaffold.kt`
- `app/src/main/java/com/cen/feedback/ui/admin/AdminMainScaffold.kt`

### 3）首页/看板界面优化

- 学生、教师、管理员首页统一使用 Hero 头图风格
- 关键区域间距替换为 token（`AppDimens`）
- 关键横向卡片布局支持更好的自适应宽度
- 空状态展示在核心分区内统一

已更新文件：
- `app/src/main/java/com/cen/feedback/ui/student/home/StudentHomeScreen.kt`
- `app/src/main/java/com/cen/feedback/ui/teacher/dashboard/TeacherHomeScreen.kt`
- `app/src/main/java/com/cen/feedback/ui/admin/dashboard/AdminDashboardScreen.kt`

### 4）课程详情页一致性优化

- 学生端与教师端课程详情页统一为 `CourseDetailHero` 顶部结构
- 详情页 Tab 标题改为字符串资源驱动
- 学期文本改为格式化资源（便于国际化）

已更新文件：
- `app/src/main/java/com/cen/feedback/ui/student/course/CourseDetailScreen.kt`
- `app/src/main/java/com/cen/feedback/ui/teacher/course/TeacherCourseDetailScreen.kt`

### 5）AI 面板 UI 与文案资源化

- 打字指示器增强，支持本地化“正在回复”文案
- AI 面板关键文案（标题、占位、按钮、关闭说明）迁移到资源文件
- 学生端/教师端 AI 快捷操作的标签与提示词改为资源驱动

已更新文件：
- `app/src/main/java/com/cen/feedback/ui/components/AiAssistantBubble.kt`

### 6）登录/注册页面国际化改造

- 登录页与注册页核心可见文案改为 `stringResource`
- 角色切换文案改为资源驱动

已更新文件：
- `app/src/main/java/com/cen/feedback/ui/auth/LoginScreen.kt`
- `app/src/main/java/com/cen/feedback/ui/auth/RegisterScreen.kt`

### 7）i18n 资源基础落地

- 扩展中文资源：`values/strings.xml`
- 新增英文资源：`values-en/strings.xml`
- 已覆盖：底栏 Tab、首页/看板关键文案、登录注册核心文案、AI 面板文案、课程详情 Tab 文案等

已更新文件：
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/values-en/strings.xml`

## 已完成效果总结

- 学生/教师/管理员核心页面的结构风格更统一
- 组件复用程度明显提升，页面样式重复代码减少
- 核心流程中大量用户可见文案已迁移到资源体系
- 已具备中英双语切换所需的基础资源能力
