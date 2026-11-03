package com.andrey.scoreboardsettings

import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.FileConfiguration

class Toggle(val id: String, val text: String, val rows: Set<Int>?)

class Settings(config: FileConfiguration) {
    val command: String = config.getString("command", "sbsettings")!!.removePrefix("/").lowercase()
    val aliases: List<String> = config.getStringList("aliases").map { it.removePrefix("/").lowercase() }
    val storage: ConfigurationSection = config.getConfigurationSection("storage") ?: config.createSection("storage")

    val updateTicks: Long = config.getLong("scoreboard.update-ticks", 20).coerceAtLeast(1)
    val title: String = config.getString("scoreboard.title", "")!!
    val lines: List<String> = config.getStringList("scoreboard.lines").take(15)

    val dialogTitle: String = config.getString("dialog.title", "")!!
    val on: String = config.getString("dialog.on", "&aON")!!
    val off: String = config.getString("dialog.off", "&cOFF")!!
    val columns: Int = config.getInt("dialog.columns", 1).coerceAtLeast(1)
    val buttonWidth: Int = config.getInt("dialog.button-width", 250).coerceIn(1, 1024)
    val closeText: String = config.getString("dialog.close.text", "Close")!!
    val closeCommand: String = config.getString("dialog.close.command", "")!!.removePrefix("/")

    val reloaded: String = config.getString("messages.reloaded", "&aScoreboardSettings reloaded.")!!

    val toggles: List<Toggle> = config.getConfigurationSection("dialog.buttons")?.let { section ->
        section.getKeys(false).mapNotNull { id ->
            val button = section.getConfigurationSection(id) ?: return@mapNotNull null
            Toggle(id, button.getString("text", "")!!, rows(button.get("row")))
        }
    } ?: emptyList()

    private fun rows(raw: Any?): Set<Int>? = when (raw) {
        is Number -> setOf(raw.toInt())
        is List<*> -> raw.mapNotNull { it.toString().trim().toIntOrNull() }.toSet()
        else -> raw?.toString()?.trim()?.toIntOrNull()?.let(::setOf)
    }

    fun visibleLines(hidden: Set<String>): List<String>? {
        val off = toggles.filter { it.id in hidden }
        if (off.any { it.rows == null }) return null
        val rows = off.flatMap { it.rows!! }.toSet()
        return lines.filterIndexed { index, _ -> index + 1 !in rows }
    }
}
