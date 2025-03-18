package wtf.amari.daisyVotes.utils

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object TextUtils {
    private val mm = MiniMessage.miniMessage()
    private val logFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    object Colors {
        const val PRIMARY = "#3498db"
        const val SECONDARY = "#2ecc71"
        const val ERROR = "#e74c3c"
        const val SUCCESS = "#2ecc71"
        const val WARNING = "#f1c40f"
        const val INFO = "#3498db"
        const val BROADCAST = "#9b59b6"
        const val SYSTEM = "#34495e"
    }

    object Prefix {
        val ERROR = "<${Colors.ERROR}>✖ ".mm()
        val SUCCESS = "<${Colors.SUCCESS}>✔ ".mm()
        val INFO = "<${Colors.INFO}>✦ ".mm()
        val WARNING = "<${Colors.WARNING}>⚠ ".mm()
        val ADMIN = "<${Colors.ERROR}>⚡ ".mm()
        val BROADCAST = "<${Colors.BROADCAST}>» ".mm()
        val SYSTEM = "<${Colors.SYSTEM}>✦ ".mm()
    }

    fun log(
        message: String,
        level: String = "INFO",
        throwable: Throwable? = null,
        additionalContext: Map<String, Any> = mapOf(),
    ) {
        val timestamp = LocalDateTime.now().format(logFormatter)
        val (color, prefix) =
            when (level.uppercase()) {
                "ERROR" -> Colors.ERROR to "✖ "
                "SUCCESS" -> Colors.SUCCESS to "✔ "
                "WARNING" -> Colors.WARNING to "⚠ "
                "BROADCAST" -> Colors.BROADCAST to "» "
                "SYSTEM" -> Colors.SYSTEM to "✦ "
                else -> Colors.INFO to "✦ "
            }

        val detailedMessage =
            buildString {
                append("[$timestamp] ")
                append("[$level] ")
                append(message)
                additionalContext.forEach { (key, value) -> append(" | $key: $value") }
            }

        Bukkit.getConsoleSender().sendMessage("<$color>$prefix$detailedMessage</$color>".mm())

        throwable?.let {
            Bukkit.getConsoleSender().sendMessage(Prefix.ERROR.append("<${Colors.ERROR}>${it.message}</${Colors.ERROR}>".mm()))
            it.printStackTrace()
        }
    }

    fun String.mm(): Component = mm.deserialize(convertLegacyColors()).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE)

    fun String.convertLegacyColors(): String =
        replace(legacyColorRegex) { match ->
            val code = match.groupValues[1]
            val miniMessageColor = legacyColorMap[code] ?: legacyFormattingMap[code] ?: return@replace match.value
            when {
                miniMessageColor == "reset" -> "<reset><color:white>"
                miniMessageColor.startsWith("color:") -> "<${miniMessageColor.removePrefix("color:")}>"
                miniMessageColor.startsWith("format:") -> "<${miniMessageColor.removePrefix("format:")}>"
                else -> "<$miniMessageColor>"
            }
        }

    private val legacyColorMap =
        mapOf(
            "0" to "color:black",
            "1" to "color:dark_blue",
            "2" to "color:dark_green",
            "3" to "color:dark_aqua",
            "4" to "color:dark_red",
            "5" to "color:dark_purple",
            "6" to "color:gold",
            "7" to "color:gray",
            "8" to "color:dark_gray",
            "9" to "color:blue",
            "a" to "color:green",
            "b" to "color:aqua",
            "c" to "color:red",
            "d" to "color:light_purple",
            "e" to "color:yellow",
            "f" to "color:white",
        )

    private val legacyFormattingMap =
        mapOf(
            "k" to "format:obf",
            "l" to "format:bold",
            "m" to "format:strikethrough",
            "n" to "format:underline",
            "o" to "format:italic",
            "r" to "reset",
        )

    private val legacyColorRegex = """&([0-9a-fk-or])""".toRegex()

    fun String.convertNewColors(): String = replace(legacyColorRegex) { match -> "§" + match.groupValues[1] }

    fun String.gradient(vararg colors: String): Component = "<gradient:${colors.joinToString(":")}>$this</gradient>".mm()

    private fun String.escape(): String = replace("'", "\\'")
}
