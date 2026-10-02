# 📚 Reader App — 现代 Android 全格式小说与电子书阅读器

> 一款基于 **Kotlin** 与 **Jetpack Compose** 打造的高性能、纯净本地、现代化 Android 离线小说与电子书阅读器。
> 专为纯粹的阅读体验而生，融合自研高精度中文排版与丝滑拟真翻页引擎，全格式通吃，无广告、不侵权、极速秒开。

---

## 🌟 核心特性 (Features)

- 📖 **主流电子书全格式支持**
  - **TXT**：大文件 NIO 内存映射秒开、`juniversalchardet` 编码自适应、智能多规则异步正则分章。
  - **EPUB 2 / EPUB 3**：流式解包、章节目录层级提取、图文混排、内嵌字体与图片大图预览。
  - **MOBI / AZW / AZW3**：PalmDOC 容器高效解构与 KF8 富文本解析。
  - **PDF**：基于 `PdfiumCore` 矢量渲染，流畅缩放与极速页面导航。
- ⚡ **自研高精度中文排版与渲染引擎**
  - **专业中文排版**：中文字符网格对齐、避头尾法则（标点禁则）、两端对齐（Justified Alignment）、首行空两格、行高/段距/字距微调。
  - **多模式翻页手感**：
    - 拟真 3D 仿真翻页（贝塞尔曲线纸张弯曲与真实折角阴影）；
    - 水平覆盖翻页、平滑滑动翻页、无动画翻页；
    - 现代化连续无缝垂直滚动模式。
  - **自适应阅读定位器 (Unified Locator)**：换字号、换行距、换屏幕方向，阅读进度 100% 精确自适应恢复，绝不迷失跳页。
- 🎨 **现代 Material 3 / Material You 设计语言**
  - 深度支持 Android 12+ 动态取色 (Dynamic Color)；
  - 沉浸式 Edge-to-Edge 边缘全屏体验；
  - 内置经典阅读主题（日间纸白、羊皮复古、护眼豆沙绿、水墨屏极简、OLED 纯黑暗色模式）；
  - 支持导入自定义 TTF / OTF 外部字体。
- 🗂️ **优雅高效的书架管理**
  - 本地单书 / 文件夹批量扫描导入；
  - 网格模式与列表模式自由切换、拖拽排序、自定义分类与标签管理；
  - 阅读历史、进度百分比、最后阅读时间实时显示。
- 🎧 **辅助功能与扩展生态**
  - **TTS 语音听书**：系统原生 TextToSpeech 驱动，支持 MediaSession 后台锁屏控制、倍速调节、定时关闭与语文字同步高亮。
  - **WebDAV 远端同步**：兼容坚果云、Nextcloud 等私有云，多设备增量同步书架与阅读进度。
  - **阅读统计看板**：记录每日阅读时长、字数流水，提供阅读日历热力图。
- 🛡️ **安全与合规**
  - 定位纯粹的本地阅读工具，零未授权爬虫，保障版权安全与数据隐私；
  - 适配 Android 10+ 分区存储 (Scoped Storage)，保障文件访问安全。

---

## 🛠️ 核心技术栈 (Tech Stack)

| 领域 | 选型 | 说明 |
| :--- | :--- | :--- |
| **编程语言** | Kotlin 2.0+ | 启用 K2 编译器，极速构建与强类型安全 |
| **UI 框架** | Jetpack Compose + Material 3 | 现代声明式 UI、沉浸式 Edge-to-Edge |
| **应用架构** | Modern Android Architecture | Clean Architecture + MVI 单向数据流 + 模块化多 Module |
| **异步流式处理** | Kotlin Coroutines + Flow | 高并发后台分章、流式数据响应 |
| **依赖注入** | Dagger Hilt 2.50+ | 模块化解耦与依赖装配 |
| **数据持久化** | Room 2.6+ (KSP) + DataStore | SQLite 高性能元数据管理 + 偏好设置响应式存储 |
| **底层排版渲染** | 自研 Native Canvas + PdfiumCore | 硬件加速双缓冲 Canvas 仿真翻页与 PDF 矢量渲染 |
| **图片加载** | Coil 3 | 现代 Compose 异步图片与封面加载 |
| **网络与同步** | OkHttp + WebDAV 客户端 | 进度与书架轻量云同步 |

---

## 📐 架构设计概览

本项目严格解耦**表现层 (UI)**、**领域层 (Domain)**、**数据层 (Data)** 与 **核心引擎层 (Core Engines)**：

```
app (Application & Navigation)
├── feature:bookshelf        # 书架主页、分类与导入
├── feature:reader           # 沉浸式阅读器容器与控制面板
├── feature:settings         # 排版配置与个性化主题
├── feature:stats            # 阅读数据看板与热力图
├── engine:typography        # 字符测量、避头尾法则与分页算法
├── engine:parser            # TXT, EPUB, MOBI/AZW3, PDF 格式解析器
├── engine:render            # Canvas 仿真/覆盖翻页与 Compose 滚动视图
└── core                     # Common, Database, DataStore, DesignSystem, Model
```

---

## 📚 规划与设计文档导航

项目包含三份深度系统文档，请查阅：
1. **[开源项目调研与经验报告](docs/RESEARCH_OPEN_SOURCE.md)**：深入剖析 Legado、Readium、Book's Story、LightNovelReader 等 6 款优秀开源阅读器的架构优劣与避坑经验。
2. **[系统架构设计与技术规范](docs/ARCHITECTURE.md)**：定义多 Module 划分、核心模型、Unified Locator 定位器模型、TXT 内存映射解析管道以及 Canvas 仿真翻页技术规范。
3. **[长期开发路线图与实施计划](docs/ROADMAP.md)**：分 Phase 0 到 Phase 5 六个阶段的具体任务拆解、时间表与交付里程碑。

---

## 🚀 推荐开发环境要求

- **Android Studio**：Ladybug (2024.2.1) 或更高版本
- **JDK**：OpenJDK 17 或 21
- **Gradle**：8.7+
- **Android SDK**：
  - `compileSdk`: 35
  - `minSdk`: 26 (Android 8.0 Oreo，兼顾 95%+ 现有设备)
  - `targetSdk`: 35 (Android 15)
