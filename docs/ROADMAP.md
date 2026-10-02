# 项目长期开发路线图与实施计划 (Roadmap & Implementation Plan)

> 本路线图制定了 Android 离线小说/全格式电子书阅读器的阶段性实施目标、核心里程碑、交付产物与验证标准。遵循敏捷迭代、引擎先行的工程开发原则。

---

## 阶段概览 (Phases Overview)

```
[Phase 0] 工程基建与核心规范 (Project Setup & Architecture Baseline)
    │
    ▼
[Phase 1] 核心排版引擎与 TXT 小说专精 (Core Typography & TXT Perfection)
    │
    ▼
[Phase 2] 标准电子书格式生态 (EPUB, MOBI/AZW3, PDF Pipeline)
    │
    ▼
[Phase 3] 现代书架、全能阅读控制器与个性化 (Bookshelf & Reader Experience)
    │
    ▼
[Phase 4] 辅助增强与数据同步 (TTS, WebDAV, Reading Stats)
    │
    ▼
[Phase 5] 极限性能优化、稳定性与开源发布 (Polish & Production Release)
```

---

## 详细实施计划

### 🎯 Phase 0：工程基建与核心规范搭建 (第 1 - 2 周)
- **目标**：建立现代化多 Module Gradle 工程骨架、CI/CD 构建流水线与基础 Design System。
- **核心任务**：
  1. **构建系统配置**：
     - 配置 Gradle Version Catalogs (`libs.versions.toml`)，统一依赖管理；
     - 配置 Android SDK（`compileSdk 35`, `minSdk 26`, `targetSdk 35`）；
     - 启用 Kotlin 2.0+ (K2 编译器) 与 Jetpack Compose 编译器插件。
  2. **多模块拆分**：
     - 创建 `app`, `core:model`, `core:common`, `core:database`, `core:datastore`, `core:designsystem`；
     - 创建 `engine:parser`, `engine:typography`, `engine:render`；
     - 创建 `feature:bookshelf`, `feature:reader`, `feature:settings`, `feature:stats`。
  3. **架构基础库接入**：
     - Hilt 依赖注入装配；
     - Room 数据库基础脚手架；
     - Material 3 基础色彩方案、动态取色 (Dynamic Color) 与沉浸式边缘到边缘 (Edge-to-Edge) 主题。
- **里程碑产物**：
  - 工程可直接成功编译运行并展示带 Edge-to-Edge 的应用骨架。

---

### 🎯 Phase 1：自研排版引擎与 TXT 小说专精 (第 3 - 5 周)
- **目标**：打造业界顶尖的小说排版算法与极速 TXT 文件秒开体验。
- **核心任务**：
  1. **TXT 解析与内存优化**：
     - 基于 NIO `FileChannel` / `RandomAccessFile` 的分块流式读取器；
     - 集成 `juniversalchardet` 自动识别 UTF-8 / GBK / GB2312 / UTF-16；
     - 智能异步正则分章引擎，毫秒级提取目录并在后台生成章节索引。
  2. **文本排版与分页计算核心 (Typography Engine)**：
     - 实现字符精确测量（`TextPaint` 宽度计算）；
     - 实现**中文避头尾法则**（标点禁则）与两端对齐（Justification）微调；
     - 首行缩进、行高倍数、段间距、字间距自适应重排；
     - 构建抽象 `ReadLocator`，实现字体调节与旋转屏幕后阅读进度 100% 精确恢复。
  3. **翻页与渲染视图 (Render View)**：
     - 实现双缓冲 Native Canvas 仿真翻页（3D 纸张折角阴影、贝塞尔曲线建模）；
     - 实现覆盖翻页（Cover）、水平滑动翻页（Slide）与无动画翻页；
     - 页眉页脚实时绘制（章节名、电量图标、当前时间、精确页码、进度百分比）。
- **里程碑产物**：
  - 支持直接导入 10MB+ 的 TXT 大文件，实现 0.2 秒内闪电打开，拥有如丝般顺滑的仿真翻页手感。

---

### 🎯 Phase 2：标准电子书格式生态完善 (第 6 - 8 周)
- **目标**：全面支持主流电子书格式（EPUB 2/3、MOBI、AZW3、PDF）。
- **核心任务**：
  1. **EPUB 2/3 解析管道**：
     - 流式解包 `container.xml`、`content.opf`（Manifest/Spine）与 `toc.ncx` / `nav.xhtml`；
     - 封面图片抽取并建立缩略图缓存；
     - 基于轻量 XHTML 抽取器提取图文结构，保留正文排版与插图占位；
     - 对接排版引擎，支持小说类 EPUB 图文排版与图文点击大图预览。
  2. **MOBI / AZW3 (KF8) 解析管道**：
     - PalmDOC 容器头解析与 LZ77 数据块解压；
     - 提取 AZW3 内部 EPUB 流并对接统一渲染器；
     - 提取书籍元数据（标题、作者、封面）。
  3. **PDF 文档渲染引擎**：
     - 集成 `PdfiumCore`，实现多分辨率按需渲染；
     - 实现 PDF 双指缩放、手势平移与快速滑动缩略图导航。
- **里程碑产物**：
  - 应用可稳定解析并流畅阅读主流 EPUB、MOBI、AZW3 与 PDF 电子书文件。

---

### 🎯 Phase 3：现代书架与沉浸式阅读控制器 (第 9 - 11 周)
- **目标**：打造极具现代感、交互优雅的 Material 3 书架与全能阅读控制面板。
- **核心任务**：
  1. **书架模块 (Bookshelf)**：
     - 本地文件扫描器与 SAF 单选/多选导入；
     - 书架网格视图与列表视图切换；
     - 书籍拖拽排序、自定义分组/分类标签（如“在读”、“已读”、“完结”）；
     - 快速搜索、最近阅读置顶与批量管理。
  2. **阅读交互面板 (Reader Controls)**：
     - 顶部栏（返回、书签、搜索正文、更多选项）；
     - 底部栏（章节进度滑块、上一章/下一章切换）；
     - 目录抽屉（支持树形层级、正逆序切换、书签管理、章节搜索）；
     - 排版设置弹窗：
       - 字号大小、行距、字距、段距、内边距调节；
       - 自定义外挂字体导入（TTF/OTF）；
       - 预设阅读主题（日间、复古羊皮纸、护眼豆沙绿、水墨屏黑白、OLED 纯黑夜间）；
       - 翻页模式快速切换（仿真 / 覆盖 / 滑动 / 连续上下滚动）。
- **里程碑产物**：
  - 拥有完整的阅读器主交互闭环，界面美观现代，操作顺手自然。

---

### 🎯 Phase 4：辅助增强与数据同步 (第 12 - 13 周)
- **目标**：提供听书/TTS、WebDAV 数据备份同步以及阅读数据看板。
- **核心任务**：
  1. **TTS 语音朗读**：
     - 基于 Android 原生 `TextToSpeech` 引擎与 `MediaSessionService` 前台服务；
     - 锁屏通知控制（播放/暂停、上一句/下一句、倍速调节）；
     - 定时停止（15分钟、30分钟、本章读完）；
     - 边听边读：正文文字随语音高亮同步跟随。
  2. **WebDAV 远端备份与同步**：
     - 支持坚果云、Nextcloud、本地 NAS 等 WebDAV 服务配置；
     - 双向增量同步阅读进度、书签、书架分类与配置；
     - 冲突检测与版本合并。
  3. **阅读数据统计与热力图**：
     - 统计每日阅读时长与阅读字数；
     - 实现类似 GitHub 提交图的“阅读日历热力图”；
     - 生成个人专属阅读报告看板。
- **里程碑产物**：
  - 具备听书能力、跨设备多端进度同步与充满成就感的阅读数据看板。

---

### 🎯 Phase 5：极限性能优化与发布打磨 (第 14 - 15 周)
- **目标**：达到发布级质量，零崩溃、满帧流畅度与完备的开源文档。
- **核心任务**：
  1. **内存与帧率优化**：
     - 使用 Android Studio Profiler 排查内存泄漏（检测 Bitmap 回收、Coroutines 泄露）；
     - Baseline Profiles 基线配置文件接入，大幅缩短应用冷启动耗时；
     - 优化多并发文本测量，确保快速划屏翻页 120 FPS 满帧。
  2. **自动化测试**：
     - 单元测试覆盖核心解析器（TXT/EPUB/MOBI 断言测试）；
     - 排版断行算法测试用例库（中文生僻字、长英文单词、混合标点符号）；
     - UI Benchmark 自动化基准测试。
  3. **发布与合规**：
     - 严谨的开源协议（如 GPL v3 或 Apache 2.0）；
     - 完善的中英文说明文档、截图展示与 F-Droid / GitHub Releases 自动打包发布。
- **里程碑产物**：
  - 达成发布版 APK / AAB，进入开源社区发布与持续维护阶段。
