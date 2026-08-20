package polar.ru.client.modules.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.block.BlockState;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Position;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.chunk.WorldChunk;
import org.joml.Matrix4f;
import polar.ru.api.events.EventLink;
import polar.ru.api.events.implement.Event3DRender;
import polar.ru.client.modules.Module;
import polar.ru.client.modules.settings.implement.FloatSetting;

public class BlockESP
extends Module {
    public static BlockESP INSTANCE = new BlockESP();
    private static final float BOX_LINE_WIDTH = 2.0f;
    private static final float FILL_ALPHA = 0.18f;
    private static final float GREEN_R = 0.1f;
    private static final float GREEN_G = 1.0f;
    private static final float GREEN_B = 0.15f;
    private static final long SCAN_INTERVAL_MS = 50L;
    private static final int MAX_CHUNKS_PER_PASS = 2;
    private final FloatSetting distance = new FloatSetting("Дистанция", 60.0f, 10.0f, 120.0f, 1.0f);
    private final Set<String> trackedBlocks = ConcurrentHashMap.newKeySet();
    private final Map<BlockPos, String> foundBlocks = new ConcurrentHashMap<BlockPos, String>();
    private final Set<ChunkPos> scannedChunks = ConcurrentHashMap.newKeySet();
    private ChunkPos lastPlayerChunk;
    private int lastScanRadius = -1;
    private long lastScanTime;

    public BlockESP() {
        super("BlockESP", "Показывает выбранные блоки через стену", Module.ModuleCategory.RENDER);
        this.addSettings(this.distance);
    }

    @Override
    public void onEnable() {
        this.resetScanState();
        super.onEnable();
    }

    @Override
    public void onDisable() {
        this.resetScanState();
        super.onDisable();
    }

    @EventLink(priority=100)
    public void onRender3D(Event3DRender event) {
        long now;
        if (BlockESP.mc.world == null || BlockESP.mc.player == null || this.trackedBlocks.isEmpty()) {
            return;
        }
        int scanRadius = this.getDistance();
        ChunkPos currentChunk = new ChunkPos(BlockESP.mc.player.getBlockPos());
        if (scanRadius != this.lastScanRadius) {
            this.resetScanState();
            this.lastScanRadius = scanRadius;
        }
        if (this.lastPlayerChunk == null || !this.lastPlayerChunk.equals((Object)currentChunk)) {
            this.scannedChunks.clear();
            this.lastPlayerChunk = currentChunk;
        }
        if ((now = System.currentTimeMillis()) - this.lastScanTime >= 50L) {
            this.scanNearbyBlocks(scanRadius);
            this.lastScanTime = now;
        }
        this.cleanupInvalidAndDistantBlocks(BlockESP.mc.player.getPos(), scanRadius);
        this.renderFoundBlocks(event.getMatrices());
    }

    private void scanNearbyBlocks(int scanRadius) {
        if (BlockESP.mc.world == null || BlockESP.mc.player == null) {
            return;
        }
        BlockPos playerPos = BlockESP.mc.player.getBlockPos();
        int playerChunkX = playerPos.getX() >> 4;
        int playerChunkZ = playerPos.getZ() >> 4;
        int chunkRange = (scanRadius >> 4) + 2;
        ArrayList<ChunkPos> candidates = new ArrayList<ChunkPos>();
        for (int cx = -chunkRange; cx <= chunkRange; ++cx) {
            for (int cz = -chunkRange; cz <= chunkRange; ++cz) {
                ChunkPos chunkPos = new ChunkPos(playerChunkX + cx, playerChunkZ + cz);
                if (this.scannedChunks.contains(chunkPos)) continue;
                candidates.add(chunkPos);
            }
        }
        candidates.sort((a2, b2) -> {
            long da = this.chunkDistanceSq((ChunkPos)a2, playerChunkX, playerChunkZ);
            long db = this.chunkDistanceSq((ChunkPos)b2, playerChunkX, playerChunkZ);
            return Long.compare(da, db);
        });
        int scannedThisPass = 0;
        for (ChunkPos chunkPos : candidates) {
            if (scannedThisPass >= 2) break;
            WorldChunk chunk = BlockESP.mc.world.getChunk(chunkPos.x, chunkPos.z);
            if (chunk == null) continue;
            this.scanChunk(chunk, playerPos, scanRadius);
            this.scannedChunks.add(chunkPos);
            ++scannedThisPass;
        }
    }

    private void scanChunk(WorldChunk chunk, BlockPos playerPos, int scanRadius) {
        int minX = chunk.getPos().getStartX();
        int minZ = chunk.getPos().getStartZ();
        int maxX = minX + 15;
        int maxZ = minZ + 15;
        int minY = Math.max(BlockESP.mc.world.getBottomY(), playerPos.getY() - scanRadius);
        int maxY = Math.min(BlockESP.mc.world.getTopYInclusive(), playerPos.getY() + scanRadius);
        int radiusSq = scanRadius * scanRadius;
        BlockPos.Mutable mutable = new BlockPos.Mutable();
        for (int x2 = minX; x2 <= maxX; ++x2) {
            for (int z2 = minZ; z2 <= maxZ; ++z2) {
                for (int y2 = minY; y2 <= maxY; ++y2) {
                    String blockName;
                    BlockState state;
                    mutable.set(x2, y2, z2);
                    if (mutable.getSquaredDistance((Vec3i)playerPos) > (double)radiusSq || (state = chunk.getBlockState((BlockPos)mutable)).isAir() || !this.trackedBlocks.contains(blockName = Registries.BLOCK.getId(state.getBlock()).getPath().toLowerCase())) continue;
                    this.foundBlocks.put(mutable.toImmutable(), blockName);
                }
            }
        }
    }

    private void cleanupInvalidAndDistantBlocks(Vec3d playerPos, int renderDistance) {
        if (BlockESP.mc.world == null) {
            this.foundBlocks.clear();
            return;
        }
        int renderDistanceSq = renderDistance * renderDistance;
        this.foundBlocks.entrySet().removeIf(entry -> {
            BlockPos pos = (BlockPos)entry.getKey();
            BlockState currentState = BlockESP.mc.world.getBlockState(pos);
            if (currentState.isAir()) {
                return true;
            }
            String currentBlockName = Registries.BLOCK.getId(currentState.getBlock()).getPath().toLowerCase();
            if (!this.trackedBlocks.contains(currentBlockName)) {
                return true;
            }
            return pos.getSquaredDistance((Position)playerPos) > (double)renderDistanceSq;
        });
    }

    private void renderFoundBlocks(MatrixStack matrices) {
        if (this.foundBlocks.isEmpty()) {
            return;
        }
        Vec3d camera = BlockESP.mc.gameRenderer.getCamera().getPos();
        matrices.push();
        matrices.translate(-camera.x, -camera.y, -camera.z);
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask((boolean)false);
        RenderSystem.setShader((ShaderProgramKey)ShaderProgramKeys.POSITION_COLOR);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder fillBuffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        for (BlockPos pos : this.foundBlocks.keySet()) {
            this.addFilledBox(fillBuffer, matrix, pos, 0.1f, 1.0f, 0.15f, 0.18f);
        }
        BufferRenderer.drawWithGlobalProgram((BuiltBuffer)fillBuffer.end());
        RenderSystem.lineWidth((float)2.0f);
        BufferBuilder lineBuffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        for (BlockPos pos : this.foundBlocks.keySet()) {
            this.addOutlinedBox(lineBuffer, matrix, pos, 0.1f, 1.0f, 0.15f, 1.0f);
        }
        BufferRenderer.drawWithGlobalProgram((BuiltBuffer)lineBuffer.end());
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask((boolean)true);
        RenderSystem.disableBlend();
        matrices.pop();
    }

    private void addFilledBox(BufferBuilder buffer, Matrix4f matrix, BlockPos pos, float r2, float g2, float b2, float a2) {
        float minX = pos.getX();
        float minY = pos.getY();
        float minZ = pos.getZ();
        float maxX = minX + 1.0f;
        float maxY = minY + 1.0f;
        float maxZ = minZ + 1.0f;
        buffer.vertex(matrix, minX, minY, minZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, maxX, minY, minZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, maxX, minY, maxZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, minX, minY, maxZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, minX, maxY, minZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, minX, maxY, maxZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, maxX, maxY, minZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, minX, minY, minZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, minX, maxY, minZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, maxX, maxY, minZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, maxX, minY, minZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, minX, minY, maxZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, maxX, minY, maxZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, minX, maxY, maxZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, minX, minY, minZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, minX, minY, maxZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, minX, maxY, maxZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, minX, maxY, minZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, maxX, minY, minZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, maxX, maxY, minZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, maxX, minY, maxZ).color(r2, g2, b2, a2);
    }

    private void addOutlinedBox(BufferBuilder buffer, Matrix4f matrix, BlockPos pos, float r2, float g2, float b2, float a2) {
        float minX = pos.getX();
        float minY = pos.getY();
        float minZ = pos.getZ();
        float maxX = minX + 1.0f;
        float maxY = minY + 1.0f;
        float maxZ = minZ + 1.0f;
        buffer.vertex(matrix, minX, minY, minZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, maxX, minY, minZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, maxX, minY, minZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, maxX, minY, maxZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, maxX, minY, maxZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, minX, minY, maxZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, minX, minY, maxZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, minX, minY, minZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, minX, maxY, minZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, maxX, maxY, minZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, maxX, maxY, minZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, minX, maxY, maxZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, minX, maxY, maxZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, minX, maxY, minZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, minX, minY, minZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, minX, maxY, minZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, maxX, minY, minZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, maxX, maxY, minZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, maxX, minY, maxZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, minX, minY, maxZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, minX, maxY, maxZ).color(r2, g2, b2, a2);
    }

    public void addBlock(String blockName) {
        this.trackedBlocks.add(blockName.toLowerCase());
        this.scannedChunks.clear();
        this.foundBlocks.clear();
    }

    public void removeBlock(String blockName) {
        this.trackedBlocks.remove(blockName.toLowerCase());
        this.foundBlocks.entrySet().removeIf(entry -> ((String)entry.getValue()).equalsIgnoreCase(blockName));
    }

    public void clearBlocks() {
        this.trackedBlocks.clear();
        this.resetScanState();
    }

    public Set<String> getTrackedBlocks() {
        return new HashSet<String>(this.trackedBlocks);
    }

    public boolean isTracking(String blockName) {
        return this.trackedBlocks.contains(blockName.toLowerCase());
    }

    private int getDistance() {
        return Math.round(this.distance.get());
    }

    private long chunkDistanceSq(ChunkPos chunkPos, int playerChunkX, int playerChunkZ) {
        long dx = chunkPos.x - playerChunkX;
        long dz = chunkPos.z - playerChunkZ;
        return dx * dx + dz * dz;
    }

    private void resetScanState() {
        this.foundBlocks.clear();
        this.scannedChunks.clear();
        this.lastPlayerChunk = null;
        this.lastScanTime = 0L;
        this.lastScanRadius = -1;
    }
}

