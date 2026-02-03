# JCStaff - Pixiv 第三方 Android 客户端

## 项目概览

JCStaff 是一个使用 **Jetpack Compose + Material Design 3** 构建的 Pixiv 第三方 Android 客户端。采用 **MVVM 架构**，支持多账号登录、插画/小说浏览、搜索、收藏、下载等完整功能。

**技术栈一览：**

| 层次 | 技术 |
|------|------|
| UI 框架 | Jetpack Compose + Material 3 |
| 架构模式 | MVVM (ViewModel + StateFlow) |
| 网络请求 | Retrofit2 + OkHttp + Gson |
| 本地存储 | Room 数据库 + DataStore |
| 图片加载 | Coil + 自定义进度加载器 |
| 认证方式 | OAuth 2.0 + PKCE |
| 导航 | 自定义栈式导航 (NavigationViewModel) |
| 动画 | Compose AnimatedContent |
| 特效 | AGSL RuntimeShader |

---

## 目录结构

```
ceui.lisa.jcstaff/
├── MainActivity.kt              # 入口 Activity，主题/语言/导航的根节点
├── auth/                         # 认证与多账号系统
│   ├── AuthViewModel.kt          # 认证状态管理
│   ├── AuthRepository.kt         # Token 持久化与刷新
│   ├── AuthState.kt              # 认证状态定义
│   ├── AccountRegistry.kt        # 多账号注册表
│   ├── AccountSessionManager.kt  # 账号会话生命周期
│   ├── AccountMigration.kt       # 单账号→多账号迁移
│   └── AccountCleanup.kt         # 账号数据清理
├── network/                      # 网络层
│   ├── PixivClient.kt            # Retrofit 客户端工厂
│   ├── PixivApi.kt               # Pixiv API 接口定义
│   ├── OAuthApi.kt               # OAuth 认证接口
│   ├── Models.kt                 # 数据模型（Illust, Novel, User 等）
│   ├── TokenManager.kt           # Access Token 内存管理
│   ├── TokenRefreshInterceptor.kt# 401 自动刷新拦截器
│   ├── HeaderInterceptor.kt      # 请求头注入
│   ├── ApiCacheInterceptor.kt    # API 响应缓存拦截器
│   ├── PkceUtil.kt               # PKCE 工具
│   ├── PixivWebScraper.kt        # 网页抓取服务（ugoira 排行榜、标签搜索等）
│   ├── WebRankingModels.kt       # 网页排行榜数据模型
│   └── WebSearchModels.kt        # 网页标签搜索数据模型
├── cache/                        # 本地缓存
│   ├── AppDatabase.kt            # Room 数据库
│   ├── ApiCacheManager.kt        # API 缓存管理器
│   ├── ApiCacheEntity.kt         # 缓存实体
│   ├── ApiCacheDao.kt            # 缓存 DAO
│   ├── BrowseHistoryRepository.kt# 浏览历史与搜索历史管理器
│   ├── BrowseHistoryEntity.kt    # 浏览历史实体
│   ├── BrowseHistoryDao.kt       # 浏览历史 DAO
│   ├── SearchHistoryEntity.kt    # 搜索历史实体
│   ├── SearchHistoryDao.kt       # 搜索历史 DAO
│   └── DownloadHistoryEntity.kt  # 下载任务实体与 DAO
├── core/                         # 核心工具
│   ├── ObjectStore.kt            # 全局响应式对象池
│   ├── PagedDataLoader.kt        # 通用分页数据加载器
│   ├── SettingsStore.kt          # 用户设置（DataStore）
│   ├── LanguageManager.kt        # 多语言管理
│   ├── AppLanguage.kt            # 语言定义
│   ├── IllustListViewModel.kt    # 通用插画列表 ViewModel
│   ├── ImageDownloader.kt        # 图片下载器
│   ├── DownloadTaskManager.kt    # 下载任务队列管理器
│   ├── LoadTaskManager.kt        # 原图加载任务管理
│   ├── ProgressImageLoader.kt    # 带进度的图片加载
│   ├── SelectionManager.kt       # 多选管理器（CompositionLocal）
│   └── ScrollPositionStore.kt    # 滚动位置持久化
├── navigation/                   # 导航系统
│   ├── NavRoutes.kt              # 路由定义
│   └── NavigationViewModel.kt    # 导航栈管理
├── home/                         # 首页
│   ├── HomeScreen.kt             # 首页界面（三 Tab + 抽屉 + 嵌套 ViewPager）
│   ├── RecommendedViewModel.kt   # 推荐插画（旧版，保留兼容）
│   ├── RecommendedContentViewModel.kt # 推荐插画/漫画（带 contentType 参数）
│   ├── RecommendedNovelsViewModel.kt # 推荐小说
│   ├── RecommendedUsersViewModel.kt  # 推荐作者
│   ├── TrendingViewModel.kt      # 热门排行（旧版）
│   ├── TrendingIllustTagsViewModel.kt  # 热门插画标签
│   ├── TrendingNovelTagsViewModel.kt   # 热门小说标签
│   ├── FollowingViewModel.kt     # 关注的插画/漫画新作
│   ├── FollowingNovelsViewModel.kt   # 关注的小说新作
│   ├── LatestContentViewModel.kt # 全站最新插画/漫画/小说
│   ├── RankingViewModel.kt       # 排行榜详情
│   ├── UgoiraRankingViewModel.kt # 动图排行榜（网页抓取）
│   └── WebTagDetailViewModel.kt  # 网页标签详情（Web AJAX API）
├── screens/                      # 各功能页面
│   ├── LandingScreen.kt          # 登录引导页（3 页 ViewPager）
│   ├── OnboardScreen.kt          # 引导页第 3 页（图片轮播+登录）
│   ├── LanguageSelectionScreen.kt# 语言选择页
│   ├── IllustDetailScreen.kt     # 插画详情页
│   ├── ImageViewerScreen.kt      # 全屏图片查看器
│   ├── NovelDetailScreen.kt      # 小说详情页
│   ├── SearchScreen.kt           # 搜索页
│   ├── TagDetailScreen.kt        # 标签详情页（插画+小说双 Tab）
│   ├── UserProfileScreen.kt      # 用户主页
│   ├── BookmarksScreen.kt        # 收藏列表
│   ├── BrowseHistoryScreen.kt    # 浏览历史（插画+小说+用户三 Tab）
│   ├── DownloadHistoryScreen.kt  # 下载历史（任务队列管理）
│   ├── SettingsScreen.kt         # 设置页（含退出登录+账号管理入口）
│   ├── AccountManagementScreen.kt# 账号管理页
│   ├── CommentScreen.kt          # 评论页（完整评论列表+回复+发布）
│   ├── RankingDetailScreen.kt    # 排行榜详情页（多模式 + 日期选择）
│   ├── UgoiraRankingScreen.kt    # 动图排行榜页面（网页抓取）
│   ├── WebTagDetailScreen.kt     # 网页标签详情页（MD3 设计）
│   ├── ShaderDemoScreen.kt       # AGSL 着色器演示页（5 种效果）
│   ├── LatestWorksScreen.kt      # 最新作品页（插画/漫画/小说三 Tab）
│   ├── ListScreen.kt             # 通用列表页
│   └── DetailScreen.kt           # 通用详情页
├── components/                   # 可复用 UI 组件
│   ├── BookmarkTagDialog.kt      # 收藏标签选择底部弹窗
│   ├── IllustCard.kt             # 插画卡片
│   ├── IllustGrid.kt             # 插画瀑布流网格
│   ├── IllustFeedCard.kt         # 社交信息流风格插画卡片
│   ├── IllustFeed.kt             # 社交信息流插画列表（纵向）
│   ├── NovelCard.kt              # 小说卡片
│   ├── NovelList.kt              # 小说列表
│   ├── ErrorRetryState.kt        # 通用错误重试状态（支持下拉刷新+重试按钮）
│   ├── LoadingIndicator.kt       # MD3 加载指示器
│   ├── FloatingTopBar.kt         # 浮动顶部栏（返回+分享）
│   ├── ScrollAwareTopBar.kt      # 滚动感知顶部栏（插画/用户详情页）
│   ├── SelectionTopBar.kt        # 多选操作栏
│   ├── ProgressiveImage.kt       # 渐进式图片
│   ├── MetaInfoRow.kt            # 元信息行
│   ├── ShaderBackground.kt       # AGSL 着色器背景（含 TracedTunnel）
│   ├── UserHistoryCard.kt        # 用户浏览历史卡片
│   ├── illust/                   # 插画详情组件
│   │   ├── CollapsibleImageSection.kt  # 可折叠图片区域
│   │   ├── IllustActionBar.kt    # 收藏/下载操作栏
│   │   ├── IllustAuthorRow.kt    # 作者信息行
│   │   ├── IllustCaption.kt      # 作品简介
│   │   ├── IllustMetaInfo.kt     # 元数据（尺寸、日期等）
│   │   └── IllustTags.kt         # 标签流
│   ├── comment/                  # 评论组件
│   │   ├── CommentCard.kt        # 评论卡片（完整版+紧凑版）
│   │   └── CommentPreview.kt     # 评论预览区（详情页嵌入）
│   ├── novel/                    # 小说组件
│   │   └── NovelActionBar.kt     # 小说操作栏
│   └── user/                     # 用户组件
│       ├── UserProfileHeader.kt  # 用户主页头部
│       ├── UserInfoCard.kt       # 用户信息卡片
│       ├── UserAvatarSection.kt  # 头像区域
│       ├── UserStatsRow.kt       # 统计数据行
│       ├── UserFollowButton.kt   # 关注按钮
│       ├── UserWorkspaceCard.kt  # 工作环境卡片
│       ├── WorkspaceRow.kt       # 工作环境行
│       └── StatItem.kt           # 统计项
├── comment/                      # 评论系统
│   └── CommentViewModel.kt      # 评论业务逻辑与状态管理
├── search/                       # 搜索模块
│   └── SearchViewModel.kt        # 搜索逻辑
├── profile/                      # 用户主页模块
│   └── UserProfileViewModel.kt   # 用户主页逻辑
├── tagdetail/                    # 标签详情模块
│   ├── TagDetailViewModel.kt     # 搜索排序/匹配选项枚举定义
│   ├── TagIllustSearchViewModel.kt   # 标签插画搜索 ViewModel
│   └── TagNovelSearchViewModel.kt    # 标签小说搜索 ViewModel
├── history/                      # 历史记录模块
│   ├── BrowseHistoryViewModel.kt # 浏览历史逻辑
│   └── DownloadHistoryViewModel.kt # 下载历史逻辑
├── ugoira/                       # Ugoira (动图) 模块
│   ├── UgoiraPlayer.kt           # Ugoira 播放器组件
│   ├── UgoiraViewModel.kt        # Ugoira 状态管理
│   ├── UgoiraRepository.kt       # Ugoira 下载/编码仓库
│   └── AnimatedGifEncoder.kt     # GIF 编码器（NeuQuant 神经网络颜色量化）
├── ui/theme/                     # 主题
│   ├── Color.kt                  # 颜色定义
│   ├── Theme.kt                  # 主题配置
│   └── Type.kt                   # 字体排版
└── utils/                        # 工具类
    ├── Formatters.kt             # 格式化工具
    └── EmojiUtils.kt             # Pixiv 评论表情工具
```

---

## 功能详解

### 1. 登录与认证系统

#### 用户视角
- 首次打开 App 进入 **3 页引导流程**（HorizontalPager）：
  - **第 1 页 — 欢迎页**：Image Tunnel 着色器动态背景（448 张 Pixiv 插画瓷砖墙）+ 渐变遮罩，展示应用名称和欢迎语
  - **第 2 页 — 语言选择**：选择显示语言（中/繁中/英/日/韩）
  - **第 3 页 — 登录轮播**：Pixiv 作品幻灯片背景，提供登录/注册按钮
- 底部有页面指示器（圆点），支持左右滑动切换
- 点击「登录」或「注册」跳转至 Pixiv 官方网页完成授权
- 授权完成后自动返回 App，显示首页内容
- 支持同时登录多个账号，在抽屉中切换

#### 实现原理

**OAuth 2.0 + PKCE 流程：**

```
┌──────────┐     ①生成 PKCE      ┌──────────┐
│   App    │ ─────────────────→ │  PKCE    │
│          │     code_verifier  │  Util    │
│          │     code_challenge │          │
└────┬─────┘                    └──────────┘
     │  ②打开 Chrome Custom Tab
     │  带 code_challenge 参数
     ▼
┌──────────┐     ③用户登录       ┌──────────┐
│ Chrome   │ ─────────────────→ │  Pixiv   │
│ Custom   │                    │  Server  │
│ Tab      │ ←───────────────── │          │
└────┬─────┘  ④回调 pixiv://     └──────────┘
     │        携带 auth_code
     ▼
┌──────────┐     ⑤用 code +     ┌──────────┐
│   App    │     verifier 换    │  OAuth   │
│  Auth    │ ─────────────────→ │  Server  │
│ ViewModel│                    │          │
│          │ ←───────────────── │          │
└──────────┘  ⑥返回 access_token └──────────┘
              + refresh_token
```

**关键文件：**
- `PkceUtil.kt` — 生成 SHA256 code_challenge 和随机 code_verifier
- `OAuthApi.kt` — 定义 `/auth/token` 接口（登录+刷新）
- `AuthRepository.kt` — 通过 DataStore 持久化 token，管理 token 刷新回调
- `AuthViewModel.kt` — 管理认证状态机（Loading → Authenticated / NotAuthenticated）

**Token 自动刷新：**
当 API 返回 401 时，`TokenRefreshInterceptor` 自动拦截请求、刷新 token 并重试：
```
请求 → 401 → 加锁 → 刷新 token → 更新内存+DataStore → 重试原请求
```
使用 `ReentrantLock` 确保并发请求只触发一次刷新。

---

### 2. 多账号系统

#### 用户视角
- 在抽屉菜单中查看所有已登录账号
- 点击其他账号即可快速切换
- 点击「添加账号」可登录新账号
- 长按可移除指定账号

#### 实现原理

**数据隔离架构：**

每个账号拥有独立的：
- DataStore 文件：`auth_prefs_{userId}` 存储 token，`settings_{userId}` 存储设置
- Room 数据库：`jcstaff_db_{userId}` 存储缓存和浏览历史
- 缓存目录：`image_load_cache_{userId}` 存储图片缓存

**切换账号流程：**
```
用户点击切换
    → AccountSessionManager.switchAccount()
        → teardownCurrentSession()     // 关闭当前 DB、重置缓存
        → AccountRegistry.setActiveAccount(userId)
        → initializeAuth(userId)       // 加载新账号 token
        → initializeServices(userId)   // 创建新 DB、缓存实例
    → UI 通过 key(activeUserId) 强制重建所有页面
```

**关键文件：**
- `AccountRegistry.kt` — DataStore 存储账号列表和当前活跃 ID
- `AccountSessionManager.kt` — 会话生命周期协调器（初始化/切换/拆卸）
- `AccountMigration.kt` — 旧版单账号数据自动迁移至新的 per-user 命名

---

### 3. 首页（三 Tab + 抽屉）

#### 用户视角
- **推荐 Tab**：展示 Pixiv 推荐内容，包含三个子 Tab：
  - **插画**：顶部精美排行榜轮播（金银铜牌徽章 + 作者头像 + 收藏数）+ 「为你推荐」Section Header + 瀑布流推荐
  - **漫画**：顶部精美排行榜轮播 + 「为你推荐」Section Header + 瀑布流推荐
  - **小说**：小说推荐列表
- **发现 Tab**：探索入口，包含三个子 Tab：
  - **插画漫画标签**：热门标签网格（首个全宽展示，1dp 间距，直角边框）
  - **小说标签**：小说热门标签网格
  - **推荐作者**：可能喜欢的作者列表（含示例作品）
- **新作 Tab**：基于时间线的最新内容，包含四个子 Tab：
  - **关注插画漫画**：已关注画师的最新插画/漫画
  - **关注小说**：已关注作者的最新小说
  - **最新插画漫画**：全站最新插画/漫画
  - **最新小说**：全站最新小说
- 左侧抽屉显示用户信息、多账号切换、快捷入口（浏览历史、收藏、设置、Shader Demo）

#### 实现原理

**页面结构：**
```
ModalNavigationDrawer（侧滑抽屉）
└── Scaffold
    ├── TopAppBar（用户头像 + 搜索按钮）
    ├── BottomNavigation（推荐 / 发现 / 新作）
    └── HorizontalPager（三个 Tab 页面，userScrollEnabled=false）
        ├── Tab 0: RecommendedTabPage
        │   └── HorizontalPager（插画 / 漫画 / 小说）
        │       ├── IllustGrid + RankingCarousel（插画）
        │       ├── IllustGrid + RankingCarousel（漫画）
        │       └── NovelList（小说）
        ├── Tab 1: DiscoverTabPage
        │   └── HorizontalPager（插画漫画标签 / 小说标签 / 推荐作者）
        │       ├── TrendingTagGrid（插画漫画）
        │       ├── TrendingTagGrid（小说）
        │       └── RecommendedUsersList
        └── Tab 2: NewWorksTabPage
            └── HorizontalPager（关注插画漫画 / 关注小说 / 最新插画漫画 / 最新小说）
                ├── IllustFeed（关注插画漫画 — 社交信息流风格）
                ├── NovelList（关注小说）
                ├── IllustGrid（最新插画漫画）
                └── NovelList（最新小说）
```

**多个独立 ViewModel** 分别管理各页面数据：
- `RecommendedContentViewModel` — 推荐插画/漫画 + 对应排行榜（带 contentType 参数）
- `RecommendedNovelsViewModel` — 推荐小说
- `TrendingTagsViewModel` — 热门标签（插画漫画 + 小说）
- `RecommendedUsersViewModel` — 推荐作者列表
- `FollowingViewModel` — 关注的插画/漫画新作
- `FollowingNovelsViewModel` — 关注的小说新作
- `LatestIllustsViewModel` — 全站最新插画/漫画（带 contentType 参数）
- `LatestNovelsViewModel` — 全站最新小说
- `RankingViewModel` — 排行榜详情页（带 mode 参数）

每个 ViewModel 在 `init` 时自动加载数据，采用 **Stale-While-Revalidate** 缓存策略：

| 缓存状态 | 时间范围 | 行为 |
|---------|---------|------|
| 新鲜缓存 | 0-15 分钟 | 直接使用缓存，不发网络请求 |
| 过期缓存 | 15 分钟-7 天 | 先展示缓存，后台刷新最新数据 |
| 无缓存/超期 | >7 天 | 显示 loading，等待网络响应 |

**错误处理：** 网络请求失败时，如果有缓存数据则静默失败（保持展示缓存），无缓存时显示 `ErrorRetryState` 组件（支持下拉刷新+重试按钮）。

**性能说明：** HorizontalPager 是懒加载的，默认只组合当前页和相邻页（`beyondBoundsPageCount = 0`），10 个子页面和 3 个子页面对性能影响几乎相同。

---

### 3.1 排行榜详情页

#### 用户视角
- 从推荐页的排行榜轮播点击「查看完整排行榜」进入
- 顶部可切换排行榜模式（日榜/周榜/月榜/AI日榜/男性向/女性向/原创/新人）
- 漫画有独立的排行榜模式（漫画日榜/周榜/月榜/新人）
- 顶部有日期选择按钮，可查看历史排行榜（最早 2008-08-01）
- 瀑布流展示排行榜内容，支持下拉刷新和无限加载

#### 实现原理

- `RankingDetailScreen` 使用 `ScrollableTabRow` + `HorizontalPager` 展示多个排行榜模式
- `RankingViewModel` 接收 `mode` 参数，调用 `getRankingIllusts(mode, date)` API
- 日期选择使用 Android `DatePickerDialog`，支持 2008-08-01 到昨天的范围
- 导航路由 `NavRoute.RankingDetail(objectType)` 区分插画和漫画排行榜

---

### 3.2 排行榜轮播组件 (RankingCarousel)

#### 用户视角
- 在推荐 Tab 的插画/漫画子页面顶部展示
- 精美的 Header 区域：渐变背景 + 趋势图标 + 标题副标题 + 圆角按钮
- 横向滑动的排行榜卡片，前三名有金银铜牌徽章
- 每张卡片展示：作品图片、排名徽章、作者头像、标题、作者名、收藏数

#### 实现原理

**RankingCarousel 组件：**
- Header 使用 `primaryContainer → tertiaryContainer` 横向渐变背景
- 圆形图标容器包含 `TrendingUp` 图标
- 「查看全部」使用 `Surface` 圆角按钮样式
- `LazyRow` 横向滑动，12dp 间距，16dp 水平内边距

**RankingCard 设计：**
- 160dp 宽度，0.8 宽高比
- Top 3 使用金/银/铜色圆形徽章（带边框光泽）
- 其他排名使用简洁的 `#N` 矩形徽章
- 作者头像显示在图片右下角（28dp，白色边框）
- 底部信息区：标题（bodyMedium + SemiBold）、作者名（bodySmall）、收藏数（带心形图标）
- Top 3 卡片有更高的阴影（8dp vs 4dp）

**SectionHeader 组件：**
- 用于分隔排行榜和推荐瀑布流
- 标题（titleMedium + Bold）+ 副标题（bodySmall）
- 暖色渐变图标（珊瑚红 → 橙色 → 黄色，40dp 背景 + 22dp 图标）
- 「为你推荐」使用心形图标

---

### 4. 插画详情页

#### 用户视角
- 从任何列表点击插画卡片进入详情
- 顶部显示作品图片（多图可展开）
- 下方依次展示：标题、作者信息（可关注）、操作栏（收藏/下载/分享）、标签、简介、元数据
- 底部展示相关推荐作品的瀑布流
- **滚动感知顶部栏**：当列表向下滚动超过阈值时，顶部显示「作品标题 by 作者名」的 TopAppBar，包含返回按钮和回到顶部按钮

#### 实现原理

**数据获取流程：**
1. 从 `ObjectStore`（内存缓存池）获取上一个页面传递的缓存数据，**即时展示**
2. 如果缓存不存在，调用 `getIllustDetail` API 获取
3. 绑定 `IllustLoader` 加载相关作品列表
4. 通过 `BrowseHistoryManager.recordView()` 记录浏览历史

**ObjectStore 响应式数据共享：**
```
列表页 put(illust) → ObjectStore[StoreKey] = MutableStateFlow(illust)
                              ↕ 双向同步
详情页 get(StoreKey) → 收到 StateFlow → collectAsState()

当详情页更新收藏状态:
    ObjectStore.updateTyped(key) { illust.copy(is_bookmarked = true) }
    → 列表页的 Flow 同步更新 → 卡片自动刷新收藏图标
```

---

### 5. 全屏图片查看器

#### 用户视角
- 点击详情页中的图片进入全屏模式
- 先显示预览图，后台加载原图并显示下载进度（百分比环形指示器）
- 原图加载完成后可双指缩放和拖拽

#### 实现原理

**分层加载策略：**
```
┌─────────────────────────────────┐
│  ZoomableAsyncImage（原图/可缩放）│ ← 原图下载完成后显示
├─────────────────────────────────┤
│  AsyncImage（预览图/底层）       │ ← 立即显示
└─────────────────────────────────┘
```

**LoadTaskManager 任务管理：**
- 每个 URL 对应一个下载任务，使用 `StateFlow` 广播进度
- 详情页和图片查看器**共享同一个下载任务**：
  - 在详情页点击图片时，下载可能已开始
  - 进入图片查看器后，直接续上已有任务的进度
  - 退出查看器时取消监听但不取消下载
- 下载完成后缓存到本地文件，再次查看时**瞬间加载**

**缩放功能：** 使用 `telephoto` 库的 `ZoomableAsyncImage` 组件。

---

### 6. 搜索系统

#### 用户视角
- 点击首页搜索图标进入搜索页
- 顶部 TopAppBar 显示「搜索」标题和返回按钮
- MD3 风格搜索框（药丸形状、填充背景）自动聚焦
- 展示两个 Section：
  - **最近搜索**：搜索历史，点击 × 可删除（带二次确认弹窗）
  - **热门搜索**：从 Trending Tags API 获取的热门标签
- 输入关键词时展示实时联想（500ms debounce）
- 联想列表高亮匹配的关键词
- 点击联想或热门标签进入 TagDetail 页面
- 从 TagDetail 返回时光标自动定位到文字末尾
- 返回手势：有文字时清空，无文字时返回上一页

#### 实现原理

- 使用普通 `Column` + `TextField` 布局，避免 SearchBar 的预测性返回动画
- `TextFieldValue` 控制光标位置，`DisposableEffect` + `LifecycleEventObserver` 监听 `ON_RESUME` 事件
- 搜索历史使用 Room 数据库持久化存储（`SearchHistoryEntity`）
- `BrowseHistoryRepository.recordSearch(tag)` 统一记录搜索历史（手动搜索、标签点击、热门标签均会记录）
- 关键词联想使用 `snapshotFlow` + `debounce(500)` + `collectLatest` 实现
- `HighlightedText` 使用 `AnnotatedString` + `SpanStyle` 实现关键词高亮
- 热门标签通过 `getTrendingTags()` API 获取
- 删除历史时弹出 `AlertDialog` 确认

---

### 7. 标签详情页

#### 用户视角
- 从插画详情页点击标签进入
- 顶部显示标签名和翻译名
- **双 Tab 页面**（HorizontalPager + TabRow）：
  - **插画 Tab**：展示该标签下的插画作品（瀑布流）
  - **小说 Tab**：展示该标签下的小说作品（线性列表）
- 支持筛选条件：排序方式（Dropdown）、搜索目标（FilterChip）
- 支持添加/移除额外标签进行组合搜索（InputChip）
- Premium 用户可按热度排序，普通用户按时间排序
- 可通过 `initialTab` 参数指定初始 Tab

#### 实现原理

- `TagDetailViewModel` 接收 `Tag` 对象，管理 `SearchSort`、`SearchTarget` 等筛选状态
- 普通用户使用 `sort=date_desc`，Premium 用户使用 `sort=popular_desc`
- 插画使用 `IllustGrid` 组件，小说使用 `NovelList` 组件
- 导航路由 `NavRoute.TagDetail(tag, initialTab)` 支持指定打开时的初始 Tab

---

### 8. 用户主页

#### 用户视角
- 沉浸式头部：背景图 + 头像 + 用户名 + 简介
- 统计数据：投稿数、收藏数、关注数
- 关注/取消关注按钮
- 下方展示该用户的所有投稿作品（瀑布流）
- **滚动感知顶部栏**：当列表向下滚动超过阈值时，顶部显示用户头像、用户名和 @账号的 TopAppBar，包含返回按钮和回到顶部按钮

#### 实现原理

- `UserProfileViewModel` 并行加载：
  - `getUserDetail()` — 用户基本信息和统计
  - `getUserIllusts()` — 用户作品列表（分页）
- `UserProfileHeader` 组件：
  - 背景图使用 `background_image_url`
  - 如无背景图，使用渐变色兜底
  - 信息卡片包含工作环境（电脑、显示器、绘图板等）

---

### 9. 收藏管理

#### 用户视角
- 在插画详情页点击收藏按钮即时收藏/取消收藏
- 在抽屉菜单中进入「我的收藏」查看所有收藏
- 支持多选批量下载收藏的作品

#### 实现原理

**收藏操作流程：**
```
用户点击收藏
    → 调用 addBookmark/deleteBookmark API
    → 更新本地 ObjectStore 中的 is_bookmarked
    → 所有引用该 Illust 的 UI 自动更新
```

**收藏列表：** 使用 `IllustListViewModel` + `IllustLoader`，绑定 `getUserBookmarks()` API。

---

### 10. 小说功能

#### 用户视角
- 在首页推荐 Tab 的「小说」子 Tab 中浏览推荐小说
- 小说卡片展示封面、标题、作者、字数等信息
- 点击进入小说详情页查看完整信息

#### 实现原理

- `NovelCard` 组件：较大的卡片样式，展示封面图、标题、作者、字数、标签
- `NovelList` 使用 `LazyColumn` 而非瀑布流（小说适合线性列表）
- `RecommendedNovelsViewModel` 调用 `getRecommendedNovels()` API

---

### 11. 浏览历史

#### 用户视角
- 在抽屉菜单中进入「浏览历史」
- **三 Tab 页面**（HorizontalPager + TabRow）：
  - **插画 Tab**：展示最近浏览过的插画，瀑布流展示，支持多选批量下载
  - **小说 Tab**：展示最近浏览过的小说，线性列表展示
  - **用户 Tab**：展示最近浏览过的用户主页，使用 `UserHistoryCard` 卡片
- 每个 Tab 独立支持「清空历史」操作（带确认对话框）
- 按时间排序

#### 实现原理

- `BrowseHistoryManager` 使用 Room 数据库记录浏览：
  - 每次进入插画/小说/用户详情页时分别调用对应的 `recordView()` 写入记录
  - 包含 ID、标题、预览图 URL、作者信息、时间戳
  - 重复浏览同一内容时更新时间戳（REPLACE 策略）
  - 启动时异步清理 30 天前的旧记录
- `BrowseHistoryViewModel` 管理三种历史状态：`illustState`、`novelState`、`userState`
- 各 Tab 分别使用 `IllustHistoryPage`、`NovelHistoryPage`、`UserHistoryPage` 组件

---

### 12. 图片下载与下载任务管理

#### 用户视角
- 在插画详情页点击下载按钮保存原图
- 在多选模式下可批量添加下载任务
- 侧边栏「下载历史」查看所有下载任务
- 支持查看下载状态：等待中、下载中、已完成、失败
- 失败任务可点击重试
- App 重启后自动恢复未完成的下载任务

#### 实现原理

**`DownloadTaskManager` — 下载任务队列管理器：**
```
用户多选下载
    → addTasks(illusts)         // 批量添加任务
    → 写入 Room 数据库          // 状态: PENDING
    → processQueue()            // 开始处理队列
        → 更新状态: DOWNLOADING
        → 下载每一页图片（实时更新字节级进度）
        → 更新进度: downloadedPages / totalPages + currentPageProgress
        → 下载完成: COMPLETED
        → 下载失败: FAILED + errorMessage
```

**下载状态枚举：**
| 状态 | 说明 |
|------|------|
| `PENDING` | 等待中，在队列中等待下载 |
| `DOWNLOADING` | 下载中，显示进度（单图显示百分比，多图显示页数+进度条） |
| `COMPLETED` | 已完成 |
| `FAILED` | 失败，可重试 |

**下载进度追踪：**
- 单图作品：显示字节级下载进度百分比（0-100%）+ 水平进度条
- 多图作品：显示 `downloadedPages/totalPages` + 整体进度条
- `currentPageProgress` 字段追踪当前页的下载进度
- 进度计算公式：`(downloadedPages + currentPageProgress/100) / totalPages`

**断点续传支持：**
- 多图作品记录 `downloadedPages`，重试时从断点继续
- App 重启时，`DOWNLOADING` 状态的任务自动重置为 `PENDING`

**`ImageDownloader` 立即下载：**
1. 使用独立的 OkHttpClient 下载原图（添加 Referer 头）
2. 解码为 Bitmap
3. 通过 `MediaStore` API（Android 10+）或直接文件写入保存到相册
4. 保存路径：`Pictures/JCStaff/pixiv_{illustId}.jpg`

**缓存优先：**
- `LoadTaskManager` 管理原图缓存
- 如果原图已通过图片查看器下载过，直接从缓存复制到相册（瞬间完成）

---

### 12.1 下载管理器架构详解

#### 架构设计

```
DownloadTaskManager (单例)
├── 队列管理 (Task Queue)
├── 并发控制 (Mutex + Coroutines)
├── 数据持久化 (Room Database)
├── 进度追踪 (StateFlow)
└── 生命周期管理 (重启恢复)
```

#### 关键文件

| 功能模块 | 文件路径 |
|---------|--------|
| 下载任务管理器 | `core/DownloadTaskManager.kt` |
| 图片下载器 | `core/ImageDownloader.kt` |
| 下载任务实体 | `cache/DownloadHistoryEntity.kt` |
| 数据库 | `cache/AppDatabase.kt` |
| 下载历史 ViewModel | `history/DownloadHistoryViewModel.kt` |
| 下载历史 UI | `screens/DownloadHistoryScreen.kt` |
| 加载任务管理器 | `core/LoadTaskManager.kt` |
| 进度追踪管理器 | `core/ProgressImageLoader.kt` |
| 渐进式图片组件 | `components/ProgressiveImage.kt` |

#### 进度追踪机制（三层级）

| 层级 | 字段 | 精度 | 说明 |
|------|------|------|------|
| 字节级 | `currentPageProgress: Int` | 0-100 | 当前页正在下载的字节进度 |
| 页级 | `downloadedPages / totalPages` | 页数 | 已完成的页数统计 |
| 综合 | `progress: Float` | 0.0-1.0 | `(downloadedPages + currentPageProgress/100f) / totalPages` |

#### 核心技术栈

| 技术 | 用途 |
|-----|-----|
| Kotlin Coroutines | 异步任务、后台线程管理 |
| Mutex | 并发控制、互斥访问 |
| StateFlow | 实时数据流、UI 订阅 |
| Room Database | 本地数据持久化 |
| Flow | 流式查询、变更监听 |
| OkHttp3 | HTTP 网络请求、字节流 |
| Jetpack Compose | 声明式 UI |

#### UI 性能优化

**潜在问题：**

| 问题 | 位置 | 影响 |
|------|------|------|
| 频繁数据库更新 | `updateCurrentPageProgress()` | 磁盘 I/O 压力 |
| StateFlow 频繁发射 | `getAllTasksFlow()` | Compose 重组 |
| 整个列表重组 | LazyColumn 无稳定 key | 列表闪烁 |

**已实施的优化：**

1. **LazyColumn 稳定 key** — 使用 `key = { it.illustId }` 避免不必要重组
2. **derivedStateOf 缓存** — 计算属性使用 `derivedStateOf` 减少重组：
   ```kotlin
   val failedCount by remember { derivedStateOf { state.failedCount } }
   val completedCount by remember { derivedStateOf { state.completedCount } }
   val tasks by remember { derivedStateOf { state.tasks } }
   val isEmpty by remember { derivedStateOf { state.isEmpty } }
   ```
3. **并发下载数限制** — `MAX_CONCURRENT_DOWNLOADS = 1`，顺序下载避免资源竞争

**现代手机实际影响：**

对于 2020 年后的中高端设备，当前实现方案性能可接受：
- Room 的 Flow 在 IO 线程执行
- Compose 的 `collectAsState` 自动切换到主线程
- `LinearProgressIndicator` 是轻量组件
- 真正的并发下载数限制为 1 个

**大批量下载（50+ 任务）的额外优化建议：**
- 进度更新节流（每 5% 更新一次）
- 分离"活跃任务"和"历史任务"的数据流
- 使用虚拟列表仅渲染可见项

---

### 13. 多选模式

#### 用户视角
- 长按任意插画卡片进入多选模式
- 顶部出现操作栏，显示已选数量
- 支持全选、批量下载
- 按返回键退出多选

#### 实现原理

- `SelectionManager`（使用 CompositionLocal 全局提供，类似 React useContext）：
  - 通过 `LocalSelectionManager.current` 在任意组件中访问
  - `isSelectionMode` — 是否处于选择模式
  - `selectedIds` — 已选 ID 集合
  - `onLongPress(illust)` — 触发进入选择模式
  - `toggleSelection(illust)` — 切换选中状态
- `SelectionTopBar` 悬浮在页面顶部，显示操作按钮（自动获取 SelectionManager）
- `IllustCard` 在选择模式下显示勾选框覆盖层（自动获取 SelectionManager）

---

### 14. 多语言支持

#### 用户视角
- 首次打开 App 选择显示语言
- 支持：简体中文、繁体中文、英语、日语、韩语
- 语言设置影响 App UI 和 Pixiv API 返回的内容语言

#### 实现原理

- `AppLanguage` 枚举定义所有支持的语言，每种语言包含：
  - `tag` — BCP 47 语言标签（如 `zh-Hans`）
  - `acceptLanguage` — HTTP Accept-Language 头
  - `appAcceptLanguage` — Pixiv 特有的 App-Accept-Language 头
- `LanguageManager` 管理当前语言状态：
  - 启动时从 DataStore 异步读取已保存的语言
  - 通过 `StateFlow` 通知 UI 语言是否已初始化
  - 设置语言时调用 `AppCompatDelegate.setApplicationLocales()` 更新系统 Locale
- `HeaderInterceptor` 在每个 API 请求中注入对应的语言头

---

### 15. 设置页

#### 用户视角
- 切换显示语言（弹窗选择）
- 开关：是否在卡片上显示标题和作者名
- 滑块：卡片圆角大小（0~24dp）
- 开关：是否开启瀑布流间距
- 账号管理入口（跳转至 AccountManagementScreen）
- 查看缓存入口（跳转至 CacheBrowserScreen）
- 退出登录按钮（底部红色按钮，带确认对话框）

#### 实现原理

- `SettingsStore` 使用 DataStore 持久化设置：
  - `showIllustInfo` — 布尔值，控制卡片信息显示
  - `illustCardCornerRadius` — 整数，圆角半径
  - `gridSpacingEnabled` — 布尔值，瀑布流间距
  - `selectedLanguage` — 字符串，全局语言设置
- 每个设置项都是 `Flow`，UI 通过 `collectAsState()` 实时响应变化
- per-user 设置和全局设置（语言）分开存储
- 账号管理通过 `NavRoute.AccountManagement` 导航
- 缓存浏览通过 `NavRoute.CacheBrowser` 导航
- 退出登录通过 `authViewModel.logout()` 回调

---

### 15.1 缓存浏览器 (CacheBrowserScreen)

#### 用户视角
- 从设置页点击「查看缓存」进入
- 文件管理器风格界面，展示 App 内所有目录和文件
- 每个文件夹显示中文描述说明用途（如「临时缓存，可安全删除」）
- 显示文件/文件夹大小、修改时间、子项数量
- 图片文件显示缩略图预览
- 支持进入子目录浏览，系统返回键智能处理：深层目录返回上级，根目录关闭页面
- 支持删除单个文件或文件夹（带确认对话框）
- **一键清理**功能：清理图片和动图缓存，保留浏览历史、账号信息等重要数据

#### 一键清理范围

| 清理 | 保留 |
|------|------|
| `cache/` — 图片库临时缓存 | `databases/` — 浏览历史、API 缓存 |
| `code_cache/` — 代码缓存 | `datastore/` — 用户设置、登录信息 |
| `files/ugoira/` — 动图 GIF 缓存 | `shared_prefs/` — 应用配置 |
| `files/image_load_cache_*/` — 原图缓存 | 其他用户数据 |

#### 实现原理

- `CacheBrowserViewModel` 管理状态（MVVM 架构）
- `LaunchedEffect` 加载目录内容，显示加载转圈
- `calculateDirSize()` 递归计算目录大小
- `getFolderDescriptionResId()` 返回文件夹描述的字符串资源 ID（支持多语言）
- 一键清理使用 `deleteRecursively()` 删除目录，刷新后更新 UI
- `BackHandler` 拦截系统返回键：非根目录时返回上级，根目录时关闭页面

---

### 16. 账号管理页

#### 用户视角
- 从设置页点击「账号管理」进入
- 顶部显示当前活跃账号（头像 + 用户名 + @handle + 勾选标记）
- 下方列出其他已登录账号，点击可快速切换
- 每个非活跃账号右侧有删除按钮，点击弹出确认对话框
- 底部有「添加账号」入口

#### 实现原理

- `AccountManagementScreen` 接收 `allAccounts`、`activeUserId` 等状态
- 使用 `LazyColumn` 展示账号列表，按活跃/非活跃分组（`SectionHeader`）
- `AccountCard` 组件展示头像（Coil AsyncImage）、用户名、@handle
- 删除账号通过 `AlertDialog` 确认后调用 `authViewModel.removeAccount(userId)`
- 切换账号通过 `authViewModel.switchAccount(userId)` 触发全局会话重建

---

### 17. AGSL 着色器演示页

#### 用户视角
- 从首页抽屉菜单中点击「Shader Demo」进入
- 全屏沉浸式展示，左右滑动切换不同着色器效果
- 底部显示效果名称和页面指示器
- 当前包含 5 种效果：
  - **Neon Plasma** — 霓虹等离子体流动效果
  - **Fire Storm** — 火焰风暴效果
  - **Traced Tunnel** — 光线追踪隧道效果（程序生成色彩）
  - **Tunnel (Image)** — 图片隧道效果（448 张 Pixiv 插画图集，每块瓷砖随机展示不同图片，优化后的着色器）
  - **Magic Circle** — 旋转发光魔法阵（多层同心圆、符文标记、六芒星/八芒星几何图案、粒子火花、能量流动）

#### 实现原理

- `ShaderDemoScreen.kt` 包含所有着色器 AGSL 源码（作为 `const val` 字符串常量）
- 使用 `HorizontalPager` 实现左右滑动切换
- `ShaderCanvas` 组件：
  - 创建 `RuntimeShader(shaderSrc)` 实例
  - 通过 `LaunchedEffect` + `withFrameNanos` 驱动时间变量
  - 使用 `ShaderBrush` + `drawBehind` 进行 GPU 加速渲染（不触发 recomposition）
  - 每帧设置 `iResolution` 和 `iTime` uniform 变量
- 着色器算法采用 SDF（Signed Distance Field）技术绘制几何图形
- 需要 Android 13+（`@RequiresApi(Build.VERSION_CODES.TIRAMISU)`）

---

### 18. 网页标签详情页 (WebTagDetailScreen)

#### 用户视角
- 点击热门标签进入该页面（使用 Pixiv Web AJAX API 获取更丰富的数据）
- 采用 Material Design 3 设计规范：
  - **可折叠 Hero Header**：280dp 展开高度，背景图来自 Pixpedia 或热门作品，支持滚动折叠
  - **Styled TabRow**：统一的 Tab 样式，已应用至全项目
    - 使用 `TabRow` 实现全宽度均匀分布
    - 32dp 宽度 `PrimaryIndicator`，顶部 3dp 圆角
    - 选中 Tab 文字加粗（`FontWeight.Bold`）
    - 底部分割线保留
  - **ViewPager 三页**：
    - **百科页**：Pixpedia 简介、多语言翻译、父/同级/子/相关标签
    - **插画页**：顶部热门作品横滑 + 下方最新插画瀑布流
    - **小说页**：小说卡片列表

**折叠效果：**
- 展开高度：280dp
- 折叠高度：状态栏 + 60dp Toolbar
- 折叠时标签名移至顶部栏，底部信息淡出

#### 实现原理

**可折叠头部架构：**
```kotlin
// NestedScrollConnection 处理滚动事件
val nestedScrollConnection = object : NestedScrollConnection {
    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        // 向上滚动折叠，向下滚动展开
        headerOffsetY = (headerOffsetY + delta).coerceIn(-headerHeightRange, 0f)
    }
}
// 折叠进度驱动动画
val collapseProgress = -headerOffsetY / headerHeightRange  // 0=展开, 1=折叠
```

**Web AJAX API 端点：**
- `GET /ajax/search/top/{tag}` — 获取标签综合信息（热门作品、最新插画/小说/漫画）
- `GET /ajax/search/tags/{tag}` — 获取标签详情（Pixpedia、翻译、相关标签）

**并行 API 请求：**
```kotlin
val searchDeferred = async { PixivWebScraper.searchTagTop(tagName) }
val tagInfoDeferred = async { PixivWebScraper.getTagInfo(tagName) }
// 两个请求并行执行，加快加载速度
```

**数据模型转换：**
- `WebIllust.toIllust()` — 将网页插画数据转换为 App 通用的 Illust 模型
- `WebNovel.toNovel()` — 将网页小说数据转换为 App 通用的 Novel 模型

**缓存策略：**
- 通过 `ApiCacheManager` 缓存 Web AJAX 响应
- 缓存有效期 15 分钟，过期后后台刷新

**关键组件：**
- `CollapsibleHeroHeader` — 可折叠背景图头部，带渐变遮罩和统计徽章
- `PixpediaPage` — 百科信息页，展示简介和相关标签
- `IllustMangaPage` — 插画页，热门作品横滑 + 瀑布流
- `NovelPage` — 小说页，卡片式列表

---

### 19. 热门标签网格

#### 用户视角
- 在首页发现 Tab 中以网格形式展示热门标签
- 首个标签全宽展示，其余 3 列排列
- 每个标签卡片显示标签名称、翻译名和预览图
- 采用 1dp 间距、直角边框的紧凑设计
- 点击标签进入 WebTagDetail 页面查看该标签下的作品（使用 Web AJAX API）

#### 实现原理

- `TrendingTagsViewModel` 调用 API 获取热门标签列表（插画漫画 + 小说分开）
- 使用 `LazyVerticalGrid` 3 列网格展示，首项 `GridItemSpan(3)` 全宽
- `TrendingTagCard` 组件：正方形比例、底部渐变遮罩、直角边框
- 点击导航至 `NavRoute.WebTagDetail(tag)`（使用更丰富的 Web API 数据）

---

### 19.1 推荐作者列表 (RecommendedUsersList)

#### 用户视角
- 在首页发现 Tab 的「推荐作者」子页面中以列表形式展示推荐用户
- 每张用户卡片采用 Material Design 3 风格：
  - 64dp 头像，Premium 用户有金色渐变扫描边框 + 认证徽章
  - 用户名（titleMedium + Bold）+ @账号（bodySmall）
  - 作品标题预览（最多 3 个，用 · 分隔，primary 颜色）
  - 用户简介（bodySmall，85% 透明度）
  - 关注状态按钮（已关注/关注，使用 MD3 Container 颜色）
  - 作品预览画廊（3 张等宽图片，首尾大圆角，中间小圆角）
  - 每张作品右下角显示收藏数浮层

#### 实现原理

- `UserPreviewCard` 组件：
  - 头像边框：Premium 用户使用 `sweepGradient`（金 → 橙 → 粉 → 金），普通用户使用 `linearGradient`（primary → tertiary）
  - Premium 徽章：20dp 金色圆形 + 白色认证图标
  - 作品标题行：`mapNotNull { it.title }` 提取标题，`joinToString(" · ")` 连接
  - 作品画廊：`Row` + `weight(1f)` 等宽分布，`aspectRatio(1f)` 正方形
  - 收藏数浮层：暗色半透明背景 + 心形图标 + 格式化数字（支持 K/W/M）
- `RecommendedUsersList` 使用 `LazyColumn` + `PullToRefreshBox` 实现
- 支持无限滚动加载更多

---

### 20. 社交信息流 (IllustFeed)

#### 用户视角
- 在首页新作 Tab 的「关注插画漫画」子页面中，以类似 Twitter/微博的信息流形式展示
- 每张卡片纵向排列，包含完整信息：
  - 顶部：作者头像（渐变边框）、用户名、发布时间
  - 中间：作品图片（16:10 圆角）、多图数量徽章
  - 底部：标签流（`#tag` 格式）、操作栏（收藏/分享）、统计信息（收藏数/浏览数）
- 支持下拉刷新和无限滚动加载
- AI 生成作品显示特殊标识

#### 实现原理

- `IllustFeedCard` 组件：
  - 作者头像使用渐变边框（`primary → tertiary`）+ `ContentScale.Crop`
  - 发布时间使用 `formatRelativeDate()` 统一格式化（如「3 小时前」「昨天」）
  - 标签使用 `bodySmall` 字号，`primary.copy(alpha = 0.8f)` 颜色
  - 操作栏使用 `IconButton` + `FilledTonalIconButton`（收藏状态切换）
- `IllustFeed` 组件：
  - 使用 `LazyColumn` 纵向排列 `IllustFeedCard`
  - `PullToRefreshBox` 实现下拉刷新
  - 监听滚动位置，接近底部时触发 `onLoadMore`
  - 底部显示加载更多指示器

---

### 21. Ugoira (动图) 播放器

#### 用户视角
- 在插画详情页中，如果作品是 Ugoira (动图) 类型，会自动显示动图播放器
- 播放器按顺序执行以下步骤，并显示实时进度：
  - **获取元数据**：从 Pixiv API 获取动图信息
  - **下载资源**：下载 ZIP 压缩包，显示下载进度 (0-100%)
  - **解压帧**：解压 ZIP 获取所有帧图片
  - **压制 GIF**：将帧图片编码为 GIF，显示压制进度 (0-100%)
- 压制完成后自动播放 GIF 动画（无限循环）
- GIF 文件会缓存到本地，再次查看时瞬间播放

#### 实现原理

**处理流程：**
```
UgoiraState.FetchingMetadata  → 获取 ugoira_metadata API
UgoiraState.Downloading(progress)  → OkHttp 下载 ZIP，实时更新进度
UgoiraState.Extracting  → 解压 ZIP 到临时目录
UgoiraState.Encoding(progress)  → 逐帧编码 GIF，实时更新进度
UgoiraState.Done(gifFile)  → 使用 Coil GifDecoder 播放
```

**AnimatedGifEncoder — NeuQuant 神经网络颜色量化：**
- 使用 NeuQuant 算法将 24 位真彩色图像量化为 256 色调色板
- 像素数据按 BGR 顺序存储（符合 NeuQuant 期望格式）
- 支持设置量化质量 (1-20)，数值越小质量越高
- LZW 编码压缩输出 GIF 数据流

**进度显示：**
- `UgoiraState.Downloading(progress)` — 按已下载字节数计算百分比
- `UgoiraState.Encoding(progress)` — 按已处理帧数计算百分比
- UI 层使用 `animateFloatAsState` 实现 300ms 平滑过渡动画

**缓存策略：**
- 内存缓存：`gifCache: MutableMap<Long, UgoiraData>`
- 磁盘缓存：`filesDir/ugoira/{illustId}.gif`
- 优先使用缓存，避免重复下载和编码

---

### 22. Spotlight/Pixivision 专题

#### 用户视角
- 在首页抽屉菜单或特定入口访问 Pixiv 官方精选专题
- 列表页展示专题卡片：缩略图 + 渐变遮罩 + 标题 + 分类标签 + 发布日期
- 详情页采用沉浸式设计：
  - 顶部 Hero Header（4:3 比例缩略图 + 渐变遮罩 + 分类标签）
  - 悬浮导航栏（半透明返回按钮 + 浏览器打开按钮）
  - 文章描述和收录作品列表
  - 作品项：用户头像（渐变边框）→ 作品图片（16:10 圆角）→ 作品标题

#### 实现原理

- `SpotlightCard` 组件：
  - 使用 `Box` 叠加图片和渐变遮罩
  - 底部信息区覆盖在图片上（白色文字 + 阴影）
  - 发布日期使用 `formatRelativeDate()` 统一格式化
- `SpotlightDetailScreen` 组件：
  - `LazyColumn` 展示 Hero Header + 文章信息 + 作品列表
  - 悬浮导航栏使用 `windowInsetsPadding(WindowInsets.statusBars)` 适配刘海屏
  - `SpotlightArtworkItem` 展示收录作品，点击跳转插画详情

---

### 23. 评论系统

#### 用户视角
- 在插画详情页和小说详情页底部显示评论预览（最多 3 条）
- 点击「查看全部评论」进入完整评论页面
- 支持发表评论、回复他人评论、删除自己的评论
- 评论中支持 Pixiv 表情（38 种），通过表情选择器插入
- 评论支持查看子回复（展开/折叠）
- 删除评论需确认对话框

#### 实现原理

**评论 API 端点：**
- `GET /v3/illust/comments` — 获取插画评论
- `GET /v3/novel/comments` — 获取小说评论
- `GET /v2/{type}/comment/replies` — 获取回复评论
- `POST /v1/illust/comment/add` — 发表插画评论
- `POST /v1/novel/comment/add` — 发表小说评论
- `POST /v1/{type}/comment/delete` — 删除评论
- `GET next_url` — 评论分页加载

**缓存策略：**
- `CommentViewModel.commentsCache` 使用 `MutableMap<String, List<Comment>>` 缓存评论列表
- 详情页的 `CommentPreviewSection` 预取并缓存第一页评论
- 进入完整评论页时复用缓存，避免重复请求

**表情系统：**
- `EmojiUtils.kt` 定义 38 种 Pixiv 表情，映射名称到 PNG 资源文件
- 表情 URL 格式：`https://s.pximg.net/common/images/emoji/{code}.png`
- `parseCommentWithEmojis()` 使用正则解析评论文本为文字和表情片段
- 使用 Compose `InlineTextContent` 实现文字中内联表情图片

**MD3 设计规范：**
- 评论卡片：40dp 头像 + 用户名 + 相对时间 + 评论内容/贴图
- 子回复：48dp 左缩进、32dp 头像、`surfaceContainerLow` 背景
- 底部输入栏：表情选择器 + 文本框 + 发送按钮
- 表情选择器：6 列网格、48dp 单元格、`surfaceContainerHigh` 背景

---

## 架构深入解析

### 导航系统

不使用 Jetpack Navigation，而是自定义实现：

```kotlin
class NavigationViewModel : ViewModel() {
    val backStack: SnapshotStateList<NavRoute> = mutableStateListOf()

    val currentRoute: NavRoute? get() = backStack.lastOrNull()

    fun navigate(route: NavRoute) { backStack.add(route) }
    fun goBack() { if (backStack.size > 1) backStack.removeLast() }
    fun clearAndNavigate(route: NavRoute) { backStack.clear(); backStack.add(route) }
}
```

**优势：**
- 使用 Compose `SnapshotStateList` 自动触发重组
- 路由是类型安全的 `sealed interface`，参数直接嵌入
- `AnimatedContent` 实现页面切换动画（`fadeIn togetherWith fadeOut`）
- `SaveableStateProvider(route.stableKey)` 保存每个页面的 UI 状态
- 通过 `CompositionLocalProvider` 全局提供 `navViewModel`
- `key(activeUserId)` 在账号切换时强制重建所有页面和 ViewModel

**路由定义：**

| 路由 | 参数 | 说明 |
|------|------|------|
| `Landing` | 无 | 登录引导页（3 页 ViewPager） |
| `Home` | 无 | 首页 |
| `Search` | 无 | 搜索页 |
| `IllustDetail` | illustId, title, previewUrl, aspectRatio | 插画详情 |
| `ImageViewer` | imageUrl, originalUrl, sharedElementKey | 全屏看图 |
| `NovelDetail` | novelId | 小说详情 |
| `TagDetail` | tag (Tag 对象), initialTab | 标签详情（插画+小说双 Tab） |
| `WebTagDetail` | tag (Tag 对象) | 网页标签详情（Web AJAX API） |
| `UserProfile` | userId | 用户主页 |
| `Bookmarks` | userId | 收藏列表 |
| `BrowseHistory` | 无 | 浏览历史（插画+小说+用户三 Tab） |
| `DownloadHistory` | 无 | 下载历史（任务队列管理） |
| `Settings` | 无 | 设置页 |
| `ShaderDemo` | 无 | AGSL 着色器演示 |
| `AccountManagement` | 无 | 账号管理 |
| `CommentDetail` | objectId, objectType | 评论页面（插画/小说共用） |
| `RankingDetail` | objectType | 排行榜详情（插画/漫画） |

---

### 网络层架构

```
┌────────────────────────────────────────────────────┐
│                   PixivApi (Retrofit)               │
│  getRecommendedIllusts / searchIllusts / ...       │
└───────────────────────┬────────────────────────────┘
                        │
┌───────────────────────▼────────────────────────────┐
│                  OkHttp Client                      │
│  ┌──────────────────────────────────────────────┐  │
│  │ Interceptor Chain:                            │  │
│  │  1. ApiCacheInterceptor  → 读/写 Room 缓存    │  │
│  │  2. TokenRefreshInterceptor → 401 自动刷新    │  │
│  │  3. HeaderInterceptor    → 注入认证头/语言头   │  │
│  │  4. HttpLoggingInterceptor → 日志             │  │
│  └──────────────────────────────────────────────┘  │
└───────────────────────┬────────────────────────────┘
                        │
                        ▼
                  Pixiv API Server
```

**ApiCacheInterceptor 工作原理：**
1. 请求发出前，检查 Room 数据库中是否有有效缓存（15 分钟有效期）
2. 如果缓存命中：构造 304 响应直接返回（不发网络请求）
3. 如果缓存未命中或过期：正常发送请求
4. 收到 200 响应后：将响应体存入 Room 缓存

**Stale-While-Revalidate：**
各 ViewModel 使用 `PixivClient.getFromStaleCache()` 先展示过期缓存数据，同时发起网络请求更新。用户看到的是：旧数据立即显示 → 新数据刷入替换。

**通用分页加载：**
使用单一的 `getNextPage(@Url nextUrl: String): ResponseBody` 端点处理所有分页请求，由 `PixivClient.getNextPage(url, clazz)` 解析为具体类型。

---

### PagedDataLoader — 通用分页数据加载器

**设计思想：** 消除各 ViewModel 中重复的分页加载逻辑。

```kotlin
// PagedResponse 接口约束所有分页响应类型
interface PagedResponse<T> {
    val displayList: List<T>
    val nextUrl: String?
}

// 响应类实现接口
data class IllustResponse(
    val illusts: List<Illust>,
    val next_url: String?
) : PagedResponse<Illust> {
    override val displayList get() = illusts
    override val nextUrl get() = next_url
}

// PagedDataLoader 通过泛型约束自动提取数据
class PagedDataLoader<T, R : PagedResponse<T>>(
    private val cacheConfig: CacheConfig?,
    private val responseClass: Class<R>,
    private val loadFirstPage: suspend () -> R,
    private val onItemsLoaded: (List<T>) -> Unit = {}
)
```

**ViewModel 使用示例：**
```kotlin
private val loader = PagedDataLoader(
    cacheConfig = CacheConfig(path = "/v2/illust/follow", ...),
    responseClass = IllustResponse::class.java,
    loadFirstPage = { PixivClient.pixivApi.getFollowingIllusts() },
    onItemsLoaded = { storeIllusts(it) }
)
val state: StateFlow<PagedState<Illust>> = loader.state
```

**优势：**
- 无需传入 `extractItems`、`extractNextUrl`、`loadNextPage` — 全部由接口约束自动推导
- 统一的缓存加载、网络加载、错误处理逻辑
- `PagedState<T>` 提供 `items`、`isLoading`、`isLoadingMore`、`canLoadMore` 等标准状态

**非分页数据：**
对于不需要分页的列表（如热门标签），使用 `SimpleState<T>`：
```kotlin
typealias TrendingIllustTagsState = SimpleState<TrendingTag>
// SimpleState 只有 items、isLoading、error，无分页相关字段
```

**CacheConfig 与 CacheResult — 统一缓存策略：**

所有 ViewModel 共享同一套缓存逻辑，避免重复代码：

```kotlin
// CacheConfig 负责构建 URL 和从缓存加载
data class CacheConfig(val path: String, val queryParams: Map<String, String>) {
    fun buildUrl(): String
    fun buildCacheKey(): String
    suspend fun <T> loadFromCache(clazz: Class<T>): CacheResult<T>?
}

// CacheResult 封装缓存数据，知道自己是否过期
data class CacheResult<T>(val data: T, val timestamp: Long) {
    val isFresh: Boolean  // 15 分钟内为 true
    fun shouldFetch(forceRefresh: Boolean): Boolean
}

// 扩展函数处理 null 情况
fun CacheResult<*>?.shouldFetch(forceRefresh: Boolean): Boolean
```

**ViewModel 使用示例：**
```kotlin
val cacheResult = cacheConfig.loadFromCache(Response::class.java)
if (cacheResult != null) { /* 展示缓存 */ }
if (!cacheResult.shouldFetch(forceRefresh)) return@launch
// 发网络请求
```

这种设计将缓存逻辑内聚到数据类本身，无需单例或工具类。

---

### ObjectStore — 全局响应式对象池

**设计思想：** 解决列表页和详情页数据不同步的问题。

```
                    ┌─────────────────────────┐
                    │     ObjectStore          │
                    │  ConcurrentHashMap<      │
推荐列表 ──put()──→ │    StoreKey,             │ ←──get()── 详情页
热门列表 ──put()──→ │    MutableStateFlow<     │ ←──get()── 相关作品
搜索结果 ──put()──→ │      Storable>           │ ←──get()── 用户主页
                    │  >                       │
                    └─────────────────────────┘
                              │
                              │ StateFlow 自动通知
                              ▼
                    所有订阅者的 UI 自动更新
```

**场景举例：** 在插画详情页收藏了作品 → ObjectStore 中对应的 StateFlow 更新 → 返回推荐列表后，该作品的卡片上收藏图标已同步变化。

---

### API 缓存系统

**两层缓存架构：**

| 层次 | 存储 | 有效期 | 用途 |
|------|------|--------|------|
| L1 — ObjectStore | 内存 (ConcurrentHashMap) | 进程生命周期 | 页面间数据共享 |
| L2 — ApiCacheManager | Room 数据库 | 15 分钟 | API 响应级别缓存 |

**ApiCacheManager 特性：**
- 最大 100 条缓存，超出时 LRU 淘汰最旧的 10 条
- 启动时异步清理过期条目
- 支持强制失效（用于下拉刷新等场景）
- per-user 隔离：每个账号独立的数据库文件

---

### AGSL 着色器系统

项目在多处使用 Android 13+ 的 AGSL (Android Graphics Shading Language) 实现 GPU 加速的动态视觉效果：

#### ShaderBackground.kt — 动态背景组件

**`AnimatedShaderBackground`：**
1. **Domain Warping FBM（分形布朗运动域扭曲）：**
   - 5 层 octave 的 FBM 噪声
   - 两级域扭曲：`q = fbm(uv)` → `r = fbm(uv + q)` → `f = fbm(uv + r)`
   - 产生有机、流动的纹理图案

2. **动态色板：**
   - 5 个颜色通道以不同周期正弦变化（14~21 秒）
   - 颜色从深靛蓝、紫罗兰到翡翠、珊瑚不断渐变
   - 基于噪声值混合不同颜色层

3. **示波器波纹：**
   - 两条螺旋缠绕的波纹线（青蓝色 + 品红色）
   - 主螺旋 + 多次谐波叠加
   - 使用 `smoothstep` 实现线条发光效果

**`TracedTunnelBackground`：** 光线追踪隧道效果（程序生成色彩瓷砖）。接受 `content` lambda 参数，可在着色器上层叠加 UI 内容。

**`TracedTunnelImageBackground`：** 图片隧道效果，用于 Landing Screen 的沉浸式动态背景。从 `assets/prime_square/` 加载 448 张 Pixiv 正方形插画，创建 28×16 图集纹理（200×200 每块，双线性滤波缩放），每块瓷砖通过哈希函数随机映射到不同图片。图集在后台线程异步加载，加载期间显示居中转圈指示器，加载完成后淡入显示着色器。着色器经过优化：内联光线求交、预计算常量、移除冗余计算，确保流畅渲染。

#### ShaderDemoScreen.kt — 着色器演示页

包含 5 种独立的 AGSL 着色器效果：
- **Neon Plasma**：正弦波叠加产生的霓虹等离子体色彩流动
- **Fire Storm**：基于噪声的火焰模拟效果
- **Traced Tunnel**：复用 `SHADER_TRACED_TUNNEL` 的隧道穿行效果（程序生成色彩瓷砖）
- **Tunnel (Image)**：图片隧道效果，从 448 张 Pixiv 正方形插画中随机选取，创建 28×16 图集纹理（200×200 每块，双线性滤波缩放），每块瓷砖通过哈希函数随机映射到不同图片。图集在后台线程异步加载，加载期间显示居中转圈指示器，加载完成后淡入显示着色器。着色器经过性能优化：内联光线-平面求交、预计算 sin/cos、移除高光计算、避免 mod() 函数。
- **Magic Circle**：多层旋转魔法阵，使用 SDF 绘制同心圆环、六芒星/八芒星几何、符文标记，配合粒子火花和能量流动

另外还包含备用着色器常量（Voronoi、Aurora、Galaxy、Sakura Card 等），未在入口列表中启用。

**降级方案：** Android 13 以下设备使用静态渐变背景作为兜底。

**性能优化：** 时间变量仅在 `drawBehind` 绘制阶段读取，不触发 recomposition。

---

### 启动流程

```
onCreate()
    ├── installSplashScreen()       // 安装系统级 SplashScreen（必须在 super.onCreate() 之前）
    │   └── setKeepOnScreenCondition { authState == Loading }  // 持续显示直到认证完成
    │
    ├── enableEdgeToEdge()          // 沉浸式状态栏
    ├── Coil.setImageLoader(...)    // 全局图片加载器（添加 Referer 头）
    ├── SettingsStore.initialize()  // 创建 DataStore 实例（不读盘）
    ├── authViewModel               // 触发 AuthViewModel 创建
    │   └── init {
    │       ├── AccountRegistry.initialize()    // 同步
    │       ├── launch(IO) {                    // 异步
    │       │   ├── AccountMigration.migrateIfNeeded()  // 首次升级迁移
    │       │   ├── AccountRegistry.getActiveUserId()   // DataStore 读取
    │       │   ├── _activeUserId.value = activeId      // 提前设置
    │       │   ├── AccountSessionManager.initializeAuth()  // Token 加载
    │       │   ├── _authState.value = Authenticated    // 设置认证状态
    │       │   └── launch { initializeServices() }     // 后台初始化 DB
    │       └── }
    │
    ├── launch(IO) {               // 并行: 语言初始化
    │   ├── SettingsStore.selectedLanguage.first()
    │   └── LanguageManager.initialize(savedTag)
    │   }
    │
    └── setContent {
        └── JCStaffTheme {
            ├── !isInitialized → 空白（语言加载中）
            └── isInitialized → AppNavigation()
                ├── authState == Loading → 转圈（认证中）
                ├── authState == Authenticated → HomeScreen
                └── authState == NotAuthenticated → LandingScreen
                    ├── Page 0: 欢迎页（Traced Tunnel 背景）
                    ├── Page 1: 语言选择
                    └── Page 2: 登录轮播（OnboardScreen）
        }
    }
```

**冷启动优化：** 使用 Android SplashScreen API (`androidx.core:core-splashscreen`) 解决 Compose 首次组合时 Loading 动画卡顿问题：
- 系统级 SplashScreen 在独立窗口渲染，不受主线程阻塞影响
- `setKeepOnScreenCondition` 控制 SplashScreen 持续显示到认证状态加载完成
- 支持亮色/暗色模式自动切换背景色（白色 `#FFFFFF` / 深灰 `#121212`）
- 兼容 Android 12 以下设备（通过 compat 库降级处理）

**并行优化：** 语言初始化和认证初始化同时进行，总启动时间 = max(语言加载, 认证加载)。

---

### 数据持久化一览

| 存储类型 | 文件/位置 | 内容 | 生命周期 |
|---------|----------|------|---------|
| DataStore | `account_registry` | 账号列表、活跃 ID、迁移标记 | 全局 |
| DataStore | `settings_global` | 语言设置 | 全局 |
| DataStore | `auth_prefs_{userId}` | Access Token、Refresh Token | per-user |
| DataStore | `settings_{userId}` | 用户偏好设置 | per-user |
| Room DB | `jcstaff_db_{userId}` | API 缓存、浏览历史 | per-user |
| 文件 | `image_load_cache_{userId}/` | 原图缓存文件 | per-user |

---

## 项目统计

| 指标 | 数值 |
|------|------|
| Kotlin 源文件数 | 111 |
| 页面数 | 19 |
| ViewModel 数 | 18+ |
| API 接口数 | 30+ |
| Web AJAX 接口数 | 2 (标签搜索、标签详情) |
| 支持语言 | 5 (中/繁中/英/日/韩)，完整翻译覆盖 |
| Room Entity | 6 (ApiCache, BrowseHistory, NovelBrowseHistory, UserBrowseHistory, SearchHistory, DownloadTask) |
| OkHttp Interceptor | 4 |
| AGSL 着色器效果 | 5 (Neon Plasma, Fire Storm, Traced Tunnel, Tunnel Image, Magic Circle) |
| 导航路由数 | 18 |
| 首页子页面数 | 10 (3 Tab × 内嵌 ViewPager) |
