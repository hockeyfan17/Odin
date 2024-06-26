package me.odinmain.features.impl.dungeon

import me.odinmain.events.impl.RenderEntityModelEvent
import me.odinmain.features.Category
import me.odinmain.features.Module
import me.odinmain.features.settings.impl.BooleanSetting
import me.odinmain.features.settings.impl.NumberSetting
import me.odinmain.utils.addVec
import me.odinmain.utils.profile
import me.odinmain.utils.render.OutlineUtils
import me.odinmain.utils.render.RenderUtils.renderVec
import me.odinmain.utils.render.Renderer
import me.odinmain.utils.skyblock.dungeon.DungeonUtils
import me.odinmain.utils.skyblock.dungeon.DungeonUtils.dungeonTeammatesNoSelf
import net.minecraftforge.client.event.RenderLivingEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object TeammatesHighlight : Module(
    "Teammate Highlight",
    category = Category.DUNGEON,
    description = "Enhances visibility of your dungeon teammates and their name tags."
) {
    private val showClass: Boolean by BooleanSetting("Show Class", true, description = "Shows the class of the teammate.")
    private val outline: Boolean by BooleanSetting("Outline", true, description = "Highlights teammates with an outline.")
    private val thickness: Float by NumberSetting("Line Width", 4f, 1.0, 10.0, 0.5, description = "The thickness of the outline.")
    private val whenVisible: Boolean by BooleanSetting("When Visible", true, description = "Highlights teammates only when they are visible.")
    private val inBoss: Boolean by BooleanSetting("In Boss", true, description = "Highlights teammates in boss rooms.")

    @SubscribeEvent
    fun onRenderEntityModel(event: RenderEntityModelEvent) {
        if (!DungeonUtils.inDungeons || (!inBoss && DungeonUtils.inBoss) || !outline) return

        val teammate = dungeonTeammatesNoSelf.find { it.entity == event.entity } ?: return

        if (!whenVisible && mc.thePlayer.canEntityBeSeen(teammate.entity)) return

        profile("Highlight Dungeon Teammates") { OutlineUtils.outlineEntity(event, thickness, teammate.clazz.color, true) }
    }

    @SubscribeEvent
    fun handleNames(event: RenderLivingEvent.Post<*>) {
        if (!DungeonUtils.inDungeons) return
        val teammate = dungeonTeammatesNoSelf.find { it.entity == event.entity } ?: return

        Renderer.drawStringInWorld(
            if (showClass) "${teammate.name} §e[${teammate.clazz.name[0]}]" else teammate.name,
            event.entity.renderVec.addVec(y = 2.6),
            color = teammate.clazz.color,
            depth = false, renderBlackBox = false,
            scale = 0.05f
        )
    }
}