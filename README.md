# JCStaff

一个基于 Pixiv 公开 API 的第三方 Android 客户端，使用 Jetpack Compose + Material Design 3 构建。

---

## 功能概览

### 内容浏览

- **首页多标签**：发现、排行、推荐、关注、新作
- **排行榜**：插画/漫画日榜、周榜、月榜；AI 榜、男性向、女性向、新人、原创；动图排行
- **关注动态**：关注用户的最新插画、漫画、小说
- **精选专题**：Pixiv Spotlight 推荐文章
- **珍藏册**：浏览与保存用户珍藏册

### 搜索

- 插画、用户、小说全文搜索
- 标签搜索（部分一致 / 完全一致 / 标题简介）
- 搜索历史持久化
- 热门标签推荐
- 标签详情页（相关标签、父子标签、Pixiv 百科内容）

### 作品详情

- 完整作品信息：标题、作者、日期、标签、简介、尺寸、页数、类型
- AI 生成/辅助内容标注
- 多图展开浏览
- 动图（Ugoira）预览与 GIF 生成下载
- 收藏（公开/私密，支持自定义标签）
- 图片全屏查看（双指缩放、平移，共享元素动画）
- 分享图片或链接
- 评论查看与回复
- 相关作品推荐
- 以图搜源（SauceNao）

### 用户主页

- 个人资料、头像、简介、社交链接（网站、Twitter）
- 作品列表（插画、漫画、小说）
- 公开收藏
- 关注列表
- 工作环境信息（设备、软件等）
- 关注 / 取消关注（公开 / 悄悄关注）

### 小说

- 小说详情与系列导航
- 沉浸式阅读器（字号、行距可调）
- 小说收藏与标签管理
- 关注用户小说动态

### 下载管理

- 批量添加下载队列
- 实时进度追踪（单图百分比 / 多图页数进度）
- 下载队列页面（App Store 风格分区：正在下载 / 下载失败 / 已完成）
- 暂停 / 继续 / 重试 / 删除单条任务
- 重试全部失败
- 断点续传（已下载页不重复）
- 自定义文件名模板（变量：`{id}` `{title}` `{user}` `{user_id}` `{page}` `{date}`）
- Android 10+ Scoped Storage 支持

### 内容过滤

- 屏蔽用户（屏蔽后所有列表隐藏该用户内容）
- 屏蔽作品
- 屏蔽标签
- 全局隐藏 AI 生成内容开关
- 屏蔽设置页（统一管理，支持解除）
- 过滤实时响应（修改规则后所有列表自动刷新）

### 历史记录

- 浏览历史（插画、小说、用户）
- 搜索历史
- 下载历史

### 多账号

- 同时登录多个 Pixiv 账号
- 无缝切换账号
- 账号隔离（独立数据库、设置、下载历史）
- 单独移除账号及其本地数据

### 设置

| 分类 | 选项 |
|------|------|
| 语言 | 简体中文、繁体中文、日文、韩文、英文（即时切换，无需重启） |
| 外观 | 卡片圆角、卡片间距、是否显示作品信息 |
| 存储 | 图片缓存上限（256MB–4GB）、缓存浏览器、一键清理 |
| 内容 | 隐藏 AI 作品、屏蔽设置 |
| 下载 | 文件名模板 |
| 账号 | 查看主页、切换账号、管理账号、退出登录 |

---

## 技术栈

### 平台

| 项目 | 版本 |
|------|------|
| Min SDK | 26 (Android 8.0) |
| Target SDK | 36 (Android 15) |
| Kotlin | 2.0.21 |
| AGP | 9.0.0 |
| Java Target | 11 |

### 主要依赖

| 库 | 版本 | 用途 |
|----|------|------|
| Compose BOM | 2025.01.01 | UI 框架 |
| Material 3 | 1.5.0-alpha13 | 设计系统 |
| Retrofit 2 | 2.11.0 | REST 客户端 |
| OkHttp 3 | 4.12.0 | HTTP 客户端 |
| Gson | 2.11.0 | JSON 序列化 |
| Coil | 2.6.0 | 图片加载 |
| Telephoto | 0.18.0 | 可缩放图片查看器 |
| Room | 2.7.0 | 本地数据库 |
| DataStore | 1.1.1 | 设置持久化 |
| JSoup | 1.17.2 | HTML 解析（标签详情页） |
| Lottie | 6.4.0 | 动效 |

---

## 架构

### MVVM + Kotlin Flow

```
UI (Compose) ──collectAsState()──> ViewModel ──StateFlow──> Repository / Manager
```

### 核心组件

#### PagedDataLoader

通用分页加载器，支持缓存、自动过滤与解除屏蔽后内容恢复：

```kotlin
// ViewModel 中标准用法
val loader = PagedDataLoader(
    cacheConfigProvider = { CacheConfig(...) },
    responseClass = IllustListResponse::class.java,
    loadFirstPage = { api.getIllusts() },
    itemFilter = ContentFilterManager::shouldShow,
    scope = viewModelScope
)
val state = loader.state  // 稳定引用，直接 collectAsState()
```

- 保留 `_rawItems`（未过滤原始数据），屏蔽规则变更时从原始数据重新过滤，无需重新请求
- 通过 `filterVersion` StateFlow 自动响应过滤规则变化

#### ContentFilterManager

全局单例，DataStore 持久化，管理屏蔽用户/作品/标签和 AI 内容过滤：

```kotlin
ContentFilterManager.blockUser(userId)
ContentFilterManager.shouldShow(illust)   // 统一过滤判断
```

#### DownloadTaskManager

单例下载队列管理器：
- 顺序下载（同时最多 1 个），支持暂停/继续
- 暂停实现：在字节读取循环中检查 `stopRequestedIds`，响应延迟 < 8KB
- 暂停后整个队列停止，不自动拉取下一个任务
- App 重启后自动恢复未完成任务（DOWNLOADING → PENDING）
- 断点续传：记录已完成页数，续传从断点继续

#### SettingsStore

双模式 DataStore：
- **全局**：语言设置
- **用户级**：缓存上限、隐藏 AI 内容、文件名模板

### 导航

自定义栈式导航（非 Jetpack Navigation）：

```kotlin
sealed interface NavRoute : Serializable {
    val stableKey: String   // SaveableStateProvider key
}
```

`NavigationViewModel` 管理返回栈、预测性返回手势状态、App Switcher 截图捕获、Deep Link 排队（未登录时暂存）。

### 数据库（Room）

每个用户独立数据库 `jcstaff_db_{userId}`，包含：
- 浏览历史（插画、小说、用户）
- 搜索历史
- 下载任务（状态、进度、完整 JSON）
- API 响应缓存（带 TTL）

---

## 深度链接

以下 URL 可从浏览器/其他应用直接跳转至对应页面：

| URL 格式 | 跳转目标 |
|----------|----------|
| `https://www.pixiv.net/artworks/{id}` | 作品详情 |
| `https://www.pixiv.net/en/artworks/{id}` | 作品详情 |
| `https://www.pixiv.net/users/{id}` | 用户主页 |
| `https://www.pixiv.net/en/users/{id}` | 用户主页 |
| `https://www.pixiv.net/novel/show.php?id={id}` | 小说详情 |
| `pixiv://account/login?code={code}` | OAuth 回调 |

未登录时 Deep Link 会暂存，登录完成后自动跳转。

---

## 权限

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
    android:maxSdkVersion="28" />
```

Android 10+ 使用 Scoped Storage（MediaStore），无需存储权限。

---

## 构建

```bash
# Debug 构建
./gradlew assembleDebug

# Release 构建
./gradlew assembleRelease
```

签名配置在 `app/build.gradle.kts` 中，密钥文件位于 `keystore/`（开源用途，生产环境请替换自己的签名密钥）。

---

## 项目结构

```
app/src/main/java/ceui/lisa/jcstaff/
├── auth/           # OAuth、账号注册与切换
├── cache/          # Room 实体、DAO、数据库
├── components/     # 可复用 Compose 组件
│   ├── animations/ # 动画工具
│   ├── appswitcher/# 多页面切换覆层
│   ├── illust/     # 插画相关组件
│   └── novel/      # 小说相关组件
├── core/           # 核心业务逻辑
│   ├── ContentFilter.kt        # 内容过滤系统
│   ├── DownloadTaskManager.kt  # 下载队列管理
│   ├── PagedDataLoader.kt      # 通用分页加载器
│   ├── PagedViewModel.kt       # 基础 ViewModel
│   └── SettingsStore.kt        # 设置持久化
├── history/        # 浏览/下载历史 ViewModels
├── home/           # 首页各标签 ViewModels
├── navigation/     # 路由定义、NavigationViewModel
├── network/        # Retrofit 接口、数据模型、拦截器
├── profile/        # 用户主页 ViewModel
├── screens/        # 全部页面 Composable
├── tagdetail/      # 标签详情与搜索
├── ugoira/         # 动图处理与 GIF 生成
├── ui/theme/       # Material 3 主题与配色
└── utils/          # 工具函数
```

---

## 声明

本项目为开源第三方客户端，与 Pixiv 官方无关。请遵守 [Pixiv 使用条款](https://www.pixiv.net/terms.php)。
