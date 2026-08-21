package zov.alphadlc.module.list.combat;

import com.google.common.eventbus.Subscribe;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import zov.alphadlc.event.list.EventTick;
import zov.alphadlc.util.friend.FriendRepository;
import zov.alphadlc.module.Module;
import zov.alphadlc.module.ModuleCategory;
import zov.alphadlc.module.ModuleInformation;
import zov.alphadlc.module.settings.BooleanSetting;
import zov.alphadlc.module.settings.SliderSetting;
import zov.alphadlc.util.math.StopWatch;

@ModuleInformation(moduleName = "MaceTarget", moduleDesc = "Автоматическая атака булавой", moduleCategory = ModuleCategory.COMBAT)
public class MaceTarget extends Module {
    private final SliderSetting distance = new SliderSetting("Дистанция", 3.0, 2.0, 3.0, 0.5);
    private final SliderSetting minHeight = new SliderSetting("Мин. высота падения", 3.0, 0.0, 20.0, 0.5);
    private final BooleanSetting switchToMace = new BooleanSetting("Свап на булаву", true);
    private final BooleanSetting onlyFalling = new BooleanSetting("Только при падении вниз", true);
    private final StopWatch cooldown = new StopWatch();
    private int savedSlot = -1;

    @Override
    public void onDisable() {
        this.restoreSlot();
        super.onDisable();
    }

    @Subscribe
    public void onTick(EventTick event) {
        if (this.mc.player == null || this.mc.world == null) {
            return;
        }
        if (this.mc.player.isOnGround()) {
            this.restoreSlot();
            return;
        }
        if (this.onlyFalling.getValue() && this.mc.player.getVelocity().y >= 0.0) {
            this.restoreSlot();
            return;
        }
        if ((double)this.mc.player.fallDistance < this.minHeight.getValue()) {
            this.restoreSlot();
            return;
        }
        LivingEntity target = this.findTarget();
        if (target == null) {
            this.restoreSlot();
            return;
        }
        if (this.switchToMace.getValue()) {
            int maceSlot = this.findMaceSlot();
            if (maceSlot == -1) {
                return;
            }
            if (this.mc.player.getInventory().selectedSlot != maceSlot) {
                this.savedSlot = this.mc.player.getInventory().selectedSlot;
                this.mc.player.getInventory().selectedSlot = maceSlot;
                this.mc.player.networkHandler.sendPacket((Packet)new UpdateSelectedSlotC2SPacket(maceSlot));
            }
        } else if (this.mc.player.getMainHandStack().getItem() != Items.MACE) {
            return;
        }
        if (!this.cooldown.isReached(450L)) {
            return;
        }
        this.mc.interactionManager.attackEntity((PlayerEntity)this.mc.player, (Entity)target);
        this.mc.player.swingHand(Hand.MAIN_HAND);
        this.restoreSlot();
        this.cooldown.reset();
    }

    private LivingEntity findTarget() {
        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (Entity entity : this.mc.world.getEntities()) {
            double dist;
            PlayerEntity p2;
            if (!(entity instanceof LivingEntity)) continue;
            LivingEntity living = (LivingEntity)entity;
            if (entity == this.mc.player || !entity.isAlive() || entity instanceof ArmorStandEntity || entity instanceof PlayerEntity && !FriendRepository.shouldAttack(p2 = (PlayerEntity)entity) || (dist = (double)this.mc.player.distanceTo(entity)) > this.distance.getValue() || !(dist < bestDist)) continue;
            bestDist = dist;
            best = living;
        }
        return best;
    }

    private int findMaceSlot() {
        for (int i2 = 0; i2 < 9; ++i2) {
            if (this.mc.player.getInventory().getStack(i2).getItem() != Items.MACE) continue;
            return i2;
        }
        return -1;
    }

    private void restoreSlot() {
        if (this.savedSlot != -1 && this.mc.player != null) {
            this.mc.player.getInventory().selectedSlot = this.savedSlot;
            this.mc.player.networkHandler.sendPacket((Packet)new UpdateSelectedSlotC2SPacket(this.savedSlot));
            this.savedSlot = -1;
        }
    }
}
