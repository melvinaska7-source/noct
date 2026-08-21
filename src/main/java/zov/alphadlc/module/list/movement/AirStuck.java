package zov.alphadlc.module.list.movement;

import com.google.common.eventbus.Subscribe;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.Vec3d;
import zov.alphadlc.event.list.EventPacket;
import zov.alphadlc.event.list.EventTick;
import zov.alphadlc.module.Module;
import zov.alphadlc.module.ModuleCategory;
import zov.alphadlc.module.ModuleInformation;
import zov.alphadlc.module.settings.BooleanSetting;
import zov.alphadlc.module.settings.ModeSetting;
import zov.alphadlc.module.settings.SliderSetting;
import zov.alphadlc.module.list.player.ElytraHelper;
import zov.alphadlc.util.base.Instance;

@ModuleInformation(moduleName = "Air Stuck", moduleDesc = "Зависание в воздухе", moduleCategory = ModuleCategory.MOVEMENT)
public class AirStuck extends Module {
    private final ModeSetting mode = new ModeSetting("Мод", "Vanilla", "Vanilla", "Grim", "Polar");
    private final BooleanSetting autoSwapChest = new BooleanSetting("Свап на нагрудник", true);
    private final BooleanSetting backElytra = new BooleanSetting("Вернуть при выкл", true).setVisible(this.autoSwapChest::getValue);
    private final BooleanSetting fallCheck = new BooleanSetting("Проверка на падение", true);
    public final BooleanSetting useKillAuraDistance = new BooleanSetting("Своя дистанция", false);
    public final SliderSetting killAuraDistance = new SliderSetting("Дистанция", 6.0, 2.0, 8.0, 0.1).setVisible(this.useKillAuraDistance::getValue);
    private Vec3d savedVelocity = Vec3d.ZERO;
    private boolean isElytra;
    private boolean hasFailed = false;
    private int pauseTicks = 0;

    @Subscribe
    private void onPacket(EventPacket e2) {
        if (this.mc.player == null || this.hasFailed) {
            return;
        }
        if (e2.getPacket() instanceof PlayerMoveC2SPacket) {
            if (this.pauseTicks > 0) {
                return;
            }
            e2.cancelEvent();
        }
    }

    @Subscribe
    private void onTick(EventTick e2) {
        if (this.mc.player == null || this.hasFailed) {
            return;
        }
        if (this.pauseTicks > 0) {
            --this.pauseTicks;
            return;
        }
        this.mc.player.setVelocity(0.0, 0.0, 0.0);
        this.mc.player.setNoGravity(true);
    }

    @Override
    public void onEnable() {
        boolean wearingElytra;
        super.onEnable();
        if (this.mc.player == null || this.mc.world == null) {
            return;
        }
        this.hasFailed = false;
        if (this.mc.player.fallDistance == 0.0f && this.fallCheck.getValue()) {
            this.setEnabled(false);
            return;
        }
        this.mc.player.setNoGravity(true);
        this.savedVelocity = this.mc.player.getVelocity();
        boolean bl = wearingElytra = this.mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem() == Items.ELYTRA;
        if (wearingElytra && this.autoSwapChest.getValue()) {
            this.isElytra = true;
            Instance.get(ElytraHelper.class).swap(this.mode, true);
        } else {
            this.isElytra = false;
        }
        if (this.mc.player.isOnGround() && this.mc.player.fallDistance == 0.0f) {
            this.setEnabled(false);
            return;
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (this.mc.player == null) {
            return;
        }
        if (this.savedVelocity != null && !this.hasFailed) {
            this.mc.player.setVelocity(this.savedVelocity);
        }
        this.mc.player.setNoGravity(false);
        if (this.isElytra) {
            boolean wearingChestPlate = this.mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem() instanceof ArmorItem;
            if (wearingChestPlate && this.backElytra.getValue()) {
                Instance.get(ElytraHelper.class).swap(this.mode, false);
            }
            this.isElytra = false;
        }
        this.hasFailed = false;
        this.savedVelocity = Vec3d.ZERO;
    }

    public void forceDisable() {
        if (this.isEnabled()) {
            this.hasFailed = true;
            this.setEnabled(false);
        }
    }

    public void pauseForAttack() {
        this.pauseTicks = 1;
    }
}
