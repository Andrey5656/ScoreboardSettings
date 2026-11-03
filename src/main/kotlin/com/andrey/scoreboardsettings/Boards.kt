package com.andrey.scoreboardsettings

import io.papermc.paper.threadedregions.scheduler.ScheduledTask
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class Board(val player: Player) {
    @Volatile var loaded = false
    var hidden: MutableSet<String> = HashSet()
    var task: ScheduledTask? = null
    var objective: Any? = null
    var title: String? = null
    val sent = ArrayList<String>()
}

class Boards(private val plugin: ScoreboardSettings, private val packets: Packets) : Listener {
    private val boards = ConcurrentHashMap<UUID, Board>()

    fun get(player: Player): Board? = boards[player.uniqueId]?.takeIf { it.loaded }

    fun start(player: Player) {
        val board = Board(player)
        boards[player.uniqueId] = board
        plugin.storage.load(player.uniqueId) { hidden ->
            player.scheduler.run(plugin, {
                if (boards[player.uniqueId] !== board) return@run
                board.hidden = hidden.toMutableSet()
                board.loaded = true
                schedule(board)
            }, null)
        }
    }

    private fun schedule(board: Board) {
        board.task = board.player.scheduler.runAtFixedRate(plugin, { update(board) }, null, 1, plugin.settings.updateTicks)
    }

    fun restartAll() {
        for (board in boards.values) {
            board.player.scheduler.run(plugin, {
                if (!board.loaded || boards[board.player.uniqueId] !== board) return@run
                board.task?.cancel()
                hide(board)
                schedule(board)
            }, null)
        }
    }

    fun toggle(board: Board, toggle: Toggle) {
        if (!board.hidden.remove(toggle.id)) board.hidden.add(toggle.id)
        plugin.storage.save(board.player.uniqueId, board.hidden.toSet())
        update(board)
    }

    fun update(board: Board) {
        val settings = plugin.settings
        val player = board.player
        val visible = settings.visibleLines(board.hidden)
        if (visible == null) {
            hide(board)
            return
        }
        val title = Text.papi(player, settings.title)
        val objective = board.objective
        if (objective == null) {
            val created = packets.objective(NAME, Text.parse(title))
            board.objective = created
            board.title = title
            board.sent.clear()
            packets.send(player, packets.add(created))
            packets.send(player, packets.display(created))
        } else if (title != board.title) {
            board.title = title
            packets.rename(objective, Text.parse(title))
            packets.send(player, packets.change(objective))
        }
        val lines = visible.map { Text.papi(player, it) }
        if (lines.size != board.sent.size) {
            for (index in lines.size until board.sent.size) packets.send(player, packets.reset(owner(index), NAME))
            board.sent.clear()
        }
        lines.forEachIndexed { index, line ->
            if (board.sent.getOrNull(index) == line) return@forEachIndexed
            packets.send(player, packets.score(owner(index), NAME, lines.size - index, Text.parse(line)))
            if (index < board.sent.size) board.sent[index] = line else board.sent.add(line)
        }
    }

    private fun hide(board: Board) {
        val objective = board.objective ?: return
        packets.send(board.player, packets.remove(objective))
        board.objective = null
        board.title = null
        board.sent.clear()
    }

    private fun owner(index: Int): String = "sbs_$index"

    fun stopAll() {
        for (board in boards.values) {
            board.task?.cancel()
            runCatching { hide(board) }
        }
        boards.clear()
    }

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        start(event.player)
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        boards.remove(event.player.uniqueId)?.task?.cancel()
    }

    companion object {
        const val NAME = "sbsettings"
    }
}
