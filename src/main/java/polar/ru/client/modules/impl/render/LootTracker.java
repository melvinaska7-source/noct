package polar.ru.client.modules.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.block.enums.ChestType;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.vehicle.ChestMinecartEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.state.property.Property;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.WorldChunk;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector4f;
import polar.ru.api.events.EventLink;
import polar.ru.api.events.implement.Event3DRender;
import polar.ru.api.events.implement.EventRender;
import polar.ru.api.storages.implement.helpertstorages.Theme;
import polar.ru.api.utils.color.ColorUtils;
import polar.ru.api.utils.render.RenderUtils;
import polar.ru.api.utils.render.fonts.msdf.Font;
import polar.ru.api.utils.render.fonts.msdf.Fonts;
import polar.ru.client.modules.Module;
import polar.ru.client.modules.settings.implement.BooleanSetting;
import polar.ru.client.modules.settings.implement.FloatSetting;
import polar.ru.polar;

public class LootTracker
extends Module {
    public static final LootTracker INSTANCE = new LootTracker();
    private static final float TAG_BOX_HEIGHT = 12.5f;
    private static final float TAG_PADDING = 5.0f;
    private static final float TAG_HUD_RADIUS = 1.1f;
    private static final int TAG_HUD_ALPHA = 204;
    private final BooleanSetting showSpawners = new BooleanSetting("Спавнеры", true);
    private final BooleanSetting showMinecarts = new BooleanSetting("Вагонетки", true);
    private final FloatSetting maxDistance = new FloatSetting("Макс. дистанция", 64.0f, 16.0f, 128.0f, 1.0f);
    private final Matrix4f lastProjectionMatrix = new Matrix4f();
    private final Quaternionf lastCameraRotation = new Quaternionf();
    private Vec3d lastCameraPos = Vec3d.ZERO;
    private float lastTickDelta;
    private boolean hasProjection;
    private final List<LootSource> cachedSources = new ArrayList<LootSource>();
    private long lastCacheUpdate = 0L;
    private static final long CACHE_UPDATE_INTERVAL = 500L;

    public LootTracker() {
        super("LootTracker", "Показывает залутанные спавнеры и вагонетки", Module.ModuleCategory.RENDER);
        this.addSettings(this.showSpawners, this.showMinecarts, this.maxDistance);
    }

    @Override
    public void onDisable() {
        this.hasProjection = false;
        this.cachedSources.clear();
        super.onDisable();
    }

    @EventLink(priority=100)
    public void onRender3D(Event3DRender event) {
        if (LootTracker.mc.player == null || LootTracker.mc.world == null) {
            return;
        }
        this.hasProjection = true;
        this.lastProjectionMatrix.set((Matrix4fc)event.getProjectionMatrix());
        this.lastCameraPos = event.getCamera().getPos();
        this.lastCameraRotation.set((Quaternionfc)event.getCamera().getRotation());
        this.lastTickDelta = event.getTickDelta();
        long now = System.currentTimeMillis();
        if (now - this.lastCacheUpdate > 500L) {
            this.updateCache();
            this.lastCacheUpdate = now;
        }
    }

    @EventLink(priority=100)
    public void onRender2D(EventRender.Default event) {
        if (!this.hasProjection || LootTracker.mc.world == null || LootTracker.mc.player == null) {
            return;
        }
        MatrixStack matrices = event.getContext().getMatrices();
        Font font = Fonts.getFont("sf_regular", 14);
        if (font == null) {
            return;
        }
        for (LootSource source : this.cachedSources) {
            Vec3d screenPos;
            if (LootTracker.mc.player.squaredDistanceTo((double)source.pos.getX(), (double)source.pos.getY(), (double)source.pos.getZ()) > (double)(this.maxDistance.getValue().floatValue() * this.maxDistance.getValue().floatValue()) || (screenPos = this.worldToScreen(new Vec3d((double)source.pos.getX() + 0.5, (double)source.pos.getY() + 1.5, (double)source.pos.getZ() + 0.5))) == null) continue;
            this.drawLootTag(matrices, font, (float)screenPos.x, (float)screenPos.y, source);
        }
    }

    private void updateCache() {
        this.cachedSources.clear();
        if (this.showSpawners.isState()) {
            int renderDistance = (Integer)LootTracker.mc.options.getViewDistance().getValue();
            ChunkPos playerChunk = LootTracker.mc.player.getChunkPos();
            for (int cx = -renderDistance; cx <= renderDistance; ++cx) {
                for (int cz = -renderDistance; cz <= renderDistance; ++cz) {
                    WorldChunk chunk = LootTracker.mc.world.getChunk(playerChunk.x + cx, playerChunk.z + cz);
                    if (chunk == null) continue;
                    for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                        MobSpawnerBlockEntity spawner;
                        if (!(blockEntity instanceof MobSpawnerBlockEntity) || !this.hasSingleChestNearby((spawner = (MobSpawnerBlockEntity)blockEntity).getPos())) continue;
                        int delay = this.getSpawnerDelay(spawner);
                        boolean isLooted = delay > 0 && delay != 20 || this.isAreaExplored(spawner.getPos());
                        this.cachedSources.add(new LootSource(spawner.getPos(), LootType.SPAWNER, isLooted));
                    }
                }
            }
        }
        if (this.showMinecarts.isState()) {
            for (Entity entity : LootTracker.mc.world.getEntities()) {
                if (!(entity instanceof ChestMinecartEntity)) continue;
                ChestMinecartEntity minecart = (ChestMinecartEntity)entity;
                boolean isLooted = this.isAreaExplored(minecart.getBlockPos());
                this.cachedSources.add(new LootSource(minecart.getBlockPos(), LootType.MINECART, isLooted));
            }
        }
    }

    private void drawLootTag(MatrixStack matrices, Font font, float x2, float y2, LootSource source) {
        String typeText = source.type == LootType.SPAWNER ? "Спавнер" : "Вагонетка";
        String statusText = source.isLooted ? " [Залутано]" : " [Не залутано]";
        float typeWidth = font.getStringWidth(typeText);
        float statusWidth = font.getStringWidth(statusText);
        float totalWidth = typeWidth + statusWidth;
        float boxWidth = totalWidth + 10.0f;
        float boxHeight = 12.5f;
        float tagX = x2 - boxWidth / 2.0f;
        float tagY = y2 - boxHeight / 2.0f;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        this.drawDefaultTagPanel(matrices, tagX, tagY, boxWidth, boxHeight);
        float textX = tagX + 5.0f;
        float textY = tagY + boxHeight / 2.0f - font.getHeight() * 0.1f;
        font.drawString(matrices, typeText, textX, textY, -1);
        int statusColor = source.isLooted ? -43691 : -11141291;
        font.drawString(matrices, statusText, textX += typeWidth, textY, statusColor);
        RenderSystem.disableBlend();
    }

    private void drawDefaultTagPanel(MatrixStack matrices, float x2, float y2, float width, float height) {
        int themeColor = this.getStableThemeColor();
        RenderUtils.drawDefaultHudPanel(matrices, x2, y2, width, height, 1.1f, 1.1f, ColorUtils.rgba(50, 50, 50, 204), ColorUtils.setAlphaColor(ColorUtils.darken(themeColor, 0.15f), 204), ColorUtils.setAlphaColor(ColorUtils.darken(themeColor, 0.05f), 204));
    }

    private int getStableThemeColor() {
        if (polar.INSTANCE == null || polar.INSTANCE.themeStorage == null || polar.INSTANCE.themeStorage.getThemes() == null) {
            return ColorUtils.getThemeColor(0);
        }
        Theme theme = polar.INSTANCE.themeStorage.getThemes().getTheme();
        if (theme == null || theme.color == null || theme.color.length == 0) {
            return ColorUtils.getThemeColor(0);
        }
        return theme.color[0];
    }

    private boolean isAreaExplored(BlockPos pos) {
        int radius = 20;
        int airCount = 0;
        int checkCount = 0;
        for (int x2 = -radius; x2 <= radius; x2 += 4) {
            for (int y2 = -radius; y2 <= radius; y2 += 4) {
                for (int z2 = -radius; z2 <= radius; z2 += 4) {
                    BlockPos checkPos = pos.add(x2, y2, z2);
                    BlockState state = LootTracker.mc.world.getBlockState(checkPos);
                    ++checkCount;
                    if (!state.isOf(Blocks.AIR) && !state.isOf(Blocks.CAVE_AIR)) continue;
                    ++airCount;
                }
            }
        }
        return (double)airCount > (double)checkCount * 0.3;
    }

    private int getSpawnerDelay(MobSpawnerBlockEntity spawner) {
        try {
            NbtCompound tag = spawner.createNbtWithIdentifyingData((RegistryWrapper.WrapperLookup)LootTracker.mc.world.getRegistryManager());
            return tag.contains("Delay") ? (int)tag.getShort("Delay") : 20;
        }
        catch (Exception e2) {
            return 20;
        }
    }

    private boolean hasSingleChestNearby(BlockPos spawnerPos) {
        int radius = 3;
        for (int x2 = -radius; x2 <= radius; ++x2) {
            for (int y2 = -radius; y2 <= radius; ++y2) {
                for (int z2 = -radius; z2 <= radius; ++z2) {
                    BlockPos checkPos = spawnerPos.add(x2, y2, z2);
                    BlockState state = LootTracker.mc.world.getBlockState(checkPos);
                    if (!(state.getBlock() instanceof ChestBlock) || state.get((Property)ChestBlock.CHEST_TYPE) != ChestType.SINGLE) continue;
                    return true;
                }
            }
        }
        return false;
    }

    private Vec3d worldToScreen(Vec3d worldPos) {
        Vector3f relative = new Vector3f((float)(worldPos.x - this.lastCameraPos.x), (float)(worldPos.y - this.lastCameraPos.y), (float)(worldPos.z - this.lastCameraPos.z));
        Quaternionf invCameraRotation = new Quaternionf((Quaternionfc)this.lastCameraRotation).conjugate();
        relative.rotate((Quaternionfc)invCameraRotation);
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
        if (Float.isNaN(screenX) || Float.isNaN(screenY)) {
            return null;
        }
        if (Float.isInfinite(screenX) || Float.isInfinite(screenY)) {
            return null;
        }
        return new Vec3d((double)screenX, (double)screenY, 0.0);
    }

    private record LootSource(BlockPos pos, LootType type, boolean isLooted) {
    }

    private static enum LootType {
        SPAWNER,
        MINECART;

    }
}

