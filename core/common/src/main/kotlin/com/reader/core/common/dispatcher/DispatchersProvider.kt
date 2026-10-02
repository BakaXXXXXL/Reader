package com.reader.core.common.dispatcher

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * 协程调度器提供者抽象接口。
 * 遵循 Clean Architecture 规范，在业务和数据层通过接口注入 Dispatcher，
 * 彻底消除业务逻辑对 [Dispatchers.IO] / [Dispatchers.Main] 的硬编码依赖，
 * 并支持单元测试中使用 TestDispatcher 进行精确时间与并发调度。
 */
interface DispatchersProvider {
    /** 主线程调度器，用于 UI 状态收集或轻量观察 */
    val main: CoroutineDispatcher

    /** IO 调度器，用于文件 NIO、Room 数据库、DataStore 访问 */
    val io: CoroutineDispatcher

    /** 计算调度器，用于密集型文本断行、避头尾测算、页面切割、正则匹配 */
    val default: CoroutineDispatcher

    /** 非受限调度器 */
    val unconfined: CoroutineDispatcher
}

/**
 * 生产环境标准协程调度器实现。
 */
class StandardDispatchersProvider : DispatchersProvider {
    override val main: CoroutineDispatcher get() = Dispatchers.Main
    override val io: CoroutineDispatcher get() = Dispatchers.IO
    override val default: CoroutineDispatcher get() = Dispatchers.Default
    override val unconfined: CoroutineDispatcher get() = Dispatchers.Unconfined
}
