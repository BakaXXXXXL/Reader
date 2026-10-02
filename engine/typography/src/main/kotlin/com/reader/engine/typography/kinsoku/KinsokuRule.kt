package com.reader.engine.typography.kinsoku

/**
 * 中文排版避头尾法则 (Kinsoku Shori / 标点禁则) 完整实现。
 *
 * 遵循现代中文及东亚出版物排版标准规范：
 * 1. 【行首禁则】：句号、逗号、顿号、分号、冒号、问号、叹号、右引号、右括号等不得出现在行首；
 *    若排到行尾后紧随行首禁则符，必须把前一字连同该符号一同推移至下一行首；
 * 2. 【行尾禁则】：左引号、左括号、前置货币符等不得出现在行末；
 *    若排到行末遇行尾禁则符，必须将其与后续第一个字一同移至下一行首；
 * 3. 【连贯标点处理】：破折号 (——)、省略号 (……) 及重叠标点 (？！) 严禁断裂于行首行尾；
 * 4. 【中西文混排避断】：英文单词不应被生硬拆断到两行，除非单词长度超过整行宽度；
 * 5. 【死循环防护】：保证任何极端文本（如整行全为标点符号）下每行至少保留 1 个字符，杜绝空行死循环。
 */
object KinsokuRule {

    /**
     * 行首禁则字符集 (不得出现在行首的标点与符号)。
     * 包含中文全角句读标点、闭合括号、闭合引号、连接符、单位百分号等。
     */
    val FORBIDDEN_LINE_START: Set<Char> = setOf(
        // 中文常用标点 (句读)
        '，', '。', '、', '；', '：', '？', '！',
        // 闭括号
        '）', '』', '】', '〉', '》', '」', '〗', '〕', '］', '｝',
        // 闭引号
        '”', '’', '»',
        // 连续符号与修饰符
        '…', '—', '～', '·', '•',
        // 百分号与单位符号
        '％', '‰', '℃', '℉', '°', '¢',
        // 对应半角/西文标点
        ')', ']', '}', '>', ',', '.', ';', ':', '?', '!', '%'
    )

    /**
     * 行尾禁则字符集 (不得出现在行末的标点与前置符号)。
     * 包含开括号、开引号、前置货币符号等。
     */
    val FORBIDDEN_LINE_END: Set<Char> = setOf(
        // 开括号
        '（', '『', '【', '〈', '《', '「', '〖', '〔', '［', '｛',
        // 开引号
        '“', '‘', '«',
        // 货币符号与前置符号
        '￥', '¥', '$', '€', '£',
        // 对应半角开括号
        '(', '[', '{', '<'
    )

    /**
     * 判断指定字符是否为行首禁则符 (不可作为一行之首)。
     */
    fun isForbiddenLineStart(char: Char): Boolean = char in FORBIDDEN_LINE_START

    /**
     * 判断指定字符是否为行尾禁则符 (不可作为一行之末)。
     */
    fun isForbiddenLineEnd(char: Char): Boolean = char in FORBIDDEN_LINE_END

    /**
     * 判断字符是否属于 ASCII 西文单词构成字符 (字母、数字、连字符)。
     */
    fun isWordConstituent(char: Char): Boolean {
        return (char in 'a'..'z') || (char in 'A'..'Z') || (char in '0'..'9') || char == '_'
    }

    /**
     * 根据避头尾法则与英文单词完整性，对初步测算得到的断行截断点进行精确微调。
     *
     * @param text 当前正在排版的段落或文本序列
     * @param lineStart 本行首字符在 [text] 中的起始索引 (包含)
     * @param tentativeEnd 初步宽度测算下能够容纳的最大字符截断索引 (开区间，即当前行包含 [lineStart until tentativeEnd])
     * @param paragraphEnd 当前段落在 [text] 中的结束索引 (开区间)
     * @return 经过避头尾禁则微调后的安全断行截断索引 (满足 lineStart < return <= tentativeEnd)
     */
    fun adjustLineBreak(
        text: CharSequence,
        lineStart: Int,
        tentativeEnd: Int,
        paragraphEnd: Int
    ): Int {
        val safeTentativeEnd = tentativeEnd.coerceIn(lineStart + 1, paragraphEnd)

        // 若整段已能完全容纳在本行内，无需微调截断
        if (safeTentativeEnd >= paragraphEnd) {
            return paragraphEnd
        }

        var end = safeTentativeEnd

        // 步骤 1: 西文单词断行保护 (避免在英文单词中间强行腰斩)
        // 若断点正好位于西文字符之间，且该单词不是整行占满，尝试回退到单词词首
        if (end > lineStart + 1 && end < paragraphEnd) {
            val charBefore = text[end - 1]
            val charAfter = text[end]
            if (isWordConstituent(charBefore) && isWordConstituent(charAfter)) {
                var wordStart = end - 1
                while (wordStart > lineStart && isWordConstituent(text[wordStart - 1])) {
                    wordStart--
                }
                // 只有当回退后的词首在行首之后时才回退，避免单词长于一行导致死循环
                if (wordStart > lineStart) {
                    end = wordStart
                }
            }
        }

        // 步骤 2: 循环调整避头尾禁则 (行首禁则与行尾禁则联动校验)
        // 核心不变量：必须确保 end >= lineStart + 1 (本行至少保留 1 个字符)
        var adjusted = true
        var iterationCount = 0
        val maxIterations = (safeTentativeEnd - lineStart) + 4

        while (adjusted && iterationCount < maxIterations) {
            adjusted = false
            iterationCount++

            // 情况 A: 行尾禁则检查
            // 若当前行末字符 text[end - 1] 是行尾禁则字符 (如开括号、开引号)，
            // 必须将其移至下一行首
            while (end > lineStart + 1 && isForbiddenLineEnd(text[end - 1])) {
                end--
                adjusted = true
            }

            // 情况 B: 行首禁则检查
            // 若下一行首字符 text[end] 是行首禁则字符 (如逗号、句号、闭引号等)，
            // 必须把当前行末的字符连同该符号一同推至下一行
            if (end < paragraphEnd && isForbiddenLineStart(text[end])) {
                // 如果当前行还有多于 1 个字符，将行末字符推向下一行
                if (end > lineStart + 1) {
                    end--
                    adjusted = true
                    // 如果推完后，当前行末还是行尾禁则符，会在外层循环下一轮被情况 A 捕获处理
                }
            }
        }

        // 安全兜底防护：无论标点如何极端，每行至少容纳 1 个字符，杜绝 0 步进死循环
        return end.coerceIn(lineStart + 1, safeTentativeEnd)
    }
}
