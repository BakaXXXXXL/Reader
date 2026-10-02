# 研发全局总约束规范文档 (Development Constraints & Engineering Rules)

> ⚠️ **【特别警示】所有参与本项目研发的智能体（Team Lead 与所有 Teammates）必须严格遵守本文档所规定的所有技术规范、架构铁律与协作边界。任何智能体严禁不按计划自由发挥、严禁擅自偏离架构路线、严禁跨职责破坏代码。**

---

## 一、产品定位与法律合规铁律 (Absolute Legal & Scope Boundary)

1. **纯本地离线定位**：
   - 本项目定位于**纯本地离线全格式电子书/小说阅读器**（支持 TXT, EPUB 2/3, MOBI, AZW/AZW3, PDF 等标准离线文件）。
   - **绝对禁止**引入任何网络小说网站爬虫、在线盗版书源解析引擎或未授权在线小说检索接口（彻底规避 Legado 等项目曾遭遇的法律合规风险）。
   - 唯一允许的网络通信为：**用户自建或授权的私人 WebDAV 远端备份与进度同步**。
2. **零遥测与隐私安全**：
   - 不得集成任何第三方商业统计 SDK 或未经用户授权的数据上传逻辑，所有阅读数据、书签与进度仅存储于本地 SQLite 数据库或用户配置的私有 WebDAV。

---

## 二、技术栈与依赖约束 (Tech Stack Strict Rules)

| 领域 | 强制选型 | 严禁引入/替代方案 |
| :--- | :--- | :--- |
| **编程语言** | Kotlin 2.0+ (启用 K2 编译器) | 严禁使用 Java 编写业务逻辑 |
| **UI 框架** | Jetpack Compose + Material 3 (Material You) | 严禁编写遗留 XML 布局，严禁使用旧版 ViewBinding |
| **架构规范** | Clean Architecture + MVI (Model-View-Intent) | 严禁在 UI Composable 中直接执行数据查询或文件 IO |
| **异步方案** | Kotlin Coroutines + Flow (StateFlow / SharedFlow) | 严禁使用 RxJava、AsyncTask 或原始 Java Thread |
| **持久化** | Room 2.6+ (KSP 驱动) + Jetpack DataStore (Preferences) | 严禁使用过时的 SharedPreferences 或裸 SQLiteOpenHelper |
| **依赖注入** | Dagger Hilt 2.50+ | 严禁使用 Koin 或手动 ServiceLocator 破坏架构依赖树 |
| **底层文件 IO** | NIO (FileChannel / RandomAccessFile) + SAF | 严禁全量一次性读取大文本文件到内存 (`readText()`) |

---

## 三、各核心板块研发硬性技术标准

### 1. 排版与渲染引擎板块 (Typography & Rendering)
- **中文避头尾规则 (Kinsoku Shori)**：
  - **行首禁则**：逗号、句号、顿号、分号、冒号、问号、感叹号、闭括号、闭引号（`，。、；：？！）』】〉》”’…—％`）严禁出现在任何一行的开头。遇此情况必须前移前一字或挤压标点。
  - **行尾禁则**：开括号、开引号（`（『【〈《“‘`）严禁出现在任何一行的末尾。
- **两端网格对齐 (Justified Alignment)**：
  - 中文排版必须支持整齐对齐，首行空两格（按中文字符实际字宽缩进，不得简单追加两个 ASCII 空格）。
- **统一阅读定位器 (Unified Locator)**：
  - 进度记录严禁只存“第几页”；
  - 必须采用 `ReadLocator(bookId, chapterIndex, charOffset, progressionRate)`，无论字号、行距、屏幕旋转如何改变，排版重算后进度必须无缝恢复。
- **翻页交互实现**：
  - 必须提供双缓冲 Native Canvas 仿真翻页（3D 纸张折角阴影、贝塞尔曲线建模），以及水平覆盖、平滑滑动、无动画；
  - 针对垂直模式提供 Compose 连续滚动组件。

### 2. 格式解析管道板块 (Format Parsers)
- **TXT 秒开与内存控制**：
  - 必须支持分块流式加载与内存映射；
  - 必须集成 `juniversalchardet` 自动嗅探字符编码（UTF-8, GBK, GB2312, UTF-16LE 等），杜绝乱码；
  - 正则异步分章：在后台协程流式提取章节目录，第一屏必须在 200ms 内瞬间展示，严禁分章阻塞主线程。
- **EPUB 2/3 标准流式解包**：
  - 必须标准支持 `container.xml` $\to$ `content.opf` $\to$ `toc.ncx` / `nav.xhtml`；
  - 严格提取正文结构，过滤危险无用脚本，妥善处理内嵌图片引用的缓存。
- **MOBI / AZW3 (KF8)**：
  - 解析 PalmDOC PDB Header，支持 LZ77 解码，KF8 必须抽取标准 EPUB 流无缝适配。

### 3. UI 表现层与书架板块 (UI & Features)
- **Edge-to-Edge 沉浸式**：
  - 必须全面适配 Android 15 沉浸式边缘到边缘（Edge-to-Edge）；
  - 阅读界面全屏沉浸，点击中间区域（唤醒/隐藏）顶部和底部控制栏。
- **状态驱动**：
  - 所有 UI 状态必须通过不可变数据类 `UiState` 单向暴露，事件通过 `UiIntent` / `Event` 分发，禁止状态双向穿透。

---

## 四、团队协作与写权限边界 (Write Scope & Teammate Rules)

每个智能体队友分配有专属的责任范围与 `write_scopes`，**严禁跨范围随意修改他人文件**：

| 智能体角色 (Agent Name) | 专职责任领域 | 独立写入范围 (Write Scopes) |
| :--- | :--- | :--- |
| **`core-infra-agent`** | 基础设施、数据模型、Room 数据库、DataStore、Gradle 配置 | `core/model/`, `core/common/`, `core/database/`, `core/datastore/`, `gradle/`, `*.gradle.kts` |
| **`engine-agent`** | 格式解析管道 (TXT/EPUB/MOBI/PDF)、排版测算、避头尾法则、Canvas 翻页渲染 | `engine/parser/`, `engine/typography/`, `engine/render/` |
| **`feature-ui-agent`** | Material 3 沉浸界面、书架、阅读控制器、目录抽屉、排版面板、App 组装 | `core/designsystem/`, `feature/`, `app/` |

---

## 五、质量验收与交付规范

1. **杜绝“空壳代码”**：严禁提交只有 `// TODO` 或未完成伪代码的空实现。关键排版算法、解析器与数据库交互必须提供完整、逻辑闭环的代码。
2. **单元测试与自验证**：
   - 排版引擎必须具备避头尾法则、断行计算的单测用例；
   - 编码探测与分章引擎必须具备多编码验证用例；
3. **一致的代码风格**：
   - 遵循 Kotlin 官方代码风格规范，合理划分包名与文件；
   - 所有公开接口与核心算法方法必须附有详细的 KDoc 注释。
