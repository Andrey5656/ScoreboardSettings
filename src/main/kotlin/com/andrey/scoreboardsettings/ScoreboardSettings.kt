package com.andrey.scoreboardsettings

import io.papermc.paper.command.brigadier.BasicCommand
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.util.logging.Level

class ScoreboardSettings : JavaPlugin() {
    @Volatile
    lateinit var settings: Settings
    lateinit var packets: Packets
    lateinit var storage: Storage
    lateinit var boards: Boards
    lateinit var dialog: SettingsDialog

    override fun onEnable() {
        saveDefaultConfig()
        settings = Settings(config)
        packets = runCatching { Packets() }.getOrElse {
            logger.log(Level.SEVERE, "This server version is not supported.", it)
            server.pluginManager.disablePlugin(this)
            return
        }
        storage = runCatching { Storage(this, settings.storage) }.getOrElse {
            logger.log(Level.SEVERE, "Could not connect to the database.", it)
            server.pluginManager.disablePlugin(this)
            return
        }
        boards = Boards(this, packets)
        dialog = SettingsDialog(this)
        server.pluginManager.registerEvents(boards, this)
        server.pluginManager.registerEvents(dialog, this)
        server.onlinePlayers.forEach(boards::start)

        lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS) { event ->
            event.registrar().register(settings.command, "Open the scoreboard settings.", settings.aliases, object : BasicCommand {
                override fun execute(source: CommandSourceStack, args: Array<String>) {
                    if (args.firstOrNull().equals("reload", ignoreCase = true) && source.sender.hasPermission(RELOAD_PERMISSION)) {
                        reload()
                        source.sender.sendMessage(Text.parse(settings.reloaded))
                        return
                    }
                    val player = source.executor as? Player ?: source.sender as? Player
                    if (player == null) {
                        source.sender.sendMessage("Only players can use this command.")
                        return
                    }
                    dialog.open(player)
                }

                override fun suggest(source: CommandSourceStack, args: Array<String>): Collection<String> =
                    if (args.size <= 1 && source.sender.hasPermission(RELOAD_PERMISSION) && "reload".startsWith(args.firstOrNull() ?: "", ignoreCase = true)) {
                        listOf("reload")
                    } else {
                        emptyList()
                    }
            })
        }
    }

    private fun reload() {
        reloadConfig()
        settings = Settings(config)
        boards.restartAll()
    }

    override fun onDisable() {
        if (::boards.isInitialized) boards.stopAll()
        if (::storage.isInitialized) storage.close()
    }

    companion object {
        const val RELOAD_PERMISSION = "scoreboardsettings.reload"
    }
}
