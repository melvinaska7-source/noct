package polar.ru.client.modules.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Position;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector4f;
import polar.ru.api.events.EventLink;
import polar.ru.api.events.implement.Event3DRender;
import polar.ru.api.events.implement.EventPacket;
import polar.ru.api.events.implement.EventRender;
import polar.ru.api.events.implement.EventUpdate;
import polar.ru.api.utils.color.ColorUtils;
import polar.ru.api.utils.render.RenderUtils;
import polar.ru.api.utils.render.fonts.msdf.Font;
import polar.ru.api.utils.render.fonts.msdf.Fonts;
import polar.ru.client.modules.Module;
import polar.ru.client.modules.settings.implement.ModeSetting;

public class TrapTimer
extends Module {
    private static final String TRAPKA_SOUND = "block.piston.extend";
    private static final float TRAPKA_PITCH = 0.5f;
    private static final float TRAPKA_VOLUME = 0.7f;
    private static final long TRAPKA_DURATION = 15000L;
    private static final String PLAST_SOUND = "block.anvil.place";
    private static final float PLAST_PITCH = 1.1f;
    private static final float PLAST_VOLUME = 0.7f;
    private static final long PLAST_DURATION = 15000L;
    private static final String DRAGON_TRAP_SOUND = "entity.ender_dragon.growl";
    private static final float DRAGON_TRAP_PITCH = 1.0f;
    private static final float DRAGON_TRAP_VOLUME = 0.2f;
    private static final long DRAGON_TRAP_DURATION = 30000L;
    private static final long DRAGON_PLAST_DURATION = 20000L;
    private static final float MAX_DISTANCE = 96.0f;
    private static final double RENDER_Y_OFFSET = 2.0;
    public static TrapTimer INSTANCE = new TrapTimer();
    private final ModeSetting mode = new ModeSetting("Режим", "Фантайм", "Фантайм", "спуки тайм");
    private final List<TrapPosition> trapPositions = new ArrayList<TrapPosition>();
    private final Matrix4f lastProjectionMatrix = new Matrix4f();
    private final Quaternionf lastCameraRotation = new Quaternionf();
    private Vec3d lastCameraPos = Vec3d.ZERO;
    private boolean hasProjection;
    private int previousNetheriteHoeCount = -1;

    public TrapTimer() {
        super("TrapTimer", "Отображает таймеры для трапок, пластов, драконьих ловушек и драконьих пластов", Module.ModuleCategory.RENDER);
        this.addSettings(this.mode);
    }

    @Override
    public void onDisable() {
        this.trapPositions.clear();
        this.previousNetheriteHoeCount = -1;
        this.hasProjection = false;
        super.onDisable();
    }

    public void addTrapTimer(Vec3d position) {
        if (!this.isEnable() || this.mode.is("спуки тайм")) {
            return;
        }
        this.trapPositions.add(new TrapPosition(position, System.currentTimeMillis(), TrapType.TRAPKA, 15000L, true, false));
    }

    public void addPlastTimer(Vec3d position) {
        if (!this.isEnable() || this.mode.is("спуки тайм")) {
            return;
        }
        this.trapPositions.add(new TrapPosition(position, System.currentTimeMillis(), TrapType.PLAST, 15000L, true, false));
    }

    @EventLink
    public void onUpdate(EventUpdate event) {
        if (!this.isEnable() || TrapTimer.mc.player == null || TrapTimer.mc.world == null) {
            return;
        }
        int currentCount = this.countNetheriteHoeInInventory();
        if (this.previousNetheriteHoeCount == -1) {
            this.previousNetheriteHoeCount = currentCount;
            return;
        }
        if (this.previousNetheriteHoeCount > 0 && currentCount < this.previousNetheriteHoeCount) {
            this.trapPositions.add(new TrapPosition(TrapTimer.mc.player.getPos(), System.currentTimeMillis(), TrapType.TRAPKA, 15000L, true, false));
        }
        this.previousNetheriteHoeCount = currentCount;
    }

    @EventLink
    public void onPacket(EventPacket event) {
        if (!this.isEnable() || event.getType() != EventPacket.Type.RECEIVE) {
            return;
        }
        Packet<?> var_2596_2 = event.getPacket();
        if (!(var_2596_2 instanceof PlaySoundS2CPacket)) {
            return;
        }
        PlaySoundS2CPacket packet = (PlaySoundS2CPacket)var_2596_2;
        String soundName = this.getSoundPath(packet);
        if (soundName == null) {
            return;
        }
        float pitch = packet.getPitch();
        float volume = packet.getVolume();
        Vec3d position = new Vec3d(packet.getX(), packet.getY(), packet.getZ());
        TrapType detectedType = null;
        long duration = 0L;
        if (!this.mode.is("спуки тайм") && this.mode.is("Фантайм") && soundName.equals(PLAST_SOUND) && Math.abs(pitch - 1.1f) < 0.01f && Math.abs(volume - 0.7f) < 0.01f) {
            detectedType = TrapType.PLAST;
            duration = 15000L;
        } else if (!this.mode.is("спуки тайм") && soundName.equals(TRAPKA_SOUND) && Math.abs(pitch - 0.5f) < 0.01f && Math.abs(volume - 0.7f) < 0.01f) {
            detectedType = TrapType.TRAPKA;
            duration = 15000L;
        } else if (this.mode.is("спуки тайм") && soundName.equals(TRAPKA_SOUND) && Math.abs(pitch - 0.5f) < 0.01f && Math.abs(volume - 0.5f) < 0.01f) {
            detectedType = TrapType.TRAPKA;
            duration = 15000L;
        } else {
            if (this.mode.is("спуки тайм") && soundName.equals(PLAST_SOUND) && Math.abs(pitch - 0.5f) < 0.01f && Math.abs(volume - 0.5f) < 0.01f) {
                this.trapPositions.add(new TrapPosition(position, System.currentTimeMillis(), TrapType.PLAST, 20000L, false, true));
                return;
            }
            if (this.mode.is("спуки тайм") && soundName.equals(DRAGON_TRAP_SOUND) && Math.abs(pitch - 0.7f) < 0.01f && Math.abs(volume - 0.5f) < 0.01f) {
                TrapPosition temp = new TrapPosition(position, System.currentTimeMillis(), TrapType.DRAGON_PLAST, 20000L, false, false);
                this.trapPositions.add(temp);
                this.determineDragonType(temp);
                return;
            }
            if (!this.mode.is("спуки тайм") && soundName.equals(DRAGON_TRAP_SOUND) && Math.abs(pitch - 1.0f) < 0.01f && Math.abs(volume - 0.2f) < 0.01f) {
                TrapPosition temp = new TrapPosition(position, System.currentTimeMillis(), TrapType.DRAGON_PLAST, 20000L, false, false);
                this.trapPositions.add(temp);
                this.determineDragonType(temp);
                return;
            }
        }
        if (detectedType != null) {
            this.trapPositions.add(new TrapPosition(position, System.currentTimeMillis(), detectedType, duration, true, false));
        }
    }

    @EventLink(priority=100)
    public void onRender3D(Event3DRender event) {
        if (!this.isEnable() || TrapTimer.mc.world == null || TrapTimer.mc.player == null) {
            return;
        }
        this.hasProjection = true;
        this.lastProjectionMatrix.set((Matrix4fc)event.getProjectionMatrix());
        this.lastCameraRotation.set((Quaternionfc)event.getCamera().getRotation());
        this.lastCameraPos = event.getCamera().getPos();
    }

    @EventLink(priority=100)
    public void onRender2D(EventRender.Default event) {
        if (!this.isEnable() || !this.hasProjection || TrapTimer.mc.world == null || TrapTimer.mc.player == null) {
            return;
        }
        long currentTime = System.currentTimeMillis();
        this.updateTrapStates(currentTime);
        this.trapPositions.removeIf(pos -> pos.typeDetermined && currentTime - pos.creationTime > pos.duration || !pos.typeDetermined && currentTime - pos.creationTime > 1000L);
        if (this.trapPositions.isEmpty()) {
            return;
        }
        Font font = Fonts.getFont("sf_regular", 13);
        if (font == null) {
            return;
        }
        MatrixStack matrices = event.getContext().getMatrices();
        float maxDistSq = 9216.0f;
        for (TrapPosition trap : this.trapPositions) {
            Vec3d screen;
            Vec3d renderPos;
            long elapsed;
            long remaining;
            float remainingSeconds;
            if (!trap.typeDetermined && currentTime - trap.creationTime < 50L || (remainingSeconds = (float)(remaining = trap.duration - (elapsed = currentTime - trap.creationTime)) / 1000.0f) <= 0.0f || TrapTimer.mc.player.squaredDistanceTo(renderPos = trap.position.add(0.0, 2.0, 0.0)) > (double)maxDistSq || (screen = this.worldToScreen(renderPos)) == null) continue;
            this.drawTrapTimer(event.getContext(), matrices, font, (float)screen.x, (float)screen.y + 5.0f, remainingSeconds, trap.type);
        }
    }

    private void updateTrapStates(long currentTime) {
        for (TrapPosition trap : this.trapPositions) {
            if (!trap.typeDetermined && currentTime - trap.creationTime > 50L) {
                if (trap.type == TrapType.DRAGON_PLAST) {
                    this.determineDragonType(trap);
                }
                if (currentTime - trap.creationTime > 300L) {
                    trap.typeDetermined = true;
                }
            }
            if (!trap.needsNetheriteCheck || currentTime - trap.creationTime < 500L) continue;
            if (this.hasNetheriteBlockInRadius(trap.position, 5, 9, 5)) {
                trap.type = TrapType.DRAGON_PLAST;
                trap.duration = 30000L;
            } else {
                trap.type = TrapType.PLAST;
                trap.duration = 20000L;
            }
            trap.needsNetheriteCheck = false;
            trap.typeDetermined = true;
        }
    }

    private void determineDragonType(TrapPosition trap) {
        if (trap.typeDetermined || TrapTimer.mc.world == null) {
            return;
        }
        if (this.hasRespawnAnchorInRadius(trap.position, 6)) {
            trap.type = TrapType.DRAGON_TRAP;
            trap.duration = 30000L;
            trap.typeDetermined = true;
        }
    }

    private boolean hasRespawnAnchorInRadius(Vec3d position, int radius) {
        if (TrapTimer.mc.world == null) {
            return false;
        }
        BlockPos centerPos = BlockPos.ofFloored((Position)position);
        for (int x2 = -radius; x2 <= radius; ++x2) {
            for (int z2 = -radius; z2 <= radius; ++z2) {
                for (int y2 = -3; y2 <= 3; ++y2) {
                    BlockPos checkPos = centerPos.add(x2, y2, z2);
                    if (!TrapTimer.mc.world.getBlockState(checkPos).isOf(Blocks.RESPAWN_ANCHOR)) continue;
                    return true;
                }
            }
        }
        return false;
    }

    private int countNetheriteHoeInInventory() {
        if (TrapTimer.mc.player == null) {
            return 0;
        }
        int count = 0;
        int size = TrapTimer.mc.player.getInventory().size();
        for (int i2 = 0; i2 < size; ++i2) {
            ItemStack stack = TrapTimer.mc.player.getInventory().getStack(i2);
            if (!stack.isOf(Items.NETHERITE_HOE)) continue;
            count += stack.getCount();
        }
        return count;
    }

    private boolean hasNetheriteBlockInRadius(Vec3d position, int radiusX, int radiusY, int radiusZ) {
        if (TrapTimer.mc.world == null) {
            return false;
        }
        BlockPos centerPos = BlockPos.ofFloored((Position)position);
        for (int x2 = -radiusX; x2 <= radiusX; ++x2) {
            for (int z2 = -radiusZ; z2 <= radiusZ; ++z2) {
                for (int y2 = -radiusY; y2 <= radiusY; ++y2) {
                    BlockPos checkPos = centerPos.add(x2, y2, z2);
                    if (!TrapTimer.mc.world.getBlockState(checkPos).isOf(Blocks.NETHERITE_BLOCK)) continue;
                    return true;
                }
            }
        }
        return false;
    }

    private void drawTrapTimer(DrawContext context, MatrixStack matrices, Font font, float x2, float y2, float seconds, TrapType type) {
        String timeLabel = String.format(Locale.ROOT, "%.1fс", Float.valueOf(seconds));
        ItemStack icon = switch (type.ordinal()) {
            default -> throw new MatchException(null, null);
            case 0, 2 -> new ItemStack((ItemConvertible)Items.NETHERITE_SCRAP);
            case 1, 3 -> new ItemStack((ItemConvertible)Items.DRIED_KELP);
        };
        float textWidth = font.getStringWidth(timeLabel);
        float iconSize = 10.0f;
        float iconScale = 0.62f;
        float gap = 3.0f;
        float boxWidth = iconSize + gap + textWidth + 8.0f;
        float boxHeight = 12.5f;
        float boxX = x2 - boxWidth * 0.5f;
        float boxY = y2 - boxHeight * 0.5f;
        int themeColor = ColorUtils.getThemeColor();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderUtils.drawDefaultHudThemedPanel(matrices, boxX, boxY, boxWidth, boxHeight, 2.0f, 3.0f, themeColor);
        this.drawItemIcon(context, matrices, icon, boxX + 4.0f, boxY + 1.25f, iconScale);
        float textY = boxY + (boxHeight - font.getHeight()) / 2.0f;
        font.drawString(matrices, timeLabel, boxX + 4.0f + iconSize + gap, textY, -1);
        RenderSystem.disableBlend();
    }

    private void drawItemIcon(DrawContext context, MatrixStack matrices, ItemStack icon, float x2, float y2, float scale) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask((boolean)false);
        matrices.push();
        matrices.translate(x2, y2, 0.0f);
        matrices.scale(scale, scale, 1.0f);
        context.drawItem(icon, 0, 0);
        matrices.pop();
        RenderSystem.depthMask((boolean)true);
        RenderSystem.enableDepthTest();
    }

    private String getSoundPath(PlaySoundS2CPacket packet) {
        try {
            return Registries.SOUND_EVENT.getId(((SoundEvent)packet.getSound().value())).getPath();
        }
        catch (Exception ignored) {
            return null;
        }
    }

    private Vec3d worldToScreen(Vec3d worldPos) {
        if (mc == null || mc.getWindow() == null) {
            return null;
        }
        Vector3f relative = new Vector3f((float)(worldPos.x - this.lastCameraPos.x), (float)(worldPos.y - this.lastCameraPos.y), (float)(worldPos.z - this.lastCameraPos.z));
        Quaternionf invCameraRot = new Quaternionf((Quaternionfc)this.lastCameraRotation).conjugate();
        relative.rotate((Quaternionfc)invCameraRot);
        Vector4f clip = new Vector4f(relative.x, relative.y, relative.z, 1.0f);
        this.lastProjectionMatrix.transform(clip);
        float w2 = clip.w;
        if (w2 <= 1.0E-5f) {
            return null;
        }
        float ndcX = clip.x / w2;
        float ndcY = clip.y / w2;
        float screenX = (ndcX * 0.5f + 0.5f) * (float)mc.getWindow().getScaledWidth();
        float screenY = (1.0f - (ndcY * 0.5f + 0.5f)) * (float)mc.getWindow().getScaledHeight();
        if (Float.isNaN(screenX) || Float.isNaN(screenY) || Float.isInfinite(screenX) || Float.isInfinite(screenY)) {
            return null;
        }
        if (screenX < -400.0f || screenY < -400.0f || screenX > (float)(mc.getWindow().getScaledWidth() + 400) || screenY > (float)(mc.getWindow().getScaledHeight() + 400)) {
            return null;
        }
        return new Vec3d((double)screenX, (double)screenY, (double)(clip.z / w2));
    }

    private static class TrapPosition {
        final Vec3d position;
        final long creationTime;
        TrapType type;
        long duration;
        boolean typeDetermined;
        boolean needsNetheriteCheck;

        TrapPosition(Vec3d position, long creationTime, TrapType type, long duration, boolean typeDetermined, boolean needsNetheriteCheck) {
            this.position = position;
            this.creationTime = creationTime;
            this.type = type;
            this.duration = duration;
            this.typeDetermined = typeDetermined;
            this.needsNetheriteCheck = needsNetheriteCheck;
        }
    }

    private static enum TrapType {
        TRAPKA,
        PLAST,
        DRAGON_TRAP,
        DRAGON_PLAST;

    }
}

