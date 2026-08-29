package zov.alphadlc.module.list.movement;

import com.google.common.eventbus.Subscribe;
import net.minecraft.entity.player.PlayerEntity;
import zov.alphadlc.event.list.EventTick;
import zov.alphadlc.module.Module;
import zov.alphadlc.module.ModuleCategory;
import zov.alphadlc.module.ModuleInformation;

@ModuleInformation(moduleName = "Sprint", moduleDesc = "Автоматический спринт", moduleCategory = ModuleCategory.MOVEMENT)
public class Sprint extends Module {
    @Subscribe
    public void onUpdate(EventTick event) {
        if (mc.player == null) return;
        mc.options.sprintKey.setPressed(false);
        mc.player.setSprinting(((!mc.player.isTouchingWater() || mc.player.isSubmergedInWater()) && !mc.player.isGliding() && mc.player.isWalking() && mc.player.canSprint() && !mc.player.isUsingItem() && !mc.player.isBlind() && (!mc.player.hasVehicle() || (mc.player.getVehicle().canSprintAsVehicle() && mc.player.getVehicle().isLogicalSideForUpdatingMovement()) && !mc.player.isGliding() && (!mc.player.shouldSlowDown() || mc.player.isSubmergedInWater())) && mc.player.input.hasForwardMovement() && (!mc.player.horizontalCollision && !mc.player.collidedSoftly)));
    }
}