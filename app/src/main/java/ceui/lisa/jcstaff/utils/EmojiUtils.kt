package ceui.lisa.jcstaff.utils

private const val EMOJI_BASE_URL = "https://s.pximg.net/common/images/emoji/"

data class EmojiItem(
    val name: String,
    val resourceFile: String
) {
    val url: String get() = EMOJI_BASE_URL + resourceFile
}

sealed class CommentPart {
    data class TextPart(val text: String) : CommentPart()
    data class EmojiPart(val emoji: EmojiItem) : CommentPart()
}

private val allEmojis: List<EmojiItem> = listOf(
    EmojiItem("normal", "101.png"),
    EmojiItem("surprise", "102.png"),
    EmojiItem("serious", "103.png"),
    EmojiItem("heaven", "104.png"),
    EmojiItem("happy", "105.png"),
    EmojiItem("excited", "106.png"),
    EmojiItem("sing", "107.png"),
    EmojiItem("cry", "108.png"),
    EmojiItem("normal2", "201.png"),
    EmojiItem("shame2", "202.png"),
    EmojiItem("love2", "203.png"),
    EmojiItem("interesting2", "204.png"),
    EmojiItem("blush2", "205.png"),
    EmojiItem("fire2", "206.png"),
    EmojiItem("angry2", "207.png"),
    EmojiItem("shine2", "208.png"),
    EmojiItem("panic2", "209.png"),
    EmojiItem("normal3", "301.png"),
    EmojiItem("satisfaction3", "302.png"),
    EmojiItem("surprise3", "303.png"),
    EmojiItem("smile3", "304.png"),
    EmojiItem("shock3", "305.png"),
    EmojiItem("gaze3", "306.png"),
    EmojiItem("wink3", "307.png"),
    EmojiItem("happy3", "308.png"),
    EmojiItem("excited3", "309.png"),
    EmojiItem("love3", "310.png"),
    EmojiItem("normal4", "401.png"),
    EmojiItem("surprise4", "402.png"),
    EmojiItem("serious4", "403.png"),
    EmojiItem("love4", "404.png"),
    EmojiItem("shine4", "405.png"),
    EmojiItem("sweat4", "406.png"),
    EmojiItem("shame4", "407.png"),
    EmojiItem("sleep4", "408.png"),
    EmojiItem("heart", "501.png"),
    EmojiItem("teardrop", "502.png"),
    EmojiItem("star", "503.png"),
)

private val emojiMap: Map<String, EmojiItem> = allEmojis.associateBy { it.name }

private val emojiRegex = Regex("""\(([a-z0-9]+)\)""")

fun getAllEmojis(): List<EmojiItem> = allEmojis

fun parseCommentWithEmojis(text: String): List<CommentPart> {
    val parts = mutableListOf<CommentPart>()
    var lastIndex = 0

    emojiRegex.findAll(text).forEach { match ->
        val emojiName = match.groupValues[1]
        val emoji = emojiMap[emojiName]
        if (emoji != null) {
            if (match.range.first > lastIndex) {
                parts.add(CommentPart.TextPart(text.substring(lastIndex, match.range.first)))
            }
            parts.add(CommentPart.EmojiPart(emoji))
            lastIndex = match.range.last + 1
        }
    }

    if (lastIndex < text.length) {
        parts.add(CommentPart.TextPart(text.substring(lastIndex)))
    }

    return parts
}
