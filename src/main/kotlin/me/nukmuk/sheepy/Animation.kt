package me.nukmuk.sheepy

import it.unimi.dsi.fastutil.io.FastBufferedInputStream
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.Player
import org.joml.Vector3f
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicBoolean

class Animation(
    val name: String,
    private val file: File,
    private val player: Player?,
    plugin: Sheepy,
    var location: Location,
) {

    val world: World
        get() = location.world

    private val reader = FastBufferedInputStream(FileInputStream(file))

    private var i = 0

    var playing = AtomicBoolean(false)


    var particleScale = 1.0f
    var animationScale = 1.0f
    var repeat = false

    var maxParticlesPerFrame = 1000

    var shouldBeDeleted = false


    fun start() {
        playing.set(true)
    }

    fun stop() {
        player?.let { Utils.sendMessage(it, "pausing animation $name") }
        playing.set(false)
    }

    fun remove() {
        player?.let { Utils.sendMessage(it, "removing animation $name") }
        playing.set(false)
        shouldBeDeleted = true
    }

    fun stepFrame() {
        Utils.sendMessage(player!!, "not implemented")
    }

    fun seekToStart() {
        reader.position(0)
    }

    fun animationIsOnLastFrame(): Boolean {
        return reader.position() >= reader.length()
    }

    fun getNextFrame(offset: Vector3f): Frame? {

        if (animationIsOnLastFrame()) {
            if (repeat) {
                seekToStart()
            } else {
                remove()
                return null
            }
        }


        var frame: Frame

        if (reader.available() > 0) {
            val length = getShort().toInt()

            frame = Frame(
                arrayOfNulls<AnimationParticle>(length),
                this
            )

            // loop over particles and add them to frame
            for (i in 0 until length) {
                frame.animationParticles[i] = AnimationParticle(
                    x = getPosComponent() + offset.x,
                    y = getPosComponent() + offset.y,
                    z = getPosComponent() + offset.z,
                    color = Color.fromARGB(getInt()),
                )
            }
//            Utils.sendMessage(
//                player!!,
//                "read frame with length ${frame.animationParticles.size}, remaining: ${bb.hasRemaining()}"
//            )
        } else {
            playing.set(false)
            player?.let { Utils.sendMessage(it, "buffer empty") }
            return null
        }

//        val p1 = frame.animationParticles.getOrNull(0)
//
//        val p1pos: Vector = p1?.let { p ->
//            Vector(p.x + location.x, p.y + location.y, p.z + location.z)
//        } ?: Vector(0, 0, 0)
//
//        player?.sendActionBar(Component.text("${ChatColor.GRAY}particles in current frame: ${frame.animationParticles.size}, running for: ${i}, pos: ${p1pos.x.toInt()}, ${p1pos.y.toInt()}, ${p1pos.z.toInt()}"))
        i++
        return frame
    }

    private fun getPosComponent(): Float {
        val posComponent = getShort()
        val posComponentFloat = Utils.convertToFloat(posComponent.toInt())
        return posComponentFloat
    }

    private fun getShort(): Short {
        return ByteBuffer.wrap(reader.readNBytes(2)).order(ByteOrder.LITTLE_ENDIAN).getShort()
    }

    private fun getInt(): Int {
        return ByteBuffer.wrap(reader.readNBytes(4)).order(ByteOrder.LITTLE_ENDIAN).getInt()
    }
}

data class Frame(val animationParticles: Array<AnimationParticle?>, val animation: Animation)

data class AnimationParticle(
    val x: Float,
    val y: Float,
    val z: Float,
    val color: Color
) {
    val scale: Byte
        get() = color.alpha.toByte()
}