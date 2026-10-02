package com.reader.engine.render.model

/**
 * 页眉与页脚绘制配置。
 *
 * @property fontSizeSp 辅助文字（章节名、时间、页码进度）字号 (sp)
 * @property marginHorizontalDp 页眉页脚左右内边距 (dp)
 * @property marginVerticalDp 页眉顶部/页脚底部内边距 (dp)
 * @property showHeader 是否绘制顶部页眉
 * @property showFooter 是否绘制底部页脚
 * @property showBattery 是否在页脚左侧绘制电池图标与电量
 * @property showTime 是否在页脚中间绘制当前系统时间
 * @property showProgress 是否在页脚右侧绘制本章阅读进度百分比
 * @property showPageNumber 是否在页脚右侧绘制当前页码 / 总页码
 * @property batteryIconWidthDp 电池图标主体宽度 (dp)
 * @property batteryIconHeightDp 电池图标主体高度 (dp)
 */
data class HeaderFooterConfig(
    val fontSizeSp: Float = 11f,
    val marginHorizontalDp: Float = 16f,
    val marginVerticalDp: Float = 10f,
    val showHeader: Boolean = true,
    val showFooter: Boolean = true,
    val showBattery: Boolean = true,
    val showTime: Boolean = true,
    val showProgress: Boolean = true,
    val showPageNumber: Boolean = true,
    val batteryIconWidthDp: Float = 22f,
    val batteryIconHeightDp: Float = 11f
) {
    companion object {
        val DEFAULT = HeaderFooterConfig()
    }
}
