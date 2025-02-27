package me.nukmuk.sheepy

import me.nukmuk.sheepy.renderers.ParticleRenderer
import me.nukmuk.sheepy.renderers.TextDisplayRenderer
import me.nukmuk.sheepy.renderers.packet.BlockDisplayPacketRenderer
import me.nukmuk.sheepy.renderers.packet.PacketEntityHandler
import me.nukmuk.sheepy.renderers.packet.TextDisplayPacketRenderer
import me.nukmuk.sheepy.utils.ConfigUtil
import me.nukmuk.sheepy.utils.RepeatAnimationsConfigUtil
import me.nukmuk.sheepy.utils.Utils
import org.bukkit.Location
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.scheduler.BukkitTask
import org.joml.Vector3f
import java.io.File
import java.util.*
import kotlin.math.ceil

object AnimationsManager {
    val animations = HashMap<String, Animation>()
    private val plugin = Sheepy.instance
    private lateinit var task: BukkitTask
    val debugPlayers = HashSet<UUID>()

    var maxParticlesPerTick: Int = 1000
        set(value) {
            field = value
            plugin.config.set("max-particles-per-tick", value)
            ConfigUtil.save(plugin)
        }
        get() {
            return plugin.config.getInt("max-particles-per-tick")
        }

    private var _animsInFolder = listOf<File>()
    val animsInFolder: List<File>
        get() = _animsInFolder

    fun animationNames(): Set<String> {
        return animations.keys
    }

    fun createAnimation(name: String, file: File, location: Location, repeat: Boolean): Animation {
        val animation = Animation(name, file, null, location, repeat)
        animations.put(name, animation)
        if (animation.repeat) {
            RepeatAnimationsConfigUtil.saveAnimation(animation)
        }

        return animation
    }

    fun getAnimation(name: String): Animation? {
        val animation = animations.get(name)
        if (animation == null) return null
        return animation
    }

    fun clearAnimations() {
        animations.values.forEach { it.remove() }
//        plugin.server.scheduler.runTaskLater(plugin, Runnable {
//            Utils.sendDebugMessage("clearing animations...")
//            EntityRenderer.sendRemoveAllEntitiesPacket(plugin)
//        }, 20L)
    }

    fun initialize(plugin: Sheepy) {
        maxParticlesPerTick = plugin.config.getInt("max-particles-per-tick", 1000)
//        plugin.server.onlinePlayers.filter { it.isOp }.forEach { debugPlayers.add(it.uniqueId) }
        RepeatAnimationsConfigUtil.loadAllAnimations()
        task = object : BukkitRunnable() {
            var processing = false
            var i = 0
            override fun run() {
                if (processing) {
                    sendDebugPlayersActionBar("${Config.ERROR_COLOR}previous frame still processing, animations playing: ${animations.keys} i: $i")
                    return
                }
                if (animations.values.filter { it.renderType == RenderType.TEXT_DISPLAY_PACKET || it.renderType == RenderType.BLOCK_DISPLAY_PACKET }
                        .find { it.playing } == null) {
                    PacketEntityHandler.cleanEntityRenderers(plugin)
                }
                if (animations.isEmpty()) return
                processing = true
                val framesToBePlayed = ArrayList<Frame>()
                val animationIterator = animations.values.iterator()
                while (animationIterator.hasNext()) {
                    val animation = animationIterator.next()
                    if (animation.shouldBeLeftInWorld != ShouldBeLeftInWorld.NO) animation.remove()
                    if (animation.shouldBeDeleted) animationIterator.remove()
                    if (!animation.playing) continue
                    val frame = animation.getNextFrame(
                        Vector3f(
                            animation.location.x.toFloat(),
                            animation.location.y.toFloat(), animation.location.z.toFloat()
                        )
                    )
                    if (frame == null) {
                        plugin.logger.info("${animation.name} frame null after getNextFrame")
                        animation.removeWithoutRemovingFromConfig()
                        continue
                    }
                    framesToBePlayed.add(frame)
                }
                sendDebugPlayersActionBar(
                    "${Config.PRIMARY_COLOR}playing: ${Config.VAR_COLOR}${animations.keys} ${Config.PRIMARY_COLOR}i: ${Config.VAR_COLOR}$i ${Config.PRIMARY_COLOR}total particles loaded: ${Config.VAR_COLOR}${
                        framesToBePlayed.fold(
                            0
                        ) { acc, frame -> acc + frame.animationParticles.size }
                    }"
                )
                if (framesToBePlayed.isEmpty()) {
                    processing = false
                    return
                }
                val maxParticles: Int = ceil((maxParticlesPerTick.toDouble() / framesToBePlayed.size)).toInt()
                RenderType.entries.forEach { type ->
                    val framesOfThisType = framesToBePlayed.filter { it.animation.renderType == type }
                    when (type) {
                        RenderType.PARTICLE -> ParticleRenderer.playFrames(framesOfThisType, maxParticles, plugin)
                        RenderType.BLOCK_DISPLAY_PACKET -> BlockDisplayPacketRenderer.playFrames(
                            framesOfThisType,
                            maxParticles,
                            plugin
                        )

                        RenderType.TEXT_DISPLAY_PACKET -> TextDisplayPacketRenderer.playFrames(
                            framesOfThisType,
                            maxParticles,
                            plugin
                        )

                        RenderType.TEXT_DISPLAY -> TextDisplayRenderer.prepareFrames(
                            framesOfThisType,
                            maxParticles,
                            plugin
                        )
                    }
                }

                i++
                processing = false
            }
        }.runTaskTimerAsynchronously(plugin, 0L, 1L)
    }

    fun sendDebugPlayersActionBar(message: String) {
        plugin.server.onlinePlayers.filter { debugPlayers.contains(it.uniqueId) }
            .forEach { it.sendActionBar(Utils.mm.deserialize(message)) }
    }

    fun getAnimsInFolder(plugin: Sheepy): List<File> {
        val pluginFolder = plugin.dataFolder
        val animFolder = File(pluginFolder, "")

        val files = animFolder.listFiles()?.filter { file -> file.extension == Config.FILE_EXTENSION }

        if (files != null) {
            _animsInFolder = files
        } else {
            return listOf()
        }

        return files.toList()
    }
}