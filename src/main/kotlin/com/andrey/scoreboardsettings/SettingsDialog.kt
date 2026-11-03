package com.andrey.scoreboardsettings

import io.papermc.paper.connection.PlayerGameConnection
import io.papermc.paper.dialog.Dialog
import io.papermc.paper.event.player.PlayerCustomClickEvent
import io.papermc.paper.registry.data.dialog.ActionButton
import io.papermc.paper.registry.data.dialog.DialogBase
import io.papermc.paper.registry.data.dialog.action.DialogAction
import io.papermc.paper.registry.data.dialog.type.DialogType
import net.kyori.adventure.key.Key
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

class SettingsDialog(private val plugin: ScoreboardSettings) : Listener {
    fun open(player: Player) {
        val board = plugin.boards.get(player) ?: return
        val settings = plugin.settings
        val buttons = settings.toggles.mapIndexed { index, toggle ->
            val state = if (toggle.id in board.hidden) settings.off else settings.on
            button(toggle.text.replace("%state%", state), "toggle/$index")
        }
        val close = button(settings.closeText, "close")
        val dialog = Dialog.create { factory ->
            factory.empty()
                .base(
                    DialogBase.builder(Text.parse(settings.dialogTitle))
                        .canCloseWithEscape(true)
                        .pause(false)
                        .afterAction(DialogBase.DialogAfterAction.NONE)
                        .build()
                )
                .type(DialogType.multiAction(buttons + close).columns(settings.columns).build())
        }
        player.showDialog(dialog)
    }

    private fun button(text: String, action: String): ActionButton =
        ActionButton.builder(Text.parse(text))
            .width(plugin.settings.buttonWidth)
            .action(DialogAction.customClick(Key.key(NAMESPACE, action), null))
            .build()

    @EventHandler
    fun onClick(event: PlayerCustomClickEvent) {
        val key = event.identifier
        if (key.namespace() != NAMESPACE) return
        val player = (event.commonConnection as? PlayerGameConnection)?.player ?: return
        player.scheduler.run(plugin, {
            val value = key.value()
            if (value == "close") {
                plugin.packets.closeDialog(player)
                val command = plugin.settings.closeCommand
                if (command.isNotEmpty()) player.performCommand(command)
                return@run
            }
            val toggle = plugin.settings.toggles.getOrNull(value.removePrefix("toggle/").toIntOrNull() ?: return@run) ?: return@run
            val board = plugin.boards.get(player) ?: return@run
            plugin.boards.toggle(board, toggle)
            open(player)
        }, null)
    }

    companion object {
        const val NAMESPACE = "sbsettings"
    }
}
