package me.odinmain.features.impl.dungeon.puzzlesolvers

import me.odinmain.events.impl.EnteredDungeonRoomEvent
import me.odinmain.utils.Vec2
import me.odinmain.utils.addRotationCoords
import me.odinmain.utils.addVec
import me.odinmain.utils.plus
import me.odinmain.utils.render.Color
import me.odinmain.utils.render.RenderUtils
import me.odinmain.utils.render.RenderUtils.bind
import me.odinmain.utils.render.RenderUtils.worldRenderer
import me.odinmain.utils.skyblock.IceFillFloors.floors
import me.odinmain.utils.skyblock.IceFillFloors.representativeFloors
import me.odinmain.utils.skyblock.dungeon.DungeonUtils
import me.odinmain.utils.skyblock.dungeon.tiles.Rotations
import me.odinmain.utils.skyblock.getBlockIdAt
import me.odinmain.utils.skyblock.isAir
import me.odinmain.utils.skyblock.modMessage
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import net.minecraft.util.Vec3i
import org.lwjgl.opengl.GL11

object IceFillSolver {
    var scanned = false
    var currentPatterns: MutableList<List<Vec3i>> = ArrayList()
    private var renderRotation: Rotations? = null
    private var rPos: MutableList<Vec3> = ArrayList()

    private fun renderPattern(pos: Vec3, rotation: Rotations) {
        renderRotation = rotation
        rPos.add(Vec3(pos.xCoord + 0.5, pos.yCoord + 0.1, pos.zCoord + 0.5))
    }

    fun onRenderWorldLast(color: Color) {
        if (currentPatterns.size == 0 || rPos.size == 0 || DungeonUtils.currentRoomName != "Ice Fill") return
        val rotation = renderRotation ?: return

        GlStateManager.pushMatrix()
        color.bind()
        RenderUtils.preDraw()
        GlStateManager.depthMask(true)
        GL11.glEnable(GL11.GL_LINE_SMOOTH)
        GL11.glLineWidth(3f)

        worldRenderer.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION)
        for (i in currentPatterns.indices) {
            val pattern = currentPatterns[i]
            val startPos = rPos[i]
            worldRenderer.pos(startPos.xCoord, startPos.yCoord, startPos.zCoord).endVertex()
            for (point in pattern) {
                val pos = startPos + transformTo(point, rotation)
                worldRenderer.pos(pos.xCoord, pos.yCoord, pos.zCoord).endVertex()
            }
            val stairPos = startPos + transformTo(pattern.last().addVec(1, 1), rotation)
            worldRenderer.pos(stairPos.xCoord, stairPos.yCoord, stairPos.zCoord).endVertex()
        }

        Tessellator.getInstance().draw()
        GlStateManager.depthMask(true)
        RenderUtils.postDraw()
        GlStateManager.popMatrix()
    }

    fun enterDungeonRoom(event: EnteredDungeonRoomEvent) {
        if (event.room?.room?.data?.name != "Ice Fill") return
        val rotation = event.room.room.rotation

        val centerPos = Vec2(event.room.room.x, event.room.room.z).addRotationCoords(rotation, 8)
        scanAllFloors(Vec3(centerPos.x.toDouble(), 70.0, centerPos.z.toDouble()), rotation)
    }

    private fun scanAllFloors(pos: Vec3, rotation: Rotations) {
        scan(pos, 0, rotation)

        val a = transform(Vec3i(5, 1, 0), rotation)
        scan(pos.addVec(a.x, a.y, a.z), 1, rotation)

        val b = transform(Vec3i(12, 2, 0), rotation)
        scan(pos.addVec(b.x, b.y, b.z), 2, rotation)
    }

    private fun scan(pos: Vec3, floorIndex: Int, rotation: Rotations): Boolean {
        val bPos = BlockPos(pos)

        val floorHeight = representativeFloors[floorIndex]
        val startTime = System.nanoTime()

        for (index in floorHeight.indices) {
            if (
                isAir(bPos.add(transform(floorHeight[index].first, rotation))) &&
                !isAir(bPos.add(transform(floorHeight[index].second, rotation)))
            ) {
                val scanTime: Double = (System.nanoTime() - startTime) / 1000000.0
                modMessage("Floor ${floorIndex + 1} scan took ${scanTime}ms")

                renderPattern(pos, rotation)
                currentPatterns.add(floors[floorIndex][index].toMutableList())
                return true
            }
        }
        modMessage("§cFailed to scan floor ${floorIndex + 1}")
        return false
    }

    fun transform(vec: Vec3i, rotation: Rotations): Vec3i {
        return when (rotation) {
            Rotations.EAST -> Vec3i(-vec.x, vec.y, -vec.z)
            Rotations.WEST -> Vec3i(vec.x, vec.y, vec.z) // east WORKING
            Rotations.SOUTH -> Vec3i(-vec.z, vec.y, vec.x) // south WORKING
            Rotations.NORTH -> Vec3i(vec.z, vec.y, -vec.x) // north working
            else -> vec
        }
    }

    fun transformTo(vec: Vec3i, rotation: Rotations): Vec3 {
        return when (rotation) {
            Rotations.EAST -> Vec3(-vec.x.toDouble(), vec.y.toDouble(), -vec.z.toDouble())
            Rotations.WEST -> Vec3(vec.x.toDouble(), vec.y.toDouble(), vec.z.toDouble())
            Rotations.SOUTH -> Vec3(-vec.z.toDouble(), vec.y.toDouble(), vec.x.toDouble())
            Rotations.NORTH -> Vec3(vec.z.toDouble(), vec.y.toDouble(), -vec.x.toDouble())
            else -> Vec3(vec.x.toDouble(), vec.y.toDouble(), vec.z.toDouble())
        }
    }


    fun transform(x: Int, z: Int, rotation: Rotations): Pair<Int, Int> {
        return when (rotation) {
            Rotations.EAST -> Pair(x, z)
            Rotations.WEST -> Pair(-x, -z)
            Rotations.SOUTH -> Pair(z, x)
            else -> Pair(z, -x)
        }
    }

    fun checkRotation(pos: Vec3i, floor: Int): Rotation? {
        val a = ((floor + 1) * 2) + 2
        if      (getBlockIdAt(pos.x + a, pos.y, pos.z) == 109) return Rotation.EAST
        else if (getBlockIdAt(pos.x - a, pos.y, pos.z) == 109) return Rotation.WEST
        else if (getBlockIdAt(pos.x, pos.y, pos.z + a) == 109) return Rotation.SOUTH
        else if (getBlockIdAt(pos.x, pos.y, pos.z - a) == 109) return Rotation.NORTH
        return null
    }

    fun reset() {
        currentPatterns = ArrayList()
        scanned = false
        renderRotation = null
        rPos = ArrayList()
    }
}
enum class Rotation { EAST, WEST, SOUTH, NORTH }