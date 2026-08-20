package polar.ru.client.modules.impl.misc;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.consume.UseAction;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import polar.ru.api.events.EventLink;
import polar.ru.api.events.implement.EventBinding;
import polar.ru.api.events.implement.EventMoveInput;
import polar.ru.api.events.implement.EventUpdate;
import polar.ru.api.utils.chat.ChatUtils;
import polar.ru.api.utils.player.InventoryUtils;
import polar.ru.client.modules.Module;
import polar.ru.client.modules.impl.movement.InventoryWalk;
import polar.ru.client.modules.impl.movement.Sprint;
import polar.ru.client.modules.settings.implement.BindSetting;
import polar.ru.client.modules.settings.implement.BooleanSetting;
import polar.ru.client.modules.settings.implement.ModeSetting;

public class ServerHelper
extends Module {
    public static ServerHelper INSTANCE = new ServerHelper();
    private final ModeSetting mode = new ModeSetting("Режим", "HolyWorld", "HolyWorld", "ReallyWorld", "LonyGrief", "Spooky");
    private final BindSetting stickHW = new BindSetting("Взрыв штучка", -1).visible(() -> this.mode.is("HolyWorld"));
    private final BindSetting gulHW = new BindSetting("Гул", -1).visible(() -> this.mode.is("HolyWorld"));
    private final BindSetting stunHW = new BindSetting("Стан", -1).visible(() -> this.mode.is("HolyWorld"));
    private final BindSetting trapkaHW = new BindSetting("Взрыв трап", -1).visible(() -> this.mode.is("HolyWorld"));
    private final BindSetting snowHW = new BindSetting("Снег", -1).visible(() -> this.mode.is("HolyWorld"));
    private final BindSetting trapkHW = new BindSetting("Трапка", -1).visible(() -> this.mode.is("HolyWorld"));
    private final BindSetting antipoletRW = new BindSetting("Анти Полет", -1).visible(() -> this.mode.is("ReallyWorld"));
    private final BindSetting lovushkaRW = new BindSetting("Ловушка", -1).visible(() -> this.mode.is("ReallyWorld"));
    private final BindSetting unictrapkaLG = new BindSetting("Уник. трапка", -1).visible(() -> this.mode.is("LonyGrief"));
    private final BindSetting deflivaLG = new BindSetting("Деф лива", -1).visible(() -> this.mode.is("LonyGrief"));
    private final BindSetting platformaLG = new BindSetting("Лива с платформой", -1).visible(() -> this.mode.is("LonyGrief"));
    private final BindSetting disorientationSP = new BindSetting("Дезориентация", -1).visible(() -> this.mode.is("Spooky"));
    private final BindSetting trapSP = new BindSetting("Трапка", -1).visible(() -> this.mode.is("Spooky"));
    private final BindSetting plastSP = new BindSetting("Пласт", -1).visible(() -> this.mode.is("Spooky"));
    private final BindSetting pilSP = new BindSetting("Явная пыль", -1).visible(() -> this.mode.is("Spooky"));
    private final BindSetting snegSP = new BindSetting("Снег заморозки", -1).visible(() -> this.mode.is("Spooky"));
    private final BindSetting auraSP = new BindSetting("Божья аура", -1).visible(() -> this.mode.is("Spooky"));
    private final BooleanSetting bypassGrimSP = new BooleanSetting("Обходить Grim", true).visible(() -> this.mode.is("Spooky"));
    private final BooleanSetting syncGuiMove = new BooleanSetting("Синхр. с GuiMove", true);
    private Item pendingItem;
    private Action pendingAction;
    private int bypassTicks;
    private boolean sprintPaused;
    private Action delayedAction;

    public ServerHelper() {
        super("ServerHelper", "", Module.ModuleCategory.MISC);
        this.addSettings(this.mode, this.stickHW, this.gulHW, this.stunHW, this.trapkaHW, this.snowHW, this.trapkHW, this.antipoletRW, this.lovushkaRW, this.unictrapkaLG, this.deflivaLG, this.platformaLG, this.disorientationSP, this.trapSP, this.plastSP, this.pilSP, this.snegSP, this.auraSP, this.bypassGrimSP, this.syncGuiMove);
    }

    public boolean isSpookyMode() {
        return this.mode.is("Spooky");
    }

    public boolean isLonyMode() {
        return this.mode.is("LonyGrief");
    }

    public boolean isHolyWorldMode() {
        return this.mode.is("HolyWorld");
    }

    public boolean isReallyWorldMode() {
        return this.mode.is("ReallyWorld");
    }

    public List<HelperBind> getActiveHelperBinds() {
        if (this.mode.is("HolyWorld")) {
            return this.getHolyWorldHelperBinds();
        }
        if (this.mode.is("ReallyWorld")) {
            return this.getReallyWorldHelperBinds();
        }
        if (this.mode.is("LonyGrief")) {
            return this.getLonyHelperBinds();
        }
        if (this.mode.is("Spooky")) {
            return this.getSpookyHelperBinds();
        }
        return List.of();
    }

    public List<HelperBind> getAllHelperBinds() {
        ArrayList<HelperBind> binds = new ArrayList<HelperBind>();
        binds.addAll(this.getHolyWorldHelperBinds());
        binds.addAll(this.getReallyWorldHelperBinds());
        binds.addAll(this.getLonyHelperBinds());
        binds.addAll(this.getSpookyHelperBinds());
        return binds;
    }

    public String resolveHelperBindName(Item item) {
        String currentModeName = this.resolveHelperBindName(this.getActiveHelperBinds(), item);
        if (currentModeName != null) {
            return currentModeName;
        }
        return this.resolveHelperBindName(this.getAllHelperBinds(), item);
    }

    private String resolveHelperBindName(List<HelperBind> binds, Item item) {
        String resolved = null;
        for (HelperBind helperBind : binds) {
            if (helperBind.item() != item) continue;
            if (resolved == null) {
                resolved = helperBind.name();
                continue;
            }
            if (resolved.equals(helperBind.name())) continue;
            return null;
        }
        return resolved;
    }

    public List<HelperBind> getHolyWorldHelperBinds() {
        return List.of(new HelperBind("Взрыв штучка", Items.FIRE_CHARGE, this.stickHW), new HelperBind("Гул", Items.FIREWORK_STAR, this.gulHW), new HelperBind("Стан", Items.NETHER_STAR, this.stunHW), new HelperBind("Взрыв трап", Items.PRISMARINE_SHARD, this.trapkaHW), new HelperBind("Снег", Items.SNOWBALL, this.snowHW), new HelperBind("Трапка", Items.POPPED_CHORUS_FRUIT, this.trapkHW));
    }

    public List<HelperBind> getReallyWorldHelperBinds() {
        return List.of(new HelperBind("Анти Полет", Items.FIREWORK_STAR, this.antipoletRW), new HelperBind("Ловушка", Items.HEART_OF_THE_SEA, this.lovushkaRW));
    }

    public List<HelperBind> getLonyHelperBinds() {
        return List.of(new HelperBind("Уник. трапка", Items.CRYING_OBSIDIAN, this.unictrapkaLG), new HelperBind("Деф лива", Items.MAGMA_CREAM, this.deflivaLG), new HelperBind("Лива с платформой", Items.CLAY_BALL, this.platformaLG));
    }

    public List<HelperBind> getSpookyHelperBinds() {
        return List.of(new HelperBind("Дезориентация", Items.ENDER_EYE, this.disorientationSP), new HelperBind("Трапка", Items.NETHERITE_SCRAP, this.trapSP), new HelperBind("Пласт", Items.DRIED_KELP, this.plastSP), new HelperBind("Явная пыль", Items.SUGAR, this.pilSP), new HelperBind("Снег заморозки", Items.SNOWBALL, this.snegSP), new HelperBind("Божья аура", Items.PHANTOM_MEMBRANE, this.auraSP));
    }

    @Override
    public void onEnable() {
        this.pendingItem = null;
        this.pendingAction = null;
        this.bypassTicks = 0;
        this.sprintPaused = false;
        this.delayedAction = null;
        super.onEnable();
    }

    @Override
    public void onDisable() {
        this.pendingItem = null;
        this.pendingAction = null;
        this.bypassTicks = 0;
        this.delayedAction = null;
        this.restoreSprint();
        super.onDisable();
    }

    @EventLink
    public void onBinding(EventBinding event) {
        if (ServerHelper.mc.currentScreen != null) {
            return;
        }
        int key = event.getKey();
        if (this.mode.is("HolyWorld")) {
            if (key == this.stickHW.getKey()) {
                this.pendingAction = Action.STICK_HW;
            } else if (key == this.gulHW.getKey()) {
                this.pendingAction = Action.GUL_HW;
            } else if (key == this.stunHW.getKey()) {
                this.pendingAction = Action.STUN_HW;
            } else if (key == this.trapkaHW.getKey()) {
                this.pendingAction = Action.TRAPKA_HW;
            } else if (key == this.snowHW.getKey()) {
                this.pendingAction = Action.SNOW_HW;
            } else if (key == this.trapkHW.getKey()) {
                this.pendingAction = Action.TRAPK_HW;
            }
            return;
        }
        if (this.mode.is("ReallyWorld")) {
            if (key == this.antipoletRW.getKey()) {
                this.pendingAction = Action.ANTIPOLET_RW;
            } else if (key == this.lovushkaRW.getKey()) {
                this.pendingAction = Action.LOVUSHKA_RW;
            }
            return;
        }
        if (this.mode.is("LonyGrief")) {
            if (key == this.unictrapkaLG.getKey()) {
                this.pendingItem = Items.CRYING_OBSIDIAN;
            } else if (key == this.deflivaLG.getKey()) {
                this.pendingItem = Items.MAGMA_CREAM;
            } else if (key == this.platformaLG.getKey()) {
                this.pendingItem = Items.CLAY_BALL;
            }
            return;
        }
        if (this.mode.is("Spooky")) {
            Action action = null;
            if (key == this.disorientationSP.getKey()) {
                action = Action.DISORIENTATION_SP;
            } else if (key == this.trapSP.getKey()) {
                action = Action.TRAP_SP;
            } else if (key == this.plastSP.getKey()) {
                action = Action.PLAST_SP;
            } else if (key == this.pilSP.getKey()) {
                action = Action.DUST_SP;
            } else if (key == this.snegSP.getKey()) {
                action = Action.FREEZE_SNOW_SP;
            } else if (key == this.auraSP.getKey()) {
                action = Action.AURA_SP;
            }
            if (action != null) {
                if (this.bypassGrimSP.isState()) {
                    this.delayedAction = action;
                    this.disableSprint();
                    this.bypassTicks = 2;
                } else {
                    this.pendingAction = action;
                }
            }
        }
    }

    @EventLink
    public void onInput(EventMoveInput e2) {
        if (this.mode.is("Spooky") && this.bypassGrimSP.isState() && this.bypassTicks > 0) {
            if (ServerHelper.mc.player == null) {
                return;
            }
            ServerHelper.mc.player.setSprinting(false);
            e2.setForward(0.0f);
            e2.setStrafe(0.0f);
            e2.setJump(false);
            e2.setSneak(false);
        }
    }

    @EventLink
    public void onUpdate(EventUpdate event) {
        if (ServerHelper.mc.player == null || ServerHelper.mc.world == null) {
            this.pendingItem = null;
            this.pendingAction = null;
            this.bypassTicks = 0;
            this.delayedAction = null;
            return;
        }
        if (this.mode.is("Spooky") && this.bypassGrimSP.isState() && this.bypassTicks > 0) {
            ServerHelper.mc.player.setSprinting(false);
            --this.bypassTicks;
            if (this.bypassTicks == 1 && this.delayedAction != null) {
                this.useAction(this.delayedAction);
                this.delayedAction = null;
            }
            if (this.bypassTicks == 0) {
                this.restoreSprint();
            }
            return;
        }
        if (this.mode.is("LonyGrief")) {
            if (this.pendingItem == null) {
                return;
            }
            InventoryUtils.swapAndUseHvH(this.pendingItem);
            this.pendingItem = null;
            return;
        }
        if (this.pendingAction == null || ServerHelper.mc.interactionManager == null) {
            return;
        }
        if (this.mode.is("ReallyWorld")) {
            if (this.pendingAction == Action.ANTIPOLET_RW) {
                if (!InventoryUtils.hasItem(Items.FIREWORK_STAR)) {
                    ChatUtils.sendMessage("Анти полет не найден!");
                    this.pendingAction = null;
                    return;
                }
                if (ServerHelper.mc.player.getItemCooldownManager().isCoolingDown(new ItemStack((ItemConvertible)Items.FIREWORK_STAR))) {
                    ChatUtils.sendMessage("У предмета полёта есть кд");
                    this.pendingAction = null;
                    return;
                }
                boolean used = InventoryUtils.antipoletrwfix(Items.FIREWORK_STAR);
                ChatUtils.sendMessage(used ? "Использовал анти полет!" : "Анти полет не найден!");
                this.pendingAction = null;
                return;
            }
            if (this.pendingAction == Action.LOVUSHKA_RW) {
                this.useSimpleItem(Items.HEART_OF_THE_SEA, "Использовал ловушку!", "Ловушка не найдена!");
                this.pendingAction = null;
                return;
            }
        }
        this.useAction(this.pendingAction);
        this.pendingAction = null;
    }

    private void useSimpleItem(Item item, String successText, String failText) {
        boolean guiActive;
        if (ServerHelper.mc.player.getItemCooldownManager().isCoolingDown(new ItemStack((ItemConvertible)item))) {
            ChatUtils.sendMessage("У предмета есть кд");
            return;
        }
        InventoryWalk guiMove = InventoryWalk.INSTANCE;
        boolean bl = guiActive = guiMove != null && guiMove.isEnable() && this.syncGuiMove.isState();
        if (guiActive) {
            guiMove.swapBypass = true;
        }
        if (ServerHelper.mc.player.getMainHandStack().getItem() == item) {
            ServerHelper.mc.interactionManager.interactItem((PlayerEntity)ServerHelper.mc.player, Hand.MAIN_HAND);
            ServerHelper.mc.player.swingHand(Hand.MAIN_HAND);
            ChatUtils.sendMessage(successText);
            if (guiActive) {
                guiMove.swapBypass = false;
            }
            return;
        }
        if (ServerHelper.mc.player.getOffHandStack().getItem() == item) {
            ServerHelper.mc.interactionManager.interactItem((PlayerEntity)ServerHelper.mc.player, Hand.OFF_HAND);
            ServerHelper.mc.player.swingHand(Hand.OFF_HAND);
            ChatUtils.sendMessage(successText);
            if (guiActive) {
                guiMove.swapBypass = false;
            }
            return;
        }
        int hotbarSlot = this.findItemSlot(item, 0, 8);
        if (hotbarSlot != -1) {
            int previousSlot = ServerHelper.mc.player.getInventory().selectedSlot;
            ServerHelper.mc.player.networkHandler.sendPacket((Packet)new UpdateSelectedSlotC2SPacket(hotbarSlot));
            ServerHelper.mc.interactionManager.interactItem((PlayerEntity)ServerHelper.mc.player, Hand.MAIN_HAND);
            ServerHelper.mc.player.swingHand(Hand.MAIN_HAND);
            ServerHelper.mc.player.networkHandler.sendPacket((Packet)new UpdateSelectedSlotC2SPacket(previousSlot));
            ChatUtils.sendMessage(successText);
            if (guiActive) {
                guiMove.swapBypass = false;
            }
            return;
        }
        int inventorySlot = this.findItemSlot(item, 9, 35);
        if (inventorySlot != -1) {
            this.useFromInventorySlot(inventorySlot);
            ChatUtils.sendMessage(successText);
            if (guiActive) {
                guiMove.swapBypass = false;
            }
            return;
        }
        if (guiActive) {
            guiMove.swapBypass = false;
        }
        ChatUtils.sendMessage(failText);
    }

    private void useAction(Action action) {
        boolean guiActive;
        if (ServerHelper.mc.player.getItemCooldownManager().isCoolingDown(new ItemStack((ItemConvertible)action.item))) {
            ChatUtils.sendMessage("У предмета " + action.cooldownName + " есть кд");
            return;
        }
        InventoryWalk guiMove = InventoryWalk.INSTANCE;
        boolean bl = guiActive = guiMove != null && guiMove.isEnable() && this.syncGuiMove.isState();
        if (guiActive) {
            guiMove.swapBypass = true;
        }
        if (this.matchesAction(ServerHelper.mc.player.getMainHandStack(), action)) {
            ServerHelper.mc.interactionManager.interactItem((PlayerEntity)ServerHelper.mc.player, Hand.MAIN_HAND);
            ServerHelper.mc.player.swingHand(Hand.MAIN_HAND);
            ChatUtils.sendMessage(action.successText);
            if (guiActive) {
                guiMove.swapBypass = false;
            }
            return;
        }
        if (this.matchesAction(ServerHelper.mc.player.getOffHandStack(), action)) {
            ServerHelper.mc.interactionManager.interactItem((PlayerEntity)ServerHelper.mc.player, Hand.OFF_HAND);
            ServerHelper.mc.player.swingHand(Hand.OFF_HAND);
            ChatUtils.sendMessage(action.successText);
            if (guiActive) {
                guiMove.swapBypass = false;
            }
            return;
        }
        int hotbarSlot = this.findMatchingSlot(action, 0, 8);
        if (hotbarSlot != -1) {
            int previousSlot = ServerHelper.mc.player.getInventory().selectedSlot;
            ServerHelper.mc.player.networkHandler.sendPacket((Packet)new UpdateSelectedSlotC2SPacket(hotbarSlot));
            ServerHelper.mc.interactionManager.interactItem((PlayerEntity)ServerHelper.mc.player, Hand.MAIN_HAND);
            ServerHelper.mc.player.swingHand(Hand.MAIN_HAND);
            ServerHelper.mc.player.networkHandler.sendPacket((Packet)new UpdateSelectedSlotC2SPacket(previousSlot));
            ChatUtils.sendMessage(action.successText);
            if (guiActive) {
                guiMove.swapBypass = false;
            }
            return;
        }
        int inventorySlot = this.findMatchingSlot(action, 9, 35);
        if (inventorySlot != -1) {
            this.useFromInventorySlot(inventorySlot);
            ChatUtils.sendMessage(action.successText);
            if (guiActive) {
                guiMove.swapBypass = false;
            }
            return;
        }
        if (guiActive) {
            guiMove.swapBypass = false;
        }
        ChatUtils.sendMessage(action.failText);
    }

    private void useFromInventorySlot(int inventorySlot) {
        boolean guiActive;
        InventoryWalk guiMove = InventoryWalk.INSTANCE;
        boolean bl = guiActive = guiMove != null && guiMove.isEnable() && this.syncGuiMove.isState();
        if (guiActive) {
            guiMove.swapBypass = true;
        }
        int previousSlot = ServerHelper.mc.player.getInventory().selectedSlot;
        int hotbarSlot = this.findTemporaryHotbarSlot();
        int screenSlot = this.toScreenSlot(inventorySlot);
        ServerHelper.mc.interactionManager.clickSlot(0, screenSlot, hotbarSlot, SlotActionType.SWAP, (PlayerEntity)ServerHelper.mc.player);
        ServerHelper.mc.player.networkHandler.sendPacket((Packet)new CloseHandledScreenC2SPacket(0));
        ServerHelper.mc.player.networkHandler.sendPacket((Packet)new UpdateSelectedSlotC2SPacket(hotbarSlot));
        ServerHelper.mc.interactionManager.interactItem((PlayerEntity)ServerHelper.mc.player, Hand.MAIN_HAND);
        ServerHelper.mc.player.swingHand(Hand.MAIN_HAND);
        ServerHelper.mc.player.networkHandler.sendPacket((Packet)new UpdateSelectedSlotC2SPacket(previousSlot));
        ServerHelper.mc.interactionManager.clickSlot(0, screenSlot, hotbarSlot, SlotActionType.SWAP, (PlayerEntity)ServerHelper.mc.player);
        ServerHelper.mc.player.networkHandler.sendPacket((Packet)new CloseHandledScreenC2SPacket(0));
        if (guiActive) {
            guiMove.swapBypass = false;
        }
    }

    private int findTemporaryHotbarSlot() {
        int fallback = 8;
        for (int slot = 0; slot < 9; ++slot) {
            if (slot == ServerHelper.mc.player.getInventory().selectedSlot) continue;
            ItemStack stack = ServerHelper.mc.player.getInventory().getStack(slot);
            if (stack.isEmpty()) {
                return slot;
            }
            if (stack.getUseAction() != UseAction.NONE) continue;
            fallback = slot;
        }
        return fallback;
    }

    private int findItemSlot(Item item, int start, int end) {
        for (int slot = start; slot <= end; ++slot) {
            ItemStack stack = ServerHelper.mc.player.getInventory().getStack(slot);
            if (stack.isEmpty() || stack.getItem() != item) continue;
            return slot;
        }
        return -1;
    }

    private int findMatchingSlot(Action action, int start, int end) {
        for (int slot = start; slot <= end; ++slot) {
            if (!this.matchesAction(ServerHelper.mc.player.getInventory().getStack(slot), action)) continue;
            return slot;
        }
        return -1;
    }

    private int toScreenSlot(int inventorySlot) {
        if (inventorySlot >= 0 && inventorySlot <= 8) {
            return 36 + inventorySlot;
        }
        return inventorySlot;
    }

    private boolean matchesAction(ItemStack stack, Action action) {
        if (stack == null || stack.isEmpty() || stack.getItem() != action.item) {
            return false;
        }
        return stack.getName().getString().toLowerCase(Locale.ROOT).contains(action.query);
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

    public record HelperBind(String name, Item item, BindSetting bind) {
    }

    private static enum Action {
        STICK_HW("взрыв", "штучки", Items.FIRE_CHARGE, "Использовал взрыв штучку!", "Штучка не найдена!"),
        GUL_HW("гул", "гула", Items.FIREWORK_STAR, "Использовал гул!", "Гул не найден!"),
        STUN_HW("стан", "стана", Items.NETHER_STAR, "Использовал стан!", "Стан не найден!"),
        TRAPKA_HW("взрыв", "трапки", Items.PRISMARINE_SHARD, "Использовал взрыв трап!", "Взрыв трап не найден!"),
        SNOW_HW("снег", "снега", Items.SNOWBALL, "Использовал снег!", "Снег не найден!"),
        TRAPK_HW("трапка", "трапки", Items.POPPED_CHORUS_FRUIT, "Использовал трапку!", "Трапка не найдена!"),
        ANTIPOLET_RW("анти", "полёта", Items.FIREWORK_STAR, "Использовал анти полет!", "Анти полет не найден!"),
        LOVUSHKA_RW("ловушка", "ловушки", Items.HEART_OF_THE_SEA, "Использовал ловушку!", "Ловушка не найдена!"),
        DISORIENTATION_SP("дезориентация", "дезориентации", Items.ENDER_EYE, "Использовал дезориентацию!", "Дезориентация не найдена!"),
        TRAP_SP("трапка", "трапки", Items.NETHERITE_SCRAP, "Использовал трапку!", "Трапка не найдена!"),
        PLAST_SP("пласт", "пласта", Items.DRIED_KELP, "Использовал пласт!", "Пласт не найден!"),
        DUST_SP("явная пыль", "пыли", Items.SUGAR, "Использовал пыль!", "Пыль не найдена!"),
        FREEZE_SNOW_SP("заморозка", "снега", Items.SNOWBALL, "Использовал снег!", "Снег не найден!"),
        AURA_SP("божья", "ауры", Items.PHANTOM_MEMBRANE, "Использовал ауру!", "Аура не найдена!");

        private final String query;
        private final String cooldownName;
        private final Item item;
        private final String successText;
        private final String failText;

        private Action(String query, String cooldownName, Item item, String successText, String failText) {
            this.query = query;
            this.cooldownName = cooldownName;
            this.item = item;
            this.successText = successText;
            this.failText = failText;
        }
    }
}

