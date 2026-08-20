package polar.ru.client.modules.impl.combat;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import polar.ru.api.events.EventLink;
import polar.ru.api.events.implement.EventMoveInput;
import polar.ru.api.events.implement.EventUpdate;
import polar.ru.client.modules.Module;
import polar.ru.client.modules.impl.movement.Sprint;
import polar.ru.client.modules.settings.implement.BooleanSetting;
import polar.ru.client.modules.settings.implement.FloatSetting;

public class AutoTotem
extends Module {
    public static AutoTotem INSTANCE = new AutoTotem();
    private final FloatSetting health = new FloatSetting("Здоровье", 4.0f, 1.0f, 20.0f, 0.1f);
    private final FloatSetting healthOnElytra = new FloatSetting("Здоровье на элитре", 11.0f, 1.0f, 20.0f, 0.1f);
    private final BooleanSetting saveEnchanted = new BooleanSetting("Сохранять зачарованный", false);
    private final BooleanSetting crystalsCheck = new BooleanSetting("Работать на кристалы", false);
    private final FloatSetting crystalDistance = new FloatSetting("Дистанция до кристала", 8.0f, 1.0f, 20.0f, 1.0f).visible(this.crystalsCheck::isState);
    private final BooleanSetting reactCrystalHand = new BooleanSetting("Кристал в руке", false);
    private final BooleanSetting reactFall = new BooleanSetting("Падение", false);
    private final FloatSetting fallDistance = new FloatSetting("Блоков падения", 3.0f, 1.0f, 20.0f, 1.0f).visible(this.reactFall::isState);
    private final BooleanSetting bypassgrim = new BooleanSetting("Обходить Grim", true);
    private final FloatSetting returnDelay = new FloatSetting("Возврат предмета (мс)", 500.0f, 0.0f, 2000.0f, 10.0f);
    private int swapBackSlot = -1;
    private float cooldownTicks;
    private int bypassTicks;
    private boolean sprintPaused;
    private int pendingTotemSlot = -1;
    private long lastDangerTime;

    public AutoTotem() {
        super("AutoTotem", "Автоматически берёт тотем в опасности", Module.ModuleCategory.COMBAT);
        this.addSettings(this.health, this.healthOnElytra, this.saveEnchanted, this.crystalsCheck, this.crystalDistance, this.reactCrystalHand, this.reactFall, this.fallDistance, this.bypassgrim, this.returnDelay);
    }

    @EventLink
    public void onInput(EventMoveInput e2) {
        if (this.bypassgrim.isState() && this.bypassTicks > 0) {
            if (AutoTotem.mc.player == null) {
                return;
            }
            AutoTotem.mc.player.setSprinting(false);
            e2.setForward(0.0f);
            e2.setStrafe(0.0f);
            e2.setJump(false);
            e2.setSneak(false);
        }
    }

    @EventLink
    public void onUpdate(EventUpdate e2) {
        if (AutoTotem.mc.player == null || AutoTotem.mc.world == null) {
            return;
        }
        if (this.cooldownTicks > 0.0f) {
            this.cooldownTicks -= 1.0f;
        }
        if (this.bypassgrim.isState() && this.bypassTicks > 0) {
            AutoTotem.mc.player.setSprinting(false);
            --this.bypassTicks;
            if (this.bypassTicks == 1 && this.pendingTotemSlot != -1) {
                this.swapToOffhand(this.pendingTotemSlot);
                AutoTotem.mc.player.networkHandler.sendPacket((Packet)new CloseHandledScreenC2SPacket(0));
                this.pendingTotemSlot = -1;
            }
            if (this.bypassTicks == 0) {
                this.restoreSprint();
            }
            return;
        }
        if (this.cooldownTicks > 0.0f) {
            return;
        }
        this.updateSwap();
    }

    private boolean condition() {
        if (AutoTotem.mc.player.isCreative() || AutoTotem.mc.player.isSpectator()) {
            return false;
        }
        float hp = AutoTotem.mc.player.getHealth() + AutoTotem.mc.player.getAbsorptionAmount();
        if (hp <= this.health.getValue().floatValue()) {
            return true;
        }
        if (hp <= this.healthOnElytra.getValue().floatValue() && AutoTotem.mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem() == Items.ELYTRA) {
            return true;
        }
        if (this.crystalsCheck.isState()) {
            for (Entity entity : AutoTotem.mc.world.getEntities()) {
                double dist;
                if (!(entity instanceof EndCrystalEntity) || !((dist = AutoTotem.mc.player.getEyePos().distanceTo(entity.getBoundingBox().getCenter())) <= (double)this.crystalDistance.getValue().floatValue())) continue;
                return true;
            }
        }
        if (this.reactCrystalHand.isState()) {
            for (Entity entity : AutoTotem.mc.world.getEntities()) {
                PlayerEntity player;
                if (!(entity instanceof PlayerEntity) || (player = (PlayerEntity)entity) == AutoTotem.mc.player || AutoTotem.mc.player.distanceTo((Entity)player) > 10.0f || player.getMainHandStack().getItem() != Items.END_CRYSTAL && player.getOffHandStack().getItem() != Items.END_CRYSTAL) continue;
                return true;
            }
        }
        return this.reactFall.isState() && AutoTotem.mc.player.fallDistance >= this.fallDistance.getValue().floatValue();
    }

    private boolean hasTotemInOffhand() {
        return AutoTotem.mc.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING);
    }

    private boolean hasEnchantedInOffhand() {
        ItemStack stack = AutoTotem.mc.player.getOffHandStack();
        return (stack.isOf(Items.TOTEM_OF_UNDYING) || stack.isOf(Items.FIREWORK_STAR)) && stack.hasEnchantments();
    }

    private int findTotemSlot() {
        for (int i2 = 9; i2 <= 44; ++i2) {
            ItemStack stack = AutoTotem.mc.player.playerScreenHandler.getSlot(i2).getStack();
            if (!stack.isOf(Items.TOTEM_OF_UNDYING)) continue;
            return i2;
        }
        return -1;
    }

    private int findRegularTotemSlot() {
        for (int i2 = 9; i2 <= 44; ++i2) {
            ItemStack stack = AutoTotem.mc.player.playerScreenHandler.getSlot(i2).getStack();
            if (!stack.isOf(Items.TOTEM_OF_UNDYING) || stack.hasEnchantments()) continue;
            return i2;
        }
        return -1;
    }

    private void swapToOffhand(int screenSlot) {
        if (screenSlot >= 36 && screenSlot <= 44) {
            int hotbarIndex = screenSlot - 36;
            AutoTotem.mc.interactionManager.clickSlot(0, 45, hotbarIndex, SlotActionType.SWAP, (PlayerEntity)AutoTotem.mc.player);
        } else {
            AutoTotem.mc.interactionManager.clickSlot(0, screenSlot, 40, SlotActionType.SWAP, (PlayerEntity)AutoTotem.mc.player);
        }
    }

    private void updateSwap() {
        boolean cond = this.condition();
        if (cond) {
            this.lastDangerTime = System.currentTimeMillis();
        }
        if (this.saveEnchanted.isState() && this.hasEnchantedInOffhand() && cond) {
            int regularTotemSlot = this.findRegularTotemSlot();
            if (regularTotemSlot != -1) {
                if (this.swapBackSlot == -1) {
                    this.swapBackSlot = regularTotemSlot;
                }
                if (this.bypassgrim.isState()) {
                    this.pendingTotemSlot = regularTotemSlot;
                    this.disableSprint();
                    this.bypassTicks = 2;
                    this.cooldownTicks = 2.0f;
                } else {
                    this.swapToOffhand(regularTotemSlot);
                }
                return;
            }
            return;
        }
        if (cond && !this.hasTotemInOffhand()) {
            int totemSlot = this.findTotemSlot();
            if (totemSlot == -1) {
                return;
            }
            if (this.swapBackSlot == -1) {
                ItemStack offhand = AutoTotem.mc.player.getOffHandStack();
                int n2 = this.swapBackSlot = offhand.isEmpty() ? -2 : totemSlot;
            }
            if (this.bypassgrim.isState()) {
                this.pendingTotemSlot = totemSlot;
                this.disableSprint();
                this.bypassTicks = 2;
                this.cooldownTicks = 2.0f;
            } else {
                this.swapToOffhand(totemSlot);
            }
        }
        if (!cond && this.swapBackSlot != -1 && System.currentTimeMillis() - this.lastDangerTime >= this.returnDelay.getValue().longValue()) {
            int targetSlot = -1;
            if (this.swapBackSlot >= 9) {
                targetSlot = this.swapBackSlot;
                this.swapBackSlot = -1;
            } else {
                int totemSlot = this.findTotemSlot();
                this.swapBackSlot = -1;
                if (totemSlot != -1 && this.hasTotemInOffhand()) {
                    targetSlot = totemSlot;
                }
            }
            if (targetSlot != -1) {
                if (this.bypassgrim.isState()) {
                    this.pendingTotemSlot = targetSlot;
                    this.disableSprint();
                    this.bypassTicks = 2;
                    this.cooldownTicks = 3.0f;
                } else {
                    this.swapToOffhand(targetSlot);
                    this.cooldownTicks = 3.0f;
                }
            }
        }
    }

    private void disableSprint() {
        if (this.sprintPaused) {
            return;
        }
        Sprint.pushPause(1000L);
        this.sprintPaused = true;
    }

    private void restoreSprint() {
        if (!this.sprintPaused) {
            return;
        }
        this.sprintPaused = false;
        Sprint.popPause();
    }

    @Override
    public void onEnable() {
        this.bypassTicks = 0;
        this.pendingTotemSlot = -1;
        this.sprintPaused = false;
        this.lastDangerTime = 0L;
        super.onEnable();
    }

    @Override
    public void onDisable() {
        this.cooldownTicks = 0.0f;
        this.swapBackSlot = -1;
        this.bypassTicks = 0;
        this.pendingTotemSlot = -1;
        this.lastDangerTime = 0L;
        this.restoreSprint();
        super.onDisable();
    }
}

