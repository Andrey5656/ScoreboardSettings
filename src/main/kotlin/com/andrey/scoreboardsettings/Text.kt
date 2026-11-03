package com.andrey.scoreboardsettings

import me.clip.placeholderapi.PlaceholderAPI
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.entity.Player

object Text {
    private val legacy = LegacyComponentSerializer.builder()
        .character('&')
        .hexCharacter('#')
        .hexColors()
        .build()

    fun parse(raw: String): Component =
        legacy.deserialize(raw.replace('§', '&')).decoration(TextDecoration.ITALIC, false)

    fun papi(player: Player, raw: String): String {
        if (raw.indexOf('%') < 0 || !Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) return raw
        return PlaceholderAPI.setPlaceholders(player, raw)
    }
}
