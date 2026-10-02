package com.reader.engine.parser.txt

/**
 * 章节标题匹配规则定义。
 *
 * @property id 规则唯一标识
 * @property name 规则显示名称
 * @property regex 对应的正则表达式
 * @property priority 规则匹配优先级 (数值越小优先级越高)
 */
data class TxtChapterRule(
    val id: String,
    val name: String,
    val regex: Regex,
    val priority: Int = 100
)

/**
 * 预置丰富的中文网络小说及常见格式电子书分章规则库。
 */
object TxtChapterPatterns {

    /** 核心规则 1：标准中文“第X[章回节卷篇部集话幕折]” */
    val RULE_CHINESE_STANDARD = TxtChapterRule(
        id = "rule_chinese_standard",
        name = "标准中文章节 (第X章/回/节/卷/篇/部/集/话/幕/折)",
        regex = Regex("""^\s*第\s*[0-9一二三四五六七八九十百千万零两]+\s*[章回节卷篇部集话幕折](\s+[^\r\n]{0,60})?${'$'}"""),
        priority = 10
    )

    /** 核心规则 2：卷标“第X卷 / 卷X” */
    val RULE_VOLUME = TxtChapterRule(
        id = "rule_volume",
        name = "分卷标 (第X卷/卷X)",
        regex = Regex("""^\s*(第\s*[0-9一二三四五六七八九十百千万零两]+\s*卷|卷\s*[0-9一二三四五六七八九十百千万零两]+)(\s+[^\r\n]{0,60})?${'$'}"""),
        priority = 20
    )

    /** 核心规则 3：特殊章节 (楔子/序章/前言/尾声/番外/终章/大结局等) */
    val RULE_SPECIAL_CHAPTERS = TxtChapterRule(
        id = "rule_special_chapters",
        name = "特殊章节 (楔子/序章/番外/大结局等)",
        regex = Regex("""^\s*(楔子|序章|序言|引言|引子|后记|尾声|番外[0-9一二三四五六七八九十]*|结语|前言|写在前面|写在最后|终章|大结局)(\s+[^\r\n]{0,60})?${'$'}"""),
        priority = 30
    )

    /** 核心规则 4：数字序号型 (1. 标题 / 一、标题) */
    val RULE_NUMBER_BULLET = TxtChapterRule(
        id = "rule_number_bullet",
        name = "序号条目章节 (1. / 一、)",
        regex = Regex("""^\s*[0-9一二三四五六七八九十百千万零两]+[、.．]\s*[^\r\n]{1,60}${'$'}"""),
        priority = 40
    )

    /** 核心规则 5：英文 Chapter 格式 */
    val RULE_ENGLISH_CHAPTER = TxtChapterRule(
        id = "rule_english_chapter",
        name = "英文 Chapter 章节",
        regex = Regex("""(?i)^\s*chapter\s+[0-9ivxlcdm]+(\s*[:.\-\s]\s*[^\r\n]{0,60})?${'$'}"""),
        priority = 50
    )

    /** 核心规则 6：英文 Section 格式 */
    val RULE_ENGLISH_SECTION = TxtChapterRule(
        id = "rule_english_section",
        name = "英文 Section 章节",
        regex = Regex("""(?i)^\s*section\s+[0-9ivxlcdm]+(\s*[:.\-\s]\s*[^\r\n]{0,60})?${'$'}"""),
        priority = 60
    )

    /** 核心规则 7：附录 */
    val RULE_APPENDIX = TxtChapterRule(
        id = "rule_appendix",
        name = "附录",
        regex = Regex("""^\s*附录\s*[0-9一二三四五六七八九十]*(\s+[^\r\n]{0,60})?${'$'}"""),
        priority = 70
    )

    /** 默认全量激活规则集合 (按优先级降序排列) */
    val DEFAULT_RULES: List<TxtChapterRule> = listOf(
        RULE_CHINESE_STANDARD,
        RULE_VOLUME,
        RULE_SPECIAL_CHAPTERS,
        RULE_NUMBER_BULLET,
        RULE_ENGLISH_CHAPTER,
        RULE_ENGLISH_SECTION,
        RULE_APPENDIX
    ).sortedBy { it.priority }
}

/**
 * 章节标题规则匹配器。
 *
 * 负责过滤误报（如正文中碰巧包含“第一章...”的长句子）、提取清洗干净的章节标题。
 */
class TxtChapterRuleMatcher(
    rules: List<TxtChapterRule> = TxtChapterPatterns.DEFAULT_RULES,
    customPatterns: List<Regex> = emptyList(),
    private val maxTitleLength: Int = 80
) {
    private val activeRules: List<TxtChapterRule>

    init {
        val merged = rules.toMutableList()
        customPatterns.forEachIndexed { index, regex ->
            merged.add(
                TxtChapterRule(
                    id = "custom_$index",
                    name = "自定义规则 $index",
                    regex = regex,
                    priority = 5 // 自定义规则默认优先匹配
                )
            )
        }
        activeRules = merged.sortedBy { it.priority }
    }

    // 句尾典型标点符号：出现这些标点说明大概率是普通正文句子，而非章节标题
    private val sentenceEndingPunctuation = setOf('。', '！', '？', '，', '；', '：', '”', '’', '…')

    /**
     * 判断一行文本是否为章节标题。
     *
     * @param line 候选文本行
     * @return 如果匹配成功返回规范化清洗后的标题，否则返回 null
     */
    fun match(line: CharSequence): String? {
        val trimmed = line.trim()
        val len = trimmed.length

        // 长度过滤：章节标题长度通常在 2 到 80 字符之间
        if (len < 2 || len > maxTitleLength) return null

        // 标点过滤：结尾为句子标点的通常是正文描述（例如：“第一章的内容就此告一段落。”）
        val lastChar = trimmed.last()
        if (lastChar in sentenceEndingPunctuation) {
            return null
        }

        // 依次执行正则匹配
        for (rule in activeRules) {
            if (rule.regex.matches(trimmed)) {
                return normalizeTitle(trimmed.toString())
            }
        }

        return null
    }

    /**
     * 清理并规范化章节标题中的空白字符。
     */
    private fun normalizeTitle(raw: String): String {
        return raw.replace(Regex("""\s+"""), " ").trim()
    }
}
