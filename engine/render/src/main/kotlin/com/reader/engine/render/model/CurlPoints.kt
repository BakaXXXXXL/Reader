package com.reader.engine.render.model

/**
 * 拟真 3D 仿真翻页几何模型数据类。
 * 封装由触控点 A 与屏幕角落端点 F 经中垂线与二次贝塞尔曲线推演出的全部控制点集合。
 *
 * 坐标系约定：以屏幕左上角为原点 (0, 0)，向右为 X 正向，向下为 Y 正向。
 *
 * @property a 触控拖拽点（纸张翻折顶点）
 * @property f 翻页角端点（如右下角 (width, height) 或右上角 (width, 0)）
 * @property g 线段 AF 的几何中点
 * @property e AF 中垂线与水平边界 (y = f.y) 的交点
 * @property h AF 中垂线与垂直边界 (x = f.x) 的交点
 * @property c 水平边界上的贝塞尔曲线切点/起点
 * @property j 垂直边界上的贝塞尔曲线切点/起点
 * @property b 水平折边贝塞尔曲线顶点/控制点
 * @property k 垂直折边贝塞尔曲线顶点/控制点
 * @property d 纸张背面折角水平侧控制点
 * @property i 纸张背面折角垂直侧控制点
 * @property corner 本次翻折对应的锚点角落
 */
data class CurlPoints(
    val a: TouchPoint = TouchPoint.ZERO,
    val f: TouchPoint = TouchPoint.ZERO,
    val g: TouchPoint = TouchPoint.ZERO,
    val e: TouchPoint = TouchPoint.ZERO,
    val h: TouchPoint = TouchPoint.ZERO,
    val c: TouchPoint = TouchPoint.ZERO,
    val j: TouchPoint = TouchPoint.ZERO,
    val b: TouchPoint = TouchPoint.ZERO,
    val k: TouchPoint = TouchPoint.ZERO,
    val d: TouchPoint = TouchPoint.ZERO,
    val i: TouchPoint = TouchPoint.ZERO,
    val corner: TouchCorner = TouchCorner.BOTTOM_RIGHT
) {
    /**
     * 校验所有关键控制点是否在有效数学范围内（无 NaN、无无穷大）。
     */
    val isValid: Boolean
        get() = a.isValid && f.isValid && g.isValid && e.isValid &&
                h.isValid && c.isValid && j.isValid && b.isValid &&
                k.isValid && d.isValid && i.isValid

    /**
     * 计算当前仿真翻折的角度（弧度制）。
     */
    val foldAngleRad: Float
        get() {
            val dx = a.x - f.x
            val dy = a.y - f.y
            return kotlin.math.atan2(dy, dx)
        }

    /**
     * 翻折深度：触控点 A 到角落 F 的距离。
     */
    val curlDepth: Float
        get() = a.distanceTo(f)
}
