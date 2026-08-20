package polar.ru.client.modules.impl.misc;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
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
import net.minecraft.util.math.Vec3i;
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
import polar.ru.api.utils.color.ColorUtils;
import polar.ru.api.utils.render.RenderUtils;
import polar.ru.api.utils.render.fonts.msdf.Font;
import polar.ru.api.utils.render.fonts.msdf.Fonts;
import polar.ru.client.modules.Module;

public class LayerCooldown
extends Module {
    public static LayerCooldown INSTANCE = new LayerCooldown();
    private static final long DELAYED_SCAN_MS = 250L;
    private static final int SEARCH_RADIUS = 4;
    private static final int SEARCH_HEIGHT = 4;
    private static final int MAX_TIMERS = 100;
    private static final float TIMER_SECONDS = 19.5f;
    private static final float MAX_DISTANCE = 96.0f;
    private static final double TIMER_Y_OFFSET = 0.6;
    private static final ItemStack LAYER_ICON = new ItemStack((ItemConvertible)Items.DRIED_KELP);
    private final Matrix4f lastProjectionMatrix = new Matrix4f();
    private final Quaternionf lastCameraRotation = new Quaternionf();
    private Vec3d lastCameraPos = Vec3d.ZERO;
    private boolean hasProjection;
    private final List<LayerTimer> timers = new ArrayList<LayerTimer>();
    private final List<PendingScan> pendingScans = new ArrayList<PendingScan>();

    public LayerCooldown() {
        super("LayerCooldown", "Показывает таймер возле поставленного пласта", Module.ModuleCategory.MISC);
    }

    @Override
    public void onDisable() {
        this.timers.clear();
        this.pendingScans.clear();
        this.hasProjection = false;
        super.onDisable();
    }

    @EventLink
    public void onPacket(EventPacket event) {
        if (event.getType() != EventPacket.Type.RECEIVE || LayerCooldown.mc.world == null || LayerCooldown.mc.player == null) {
            return;
        }
        Packet<?> var_2596_2 = event.getPacket();
        if (!(var_2596_2 instanceof PlaySoundS2CPacket)) {
            return;
        }
        PlaySoundS2CPacket packet = (PlaySoundS2CPacket)var_2596_2;
        String sound = this.getSoundPath(packet);
        if (sound == null) {
            return;
        }
        Vec3d soundPos = new Vec3d(packet.getX(), packet.getY(), packet.getZ());
        BlockPos blockPos = BlockPos.ofFloored((Position)soundPos);
        if ("block.piston.extend".equals(sound)) {
            this.addTimer(blockPos, soundPos);
            return;
        }
        if (this.isDelayedTrapSound(sound)) {
            this.pendingScans.add(new PendingScan(blockPos, System.currentTimeMillis() + 250L));
        }
    }

    @EventLink(priority=100)
    public void onRender3D(Event3DRender event) {
        if (LayerCooldown.mc.world == null || LayerCooldown.mc.player == null) {
            return;
        }
        this.hasProjection = true;
        this.lastProjectionMatrix.set((Matrix4fc)event.getProjectionMatrix());
        this.lastCameraRotation.set((Quaternionfc)event.getCamera().getRotation());
        this.lastCameraPos = event.getCamera().getPos();
        this.processPendingScans();
    }

    @EventLink(priority=100)
    public void onRender2D(EventRender.Default event) {
        if (!this.hasProjection || LayerCooldown.mc.world == null || LayerCooldown.mc.player == null) {
            return;
        }
        long now = System.currentTimeMillis();
        this.timers.removeIf(timer -> timer.endTime <= now);
        while (this.timers.size() > 100) {
            this.timers.remove(0);
        }
        if (this.timers.isEmpty()) {
            return;
        }
        MatrixStack matrices = event.getContext().getMatrices();
        Font font = Fonts.getFont("sf_regular", 13);
        if (font == null) {
            return;
        }
        float maxDistSq = 9216.0f;
        for (int i2 = 0; i2 < this.timers.size(); ++i2) {
            Vec3d screen;
            LayerTimer timer2 = this.timers.get(i2);
            if (LayerCooldown.mc.player.squaredDistanceTo(timer2.pos) > (double)maxDistSq || (screen = this.worldToScreen(timer2.pos)) == null) continue;
            float seconds = Math.max(0.0f, (float)(timer2.endTime - now) / 1000.0f);
            this.drawTimer(event.getContext(), matrices, font, (float)screen.x, (float)screen.y, seconds);
        }
    }

    private void processPendingScans() {
        if (this.pendingScans.isEmpty() || LayerCooldown.mc.world == null) {
            return;
        }
        long now = System.currentTimeMillis();
        Iterator<PendingScan> iterator = this.pendingScans.iterator();
        while (iterator.hasNext()) {
            PendingScan scan = iterator.next();
            if (scan.runAt > now) continue;
            BlockPos found = this.findLayerLikeBlock(scan.center);
            Vec3d pos = found == null ? Vec3d.ofCenter((Vec3i)scan.center) : new Vec3d((double)found.getX() + 0.5, (double)found.getY() + 0.65, (double)found.getZ() + 0.5);
            this.addTimer(found == null ? scan.center : found, pos);
            iterator.remove();
        }
    }

    private BlockPos findLayerLikeBlock(BlockPos center) {
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (int x2 = -4; x2 <= 4; ++x2) {
            for (int y2 = -4; y2 <= 4; ++y2) {
                for (int z2 = -4; z2 <= 4; ++z2) {
                    double distance;
                    BlockPos pos = center.add(x2, y2, z2);
                    BlockState state = LayerCooldown.mc.world.getBlockState(pos);
                    if (!this.isLayerLikeBlock(state) || !((distance = pos.getSquaredDistance((Vec3i)center)) < bestDistance)) continue;
                    bestDistance = distance;
                    best = pos;
                }
            }
        }
        return best;
    }

    private boolean isLayerLikeBlock(BlockState state) {
        if (state == null || state.isAir()) {
            return false;
        }
        Block block = state.getBlock();
        return block == Blocks.PISTON || block == Blocks.STICKY_PISTON || block == Blocks.MOVING_PISTON || block == Blocks.DRIED_KELP_BLOCK || block == Blocks.ANVIL || block == Blocks.CHIPPED_ANVIL || block == Blocks.DAMAGED_ANVIL;
    }

    private void addTimer(BlockPos blockPos, Vec3d renderPos) {
        long endTime = System.currentTimeMillis() + 19500L;
        for (int i2 = 0; i2 < this.timers.size(); ++i2) {
            LayerTimer timer = this.timers.get(i2);
            if (!(timer.blockPos.getSquaredDistance((Vec3i)blockPos) <= 2.25)) continue;
            this.timers.set(i2, new LayerTimer(blockPos, renderPos.add(0.0, 0.6, 0.0), endTime));
            return;
        }
        this.timers.add(new LayerTimer(blockPos, renderPos.add(0.0, 0.6, 0.0), endTime));
    }

    private boolean isDelayedTrapSound(String sound) {
        return "block.anvil.place".equals(sound) || "entity.zombie_horse.death".equals(sound) || "entity.ender_dragon.growl".equals(sound);
    }

    private String getSoundPath(PlaySoundS2CPacket packet) {
        try {
            return Registries.SOUND_EVENT.getId(((SoundEvent)packet.getSound().value())).getPath();
        }
        catch (Exception ignored) {
            return null;
        }
    }

    private void drawTimer(DrawContext context, MatrixStack matrices, Font font, float x2, float y2, float seconds) {
        String text = this.formatOneDecimal(seconds) + "с";
        float textWidth = font.getStringWidth(text);
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
        this.drawItemIcon(context, matrices, boxX + 4.0f, boxY + 1.25f, iconScale);
        float textY = boxY + (boxHeight - font.getHeight()) / 2.0f;
        font.drawString(matrices, text, boxX + 4.0f + iconSize + gap, textY, -1);
        RenderSystem.disableBlend();
    }

    private String formatOneDecimal(float value) {
        int scaled = Math.round(value * 10.0f);
        return scaled / 10 + "." + Math.abs(scaled % 10);
    }

    private void drawItemIcon(DrawContext context, MatrixStack matrices, float x2, float y2, float scale) {
        if (context == null) {
            return;
        }
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask((boolean)false);
        matrices.push();
        matrices.translate(x2, y2, 0.0f);
        matrices.scale(scale, scale, 1.0f);
        context.drawItem(LAYER_ICON, 0, 0);
        matrices.pop();
        RenderSystem.depthMask((boolean)true);
        RenderSystem.enableDepthTest();
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
        float ndcZ = clip.z / w2;
        float screenX = (ndcX * 0.5f + 0.5f) * (float)mc.getWindow().getScaledWidth();
        float screenY = (1.0f - (ndcY * 0.5f + 0.5f)) * (float)mc.getWindow().getScaledHeight();
        if (Float.isNaN(screenX) || Float.isNaN(screenY) || Float.isInfinite(screenX) || Float.isInfinite(screenY)) {
            return null;
        }
        if (screenX < -400.0f || screenY < -400.0f || screenX > (float)(mc.getWindow().getScaledWidth() + 400) || screenY > (float)(mc.getWindow().getScaledHeight() + 400)) {
            return null;
        }
        return new Vec3d((double)screenX, (double)screenY, (double)ndcZ);
    }

    private record PendingScan(BlockPos center, long runAt) {
    }

    private record LayerTimer(BlockPos blockPos, Vec3d pos, long endTime) {
    }
}

