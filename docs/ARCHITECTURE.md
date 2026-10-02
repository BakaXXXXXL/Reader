# 系统架构设计与技术规范 (Architecture Specification)

> 本文档定义了 Android 本地小说与全格式电子书阅读器的总体架构、模块划分、数据模型、解析管道以及核心排版渲染引擎的技术规范。

---

## 一、系统总体架构 (System Architecture)

本项目遵循 Google 推荐的 **现代 Android 应用架构指南 (Modern Android Architecture)**，结合 **Clean Architecture** 原则与 **MVI (Model-View-Intent)** 单向数据流模式：

```
+-------------------------------------------------------------------------------+
|                           Presentation Layer (UI)                             |
|          Jetpack Compose + Material 3 (Edge-to-Edge, Dynamic Color)           |
|   [Bookshelf Screen]   [Reader Screen]   [Settings Screen]   [Stats Screen]   |
+-------------------------------------------------------------------------------+
                                      ▲        │
                           UiState    │        │  UiIntent (Events)
                                      │        ▼
+-------------------------------------------------------------------------------+
|                       State Holders (MVI ViewModels)                          |
|         StateFlow / SharedFlow / Coroutines / Lifecycle-aware Scope           |
+-------------------------------------------------------------------------------+
                                      ▲        │
                                      │        ▼
+-------------------------------------------------------------------------------+
|                             Domain Layer (Use Cases)                          |
|   OpenBookUseCase | PaginateChapterUseCase | SaveProgressUseCase | SyncUseCase|
|                         Pure Kotlin / No Android Framework                    |
+-------------------------------------------------------------------------------+
                                      ▲        │
                                      │        ▼
+-------------------------------------------------------------------------------+
|                              Data Layer (Repositories)                        |
|   BookRepository | ReaderSettingsRepository | HistoryRepository | SyncRepo    |
+-------------------------------------------------------------------------------+
         │                                       │                     │
         ▼                                       ▼                     ▼
+-----------------------+              +-------------------+   +----------------+
| Local Database (Room) |              | DataStore (Prefs) |   | Cloud (WebDAV) |
| Books, Chapters, Logs |              | Typography, Theme |   | Remote Backup  |
+-----------------------+              +-------------------+   +----------------+
         │
         ▼
+-------------------------------------------------------------------------------+
|                         Core Engine Layer (Core Engines)                      |
|                                                                               |
|  [Format Parsers]              [Typography & Pagination]   [Renderer Matrix]  |
|  • TXT (MMap+Regex TOC)        • LineBreak & Kinsoku       • Native Canvas    |
|  • EPUB 2/3 (OPF+XHTML)        • Measure & Word Spacing      (Simulation/Curl)|
|  • MOBI / AZW3 (PalmDOC)       • Page Splitter             • Compose Pager    |
|  • PDF (PdfiumCore)            • Unified Locator           • Vertical Scroll  |
+-------------------------------------------------------------------------------+
```

---

## 二、模块化工程划分 (Modularization)

为保证业务解耦、降低编译耗时与支持高度单元测试，工程采用多 Module（多 Gradle 模块）组织：

```
Reader_app/
├── app/                           # 应用入口、Application、Hilt 依赖装配、全局 Navigation
├── core/
│   ├── common/                    # 通用工具类、协程扩展、结果封装 (Result/Dispatchers)
│   ├── model/                     # 跨模块核心领域实体 (Book, Chapter, Locator 等)
│   ├── database/                  # Room 数据库定义、Entities、DAOs、Migrations
│   ├── datastore/                 # 用户偏好设置（阅读排版字体、主题色、操作习惯）
│   └── designsystem/              # Design Token、Compose 基础组件、Material 3 主题
├── engine/
│   ├── parser/                    # 多格式解析统一接口与工厂
│   │   ├── txt/                   # TXT 高速分块映射与正则目录探测
│   │   ├── epub/                  # EPUB 2/3 标准容器与 XHTML 抽取
│   │   ├── mobi/                  # MOBI / AZW3 PalmDOC 格式解包
│   │   └── pdf/                   # PDF 元数据与页面适配
│   ├── typography/                # 文本测量、中文避头尾断行算法、页面分割器
│   └── render/
│       ├── canvas/                # Native Canvas 视图（仿真翻页、覆盖翻页、平移翻页）
│       ├── compose/               # Compose 连续垂直滚动视图
│       └── pdfium/                # PDF 页面渲染与手势缩放支持
├── feature/
│   ├── bookshelf/                 # 书架浏览、文件导入、分类分组、搜索与排序
│   ├── reader/                    # 阅读主容器、目录侧边栏、快捷菜单、字号行距面板
│   ├── settings/                  # 排版预设、翻页习惯、备份与恢复（WebDAV）
│   └── stats/                     # 历史阅读记录、日历热力图、阅读时长统计
└── docs/                          # 项目架构、开源调研报告与开发路线图
```

---

## 三、核心领域数据模型 (Core Domain Models)

### 1. 书籍与章节定义
```kotlin
// core/model/src/main/kotlin/.../Book.kt
data class Book(
    val id: Long = 0,
    val title: String,
    val author: String = "未知作者",
    val coverPath: String? = null,
    val uriString: String,              // SAF content:// 或绝对路径
    val format: BookFormat,             // TXT, EPUB, MOBI, AZW3, PDF
    val fileSize: Long,
    val totalChapters: Int = 0,
    val addTime: Long = System.currentTimeMillis(),
    val lastReadTime: Long = System.currentTimeMillis(),
    val archivePath: String? = null     // 缓存提取临时路径
)

enum class BookFormat(val extension: String, val mimeType: String) {
    TXT("txt", "text/plain"),
    EPUB("epub", "application/epub+zip"),
    MOBI("mobi", "application/x-mobipocket-ebook"),
    AZW3("azw3", "application/vnd.amazon.ebook"),
    PDF("pdf", "application/pdf")
}

data class Chapter(
    val id: Long = 0,
    val bookId: Long,
    val index: Int,
    val title: String,
    val startOffset: Long,              // 文件/文本流内起始偏移
    val endOffset: Long,                // 结束偏移
    val contentPath: String? = null     // EPUB 中对应的 XHTML 相对路径
)
```

### 2. 统一阅读定位符 (Unified Locator)
借鉴 W3C / Readium 设计，保证无论字号、行距、屏幕横竖屏如何变更，阅读位置均能无缝自适应映射：
```kotlin
data class ReadLocator(
    val bookId: Long,
    val chapterIndex: Int,
    val chapterTitle: String,
    val charOffset: Int,                // 章节内字符绝对偏移量 (0 .. chapterLength)
    val progression: Float,             // 进度比例 (0.0f .. 1.0f)
    val pageIndexInChapter: Int = 0,    // 当前排版下的页码快照（仅供即时恢复）
    val totalPagesInChapter: Int = 1,
    val updateTime: Long = System.currentTimeMillis()
)
```

### 3. 阅读排版配置 (ReaderConfig)
```kotlin
data class ReaderConfig(
    val fontSizeSp: Float = 18f,
    val lineHeightMultiplier: Float = 1.6f,   // 行高倍数
    val paragraphSpacingDp: Float = 12f,      // 段落间距
    val letterSpacingEm: Float = 0.05f,       // 字间距
    val firstLineIndentSpaces: Int = 2,       // 中文首行空两格
    val pagePaddingHorizontalDp: Float = 16f,
    val pagePaddingVerticalDp: Float = 24f,
    val customFontPath: String? = null,       // 用户外挂字体路径
    val pageTurnAnimation: PageTurnAnimation = PageTurnAnimation.SIMULATION, // 仿真, 覆盖, 平移, 滚动, 无
    val themePreset: ReaderThemePreset = ReaderThemePreset.DEFAULT_LIGHT,
    val keepScreenOn: Boolean = true,
    val volumeKeyPageTurn: Boolean = true     // 音量键翻页
)

enum class PageTurnAnimation {
    SIMULATION,     // 3D 拟真仿真纸张折角翻页
    COVER,          // 水平覆盖翻页
    SLIDE,          // 平滑横向滑动
    CONTINUOUS_SCROLL, // 垂直无缝滚动
    NONE            // 瞬间切换
}
```

---

## 四、格式解析管道设计 (Parser Pipeline)

所有的解析器均实现抽象接口 `BookParser`，屏蔽格式底层细节：

```kotlin
interface BookParser {
    suspend fun parseMetadata(uri: Uri): BookMetadata
    suspend fun parseChapters(uri: Uri): List<Chapter>
    suspend fun loadChapterContent(book: Book, chapter: Chapter): ChapterContent
    suspend fun extractCover(uri: Uri, outputCacheFile: File): Boolean
}
```

### 1. TXT 解析管道
- **编码探测**：利用 `UniversalDetector` (juniversalchardet) 读取头部 4KB 判定（UTF-8, GBK, GB2312, UTF-16LE 等）；
- **内存安全机制**：
  - 文件超过 2MB 时，禁止一次性 `readText()`，采用 `RandomAccessFile` / `FileChannel`；
  - 异步正则分章引擎：后台并发分块提取章节标题与偏移量，主线程通过 `Flow<List<Chapter>>` 流式刷新，实现小说“秒点秒开”；
- **目录规则库**：内置多套中文小说分章规则，例如：
  - `第[0-9一二三四五六七八九十百千万]+[章回节卷篇].*`
  - `Chapter\s+[0-9]+.*`
  - 用户可自定义正则表达式。

### 2. EPUB 解析管道
- **ZIP 流式解包**：
  - 遍历读取 `META-INF/container.xml` $\to$ 解析 OPF 主文件相对路径；
  - 解析 `content.opf`：通过 SAX / XmlPullParser 提取 `<metadata>`（书名、作者）、`<manifest>`（资源 ID 映射表）、`<spine>`（章节顺序列表）；
  - 解析 `toc.ncx` (EPUB 2) 或 `nav.xhtml` (EPUB 3) 获取完整目录层级；
  - 章节内容提取：使用轻量 `Jsoup` 清理网页垃圾元素，提取纯净段落与图片 URI，保持段落语义。

### 3. MOBI / AZW3 解析管道
- 解析 PalmDOC PDB Header，读取 Record 索引表；
- 解压缩 PalmDOC LZ77 算法数据块；
- 对 AZW3（KF8）结构提取内置的标准 EPUB 归档流，无缝复用 EPUB 解析管道。

---

## 五、自研高性能 Canvas 分页排版引擎 (Typography & Canvas Engine)

对于小说读者，排版质量与翻页手感直接决定产品成败。本项目研发的核心原生 Canvas 分页引擎技术规范如下：

### 1. 中文排版断行与避头尾法则 (Kinsoku Shori)
- **中文标点禁则**：
  - **行首禁则**：以下字符绝不能出现在一行开头：
    `， 。 、 ； ： ？ ！ ） 』 】 〉 》 ” ’ … — ％ ‰ ℃`
    *处理策略*：当排版算法计算到某行最后一个字后紧接行首禁则符号时，将前一个字符与该标点一同挤压或推移至下一行。
  - **行尾禁则**：以下字符绝不能出现在一行末尾：
    `（ 『 【 〈 《 “ ‘`
    *处理策略*：将该前标点与其后的第一个文字共同推入下一行首。
- **两端对齐 (Justified Alignment)**：
  - 当一行文字排完后，剩余空白宽度（`remainingWidth`）均匀分配到该行每个字符之间的间距中，使整页左右边缘完全平齐，形成优雅的书页网格感。

### 2. 双缓冲 Canvas 仿真翻页技术规范 (3D Curl Simulation)
- **贝塞尔曲线建模**：
  - 用户手指从屏幕边缘拖动时，以触控点 $A(x_1, y_1)$ 与屏幕对角端点 $F$ 连线的中垂线与屏幕边缘的交点建立二次贝塞尔曲线控制点；
  - 分离计算出：
    1. **当前页背部折角区域**（绘制阴影渐变、纸张半透明背面）；
    2. **下一页可见区域**（底层可见内容）；
    3. **当前页可见正表面**。
- **离屏双缓冲与硬件加速**：
  - 使用两个独立 `Bitmap` 缓冲前后两页的静态排版内容，翻页拖拽中仅进行矩阵变换（Matrix Transform）与 Path 裁剪（`clipPath`），绝不实时重新排版，确保锁定 60/120 FPS 满帧刷新。

---

## 六、数据持久化与存储安全策略

1. **Room 数据库设计**：
   - `books`: 书籍基本信息、文件 URI、格式、最后阅读时间；
   - `chapters`: 章节索引与偏移位置；
   - `read_locators`: 阅读进度定位历史；
   - `reading_logs`: 每日阅读时长与阅读字数流水统计（用于生成阅读日历）。
2. **Android 存储规范 (Scoped Storage)**：
   - 严格采用 Storage Access Framework (SAF) 获取持久化读取权限（`takePersistableUriPermission`）；
   - 内部缓存：解压临时文件均保存在 `context.cacheDir`，并在书籍移除或清理时自动回收，严防外部存储垃圾污染。
