# HANiEX 技术文档

本文档面向维护者和贡献者，描述当前项目的技术架构、关键模块、数据流和开发约定。用户侧介绍请看 [README.md](README.md)（中文）或 [README-en.md](README-en.md)（English）。

## 1. 技术概览

HANiEX 是以 Kotlin + Jetpack Compose 为主的 Android 应用，整体采用 MVVM 风格组织，并在同一个应用壳内集成多个站点模块（见第 3 章）。

核心技术（版本以 `gradle/libs.versions.toml` 为准）：

- Kotlin 2.4.x，Java 21。
- Android Gradle Plugin 9.1.0。
- Jetpack Compose（BOM 2026.08.00）+ Material 3（1.5.0-beta02）。
- Navigation Compose 2.9.x typed routes。
- ViewModel + StateFlow + SharedFlow。
- Retrofit 3 + OkHttp 5 + kotlinx.serialization converter。
- Jsoup 解析 HTML DOM。
- Room 2.8 + KSP。
- WorkManager 后台下载和更新任务。
- Coil 3 Compose 图片加载。
- Media3 ExoPlayer（含 HLS）、MPV Android 播放链路。
- Firebase Analytics、Crashlytics、Performance、Remote Config、Realtime Database。
- Shizuku API / Provider（静默安装更新）、Apache Commons Compress（解压增量补丁）、Security Crypto（站点凭据加密存储）。

当前主业务页面与播放器均为 Compose 实现，不再保留 JZVD 或其它播放器相关 View/XML 组件；仓库中仅剩检查打卡桌面小组件使用 XML 布局（`res/layout/checkin_widget.xml`），ViewBinding / DataBinding 未启用。

## 2. 模块结构

Gradle 模块：

- `:app`：主应用模块，也是 `settings.gradle.kts` 中唯一包含的模块。包含 UI、业务状态、网络解析、本地数据库、播放器、下载、更新、资源以及全部站点模块。
- `buildSrc`：构建辅助逻辑，维护版本号、构建来源、提交 SHA 等 Gradle 侧配置（`Config.kt`）。
- `yenaly_libs/`：旧通用库遗留的目录，没有构建脚本、未被引入、也没有任何代码 import，不参与构建，可安全删除。

应用包 `app/src/main/java/com/yenaly/han1meviewer/` 的关键目录：

```text
├── SitesHub/<站点>/       全部站点模块：Hanime1（logic / ui）、MissAV、HentaiMama、OnAir、Strawberry、FapFolder、SupJav、RouVideo、XHamster、XXXTube、FullVideos、FuckingSession、Wumaobi、RedGifs、Coomer、LeakGallery、Buondua、Cosplaytele、Coser（结构见第 3 章）
├── logic/                 跨站点的网络、数据库、状态、备份等基础能力（Hanime 的 NetworkRepo / Parser 位于 SitesHub/Hanime1/logic）
│   ├── network/           Retrofit Service、OkHttp、DNS/DoH、Cookie、代理、站点网络配置、更新（HUpdater / HDeltaUpdater）
│   ├── update/            UpdateManager（更新状态机）
│   ├── model/             业务模型和解析后的页面模型（含 github/ 更新相关模型）
│   ├── dao/               Room DAO 和 Database
│   ├── entity/            Room 实体和本地实体
│   ├── state/             WebsiteState、PageLoadingState、VideoLoadingState、PageState、DownloadState
│   ├── exception/         业务异常
│   ├── local/             LocalPlaylistStore（跨站点本地播放列表）
│   ├── DatabaseRepo.kt    本地仓库入口
│   ├── (SitesHub/Hanime1/logic/NetworkRepo.kt、Parser.kt：Hanime 网络仓库与 HTML/JSON 解析入口)
│   ├── BackupManager.kt   备份 / 恢复
│   └── SiteCheckIn.kt     按站点的打卡记录
├── ui/
│   ├── activity/          MainActivity、登录、Cloudflare、手动 Cookie 页面
│   ├── navigation/        UnifiedMainNavHost 所在的 main/、设置导航 settings/、站点 Hub 路由 site/，以及 NavigationManager、CrossSiteNavigator
│   ├── screen/            Compose 页面（main、home、search、video、settings、account、history、library、login、site、web）
│   ├── component/         Compose 组件
│   ├── viewmodel/         页面 ViewModel
│   ├── player/            Compose 播放器（内核抽象、控制器、Surface、控件）
│   ├── bridge/            VideoPageHost（Activity 与视频页的桥接）
│   ├── widget/            打卡桌面小组件
│   └── theme/             Compose 主题、颜色、尺寸
├── util/                  文件、网络、权限、主题、Cookie、翻译、站点下载、Bspatch 等工具
├── worker/                下载 Worker、更新 Worker、下载管理器、HLS 下载
├── shizuku/               Shizuku 更新安装器和 ShellInterface（配合 src/main/aidl）
├── base/                  BaseActivity / BaseApplication / BaseViewModel / AppLocale
├── Preferences.kt         SiteType 枚举与全局偏好
└── HanimeApplication.kt   应用入口
```

## 3. 多站点架构

### 3.1 站点类型与入口

`Preferences.kt` 中的 `SiteType` 枚举列出全部站点（共 20 个）：`HANIME`、`MISSAV`、`HENTAIMAMA`、`JAVCHU`、`ONAIR`、`FAPFOLDER`、`SUPJAV`、`ROUVIDEO`、`XHAMSTER`、`BUONDUA`、`COSPLAYTELE`、`COSER`、`FULLVIDEOS`、`FUCKINGSESSION`、`REDGIFS`、`WUMAOBI`、`XXXTUBE`、`COOMER`、`LEAKGALLERY`、`STRAWBERRY`。新增值只能追加在末尾（枚举按名称保存）。当前站点保存在偏好 `site_type`（`Preferences.siteType`），默认 `HANIME`；可通过 `Preferences.disabledSites` / `setSiteEnabled` 隐藏站点（当前所在站点不能被隐藏）。

- `HANIME` 与 `JAVCHU` 属于同一「站点家族」（`CrossSiteNavigator.isSameSiteFamily`），共用 `SitesHub/Hanime1` 下的 Hanime 实现，仅基础域名不同。
- 其余站点各自拥有独立的包，全部位于 `app/src/main/java/com/yenaly/han1meviewer/SitesHub/<Site>/`。
- 站点切换入口：`ui/component/SiteSwitchDialog.kt`（`DefaultSiteSwitchOptions` 列出名称、描述、Logo 和主题色）和 `ui/screen/main/SiteDrawer.kt`（站点侧栏条目按站点和登录状态生成）。

### 3.2 站点包结构

每个站点包（`SitesHub/<Site>/`）大致遵循同一布局（个别站点会缺少部分目录）：

```text
<Site>/
├── common/        常量、URL 工具、站点设置、图片加载器（如 HentaiMamaImageLoader）
├── cloudflare/    该站点的 Cloudflare 验证页面 / 拦截（部分站点没有）
├── data/
│   ├── remote/    Retrofit Service、<Site>Network、CookieJar、Repo / NetworkRepo、认证
│   ├── parser/    HTML / JSON 解析
│   ├── model/     站点领域模型
│   ├── local/     Room 数据库、历史 DAO / Entity（部分站点还有搜索历史、系列等）
│   └── repository/ 站点仓库（部分站点）
├── ui/            home / search / video / history / navigation / settings ... 各站点页面
└── viewmodel/     站点 ViewModel（部分站点）
```

站点特有部分：

- Strawberry：OnAir 的同构站点（`announcement/auth/chat/gate/github/picture/tag/user` 等页面一致），使用自己的 `StrawberryConstants`、`StrawberryHistoryRepo` 与 Cookie；onion 地址在网络设置中配置，切换前同样走 OnAir 的 Tor 出口检测（`CrossSiteNavigator.validate`）。
- XXXTube / FullVideos / FuckingSession：无登录的纯视频站，直链或各自的解析器播放，带 Cloudflare 处理；FullVideos 额外有「分页（pages）」底部标签页。
- Wumaobi：账号 / 登录与收藏（`WumaobiAuth`），多镜像域名。
- RedGifs：GIF / 短视频，包含信息流、创作者页、竖屏 shorts 播放器、账号。
- Coomer / LeakGallery：创作者 + 帖子 + 媒体结构；LeakGallery 使用带 `x-app-token` 的 JSON API（token 取自页面内嵌的 `ng-state`，401/403 时刷新一次），并含 shorts。
- Cosplaytele / Coser / Buondua：图集和图片查看为主，图片下载走 `SiteImageSaver`。
- OnAir：`OnAirTorChecker` 检测是否通过 Tor 出口，`OnAirAuthRepo` / `OnAirChatRepo` / `OnAirMirrorStatusRepo`，`OnAirEndpointCodec` 解码并校验 onion 端点（主机名必须符合 onion 格式）。
- FapFolder：登录、`FapFolderSecurePrefs`（EncryptedSharedPreferences，创建失败时回退为普通私有偏好）、`FapFolderSessionGuard.kt`（内含自动登录凭据存储 `FapFolderCredentialStore`，仅在加密存储可用时才保存）、`FapFolderFeedPaging`（信息流分页），页面覆盖帖子、图集、小组、话题、个人主页、关注、收藏。
- HentaiMama：额外的系列库（`HentaiMamaSeriesEntity`）、播放列表和 `HentaiMamaOptions`（assets 中的 `hentaimama_options/`）。
- MissAV：账号与认证（`MissAvAccountRepo`、`MissAvAuth`）、`MissAvTags` 和 `missav_options/` assets。
- SupJav / XHamster：独立的 `StreamResolver` 解析播放地址。
- RouVideo：账号 / 登录、`RouVideoPngWrap`（识别并解包被包装成 PNG 外观的数据）。
- Buondua：图集为主，`BuonduaDownloads` 处理图片下载。

### 3.3 导航

- `ui/navigation/main/UnifiedMainNavHost.kt` 读取 `Preferences.siteType`，用 `key(siteChangeKey)` 包裹，并 `when (siteType)` 选择 `MainNavHost`（Hanime / Javchu）或 `MissAvNavHost`、`HentaiMamaNavHost`、`OnAirNavHost`、`StrawberryNavHost`、`FapFolderNavHost`、`SupJavNavHost`、`RouVideoNavHost`、`XHamsterNavHost`、`BuonduaNavHost`、`CosplayteleNavHost`、`CoserNavHost`、`FullVideosNavHost`、`FuckingSessionNavHost`、`RedGifsNavHost`、`WumaobiNavHost`、`XXXTubeNavHost`、`CoomerNavHost`、`LeakGalleryNavHost`。
- 每个站点的 NavHost 位于 `<Site>/ui/navigation/`，通常配有 `<Site>HubConfig.kt`。
- `NavigationManager`（单例）持有当前 `NavController` 和站点类型：`initialize`、`navigateToVideo`、`navigateToSearch`、`navigateToHome`、`navigateBack`、`clearBackStack`、`switchSite(newSite, thenOpen)`。切换站点后会等待导航图就绪（`isNavigationReady` / `isAtHome` 轮询）再执行待打开的视频。
- `CrossSiteNavigator` 负责「在另一个站点打开视频」：`PendingVideoOpen`（站点、videoCode、路径、续播位置、标题、是否自动播放）、`needsSwitch`、`validate(site)`（OnAir / Strawberry 检查 Tor 出口，其他站点检查可达性）、`siteLabel`。
- `ui/component/CrossSiteOpenDialog.kt`、`CrossSiteSearch.kt` 和 `util/ClipboardVideoLinkGuard.kt` 配合剪贴板链接识别与跨站搜索。

### 3.4 站点 Hub 页

订阅、资料库、个人中心、自定义四个站点级页面由 `ui/screen/site/SiteHubRegistration.kt` 统一渲染：

- `SiteHubConfig` 描述一个站点在各 Hub 页的分区和入口（`subscriptionSections`、`librarySections`、`profileQuickActions`、`profileSections`、`customizeSections`、`customizeExtras`）。
- `registerSiteHubRoutes(navController, site, config)` 在站点 NavHost 中注册 `SiteSubscriptionRoute`、`SiteLibraryRoute`、`SiteProfileRoute`、`SiteCustomizeRoute`（定义在 `ui/navigation/site/SiteHubRoutes.kt`）。
- `rememberCommonProfileEntries` 提供所有站点共有的入口：站点设置、打卡、切换站点、自定义、全部历史、离线播放列表、下载。
- `standardSiteHubConfig` 给出标准配置，站点只需补充自己特有的入口。

### 3.5 网络配置

站点级网络配置集中在 `logic/network`：

- `SiteNetworks.profiles`：每个站点一个 `SiteNetworkProfile`（站点类型、显示名、镜像目录 key、默认 URL 列表、是否带结尾斜杠、是否套 Cloudflare）。`cloudflareFronted` 的站点会使用内置 `CLOUDFLARE_IPS`。
- `SiteMirrorCatalog`：内置的镜像目录（`HubSite`：key、名称、入口路径、主机列表、状态页 URL）。
- `SiteNetworkPrefs`：当前选中的镜像、自定义镜像（启用开关、URL、是否追加路径）、`baseUrl(site)`、`knownHosts`。`Preferences.missAvBaseUrl` 等属性最终都委托给它。
- `MirrorLatency`：镜像延迟探测，对应设置页的 `MirrorLatencyDialog`。
- `DohConfig`：DNS-over-HTTPS 预设。

### 3.6 站点级数据与偏好

- 历史：每个站点一个 Room 数据库（见第 11 章），`ui/screen/history/UnifiedHistoryScreen.kt` 汇总展示。
- Cookie：各站点有自己的 `CookieJar`；Hanime 使用 `HCookieJar`。
- 打卡：`SiteCheckIn` 按站点前缀（如 `missav`、`hentaimama`）区分记录，`HANIME` / `JAVCHU` 使用无前缀的默认记录。
- 下载：`util/SiteDownloads.kt` 提供 `DownloadLayout`（目录布局）、`SiteDownloadClients`、`SiteImageSaver`、`DownloadFailureStore`（失败分类 TOR / COOKIES / NETWORK / STORAGE / EXPIRED / OTHER）。
- 备份：`BackupManager` 为每个站点的历史和 Cookie 提供独立分区（见第 17 章）。

### 3.7 新增站点清单

新增一个站点时通常需要：

1. 在 `SiteType`（`Preferences.kt`）末尾加入枚举值，并补齐所有 `when (siteType)` 分支（编译器会提示）。
2. 创建 `SitesHub/<Site>/` 包，按 3.2 的布局建立 `common`、`data/remote`、`data/parser`、`data/local`、`ui`。
3. 在 `SiteNetworks.profiles` 中加入 `SiteNetworkProfile`，需要镜像目录时在 `SiteMirrorCatalog` 中加入 `HubSite`。
4. 实现 NavHost 与 `HubConfig`，并在 `UnifiedMainNavHost` 的 `when` 中接入。
5. 在 `SiteSwitchDialog.kt` 的 `DefaultSiteSwitchOptions` 加入条目（名称、描述、Logo、主题色），并在 `CrossSiteNavigator.siteLabel`、`SiteDrawer.siteDrawerSections`、`NavigationManager` 的各 `when` 中补充。
6. 需要历史记录时建立 Room 数据库，并接入 `UnifiedHistoryScreen`、`SiteCheckIn`、`BackupManager`。
7. 需要 Cloudflare 时复用 `util/cloudflare` 下的基类和拦截器。
更完整、带逐文件清单的流程见仓库根目录的 `add.md`。
8. 补全四套语言的字符串资源（`values`、`values-en`、`values-ja`、`values-zh-rCN`）。

## 4. 分层职责

### UI 层

UI 层主要由 Compose 页面和组件组成。以下以 Hanime1 为例；各站点在自己的 `<Site>/ui/` 下遵循相同的原则。

关键职责：

- 只消费 ViewModel 暴露的状态。
- 通过回调触发导航和业务动作。
- 不直接访问 Retrofit Service 或 Room DAO。
- `LazyColumn`、`LazyRow`、`LazyVerticalGrid` 使用稳定 key 时，数据源必须提前保证唯一。

典型文件：

- `ui/screen/home/homepage/HomePageScreen.kt`
- `ui/screen/search/SearchScreen.kt`
- `ui/screen/video/VideoIntroductionScreen.kt`
- `ui/screen/video/VideoRouteHostScreen.kt`
- `ui/screen/home/DownloadScreen.kt`
- `ui/screen/home/myplaylist/MyPlayListScreen.kt`
- `ui/component/VideoCardItem.kt`
- `ui/component/lazy/AnimatedLazy.kt`

### ViewModel 层

ViewModel 是页面状态和业务动作的入口。

约定：

- 持久页面状态使用 `MutableStateFlow`。
- 一次性事件使用 `MutableSharedFlow` 或 UI 回调。
- 不让 Compose 页面直接处理分页合并、去重、登录态判断等业务细节。
- 网络列表分页追加时应按稳定业务键去重，例如 `videoCode`、`listCode`、`id`。
- 登录用户相关页面要先检查 `Preferences.isAlreadyLogin` 和 `Preferences.savedUserId`。

典型文件：

- `ui/screen/home/homepage/HomePageViewModel.kt`
- `ui/viewmodel/AppViewModel.kt`
- `ui/viewmodel/SearchViewModel.kt`
- `ui/viewmodel/VideoViewModel.kt`
- `ui/viewmodel/CommentViewModel.kt`
- `ui/viewmodel/DownloadViewModel.kt`
- `ui/viewmodel/MyPlayListViewModelV2.kt`
- `ui/viewmodel/OnlineWatchHistoryViewModel.kt`
- `ui/viewmodel/CreatorCenterViewModel.kt`
- `ui/viewmodel/UserAccountViewModel.kt`

### 数据层

数据层由 `NetworkRepo`、`DatabaseRepo`、`Parser`、Retrofit Service 和 Room DAO 组成；其他站点在各自的 `data/` 下有对应的 Repo、Service、Parser 和 DAO。

网络数据流：

```text
ViewModel -> NetworkRepo -> HanimeNetwork Service -> Parser -> WebsiteState/PageLoadingState/VideoLoadingState -> ViewModel
```

本地数据流：

```text
ViewModel/Worker -> DatabaseRepo -> Room DAO -> Flow / suspend result -> ViewModel/UI
```

关键文件：

- `logic/NetworkRepo.kt`
- `logic/DatabaseRepo.kt`
- `logic/Parser.kt`
- `logic/network/HanimeNetwork.kt`
- `logic/network/ServiceCreator.kt`
- `logic/network/service/HanimeBaseService.kt`
- `logic/network/service/HanimeMyListService.kt`
- `logic/network/service/HanimeCommentService.kt`
- `logic/dao/HistoryDatabase.kt`
- `logic/dao/DownloadDatabase.kt`
- `logic/dao/MiscellanyDatabase.kt`
- `logic/dao/CheckInRecordDatabase.kt`

## 5. 状态模型

项目使用以下状态包装类型（`logic/state/`）：

- `WebsiteState<T>`：普通页面或操作状态，包含 Loading、Success、Error。
- `PageLoadingState<T>`：分页列表状态，包含 Loading、Success、NoMoreData、Error。
- `VideoLoadingState<T>`：视频详情状态，处理视频不存在、解析失败、加载中和成功状态。
- `PageState<T>`：较新的分页状态（Loading、Success、NoMoreData、Empty、Error），Success 携带 `isNextPageLoading` / `isRefreshing`，Error 可携带 `cachedInfo` 以便保留旧数据；提供 `hasData` / `dataOrNull` 扩展。目前仅少数页面使用，其余仍以 `WebsiteState` / `PageLoadingState` 为主。
- `DownloadState`：下载任务状态。

使用建议：

- 首页、订阅、账号、修改操作等非分页数据使用 `WebsiteState`。
- 搜索、收藏、稍后观看、播放列表、在线历史、创作中心列表使用 `PageLoadingState`。
- 视频详情使用 `VideoLoadingState`。
- UI 层不要根据异常类型硬编码网络逻辑，异常映射应尽量在 `NetworkRepo` 或 Parser 侧完成。

## 6. 导航架构

主导航基于 Navigation Compose typed routes。多站点场景下，入口为 `UnifiedMainNavHost`（见 3.3），下文的 `MainRoutes` / `MainNavHost` 指 Hanime1 / Javchu 的导航图。

关键文件：

- `ui/navigation/main/UnifiedMainNavHost.kt`
- `ui/navigation/NavigationManager.kt`
- `ui/navigation/CrossSiteNavigator.kt`
- `ui/navigation/main/MainRoutes.kt`
- `ui/navigation/main/MainNavHost.kt`
- `ui/navigation/main/MainNavigationActions.kt`
- `ui/navigation/NavControllerExt.kt`
- `ui/navigation/settings/SettingsRoutes.kt`
- `ui/navigation/settings/SettingsNavHost.kt`
- `ui/navigation/settings/GlobalSettingsGraph.kt`
- `ui/navigation/site/SiteHubRoutes.kt`

路由示例：

```kotlin
@Serializable
data class SearchRoute(
    val query: String? = null,
    val advancedSearchJson: String? = null,
)

@Serializable
data class VideoRoute(
    val videoCode: String,
    val localUri: String? = null,
)
```

导航约定：

- 普通跳转使用 `navController.navigateSafely(...)`，避免快速点击重复入栈。
- 页面间只传递路由必要参数，不传大对象。
- 复杂搜索参数通过 JSON 字符串承载，进入 `SearchRouteScreen` 后再灌入 `SearchViewModel`。
- 视频本地文件播放使用 `VideoRoute(videoCode = "-1", localUri = uri)` 这一路径。

## 7. 搜索和分页列表

搜索入口：

- 首页搜索框进入 `SearchRoute(query = keyword)`。
- 标签点击进入 `SearchRoute(query = tag)`。
- 高级搜索进入 `SearchRoute(advancedSearchJson = json)`。
- 详情页作者区域会同时传入 `query = artist.name` 和高级搜索 JSON，以保证搜索框填充并自动搜索。

`SearchScreen` 支持：

- `initialQuery` 自动填充。
- 初始关键词自动搜索。
- 高级搜索条件自动搜索。
- 搜索历史建议。
- 下拉刷新和加载更多。

分页列表去重规则：

- 视频列表按 `HanimeInfo.videoCode` 去重。
- 播放列表按 `Playlists.Playlist.listCode` 去重。
- 上传中/已上传列表按各自 `videoCode` 去重。
- 评论列表使用评论模型提供的 `stableKey`。

原因：Compose `Lazy*` 使用 `key = { ... }` 时，数据源中出现重复 key 会直接抛出：

```text
java.lang.IllegalArgumentException: Key "xxxx" was already used.
```

因此分页合并应优先在 ViewModel 层处理：

```kotlin
(previous + incoming).distinctBy(HanimeInfo::videoCode)
```

## 8. 视频页架构

Hanime1 的视频页由路由、Host、播放器、Tab 内容和详情内容协作完成（其他站点的视频页位于各自的 `<Site>/ui/video/`，复用 `ui/player` 与 `ui/component/player`）。

关键文件：

- `ui/navigation/main/VideoRoute.kt`
- `ui/screen/video/VideoRouteHostScreen.kt`
- `ui/screen/video/VideoCollapsingHeader.kt`
- `ui/screen/video/VideoRouteContent.kt`
- `ui/screen/video/VideoTabsContent.kt`
- `ui/screen/video/VideoIntroductionScreen.kt`
- `ui/screen/video/CommentScreen.kt`
- `ui/screen/video/ChildCommentScreen.kt`
- `ui/screen/video/VideoRouteActions.kt`
- `ui/viewmodel/VideoViewModel.kt`

主要状态：

- `hanimeVideoStateFlow`：视频加载状态。
- `hanimeVideoFlow`：当前视频数据。
- `videoHostUiStateFlow`：Tab、AppBar、PIP、播放器高度、评论角标等 UI 状态。
- `videoIntroUiStateMap`：按 `videoCode` 缓存详情页滚动位置、系列横向列表位置和已恢复数据。

视频详情加载：

```text
VideoRoute -> VideoViewModel.getHanimeVideo -> NetworkRepo.getHanimeVideo -> Parser.hanimeVideoVer2 -> HanimeVideo
```

本地播放：

```text
VideoRoute(videoCode = "-1", localUri) -> VideoViewModel.buildLocalPlayInfo -> HanimeVideo(videoUrls = localUri)
```

作者点击搜索：

- `VideoIntroductionScreen` 的 `ArtistSection` 触发 `onOpenArtist`。
- `VideoRouteActions.openArtistSearch` 构造 `SearchRoute`。
- 搜索页通过 `route.query` 自动填充作者名并发起搜索。

## 9. 播放器架构

播放器为纯 Compose 实现（`ui/player/`），JZVD 依赖已彻底移除。内核保留 Media3 ExoPlayer 与 libmpv 两种，通过统一接口抽象；页面、控件、手势全部 Compose 化，跨布局移动播放器不中断画面。

### 9.1 分层总览

```text
ui/player/
├── PlayerDefaults.kt          常量：倍速表、灵敏度映射、倒计时默认值
├── PlayerUtils.kt             stringForTime 等工具
├── PlayerUiState.kt           PlayerState / PlayerUiState / PlayerProgress / PlayerEvent
├── HanimePlayerController.kt  状态中枢（内核编排、进度 ticker、音频焦点等）
├── kernel/
│   ├── PlayerKernel.kt        内核接口 + KernelType + MediaRequest + 工厂
│   ├── ExoPlayerKernel.kt     Media3 ExoPlayer 实现
│   └── MpvKernel.kt           libmpv 实现（Anime4K、fd:// 本地播放）
├── surface/
│   └── PlayerSurface.kt       movableContent 视频 Surface
└── video/                     播放器 UI 组件
    ├── HanimeVideoPlayer.kt   主组件（分层组装、事件、气泡弹窗接线）
    ├── PlayerGestureLayer.kt  Modifier.playerGestures 手势层
    ├── PlayerControls.kt      顶栏 / 底栏 / 电量时钟
    ├── PlayerSlider.kt        三层轨道进度条
    ├── PlayerSideSheets.kt    气泡弹窗（倍速/清晰度/超分）+ 关键帧侧栏
    ├── PlayerGestureIndicator.kt  音量/亮度/进度手势指示浮层
    └── KeyframeCountdown.kt   关键 H 帧倒计时

ui/screen/video/
├── VideoCollapsingHeader.kt   折叠头部（嵌套滚动协议）
└── VideoRouteHostScreen.kt    播放页宿主（全屏覆盖层、PiP、平板分栏）
```

依赖方向自上而下：页面 → 控制器 → 内核。UI 组件只消费控制器状态流，不触碰内核；页面只调控制器方法，不感知内核差异。

### 9.2 内核层（kernel/）

`PlayerKernel` 接口定义一路媒体的完整生命周期：`prepare → start/pause/seekTo/setSpeed/setLooping → release`，外加 `setSurface` / `onSurfaceSizeChanged` / `setSuperResolution`（MPV 专属，其余空实现）。`KernelType.fromString` 对旧版 `MediaPlayer` 设置值回落 ExoPlayer。

**ExoPlayerKernel**：

- 播放器实例在专用 `HandlerThread` 构建与释放，避免阻塞主线程；同步查询（`isPlaying`/`currentPositionMs`/`durationMs`）经 `runOnPlayerThread` 以 300ms 超时等待结果。
- `prepare()` 内部复用 `release()` 做防重入，但会先保存并恢复 `pendingSurface` 与 `listener`——surface 丢失会导致有声无画，listener 丢失会导致收不到 READY 回调（UI 永久 loading）。
- 起播缓冲按 `ActivityManager.memoryClass` 分档（48/64/96 MB），低内存设备防 OOM。
- HLS（`.m3u8`）与普通媒体自动选择对应 MediaSource。
- 释放时延迟 500ms 再 `quitSafely()`：media3 的 `release()` 内部还会向该 looper 投递收尾消息，立即退出会触发 dead thread 警告。

**MpvKernel**：

- `MPVLib` 为进程级单例（`HanimeApplication` 中 `create` + `init`），内核实例只做选项配置与事件观察。
- **surface 迟到时延迟 loadfile**：`prepare()` 时若 surface 尚未就绪，先记下待播路径，等 `setSurface()` 到来后再 `loadfile`——否则 mpv 的 vo 初始化会因 WinID 无效直接原生崩溃（SIGABRT）。
- 换源（切清晰度）时由控制器先释放旧内核、创建新内核并把当前 surface 挂回，再 prepare。
- 本地 `file://` / `content://` 经 `ParcelFileDescriptor.detachFd()` 转为 `fd://` 播放。
- Anime4K 超分通过 `change-list glsl-shaders` 动态加载 shader。

### 9.3 控制器（HanimePlayerController）

一个播放页一个实例，取代旧版 JZVD 的静态状态机：

- **双流状态**：`uiState: StateFlow<PlayerUiState>`（低频：状态机、清晰度列表、倍速、循环、视频尺寸等）与 `progress: StateFlow<PlayerProgress>`（约 100ms 高频：position/duration/buffered）分离，避免高频进度引发整树重组。
- **进度 ticker**：播放中以 100ms 周期轮询内核位置，保证关键帧倒计时 0.1s 精度；缓冲/暂停即停。
- 收编音频焦点（`AUDIOFOCUS_GAIN_TRANSIENT`，丢失暂停、恢复续播）、非 WiFi 流量确认（事件流交页面弹框）、观看历史进度恢复（>5s 且允许恢复时自动 seek 并提示「从头开始」）、长按倍速（临时 ×N，抬起恢复）。
- 换清晰度 = 释放重建内核 + `startPositionMs` 保持进度；旧值 `MediaPlayer` 内核回落 ExoPlayer。

### 9.4 视频 Surface（surface/PlayerSurface.kt）

`rememberMovablePlayerSurface` 返回一个 `movableContentOf`，在「折叠头部小窗 / 全屏覆盖层 / 平板分栏左栏」之间移动时复用同一个 `TextureView` 实例，播放不中断：

- `onSurfaceTextureDestroyed` 返回 `false`，纹理由本层持有不释放；
- 跨容器移动后系统回调新的 `onSurfaceTextureAvailable` 时，把保留的旧纹理 `setSurfaceTexture` 塞回去，渲染管线无缝衔接。

渲染层在 `HanimeVideoPlayer` 内部**始终按视频宽高比居中**（`aspectRatio(matchHeightConstraintsFirst = ratio < 1)`）——直连 Surface 不会自动 letterbox，无此约束会在全屏切换后拉伸画面。

### 9.5 播放器 UI（video/）

`HanimeVideoPlayer` 自底向上分层：渲染层（比例约束）→ 海报层 → 手势层（`Modifier.playerGestures`）→ 控件层（顶栏/底栏/中键/锁/倒计时/迷你进度条）→ 浮层（手势指示、从头开始、重试）→ 气泡弹窗。

手势层迁移自旧版 `HJzvdStd.onTouch` 全部行为：单击切换控件、双击播放暂停、长按临时倍速（触觉反馈）、横滑调进度（灵敏度可调、越界钳制、ERROR 态禁用）、左半屏竖滑亮度、右半屏竖滑音量。两个要点：

- **消费让位**：拖动中若事件被子组件（弹窗列表、进度条）消费（`change.isConsumed`），手势立即放弃，否则拖列表仍会触发音量/亮度。
- 倍速/清晰度/超分弹窗为**锚定气泡**（对话泡 + 指向按钮的三角，按锚点坐标与方向定位、水平钳制在播放器内），弹出期间控件保持显示；关键帧列表内容长，仍用右侧滑入面板。

### 9.6 页面层：折叠头部与布局

`VideoCollapsingHeader` 取代 CoordinatorLayout + AppBarLayout + CollapsingToolbarLayout，`Column(头部 + 内容)` 结构，头部高度随折叠真实收缩：

- **性能**：折叠比例只在 `Modifier.layout` 与 `derivedStateOf` 中读取，组合期间不依赖任何折叠值——拖拽/锁定动画的每一帧只触发测量与布局，不引发整棵播放页子树重组（列表闪烁的根源）。锁定展开动画用 `Animatable`（`animateFloatAsState` 的 target 在组合期读取，会每帧重组）。
- **滚动协议**（标准 collapsing）：
  - 上滑：`onPreScroll` 先折叠头部，折完才轮到内容滚动；
  - 下滑：折叠态的下拉同样在 `onPreScroll` 拦截展开——父层 preScroll 先于子组件（列表 overscroll 拉伸与 PullToRefresh）执行，否则列表置顶时下拉量会被其消费，头部收不到；
  - 内容滚到顶后的剩余量在 `onPostScroll` 兜底展开；
  - **fling 归属**：手势期间发生过折叠消费则 `onPreFling` 吞掉惯性——否则「拖拽段折叠 + 释放惯性全给列表」会让内容总位移接近手指距离的两倍。松手即停（无端点吸附动画，保持跟手）。
- 播放中（PREPARING/BUFFERING/PLAYING）禁止折叠并平滑动画强制展开。

`VideoRouteHostScreen` 负责三种布局形态（手机折叠 / 平板横屏分栏 / PiP 全屏）与全屏覆盖层：

- 全屏 = Compose 树内覆盖层（视频页无底部导航，页面级覆盖即可全屏）+ `WindowInsetsController` 隐藏系统栏 + 方向锁定（竖屏视频全屏保持竖屏）；返回经 `BackHandler` 退全屏；**息屏/切后台只暂停不退全屏**，解锁后仍保持全屏。
- movable Surface 在头部小窗与全屏层之间移动，不重建。
- 播放中（含缓冲）`view.keepScreenOn = true` 保持屏幕常亮。
- PiP 经 `VideoPageHost` 接口桥接 Activity（`shouldEnterPip` / `enterPipMode` / 远程播放暂停按钮），控制器提供 `isActiveForPip` / `togglePlayPause`。

### 9.7 关键时序

起播（首次点中键）：

```text
startVideo → (非WiFi? → RequestWifiConfirmation 事件 → 页面弹框 → confirmWifiAndPlay)
→ reprepareKernel(释放旧内核 → 新内核 → 挂回 surface → prepare)
→ 内核 READY → listener.onPlaying → uiState=PLAYING → ticker 启动
→ 首次播放且有历史进度(>5s) → seekTo(savedProgress) + ShowResumeButton 事件
```

换清晰度：

```text
switchQuality(index) → 记录当前位置 → 释放重建内核（挂回 surface）→ prepare(startPositionMs=当前进度)
```

进入/退出全屏：

```text
enterFullscreen → movable Surface 移至覆盖层 → 隐藏系统栏 → 锁方向
exitFullscreen  → movable Surface 移回头部小窗 → 恢复系统栏/亮度/方向
```

### 9.8 约定与常见坑

- 画质信息由 `HanimeResolution` 解析后交给 `controller.setMedia`；播放器 UI 状态不写入网络模型，统一经 `HanimePlayerController` 状态流。
- 页面级状态（Tab 选中、PiP、播放器高度）归 `VideoViewModel.VideoHostUiState`；播放器内部状态由 `HanimePlayerController.uiState` 驱动。
- MPV 相关设置归 `MpvPlayerSettingsRouteScreen` 和偏好设置管理。
- **勿在组合期间读取折叠比例**（新增折叠相关 UI 时走 layout/draw 阶段），否则列表闪烁回归。
- **勿在内核 `prepare` 前让 `release()` 清掉 `pendingSurface`/`listener`**（Exo 双恢复逻辑），否则有声无画或永久 loading。
- **播放页内的列表禁用 item 入场动画**（`enableItemAnimation = false`）：折叠联动会持续扩展视口，新进入的 item 以淡入动画进场会造成闪烁，每项动画协程在快速滚动时也会掉帧。
- 评论下拉刷新常驻（PullToRefreshBox 不随折叠状态切换容器）——折叠态下拉由折叠头在 preScroll 拦截，切换容器会导致整棵列表重组重建。
- 播放页内嵌套滚动链上的子组件（列表等）不要自行消费边缘下拉/上滑，交由折叠头统一分配。

## 10. 下载架构

下载由 UI、ViewModel、下载管理器、Worker 和 Room 共同完成。

关键文件：

- `ui/screen/home/DownloadScreen.kt`
- `ui/screen/home/download/DownloadingScreen.kt`
- `ui/screen/home/download/DownloadedScreen.kt`（已下载页：文件夹网格与视频网格）
- `ui/screen/home/download/DownloadWatchFilter.kt`（观看过滤：全部 / 未观看 / 观看中 / 已观看）
- `ui/screen/home/download/DownloadSort.kt`、`DownloadFolderCard.kt`、`DownloadedCard.kt`
- `ui/viewmodel/DownloadViewModel.kt`
- `worker/HanimeDownloadManagerV2.kt`
- `worker/HanimeDownloadWorker.kt`
- `worker/HlsDownloader.kt`（HLS 分片下载）
- `util/SiteDownloads.kt`（各站点下载目录布局、下载客户端、图片保存、失败分类）
- `logic/entity/download/HanimeDownloadEntity.kt`
- `logic/entity/download/DownloadGroupEntity.kt`
- `logic/dao/download/HanimeDownloadDao.kt`
- `logic/dao/DownloadDatabase.kt`

下载数据流：

```text
DownloadScreen -> DownloadViewModel -> HanimeDownloadManagerV2 -> WorkManager -> HanimeDownloadWorker -> DownloadDatabase -> Flow -> UI
```

设计要点：

- 长任务交给 WorkManager，避免 Activity 生命周期影响下载。
- 下载进度落库，UI 通过数据库 Flow 获得刷新。
- 前台服务通知由 Worker 维护。
- 下载目录通过 SAF 相关工具处理，避免直接依赖旧外部存储权限。
- 下载完成后可走本地播放路径进入视频页。
- `worker/HanimeDownloadManager.kt` 为旧版管理器，当前入口是 `HanimeDownloadManagerV2`。

已下载页（`DownloadScreen` 的第二个标签页）：

- 文件夹模式：按分组或按站点（`folderBySite`）分文件夹，点开文件夹后显示该文件夹的视频；搜索时跨文件夹显示结果。
- 排序（`DownloadSort`）、搜索、观看过滤（`DownloadWatchFilter`，依据 `LocalWatchStore.entries` 中的 `completed` / `positionMs`：已观看＝已完成，观看中＝已开始未完成，未观看＝未开始）依次作用于去重后的下载列表。
- 卡片的「已观看」标签与续播时间来自 `rememberLocalWatchTags`（`ui/component/HistoryTags.kt`）。
- 滚动位置：视频网格与文件夹网格各自持有 `rememberSaveable` 的 `LazyGridState`；只有排序 / 文件夹 / 搜索 / 过滤真正变化时才回到顶部，因此从视频页返回不会跳回顶部。
- 播放队列：`DownloadedQueue.setContext(scope, sort)` 记录当前范围（全部 / 分组 / 站点 / 搜索结果），本地播放按此范围连续播放。

性能注意：

- 下载进度不应过于频繁落库，否则多个任务同时下载时会造成数据库和 UI 刷新压力。
- 更新间隔应在“进度实时性”和“数据库写入频率”之间折中。

## 11. 本地数据库

Room 主要用于：

- Hanime 播放历史、搜索历史、高级搜索历史。
- 下载任务、下载分组和分类。
- 共享/自定义关键 H 帧。
- 健康打卡记录。
- 各站点自己的历史记录（以及个别站点的系列库、搜索历史）。

全局数据库（`logic/dao/`）：

| 数据库 | 版本 | 实体 |
| --- | --- | --- |
| `HistoryDatabase` | 5 | `SearchHistoryEntity`、`WatchHistoryEntity`、`HanimeAdvancedSearchHistoryEntity` |
| `DownloadDatabase` | 5 | `HanimeDownloadEntity`、`DownloadCategoryEntity`、`HanimeCategoryCrossRef`、`DownloadGroupEntity` |
| `MiscellanyDatabase` | 1 | `HKeyframeEntity` |
| `CheckInRecordDatabase` | 4 | `CheckInRecordEntity`、`SideDishEntity` |

站点数据库（`<Site>/data/local/`）：

| 数据库 | 版本 | 说明 |
| --- | --- | --- |
| `MissAvDatabase` | 2 | `MissAvHistoryEntity` |
| `HentaiMamaDatabase` | 2 | 历史 `HentaiMamaHistoryEntity`、系列 `HentaiMamaSeriesEntity` |
| `OnAirDatabase` | 1 | `OnAirHistoryEntity` |
| `FapFolderDatabase` | 1 | `FapFolderHistoryEntity` |
| `SupJavDatabase` | 1 | `SupJavHistoryEntity`（库名 `supjav_history.db`） |
| `RouVideoDatabase` | 1 | `RouVideoHistoryEntity` |
| `XHamsterDatabase` | 1 | `XHamsterHistoryEntity` |
| `BuonduaDatabase` | 1 | `BuonduaHistoryEntity` |
| `StrawberryDatabase` | 1 | `StrawberryHistoryEntity` |
| `CosplayteleDatabase` | 1 | `CosplayteleHistoryEntity` |
| `CoserDatabase` | 1 | `CoserHistoryEntity` |
| `FullVideosDatabase` | 1 | `FullVideosHistoryEntity` |
| `FuckingSessionDatabase` | 1 | `FuckingSessionHistoryEntity` |
| `WumaobiDatabase` | 1 | `WumaobiHistoryEntity` |
| `XXXTubeDatabase` | 1 | `XXXTubeHistoryEntity` |
| `CoomerDatabase` | 1 | `CoomerHistoryEntity` |
| `LeakGalleryDatabase` | 1 | `LeakGalleryHistoryEntity` |

所有数据库均为 `exportSchema = false`。部分站点库（例如 SupJav）使用 `fallbackToDestructiveMigration(dropAllTables = true)`，修改其表结构会清空历史，改动前请先确认迁移策略。

关键入口：

- `logic/DatabaseRepo.kt`
- `logic/dao/HistoryDatabase.kt`
- `logic/dao/DownloadDatabase.kt`
- `logic/dao/MiscellanyDatabase.kt`
- `logic/dao/CheckInRecordDatabase.kt`
- `<Site>/data/local/<Site>Database.kt`

约定：

- UI 不直接访问 DAO。
- 跨页面复用的数据库操作集中到 `DatabaseRepo` 或站点自己的 Repo。
- 高频写入要评估 Flow 回调频率和 UI 刷新成本。
- 新增数据库或实体后，同步检查 `BackupManager` 是否需要增加备份分区。

## 12. 网络和解析

网络层入口：

- `NetworkRepo`：业务方法入口，统一包装状态。
- `HanimeNetwork`：Retrofit Service 聚合。
- `ServiceCreator`：OkHttp/Retrofit 创建。
- 其他站点：各自的 `<Site>Network`、`<Site>Service`、`<Site>Repo` / `<Site>NetworkRepo`（位于 `<Site>/data/remote/`）。
- 站点级镜像与网络配置见 3.5。
- `Parser`：HTML/JSON 转领域模型。

拦截器和辅助：

- `CloudflareInterceptor`（Hanime）、`util/cloudflare/CloudflareChallengeInterceptor`（站点通用）
- `GetchuInterceptor`
- `UserAgentInterceptor`
- `SpeedLimitInterceptor` / `SpeedLimitResponseBody`
- `UrlLoggingInterceptor`
- `HCookieJar`（各站点另有自己的 CookieJar）
- `HDns`、`GitHubDns`、`DohConfig`
- `HProxySelector`

异常类型：

- `CloudFlareBlockedException`
- `IPBlockedException`
- `HanimeNotFoundException`
- `LoginStateExpiredException`
- `ParseException`
- `HUpdaterException`

开发约定：

- Parser 应尽量容错，目标网站 DOM 改动时返回明确错误。
- `NetworkRepo` 应保留 `CancellationException` 语义，不吞掉协程取消。
- 登录态过期、Cloudflare、IP blocked 等应映射成可被 UI 理解的异常或状态。
- 解析列表时优先保证 `videoCode`、`listCode` 等业务键非空和稳定。

### 12.1 更新链路（GitHub）

更新完全依赖 GitHub，因此必须把「限额耗尽 / 密钥失效 / 权限不足 / Artifact 过期 / 网络异常」
区分开，而不是一律报「更新失败」。

相关文件：

- `logic/network/HUpdater.kt`：版本检查与安装包下载（Release / CI 两个渠道）。
- `logic/network/HDeltaUpdater.kt`：增量补丁的选择、下载、还原与校验。
- `util/Bspatch.kt`：纯 Kotlin 的 bspatch 实现（依赖 commons-compress 解压 bzip2）。
- `logic/network/GitHubErrorMapper.kt`：HTTP 响应 → `HUpdaterException` 的分类映射。
- `logic/exception/HUpdaterException.kt`：分类异常 + 对应的可展示文案。
- `logic/update/UpdateManager.kt`：更新状态机（`UpdatePhase`、`UpdateStage`、`UpdateMethod`、`UpdatePlan`、`DeltaUnavailableReason`、`UpdateUiState`），供更新对话框（`ui/component/UpdateDialog.kt`）和更新状态页（`ui/screen/settings/UpdateStatusScreen.kt`）使用。
- `worker/HUpdateWorker.kt`、`worker/UpdateActionReceiver.kt`：下载通知与失败提示、通知上的操作。
- `util/Versions.kt`：版本比较、安装包落盘位置、安装入口 `installApkPackage`。
- `shizuku/ShizukuUpdateInstaller.kt`、`shizuku/ShellInterface.kt`：Shizuku 静默安装。

渠道与回落约定：

- `HGitHubService` 的版本信息接口必须返回 `Response<T>`。返回实体类型时非 2xx 会直接抛
  `HttpException`，拿不到响应体与状态码，就永远无法区分 403 的两种含义
  （限额耗尽 vs 权限不足）。
- 渠道由 `Preferences.useCIUpdateChannel` 唯一决定，**任何情况下都不做回落**：
  CI 构建始终领先 Release，一旦因 CI 出错就回落，会把用户降级到更旧的版本。
  CI 频道出错就如实报错，由用户自己在设置页手动重试。`enable_ci_update`（Remote Config，
  默认开启，见 `FirebaseConstants`）只是作者临时下线 CI 频道的开关，关闭时同样只报错、不回落；
  读取该开关失败也会报错而不是静默继续。
- 更新链路**不做自动重试**：失败原因如实回报给用户，由用户在设置页手动重试。
  不要在这里重新引入退避重试，限额耗尽时重试只会让情况更糟。
- `checkForUpdate()` 失败时抛 `HUpdaterException.VersionCheck`，`injectUpdate()` 失败时抛
  `HUpdaterException.Download`；UI 侧用 `HUpdater.errorMessage(e)` 取可展示文案，
  详细诊断（状态码 + 响应体片段）用 `HUpdater.errorDetail(e)` 写日志。
- 下载必须先校验 `Response.isSuccessful` 再落盘，否则错误 JSON 会被写成假的 `update.apk`。
- `BuildConfig.HA_GITHUB_TOKEN` 为空时不再发送非法的 `Bearer `，改为匿名请求（限额更低但可用）。

资源命名约定（与 CI 保持一致）：

- CI 渠道：完整包为构建产物 `app-release-<sha>`，补丁为 `delta-<sha>-base-<基础 APK SHA-256 前 16 位>`；`HUpdater` 用前缀 `app-release-` 与 `delta-` 区分。
- Release 渠道：完整包为以 `.apk` 结尾的资源，补丁为名称含 `-delta-` 且以 `.zip` 结尾的资源（`HANiEX-delta-base-<前 16 位>.zip`）。

增量更新流程（`HDeltaUpdater.tryApply`）：

```text
inspect(deltas)：计算已安装 APK 的 SHA-256，选出 baseSha256 前缀匹配的补丁（没有前缀信息的补丁作为兜底）
-> 下载补丁 zip，读取 update.json（format 必须为 bsdiff40，newSize 合法且不超过 1 GiB）
-> 再次核对 baseSha256 与已安装 APK
-> bspatch 还原新 APK
-> 校验 newSize / newSha256 / 签名证书一致（hasSameSigner）
-> 失败则记录 DeltaUnavailableReason，回落为完整 APK 下载
```

补丁由 `.github/scripts/make-delta.sh` 生成：`update.json` 的字段为 `format`、`baseSha256`、`newSha256`、`newSize`、`patchSize`；补丁达到新包 80% 及以上或两个 APK 相同时不产出。修改补丁格式时，必须同时修改该脚本和 `HDeltaUpdater`。

安装方式：`Preferences.updateInstaller`（偏好键 `SettingsPreferenceKeys.UPDATE_INSTALLER`，值为 `UpdateInstaller.SYSTEM` 或 `UpdateInstaller.SHIZUKU`，默认 `system`）。`installApkPackageForResult` 在选择 Shizuku 时先尝试 `ShizukuUpdateInstaller.install`，未安装 / 未运行 / 未授权 / 失败都会弹出提示并回落到系统安装器（`FileProvider` + `ACTION_VIEW`）。

## 13. 账号、Cookie 和 Cloudflare

相关页面：

- `ui/activity/LoginActivity.kt`
- `ui/activity/ManualInputCookiesActivity.kt`
- `ui/activity/CloudflareActivity.kt`
- `ui/screen/account/AccountScreen.kt`
- `ui/screen/account/AvatarCropScreen.kt`
- `ui/viewmodel/UserAccountViewModel.kt`

关键点（以 Hanime1 为例；其他站点各有自己的认证与 CookieJar，如 `MissAvAuth`、`RouVideoAuth`、`XHamsterAuth`、`FapFolderAuthRepo`、`OnAirAuthRepo`）：

- 登录态和 Cookie 由 `Preferences`、`HCookieJar` 和相关工具共同维护。
- 手动 Cookie 输入用于绕过无法自动登录或 WebView 登录失败的情况。
- Cloudflare 页面和拦截器用于处理访问保护导致的请求失败；站点通用的验证逻辑在 `util/cloudflare/`（`BaseCloudflareActivity`、`CloudflareChallengeInterceptor`、`CloudflareCookieStore`、`CloudflareVerificationManager`）。
- OnAir / Strawberry 需要 Tor 出口，`OnAirTorChecker` 会区分超时、网络不可达和非 Tor 出口三类失败。
- FapFolder 的凭据通过 `FapFolderSecurePrefs` 保存：优先使用 EncryptedSharedPreferences；加密存储创建失败时回退为普通私有偏好，此时 `FapFolderCredentialStore.canStore()` 为 false，不会保存自动登录凭据。
- 账号信息修改、密码修改、头像上传都通过 `HanimeMyListService` 和 `NetworkRepo` 包装。

## 14. 公告系统与管理端

公告系统分为应用侧展示和管理端写入两部分。

关键文件：

- `ui/screen/home/homepage/HomePageViewModel.kt`
- `ui/screen/home/homepage/HomePageScreen.kt`
- `ui/screen/home/homepage/component/AnnouncementDialog.kt`（以及同目录的 `AnnouncementListDialog.kt`、`AnnouncementCard.kt`、`AnnouncementPanel.kt`）
- `logic/model/Announcement.kt`
- `docs/HanimeAnnouncementManagerWebUI/HanimeAnnouncementManager.html`
- `docs/HanimeAnnouncementManagerWebUI/PermitAdmin.py`

应用侧链路：

- `HomePageViewModel` 在加载 Hanime 首页时并行从 Firebase Realtime Database 的 `announcements` 节点读取公告。
- 只有 `isActive = true` 的公告会参与展示，并按 `priority` 升序排序（数值小的在前）。
- OnAir 另有自己的公告页（`OnAir/ui/announcement/`）。
- 首页通过公告卡片展示公告列表。
- 点击公告后进入 `AnnouncementDialog`，可查看标题、正文、时间、图片和按钮文案。
- 用户关闭公告后，会记录 `last_dismiss_time`（`setting_pref`），24 小时内不会再次自动弹出。

管理端链路：

- `HanimeAnnouncementManager.html` 是基于 Firebase Web SDK 的管理页面。
- 页面通过 `firebaseConfig` 连接到项目对应的 Realtime Database。
- 页面使用 `ADMIN_EMAIL` 和 `ADMIN_PASSWORD` 登录 Firebase Auth。
- 登录成功后读取 `announcements` 节点并渲染为表格。
- 页面支持新增、编辑、启用、停用和删除公告。

管理员权限：

- `PermitAdmin.py` 使用 Firebase Admin SDK 给指定 UID 写入 `isAdmin: true` 自定义声明。
- 该脚本依赖本地 `serviceAccountKey.json`。
- 运行前需要安装 `firebase-admin`，并填写目标用户 UID。
- 该自定义声明用于管理端权限校验。

公告字段约定：

- `title`：公告标题。
- `content`：公告正文。
- `imageUrl`：公告图片地址，可选。
- `positiveText`：确认按钮文本，可选。
- `negativeText`：取消按钮文本，可选。
- `priority`：排序优先级。
- `timestamp`：更新时间戳，单位为秒。
- `isActive`：是否启用。

实现约定：

- 公告数据应兼容部分字段缺失的情况。
- 管理端更新公告时会刷新 `timestamp`，便于前端显示最新时间。
- 停用公告时只更新 `isActive`，删除则直接移除节点。
- 后续如果新增公告字段，需要同步更新 `Announcement` 模型、管理端表单和展示弹窗。

## 15. 设置体系

设置页采用 Compose + typed routes。设置首页按 `SettingsCategory` 分为外观（appearance）、播放（playback）、网络（network）、下载（download）、关键帧（keyframes）、隐私（privacy）、数据（data）、关于（about）；网络设置作用于「当前选中的站点」，站点专属的设置（可见性、站点播放器设置、各站点设置页）入口见 `ui/screen/settings/SiteSettingsHubScreen.kt`、`SiteVisibilitySettings.kt`、`SitePlayerSettingsScreen.kt`。

关键文件：

- `ui/navigation/settings/SettingsRoutes.kt`
- `ui/navigation/settings/SettingsNavHost.kt`
- `ui/navigation/settings/SettingsCategory.kt`
- `ui/navigation/settings/SettingsPreferenceKeys.kt`
- `ui/navigation/settings/HomeSettingsRoute.kt`
- `ui/screen/settings/HomeSettingsScreen.kt`
- `ui/screen/settings/PlayerSettingsScreen.kt`
- `ui/screen/settings/MpvPlayerSettingsScreen.kt`
- `ui/screen/settings/DownloadSettingsScreen.kt`
- `ui/screen/settings/NetworkSettingsScreen.kt`
- `ui/screen/settings/HKeyframeSettingsScreen.kt`
- `ui/screen/settings/UpdateStatusScreen.kt`
- `ui/screen/settings/TranslationSettingsScreens.kt`

偏好配置入口：

- `Preferences.kt`、`util/Preferences.kt`
- `SearchGridColumnsConfig.kt`
- `HorizontalCardCountConfig.kt`

约定：

- 新设置项应集中放在 `Preferences` 或明确的配置类里。
- UI 设置页只负责展示和修改偏好，不直接散落业务逻辑。
- 与播放器、网络、下载有关的设置要同步检查对应模块读取点。

## 16. 共享关键 H 帧

共享关键 H 帧仍使用 assets 中的 JSON 文件作为输入。

关键文件：

- `app/src/main/assets/h_keyframes/`
- `app/src/main/assets/h_keyframes/README.md`
- `logic/entity/HKeyframeEntity.kt`
- `logic/dao/HKeyframeDao.kt`
- `logic/DatabaseRepo.kt`
- `ui/screen/settings/HKeyframesScreen.kt`
- `ui/screen/settings/SharedHKeyframesScreen.kt`

读取思路：

```text
assets/h_keyframes/*.json -> Json.decodeFromStream<HKeyframeEntity> -> sort/group/flatten -> UI
```

分组扁平化示意：

```kotlin
entities
    .sortedWith(compareBy<HKeyframeEntity> { it.group }.thenBy { it.episode })
    .groupBy { it.group ?: "???" }
    .flatMap { (group, entities) ->
        listOf(HKeyframeHeader(title = group, attached = entities)) + entities
    }
```

这种结构避免了在 assets 里维护复杂目录索引，也便于贡献者直接提交单个 JSON 文件。

## 17. 备份、翻译与打卡

### 17.1 备份与恢复

`logic/BackupManager.kt` 将用户数据导出为 JSON（`BACKUP_VERSION = 1`，`ignoreUnknownKeys`，导入时对未知字段容错），按 `Section` 分区，每个分区属于 `Group.DATA` 或 `Group.COOKIES`：

- 数据：`SETTINGS`、`CHECK_IN`、`KEYFRAMES`、`HANIME_HISTORY`、`DOWNLOADS`，以及 `MISSAV_HISTORY`、`HENTAIMAMA_HISTORY`、`ONAIR_HISTORY`、`FAPFOLDER_HISTORY`。
- Cookie：`COOKIES_HANIME`、`COOKIES_MISSAV`、`COOKIES_HENTAIMAMA`、`COOKIES_ONAIR`、`COOKIES_FAPFOLDER`。
- 入口为 `exportTo(...)` / `importFrom(...)`，选择分区的界面是 `ui/component/BackupSectionDialog.kt`。

注意：SupJav、RouVideo、XHamster、Buondua 的历史库目前没有对应的备份分区；新增站点或数据库时请检查是否需要补充分区（Cookie 分区同理）。

### 17.2 翻译与本地化

- `util/translation/AppTranslation.kt`：标签（最多 2000 条）、标题（3000）、简介（300）、评论（1000）四类翻译缓存。
- `util/TagLocalizer.kt`、`util/DisplayTextLocalizer.kt`、`util/Translated.kt`：展示文本的本地化与翻译结果展示。
- `util/ChineseConverter.kt`：简繁转换。
- `base/AppLocale.kt`：应用内语言切换；`bundle.language.enableSplit = false`，所有语言资源会随包发布。
- 字符串资源位于 `values`、`values-en`、`values-ja`、`values-zh-rCN`，新增文案要同步补齐。

### 17.3 打卡

- `logic/SiteCheckIn.kt` 按站点区分打卡记录，`CheckInRecordDatabase` 存储记录与「配菜」（`SideDishEntity`）。
- 页面：`ui/screen/home/dailycheckin/`、`DailyCheckInScreen.kt`、`CheckInCalendarViewModel.kt`。
- 桌面小组件：`ui/widget/CheckInWidgetProvider.kt`，布局 `res/layout/checkin_widget.xml`（`layout-v31` 有 Android 12 版本）。

## 18. Compose 列表封装

项目提供了 `AnimatedLazy.kt` 封装：

- `LazyColumn`
- `LazyRow`
- `LazyVerticalGrid`
- `AnimatedLazyListScope`
- `AnimatedLazyGridScope`

目的：

- 尽量兼容原生 Lazy API 的常用调用方式。
- 为列表 item 提供轻量入场动画。
- 在全项目保持较一致的列表观感。

注意：

- 它不会替代 Compose 对 key 唯一性的要求。
- 使用 `items(list, key = { ... })` 时，调用方仍必须保证 key 唯一。
- 网络分页列表优先在 ViewModel 层去重，不建议在 UI 层临时过滤后再展示，除非数据源来自单次接口且不参与后续业务逻辑。

## 19. 构建和版本

关键文件：

- `settings.gradle.kts`
- `build.gradle.kts`
- `app/build.gradle.kts`
- `gradle.properties`
- `gradle/libs.versions.toml`
- `buildSrc/src/main/java/Config.kt`
- `.github/workflows/ci.yml`、`pr_check.yml`、`debug-apk.yml`
- `.github/scripts/make-delta.sh`

当前构建特征：

- `compileSdk=37`、`minSdk=27`、`targetSdk=37`（`gradle.properties`，并设置了 `android.suppressUnsupportedCompileSdk=37`）
- Java 21
- Compose Compiler 插件随 Kotlin 版本
- Release 默认启用混淆（R8）和资源压缩，并随 CI 上传 R8 映射
- Release 只打 `arm64-v8a` ABI split
- `applicationId` 为 `com.yenaly.hani.extended`，debug 追加 `.debug`；Kotlin `namespace` 仍为 `com.yenaly.han1meviewer`
- APK 文件名格式：`HANiEX-v{versionName}.apk`
- 已启用 Gradle 构建缓存、并行构建和 Kotlin 增量编译；未启用 configuration cache（构建脚本在配置阶段读取环境变量并执行 git）

版本号（`Config.kt` 的 `createVersion`）：

- Debug：versionCode `1`，versionName `debug+1`，`COMMIT_SHA` 写死，因此 CI 渠道比较对 debug 无意义。
- 其他：versionCode 为 UTC 时间 `yyMMddHH`，versionName 为 `<major>.<minor>.<patch>-<来源>+<versionCode>`；来源取环境变量 `HA1_VERSION_SOURCE`（CI 设为 `ci`），否则 release 任务为 `release`。
- `BuildConfig` 暴露 `COMMIT_SHA`、`VERSION_NAME`、`VERSION_CODE`、`VERSION_SOURCE`、`HA_GITHUB_TOKEN`、`SEARCH_YEAR_RANGE_END`。
- `HA_GITHUB_TOKEN` 先读环境变量，再读 `app/ha1_github_token.txt`。

签名：release 使用 `$HOME/.android/keystore.jks`，环境变量 `KEYSTORE_PASSWORD`（同时作为密钥密码）和 `KEY_ALIAS`。

CI 概要（详见 README 的「CI 与发布」）：

- `ci.yml`：推送到 `main` 触发（仅改 `*.md` 时忽略）。构建 → 对比最近最多 3 次成功 CI 构建与最新 Release 生成 bsdiff 补丁 → 上传 `app-release-<sha>`、`r8-mapping-<sha>` 与各 `delta-*` 产物 → 提交信息含 `[release]` 时生成更新说明、Release 补丁并创建 GitHub Release。
- `pr_check.yml`：PR 触发的 debug 构建检查。
- `debug-apk.yml`：手动触发，构建未混淆 debug APK 用于对照 R8 问题。
- 需要的 Secrets：`GOOGLE_SERVICES_JSON_BASE64`、`KEYSTORE_BASE64`、`KEYSTORE_PASSWORD`、`KEY_ALIAS`、`HA_GITHUB_TOKEN`、`APP_TOKEN`。

本地常用验证：

```powershell
.\gradlew.bat :app:compileDebugKotlin
.\gradlew.bat :app:assembleDebug
```

提交前建议至少运行：

```powershell
.\gradlew.bat :app:compileDebugKotlin
```

## 20. 开发约定

代码约定：

- 优先做最小正确修改。
- 新 UI 优先使用 Compose。
- 页面状态优先使用 `StateFlow`，一次性事件用 `SharedFlow` 或回调。
- 网络和数据库访问不要写进 Composable。
- 列表分页合并要去重。
- 文件末尾保留一个换行符。
- 新站点代码放在独立的 `<Site>/` 包，不要把站点特有逻辑塞进 Hanime 的 `logic/` 里。

排查问题时优先看：

- 页面是否重复触发网络请求。
- ViewModel 是否在 Loading 状态下清空了不该清空的列表。
- `Lazy*` key 是否唯一。
- Parser 是否因 DOM 改动解析为空。
- 登录态或 Cookie 是否过期。
- Cloudflare/IP blocked 是否被正确映射。
- 问题是否只出现在某个站点或某个镜像上（先用镜像延迟测试和站点切换确认）。

## 21. 常见改动入口

新增首页模块：

- `ui/screen/home/homepage/HomePageScreen.kt`
- `ui/screen/home/homepage/HomePageViewModel.kt`
- `Parser.homePageVer2`
- `HomePage` 相关 model

新增搜索过滤条件：

- `SearchScreen.kt`
- `AdvancedSearchSheet.kt`
- `SearchViewModel.kt`
- `HAdvancedSearch.kt`
- `NetworkRepo.getHanimeSearchResult`
- `HanimeBaseService.getHanimeSearchResult`

新增视频详情字段：

- `HanimeVideo.kt`
- `Parser.hanimeVideoVer2`
- `VideoIntroductionScreen.kt`
- `VideoRouteActions.kt`，如果字段涉及点击动作或外部跳转

新增我的页面列表：

- `MainRoutes.kt`
- `MainNavHost.kt`
- 对应 `RouteScreen`
- 对应 `ViewModel`
- `NetworkRepo` 和 `Parser`

新增站点：

- 见 3.7「新增站点清单」。

新增站点内的页面或数据：

- `<Site>/ui/` 下的页面与 ViewModel
- `<Site>/data/remote/`、`<Site>/data/parser/`
- `<Site>/ui/navigation/` 的 NavHost 和 `HubConfig`

新增备份分区：

- `BackupManager.Section` 与对应的导出 / 导入逻辑
- `ui/component/BackupSectionDialog.kt` 的展示文案

新增下载相关能力：

- `DownloadViewModel.kt`
- `HanimeDownloadManagerV2.kt`
- `HanimeDownloadWorker.kt`
- `DownloadDatabase` / DAO / Entity
- 下载设置页和权限/SAF 工具

新增设置项：

- `Preferences.kt` 或对应配置类
- `HomeSettingsScreen.kt` / 子设置页面
- 实际业务读取点

## 22. 维护提示

- 目标网站 DOM 改版时，优先检查对应的解析器：Hanime 为 `Parser.kt`，其他站点为 `<Site>/data/parser/`。
- 出现网络请求异常时，优先检查拦截器、Cookie、Cloudflare 和备用域名逻辑。
- 出现列表崩溃 `Key "..." was already used` 时，优先检查 ViewModel 合并列表是否去重。
- 出现视频页状态错乱时，优先检查 `VideoViewModel.VideoHostUiState` 和 `videoIntroUiStateMap`。
- 某个站点突然全部失败时，先检查镜像（`SiteMirrorCatalog` / 站点网络设置）、Cloudflare 验证和 Cookie，再怀疑解析器。
- OnAir 无法打开时，先检查 Tor 出口（`OnAirTorChecker`）。
- 出现下载进度异常时，优先检查 Worker、Room 实体和 DAO Flow。
- 更新出问题时，先区分渠道（Release / CI）、是否走了增量（`UpdateManager` 状态与 `DeltaUnavailableReason`），再检查 `make-delta.sh` 与 CI 产物命名。
- 更新依赖版本后，优先跑 `:app:compileDebugKotlin`，播放器、导航和 KSP/Room 是高风险区域。
