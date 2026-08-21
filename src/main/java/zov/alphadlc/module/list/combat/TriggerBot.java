package zov.alphadlc.module.list.combat;

import com.google.common.eventbus.Subscribe;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import zov.alphadlc.event.list.EventTick;
import zov.alphadlc.module.Module;
import zov.alphadlc.module.ModuleCategory;
import zov.alphadlc.module.ModuleInformation;
import zov.alphadlc.module.settings.BooleanSetting;

@ModuleInformation(moduleName = "Trigger Bot", moduleDesc = "Автоматическая атака при наведении", moduleCategory = ModuleCategory.COMBAT)
public class TriggerBot extends Module {
    public final BooleanSetting pauseEating = new BooleanSetting("Остановка при еде", true);
    public final BooleanSetting onlyCriticals = new BooleanSetting("Только криты", true);
    public final BooleanSetting spaceOnly = new BooleanSetting("Умные криты", false);
    private int delay;

    @Subscribe
    public void onEvent(EventTick e2) {
        if (this.mc.player == null) {
            return;
        }
        if (this.mc.player.isUsingItem() && this.pauseEating.getValue()) {
            return;
        }
        if (this.delay > 0) {
            --this.delay;
            return;
        }
        if (!this.autoCrit()) {
            return;
        }
        Entity ent = this.mc.targetedEntity;
        if (ent != null) {
            this.mc.interactionManager.attackEntity((PlayerEntity)this.mc.player, ent);
            this.mc.player.swingHand(Hand.MAIN_HAND);
            this.delay = 10;
        }
    }

    @Override
    public void onDisable() {
        this.delay = 0;
        super.onDisable();
    }

    private boolean autoCrit() {
        boolean reasonForSkipCrit = !this.onlyCriticals.getValue() || this.mc.player.getAbilities().flying || this.mc.player.hasStatusEffect(StatusEffects.LEVITATION) || this.mc.player.hasStatusEffect(StatusEffects.BLINDNESS) || this.mc.world.getBlockState(this.mc.player.getBlockPos()).getBlock() == Blocks.LADDER;
        float f2 = this.mc.player.getAttackCooldownProgress(0.5f);
        float f3 = this.mc.player.isOnGround() ? 1.0f : 0.9f;
        if (f2 < f3) {
            return false;
        }
        boolean mergeWithSpeed = this.mc.player.isOnGround();
        if (!this.mc.options.jumpKey.isPressed() && mergeWithSpeed && this.spaceOnly.getValue()) {
            return true;
        }
        if (this.mc.player.isInLava()) {
            return true;
        }
        if (!reasonForSkipCrit) {
            return !this.mc.player.isOnGround() && this.mc.player.fallDistance > 0.0f;
        }
        return true;
    }
}
