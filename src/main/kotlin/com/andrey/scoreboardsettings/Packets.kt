package com.andrey.scoreboardsettings

import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.util.Optional

class Packets {
    private val lookup = MethodHandles.lookup()
    private val void = Void.TYPE
    private val int = Int::class.javaPrimitiveType!!

    private val craftPlayer = Class.forName("org.bukkit.craftbukkit.entity.CraftPlayer")
    private val serverPlayer = Class.forName("net.minecraft.server.level.ServerPlayer")
    private val gameListener = Class.forName("net.minecraft.server.network.ServerGamePacketListenerImpl")
    private val commonListener = Class.forName("net.minecraft.server.network.ServerCommonPacketListenerImpl")
    private val packet = Class.forName("net.minecraft.network.protocol.Packet")
    private val vanillaComponent = Class.forName("net.minecraft.network.chat.Component")
    private val numberFormat = Class.forName("net.minecraft.network.chat.numbers.NumberFormat")
    private val scoreboard = Class.forName("net.minecraft.world.scores.Scoreboard")
    private val objective = Class.forName("net.minecraft.world.scores.Objective")
    private val criteria = Class.forName("net.minecraft.world.scores.criteria.ObjectiveCriteria")
    private val renderType = Class.forName("net.minecraft.world.scores.criteria.ObjectiveCriteria\$RenderType")
    private val displaySlot = Class.forName("net.minecraft.world.scores.DisplaySlot")
    private val setObjective = Class.forName("net.minecraft.network.protocol.game.ClientboundSetObjectivePacket")
    private val setDisplay = Class.forName("net.minecraft.network.protocol.game.ClientboundSetDisplayObjectivePacket")
    private val setScore = Class.forName("net.minecraft.network.protocol.game.ClientboundSetScorePacket")
    private val resetScore = Class.forName("net.minecraft.network.protocol.game.ClientboundResetScorePacket")

    private val getHandle: MethodHandle = lookup.findVirtual(craftPlayer, "getHandle", MethodType.methodType(serverPlayer))
    private val connection: MethodHandle = lookup.findGetter(serverPlayer, "connection", gameListener)
    private val send: MethodHandle = lookup.findVirtual(commonListener, "send", MethodType.methodType(void, packet))
    private val asVanilla: MethodHandle = lookup.findStatic(
        Class.forName("io.papermc.paper.adventure.PaperAdventure"), "asVanilla", MethodType.methodType(vanillaComponent, Component::class.java)
    )
    private val newObjective: MethodHandle = lookup.findConstructor(
        objective,
        MethodType.methodType(void, scoreboard, String::class.java, criteria, vanillaComponent, renderType, Boolean::class.javaPrimitiveType, numberFormat)
    )
    private val setDisplayName: MethodHandle = lookup.findVirtual(objective, "setDisplayName", MethodType.methodType(void, vanillaComponent))
    private val newSetObjective: MethodHandle = lookup.findConstructor(setObjective, MethodType.methodType(void, objective, int))
    private val newSetDisplay: MethodHandle = lookup.findConstructor(setDisplay, MethodType.methodType(void, displaySlot, objective))
    private val newSetScore: MethodHandle = lookup.findConstructor(
        setScore, MethodType.methodType(void, String::class.java, String::class.java, int, Optional::class.java, Optional::class.java)
    )
    private val newResetScore: MethodHandle = lookup.findConstructor(resetScore, MethodType.methodType(void, String::class.java, String::class.java))

    private val dummyBoard: Any = lookup.findConstructor(scoreboard, MethodType.methodType(void)).invoke()
    private val dummyCriteria: Any = criteria.getField("DUMMY").get(null)
    private val integer: Any = renderType.enumConstants.first { (it as Enum<*>).name == "INTEGER" }
    private val sidebar: Any = displaySlot.enumConstants.first { (it as Enum<*>).name == "SIDEBAR" }
    private val blank: Any = Class.forName("net.minecraft.network.chat.numbers.BlankFormat").getField("INSTANCE").get(null)
    private val clearDialog: Any = Class.forName("net.minecraft.network.protocol.common.ClientboundClearDialogPacket").getField("INSTANCE").get(null)

    private fun vanilla(component: Component): Any = asVanilla.invoke(component)

    fun send(player: Player, packet: Any) {
        val listener = connection.invoke(getHandle.invoke(player)) ?: return
        send.invoke(listener, packet)
    }

    fun closeDialog(player: Player) {
        send(player, clearDialog)
    }

    fun objective(name: String, title: Component): Any =
        newObjective.invoke(dummyBoard, name, dummyCriteria, vanilla(title), integer, false, blank)

    fun rename(objective: Any, title: Component) {
        setDisplayName.invoke(objective, vanilla(title))
    }

    fun add(objective: Any): Any = newSetObjective.invoke(objective, 0)

    fun remove(objective: Any): Any = newSetObjective.invoke(objective, 1)

    fun change(objective: Any): Any = newSetObjective.invoke(objective, 2)

    fun display(objective: Any): Any = newSetDisplay.invoke(sidebar, objective)

    fun score(owner: String, objective: String, score: Int, text: Component): Any =
        newSetScore.invoke(owner, objective, score, Optional.of(vanilla(text)), Optional.of(blank))

    fun reset(owner: String, objective: String): Any = newResetScore.invoke(owner, objective)
}
