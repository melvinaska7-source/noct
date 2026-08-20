package polar.ru.client.modules.impl.movement;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.Vec3d;
import polar.ru.api.events.EventLink;
import polar.ru.api.events.implement.EventMove;
import polar.ru.api.events.implement.EventPacket;
import polar.ru.api.utils.player.InventoryUtils;
import polar.ru.client.modules.Module;
import polar.ru.client.modules.settings.implement.BooleanSetting;
import polar.ru.client.modules.settings.implement.FloatSetting;

public class AirStuck
extends Module {
    public static AirStuck INSTANCE = new AirStuck();
    private final BooleanSetting lonyGriefBypass = new BooleanSetting("Стопить в тайминг", false);
    private final BooleanSetting autoSwapChest = new BooleanSetting("Свап на нагрудник", true);
    private final BooleanSetting backElytra = new BooleanSetting("Вернуть при выкл", true);
    public final BooleanSetting extraRangeEnabled = new BooleanSetting("Доп дистанция вкл", false);
    public final FloatSetting extraRange = new FloatSetting("Доп дистанция", 1.0f, 1.0f, 5.0f, 0.1f);
    private Vec3d freezePosition = Vec3d.ZERO;
    private boolean frozen = false;
    private boolean swapped = false;

    public AirStuck() {
        super("Air Stuck", "Позволяет зависнуть в воздухе", Module.ModuleCategory.MOVEMENT);
        this.addSettings(this.lonyGriefBypass, this.autoSwapChest, this.backElytra, this.extraRangeEnabled, this.extraRange);
    }

    @Override
        public void onEnable() {
        this.frozen = false;
        this.swapped = false;
        if (AirStuck.mc.player != null) {
            int slot;
            ItemStack chestStack;
            if (!this.lonyGriefBypass.isState()) {
                this.freezePosition = AirStuck.mc.player.getPos();
                this.frozen = true;
            }
            if (this.autoSwapChest.isState() && (chestStack = AirStuck.mc.player.getEquippedStack(EquipmentSlot.CHEST)).isOf(Items.ELYTRA) && (slot = InventoryUtils.findBestChestplateSlot()) != -1) {
                this.performSwap(slot);
                this.swapped = true;
            }
        }
        super.onEnable();
    }

    @Override
    public void onDisable() {
        if (this.swapped && this.backElytra.isState() && AirStuck.mc.player != null) {
            int slot = InventoryUtils.findBestElytraSlot();
            if (slot != -1) {
                this.performSwap(slot);
            }
            this.swapped = false;
        }
        this.frozen = false;
        super.onDisable();
    }

    private void performSwap(int slot) {
        if (slot >= 0 && slot < 9) {
            AirStuck.mc.interactionManager.clickSlot(0, 6, slot, SlotActionType.SWAP, (PlayerEntity)AirStuck.mc.player);
        } else {
            AirStuck.mc.interactionManager.clickSlot(0, slot, 0, SlotActionType.SWAP, (PlayerEntity)AirStuck.mc.player);
            AirStuck.mc.interactionManager.clickSlot(0, 6, 0, SlotActionType.SWAP, (PlayerEntity)AirStuck.mc.player);
            AirStuck.mc.interactionManager.clickSlot(0, slot, 0, SlotActionType.SWAP, (PlayerEntity)AirStuck.mc.player);
        }
        AirStuck.mc.player.networkHandler.sendPacket((Packet)new CloseHandledScreenC2SPacket(0));
    }

    @EventLink
    public void onMove(EventMove e2) {
        if (AirStuck.mc.player == null) {
            return;
        }
        if (this.lonyGriefBypass.isState() && !this.frozen && AirStuck.mc.player.fallDistance > 0.0f && AirStuck.mc.player.getVelocity().y < 0.0) {
            this.freezePosition = AirStuck.mc.player.getPos();
            this.frozen = true;
        }
        if (this.frozen) {
            e2.setMovePos(Vec3d.ZERO);
            AirStuck.mc.player.setPosition(this.freezePosition.x, this.freezePosition.y, this.freezePosition.z);
            AirStuck.mc.player.setVelocity(0.0, 0.0, 0.0);
        }
    }

    @EventLink
    public void onPacket(EventPacket e2) {
        if (!this.frozen || AirStuck.mc.player == null) {
            return;
        }
        if (e2.getPacket() instanceof PlayerMoveC2SPacket) {
            e2.cancel();
        }
    }

    public float getExtraRangeValue() {
        if (this.isEnable() && this.extraRangeEnabled.isState()) {
            return this.extraRange.getValue().floatValue();
        }
        return 0.0f;
    }
}

