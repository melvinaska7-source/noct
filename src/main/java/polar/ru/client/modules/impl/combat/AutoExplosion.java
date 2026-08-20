package polar.ru.client.modules.impl.combat;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import polar.ru.api.events.EventLink;
import polar.ru.api.events.implement.EventBinding;
import polar.ru.api.events.implement.EventPacket;
import polar.ru.api.events.implement.EventUpdate;
import polar.ru.api.storages.implement.RotationStorage;
import polar.ru.api.utils.input.KeyBoardUtils;
import polar.ru.api.utils.rotate.Rotation;
import polar.ru.api.utils.rotate.RotationUtils;
import polar.ru.client.modules.Module;
import polar.ru.client.modules.settings.implement.BindSetting;
import polar.ru.client.modules.settings.implement.BooleanSetting;
import polar.ru.client.modules.settings.implement.ModeSetting;

public final class AutoExplosion
extends Module {
    public static AutoExplosion INSTANCE = new AutoExplosion();
    private final ModeSetting modeBaxa = new ModeSetting("Режим взрыва", "Авто", "Авто", "По бинду");
    private final BindSetting bind = new BindSetting("Бинд", -1).visible(() -> this.modeBaxa.is("По бинду"));
    private final BooleanSetting explosionOnRightClick = new BooleanSetting("Взрыв по ПКМ", true);
    private final BooleanSetting keepCrystal = new BooleanSetting("Оставлять кристалл", false);
    private static final double INTERACT_RANGE = 4.5;
    private BlockPos targetPos;
    private int targetSlot = -1;
    private int oldSlot = -1;
    private boolean needSync;
    private Box crystalArea;
    private boolean blocked;
    private boolean internalInteract;

    public AutoExplosion() {
        super("AutoExplosion", "Автоматически взрывает кристалл", Module.ModuleCategory.COMBAT);
        this.addSettings(this.modeBaxa, this.bind, this.explosionOnRightClick, this.keepCrystal);
    }

    @EventLink
    public void onBinding(EventBinding event) {
        if (AutoExplosion.mc.player == null || AutoExplosion.mc.world == null || AutoExplosion.mc.currentScreen != null) {
            return;
        }
        if (!this.modeBaxa.is("По бинду")) {
            return;
        }
        boolean pressed = this.bind.getKey() == -1 ? event.getKey() == KeyBoardUtils.createMouseBind(2) : event.getKey() == this.bind.getKey();
        if (pressed) {
            this.placeObsidianByCrosshair();
        }
    }

    @EventLink
    public void onPacket(EventPacket event) {
        if (AutoExplosion.mc.player == null || AutoExplosion.mc.world == null) {
            return;
        }
        if (event.getType() != EventPacket.Type.SEND) {
            return;
        }
        if (this.internalInteract) {
            return;
        }
        Packet<?> var_2596_2 = event.getPacket();
        if (var_2596_2 instanceof PlayerInteractBlockC2SPacket) {
            int crystalSlot;
            PlayerInteractBlockC2SPacket packet = (PlayerInteractBlockC2SPacket)var_2596_2;
            BlockHitResult hit = packet.getBlockHitResult();
            BlockPos clickedPos = hit.getBlockPos();
            BlockPos placePos = clickedPos.offset(hit.getSide());
            if (this.isHoldingObsidian() && this.isInRange(placePos) && !AutoExplosion.mc.player.getItemCooldownManager().isCoolingDown(new ItemStack((ItemConvertible)Items.END_CRYSTAL)) && (crystalSlot = this.findCrystalSlot()) != -1) {
                this.targetPos = placePos;
                this.targetSlot = crystalSlot;
                this.blocked = true;
            }
            if (this.explosionOnRightClick.isState() && this.shouldPlaceByRightClick(clickedPos) && this.placeCrystalFromOffhand(hit, clickedPos)) {
                event.cancel();
            }
        }
    }

    @EventLink
    public void onTick(EventUpdate event) {
        if (AutoExplosion.mc.player == null || AutoExplosion.mc.world == null) {
            this.reset();
            return;
        }
        if (this.needSync) {
            this.needSync = false;
            this.restoreSelectedSlot();
        }
        if (this.targetPos != null) {
            if (AutoExplosion.mc.world.getBlockState(this.targetPos).isAir()) {
                this.targetPos = null;
            } else if (this.blocked) {
                this.blocked = false;
            } else {
                this.tryPlaceCrystalFast(this.targetPos);
            }
        }
        this.processCrystalArea();
    }

    private void tryPlaceCrystalFast(BlockPos pos) {
        if (this.targetSlot < 0 || this.targetSlot > 8 || !this.canPlaceCrystal(pos)) {
            return;
        }
        this.rotateTo(Vec3d.ofCenter((Vec3i)pos));
        this.oldSlot = AutoExplosion.mc.player.getInventory().selectedSlot;
        mc.getNetworkHandler().sendPacket((Packet)new UpdateSelectedSlotC2SPacket(this.targetSlot));
        AutoExplosion.mc.player.getInventory().selectedSlot = this.targetSlot;
        Vec3d hitVec = Vec3d.ofCenter((Vec3i)pos).add(0.0, 0.5, 0.0);
        BlockHitResult result = new BlockHitResult(hitVec, Direction.UP, pos, false);
        this.sendInteract(Hand.MAIN_HAND, result);
        AutoExplosion.mc.player.swingHand(Hand.MAIN_HAND);
        this.needSync = true;
        this.crystalArea = this.boxFromBlock(pos.up()).expand(0.1);
        this.targetPos = null;
    }

    private void processCrystalArea() {
        if (this.crystalArea == null) {
            return;
        }
        for (Entity entity : AutoExplosion.mc.world.getOtherEntities(null, this.crystalArea)) {
            EndCrystalEntity crystal;
            if (!(entity instanceof EndCrystalEntity) || !(crystal = (EndCrystalEntity)entity).isAlive()) continue;
            if (!crystal.getBoundingBox().contains(AutoExplosion.mc.player.getEyePos())) {
                this.rotateTo(crystal.getBoundingBox().getCenter());
            }
            this.attackCrystal(crystal);
            this.crystalArea = null;
            if (!this.keepCrystal.isState()) {
                this.restoreSelectedSlot();
            }
            return;
        }
    }

    private boolean shouldPlaceByRightClick(BlockPos clickedPos) {
        if (AutoExplosion.mc.player.getItemCooldownManager().isCoolingDown(new ItemStack((ItemConvertible)Items.END_CRYSTAL))) {
            return false;
        }
        if (this.isHoldingBlockForPlace()) {
            return false;
        }
        Block block = AutoExplosion.mc.world.getBlockState(clickedPos).getBlock();
        if (block != Blocks.OBSIDIAN && block != Blocks.BEDROCK) {
            return false;
        }
        return AutoExplosion.mc.world.getBlockState(clickedPos.up()).isAir();
    }

    private boolean placeCrystalFromOffhand(BlockHitResult hit, BlockPos clickedPos) {
        int slot = this.findScreenSlot(Items.END_CRYSTAL);
        if (slot == -1 && AutoExplosion.mc.player.getOffHandStack().getItem() != Items.END_CRYSTAL) {
            return false;
        }
        boolean swapped = false;
        if (AutoExplosion.mc.player.getOffHandStack().getItem() != Items.END_CRYSTAL) {
            this.swapSlotToOffhand(slot);
            swapped = true;
        }
        this.sendInteract(Hand.OFF_HAND, hit);
        AutoExplosion.mc.player.swingHand(Hand.OFF_HAND);
        this.crystalArea = this.boxFromBlock(clickedPos.up()).expand(0.1);
        if (swapped) {
            this.swapSlotToOffhand(slot);
            AutoExplosion.mc.player.networkHandler.sendPacket((Packet)new CloseHandledScreenC2SPacket(0));
        }
        return true;
    }

    private void placeObsidianByCrosshair() {
        BlockPos placePos;
        int obsidianSlot = this.findScreenSlot(Items.OBSIDIAN);
        int crystalSlot = this.findCrystalSlot();
        if (obsidianSlot == -1 || crystalSlot == -1) {
            return;
        }
        HitResult ItemStackParticleEffect = AutoExplosion.mc.crosshairTarget;
        if (!(ItemStackParticleEffect instanceof BlockHitResult)) {
            return;
        }
        BlockHitResult hit = (BlockHitResult)ItemStackParticleEffect;
        if (hit.getType() != HitResult.Type.BLOCK) {
            return;
        }
        if (AutoExplosion.mc.world.getBlockState(hit.getBlockPos()).isAir()) {
            return;
        }
        this.targetPos = placePos = hit.getBlockPos().offset(hit.getSide());
        this.targetSlot = crystalSlot;
        this.blocked = true;
        this.swapSlotToOffhand(obsidianSlot);
        this.sendInteract(Hand.OFF_HAND, hit);
        AutoExplosion.mc.player.swingHand(Hand.OFF_HAND);
        this.swapSlotToOffhand(obsidianSlot);
        AutoExplosion.mc.player.networkHandler.sendPacket((Packet)new CloseHandledScreenC2SPacket(0));
    }

    private void attackCrystal(EndCrystalEntity crystal) {
        mc.getNetworkHandler().sendPacket((Packet)PlayerInteractEntityC2SPacket.attack((Entity)crystal, (boolean)false));
        AutoExplosion.mc.player.swingHand(Hand.MAIN_HAND);
    }

    private void sendInteract(Hand hand, BlockHitResult hitResult) {
        this.internalInteract = true;
        try {
            mc.getNetworkHandler().sendPacket((Packet)new PlayerInteractBlockC2SPacket(hand, hitResult, 0));
        }
        finally {
            this.internalInteract = false;
        }
    }

    private void rotateTo(Vec3d vec) {
        Vec2f rotation = RotationUtils.getRotations(vec);
        RotationStorage.update(new Rotation(rotation.x, rotation.y), 360.0f, 360.0f, 360.0f, 360.0f, 1, 2, false);
    }

    private boolean canPlaceCrystal(BlockPos pos) {
        BlockPos up1 = pos.up();
        BlockPos up2 = pos.up(2);
        if (!AutoExplosion.mc.world.getBlockState(up1).isAir()) {
            return false;
        }
        if (!AutoExplosion.mc.world.getBlockState(up2).isAir()) {
            return false;
        }
        Box box = new Box((double)up1.getX(), (double)up1.getY(), (double)up1.getZ(), (double)up1.getX() + 1.0, (double)up1.getY() + 2.0, (double)up1.getZ() + 1.0);
        for (Entity entity : AutoExplosion.mc.world.getOtherEntities(null, box)) {
            if (entity instanceof EndCrystalEntity) continue;
            return false;
        }
        return true;
    }

    private int findCrystalSlot() {
        for (int i2 = 0; i2 < 9; ++i2) {
            if (AutoExplosion.mc.player.getInventory().getStack(i2).getItem() != Items.END_CRYSTAL) continue;
            return i2;
        }
        return -1;
    }

    private int findScreenSlot(Item item) {
        for (int i2 = 9; i2 < 45; ++i2) {
            ItemStack stack = AutoExplosion.mc.player.playerScreenHandler.getSlot(i2).getStack();
            if (stack.getItem() != item) continue;
            return i2;
        }
        return -1;
    }

    private void swapSlotToOffhand(int slot) {
        if (slot >= 36 && slot <= 44) {
            AutoExplosion.mc.interactionManager.clickSlot(0, 45, slot - 36, SlotActionType.SWAP, (PlayerEntity)AutoExplosion.mc.player);
            return;
        }
        AutoExplosion.mc.interactionManager.clickSlot(0, slot, 0, SlotActionType.SWAP, (PlayerEntity)AutoExplosion.mc.player);
        AutoExplosion.mc.interactionManager.clickSlot(0, 45, 0, SlotActionType.SWAP, (PlayerEntity)AutoExplosion.mc.player);
        AutoExplosion.mc.interactionManager.clickSlot(0, slot, 0, SlotActionType.SWAP, (PlayerEntity)AutoExplosion.mc.player);
    }

    private void restoreSelectedSlot() {
        if (this.oldSlot != -1) {
            mc.getNetworkHandler().sendPacket((Packet)new UpdateSelectedSlotC2SPacket(this.oldSlot));
            AutoExplosion.mc.player.getInventory().selectedSlot = this.oldSlot;
            this.oldSlot = -1;
        }
    }

    private Box boxFromBlock(BlockPos pos) {
        return new Box((double)pos.getX(), (double)pos.getY(), (double)pos.getZ(), (double)pos.getX() + 1.0, (double)pos.getY() + 1.0, (double)pos.getZ() + 1.0);
    }

    private boolean isHoldingObsidian() {
        return AutoExplosion.mc.player.getMainHandStack().getItem() == Items.OBSIDIAN || AutoExplosion.mc.player.getOffHandStack().getItem() == Items.OBSIDIAN;
    }

    private boolean isHoldingBlockForPlace() {
        Item main = AutoExplosion.mc.player.getMainHandStack().getItem();
        Item off = AutoExplosion.mc.player.getOffHandStack().getItem();
        return main instanceof BlockItem && main != Items.PLAYER_HEAD || off instanceof BlockItem && off != Items.PLAYER_HEAD;
    }

    private boolean isInRange(BlockPos pos) {
        return AutoExplosion.mc.player.getEyePos().distanceTo(Vec3d.ofCenter((Vec3i)pos)) <= 4.5;
    }

    private void reset() {
        if (this.oldSlot != -1 && AutoExplosion.mc.player != null && mc.getNetworkHandler() != null) {
            this.restoreSelectedSlot();
        }
        this.targetPos = null;
        this.targetSlot = -1;
        this.needSync = false;
        this.crystalArea = null;
        this.blocked = false;
        this.internalInteract = false;
    }

    @Override
    public void onEnable() {
        super.onEnable();
        this.reset();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        this.reset();
    }
    public ModeSetting getModeBaxa() {
        return this.modeBaxa;
    }
    public BindSetting getBind() {
        return this.bind;
    }
    public BooleanSetting getExplosionOnRightClick() {
        return this.explosionOnRightClick;
    }
    public BooleanSetting getKeepCrystal() {
        return this.keepCrystal;
    }
    public BlockPos getTargetPos() {
        return this.targetPos;
    }
    public int getTargetSlot() {
        return this.targetSlot;
    }
    public int getOldSlot() {
        return this.oldSlot;
    }
    public boolean isNeedSync() {
        return this.needSync;
    }
    public Box getCrystalArea() {
        return this.crystalArea;
    }
    public boolean isBlocked() {
        return this.blocked;
    }
    public boolean isInternalInteract() {
        return this.internalInteract;
    }
    public void setTargetPos(BlockPos targetPos) {
        this.targetPos = targetPos;
    }
    public void setTargetSlot(int targetSlot) {
        this.targetSlot = targetSlot;
    }
    public void setOldSlot(int oldSlot) {
        this.oldSlot = oldSlot;
    }
    public void setNeedSync(boolean needSync) {
        this.needSync = needSync;
    }
    public void setCrystalArea(Box crystalArea) {
        this.crystalArea = crystalArea;
    }
    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
    }
    public void setInternalInteract(boolean internalInteract) {
        this.internalInteract = internalInteract;
    }
}

