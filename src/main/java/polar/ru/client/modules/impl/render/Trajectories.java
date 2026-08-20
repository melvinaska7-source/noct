package polar.ru.client.modules.impl.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.Blocks;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.entity.projectile.thrown.SnowballEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.TridentItem;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Position;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.joml.Matrix4f;
import polar.ru.api.events.EventLink;
import polar.ru.api.events.implement.Event3DRender;
import polar.ru.api.utils.color.ColorUtils;
import polar.ru.api.utils.player.InventoryUtils;
import polar.ru.client.modules.Module;
import polar.ru.client.modules.settings.implement.BooleanSetting;
import polar.ru.client.modules.settings.implement.FloatSetting;
import polar.ru.client.modules.settings.implement.ModeSetting;

public class Trajectories
extends Module {
    public static Trajectories INSTANCE = new Trajectories();
    private static final int MAX_STEPS = 150;
    private static final Identifier GLOW_TEXTURE = Identifier.of((String)"polar", (String)"textures/trajectories/glow.png");
    private final List<Predicted> predicted = new ArrayList<Predicted>();
    private final BooleanSetting arrows = new BooleanSetting("Стрелы", true);
    private final BooleanSetting tridents = new BooleanSetting("Трезубцы", true);
    private final BooleanSetting items = new BooleanSetting("Предметы", true);
    private final BooleanSetting pearls = new BooleanSetting("Жемчуг", true);
    private final BooleanSetting snowballs = new BooleanSetting("Снежки", true);
    private final BooleanSetting potions = new BooleanSetting("Зелья", true);
    private final ModeSetting renderMode = new ModeSetting("Режим рендера", "Glow", "Default", "Glow");
    private final FloatSetting glowSize = new FloatSetting("Размер свечения", 1.0f, 0.5f, 3.0f, 0.1f);
    private final BooleanSetting walls = new BooleanSetting("Сквозь стены", true);

    public Trajectories() {
        super("Trajectories", "Показывает траекторию предмета в руке", Module.ModuleCategory.RENDER);
        this.addSettings(this.arrows, this.tridents, this.items, this.pearls, this.snowballs, this.potions, this.renderMode, this.glowSize, this.walls);
    }

    @EventLink
    public void onRender3D(Event3DRender event) {
        Object params;
        if (Trajectories.mc.player == null || Trajectories.mc.world == null) {
            return;
        }
        this.predicted.clear();
        ItemStack stack = this.getHeldProjectileStack();
        if (!stack.isEmpty() && (params = this.getParams(stack)) != null) {
            Vec3d[] directions;
            float tickDelta = event.getTickDelta();
            Vec3d startPos = Trajectories.mc.player.getCameraPosVec(tickDelta);
            for (Vec3d direction : directions = this.getShotDirections(stack, tickDelta)) {
                PredictionResult result = this.predict((PlayerEntity)Trajectories.mc.player, (ProjectileParams)params, startPos, direction, true);
                if (result == null || result.points.size() < 2) continue;
                this.predicted.add(new Predicted(null, result.points, result.ticks, result.entityHit));
            }
        }
        for (Entity entity : Trajectories.mc.world.getEntities()) {
            PredictionResult result;
            ArrayList sortedPlayers;
            ProjectileEntity projectile;
            if (!this.isValid(entity)) continue;
            if (entity instanceof ProjectileEntity && (projectile = (ProjectileEntity)entity).getOwner() == null && !(sortedPlayers = new ArrayList(Trajectories.mc.world.getPlayers())).isEmpty()) {
                sortedPlayers.sort((a2, b2) -> Double.compare(((Entity)a2).distanceTo((Entity)projectile), ((Entity)b2).distanceTo((Entity)projectile)));
                projectile.setOwner((Entity)sortedPlayers.get(0));
            }
            if ((result = this.predictEntity(entity)) == null || result.points.size() < 2) continue;
            this.predicted.add(new Predicted(entity, result.points, result.ticks, result.entityHit));
        }
        if (this.predicted.isEmpty()) {
            return;
        }
        MatrixStack matrices = event.getMatrices();
        Camera camera = event.getCamera();
        Vec3d cameraPos = camera.getPos();
        int themeColor = ColorUtils.getThemeColor();
        matrices.push();
        matrices.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        if (this.walls.isState()) {
            RenderSystem.disableDepthTest();
        }
        if (this.renderMode.is("Default")) {
            this.renderDefault(matrices, themeColor, event.getTickDelta());
        } else {
            this.renderGlow(matrices, themeColor, event.getTickDelta());
        }
        matrices.pop();
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private void renderDefault(MatrixStack matrices, int themeColor, float tickDelta) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        RenderSystem.setShader((ShaderProgramKey)ShaderProgramKeys.POSITION_COLOR);
        BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        int color = ColorUtils.setAlphaColor(themeColor, 190);
        int r2 = color >> 16 & 0xFF;
        int g2 = color >> 8 & 0xFF;
        int b2 = color & 0xFF;
        int a2 = color >> 24 & 0xFF;
        for (Predicted predict : this.predicted) {
            Vec3d prevPos = predict.vectors.get(0);
            if (predict.entity != null) {
                Vec3d entityPos = this.getInterpolatedPos(predict.entity, tickDelta);
                builder.vertex(matrix, (float)entityPos.x, (float)entityPos.y, (float)entityPos.z).color(r2, g2, b2, a2);
                builder.vertex(matrix, (float)prevPos.x, (float)prevPos.y, (float)prevPos.z).color(r2, g2, b2, a2);
            }
            for (Vec3d pos : predict.vectors) {
                builder.vertex(matrix, (float)prevPos.x, (float)prevPos.y, (float)prevPos.z).color(r2, g2, b2, a2);
                builder.vertex(matrix, (float)pos.x, (float)pos.y, (float)pos.z).color(r2, g2, b2, a2);
                prevPos = pos;
            }
        }
        BufferRenderer.drawWithGlobalProgram((BuiltBuffer)builder.end());
    }

    private void renderGlow(MatrixStack matrices, int themeColor, float tickDelta) {
        RenderSystem.setShaderTexture((int)0, (Identifier)GLOW_TEXTURE);
        RenderSystem.setShader((ShaderProgramKey)ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.blendFunc((GlStateManager.SrcFactor)GlStateManager.SrcFactor.SRC_ALPHA, (GlStateManager.DstFactor)GlStateManager.DstFactor.ONE);
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        int color = ColorUtils.setAlphaColor(themeColor, 255);
        int r2 = color >> 16 & 0xFF;
        int g2 = color >> 8 & 0xFF;
        int b2 = color & 0xFF;
        for (Predicted predict : this.predicted) {
            Vec3d entityPos;
            Vec3d prevPos = predict.vectors.get(0);
            Vec3d VanillaChestLootTableGenerator = entityPos = predict.entity != null ? this.getInterpolatedPos(predict.entity, tickDelta) : prevPos;
            if (predict.entity != null && entityPos.distanceTo(Trajectories.mc.player.getEyePos()) > 2.0) {
                for (int i2 = 0; i2 < 10; ++i2) {
                    float t2 = (float)i2 / 10.0f;
                    Vec3d interpolatedPos = entityPos.add(prevPos.subtract(entityPos).multiply((double)t2));
                    float dist = (float)prevPos.distanceTo(entityPos);
                    float sizeMultiplier = this.glowSize.getValue().floatValue();
                    this.drawGlow(matrices, interpolatedPos, buffer, dist / 3.0f * sizeMultiplier, 1.0f, r2, g2, b2);
                    this.drawGlow(matrices, interpolatedPos, buffer, dist * 2.0f * sizeMultiplier, 0.05f, r2, g2, b2);
                }
            }
            for (Vec3d pos : predict.vectors) {
                if (pos.distanceTo(Trajectories.mc.player.getEyePos()) > 2.0) {
                    for (int i3 = 0; i3 < 10; ++i3) {
                        float t3 = (float)i3 / 10.0f;
                        Vec3d interpolatedPos = prevPos.add(pos.subtract(prevPos).multiply((double)t3));
                        float dist = (float)pos.distanceTo(prevPos);
                        float sizeMultiplier = this.glowSize.getValue().floatValue();
                        this.drawGlow(matrices, interpolatedPos, buffer, dist / 3.0f * sizeMultiplier, 1.0f, r2, g2, b2);
                        this.drawGlow(matrices, interpolatedPos, buffer, dist * 2.0f * sizeMultiplier, 0.05f, r2, g2, b2);
                    }
                }
                prevPos = pos;
            }
        }
        try {
            BufferRenderer.drawWithGlobalProgram((BuiltBuffer)buffer.end());
        }
        catch (IllegalStateException illegalStateException) {
            // empty catch block
        }
        RenderSystem.defaultBlendFunc();
    }

    private void drawGlow(MatrixStack ms, Vec3d pos, BufferBuilder buffer, float size, float alpha, int r2, int g2, int b2) {
        ms.push();
        ms.translate(pos.x, pos.y, pos.z);
        ms.multiply(Trajectories.mc.gameRenderer.getCamera().getRotation());
        Matrix4f matrix = ms.peek().getPositionMatrix();
        int a2 = (int)(255.0f * alpha);
        buffer.vertex(matrix, -size / 2.0f, -size / 2.0f, 0.0f).texture(0.0f, 0.0f).color(r2, g2, b2, a2);
        buffer.vertex(matrix, -size / 2.0f, size / 2.0f, 0.0f).texture(0.0f, 1.0f).color(r2, g2, b2, a2);
        buffer.vertex(matrix, size / 2.0f, size / 2.0f, 0.0f).texture(1.0f, 1.0f).color(r2, g2, b2, a2);
        buffer.vertex(matrix, size / 2.0f, -size / 2.0f, 0.0f).texture(1.0f, 0.0f).color(r2, g2, b2, a2);
        ms.pop();
    }

    private Vec3d getInterpolatedPos(Entity entity, float tickDelta) {
        double x2 = MathHelper.lerp((double)tickDelta, (double)entity.prevX, (double)entity.getX());
        double y2 = MathHelper.lerp((double)tickDelta, (double)entity.prevY, (double)entity.getY());
        double z2 = MathHelper.lerp((double)tickDelta, (double)entity.prevZ, (double)entity.getZ());
        return new Vec3d(x2, y2, z2);
    }

    private boolean isValid(Entity entity) {
        boolean valid = false;
        if (this.arrows.isState() && entity instanceof ArrowEntity) {
            valid = true;
        }
        if (this.tridents.isState() && entity instanceof TridentEntity) {
            TridentEntity trident = (TridentEntity)entity;
            if (trident.returnTimer <= 0) {
                valid = true;
            }
        }
        if (this.items.isState() && entity instanceof ItemEntity) {
            valid = true;
        }
        if (this.pearls.isState() && entity instanceof EnderPearlEntity) {
            valid = true;
        }
        if (this.snowballs.isState() && entity instanceof SnowballEntity) {
            valid = true;
        }
        if (this.potions.isState() && entity instanceof PotionEntity) {
            valid = true;
        }
        if (!valid) {
            return false;
        }
        Vec3d velocity = entity.getVelocity();
        return Math.abs(velocity.x + velocity.z) > 0.01 || Math.abs(velocity.y) > 0.2;
    }

    private PredictionResult predictEntity(Entity entity) {
        ArrayList<Vec3d> positions = new ArrayList<Vec3d>();
        Vec3d lastPos = entity.getPos();
        Vec3d lastMotion = entity.getVelocity();
        Entity collidedEntity = null;
        int ticks = 0;
        int i2 = 0;
        while (i2 < 150) {
            Vec3d motion = this.predictMotion(entity, lastMotion);
            Vec3d pos = lastPos.add(motion);
            ticks = i2++;
            Entity collided = this.checkEntityCollision(entity, pos);
            if (collided != null) {
                positions.add(pos);
                collidedEntity = collided;
                break;
            }
            BlockHitResult blockHit = Trajectories.mc.world.raycast(new RaycastContext(lastPos, pos, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, entity));
            if (blockHit.getType() != HitResult.Type.MISS) {
                positions.add(blockHit.getPos());
                break;
            }
            positions.add(pos);
            lastPos = pos;
            lastMotion = motion;
        }
        return positions.isEmpty() ? null : new PredictionResult(positions, null, (Vec3d)positions.get(positions.size() - 1), collidedEntity, ticks);
    }

    private Entity checkEntityCollision(Entity movingEntity, Vec3d predictedPos) {
        Vec3d currentPos = movingEntity.getPos();
        Vec3d direction = predictedPos.subtract(currentPos);
        if (direction.lengthSquared() == 0.0) {
            return null;
        }
        EntityHitResult hitResult = ProjectileUtil.raycast((Entity)movingEntity, (Vec3d)currentPos, (Vec3d)predictedPos, (Box)movingEntity.getBoundingBox().stretch(direction).expand(0.5), entity -> Trajectories.mc.player != entity && entity.isAlive() && !(entity instanceof ItemEntity) && !(entity instanceof ExperienceOrbEntity) && entity != movingEntity, (double)direction.lengthSquared());
        return hitResult != null ? hitResult.getEntity() : null;
    }

    private Vec3d predictMotion(Entity entity, Vec3d motion) {
        return motion.multiply(0.99).add(0.0, -entity.getFinalGravity(), 0.0);
    }

    private ItemStack getHeldProjectileStack() {
        ItemStack main = Trajectories.mc.player.getMainHandStack();
        if (!main.isEmpty() && this.getParams(main) != null) {
            return main;
        }
        ItemStack off = Trajectories.mc.player.getOffHandStack();
        if (!off.isEmpty() && this.getParams(off) != null) {
            return off;
        }
        return ItemStack.EMPTY;
    }

    private ProjectileParams getParams(ItemStack stack) {
        Item item = stack.getItem();
        if (item == Items.ENDER_PEARL || item == Items.SNOWBALL || item == Items.EGG) {
            return new ProjectileParams(1.5, 0.03, 0.99);
        }
        if (item == Items.SPLASH_POTION || item == Items.LINGERING_POTION) {
            return new ProjectileParams(0.5, 0.05, 0.99);
        }
        if (item instanceof BowItem) {
            double velocity;
            float power = 1.0f;
            if (Trajectories.mc.player.isUsingItem() && Trajectories.mc.player.getActiveItem() == stack) {
                float use = Trajectories.mc.player.getItemUseTime();
                float f2 = use / 20.0f;
                f2 = (f2 * f2 + f2 * 2.0f) / 3.0f;
                power = Math.min(f2, 1.0f);
            }
            return (velocity = 3.0 * (double)power) <= 0.01 ? null : new ProjectileParams(velocity, 0.05, 0.99);
        }
        if (item instanceof CrossbowItem) {
            if (!CrossbowItem.isCharged((ItemStack)stack)) {
                return null;
            }
            return new ProjectileParams(3.15, 0.05, 0.99);
        }
        if (item instanceof TridentItem) {
            return new ProjectileParams(2.5, 0.05, 0.99);
        }
        return null;
    }

    private Vec3d[] getShotDirections(ItemStack stack, float tickDelta) {
        Vec3d baseDir = Trajectories.mc.player.getRotationVec(tickDelta).normalize();
        if (!(stack.getItem() instanceof CrossbowItem) || InventoryUtils.getEnchantmentLevel(stack, (RegistryKey<Enchantment>)Enchantments.MULTISHOT) <= 0) {
            return new Vec3d[]{baseDir};
        }
        float baseYaw = (float)(MathHelper.atan2((double)baseDir.z, (double)baseDir.x) * 57.29577951308232) - 90.0f;
        float basePitch = (float)(-(MathHelper.atan2((double)baseDir.y, (double)MathHelper.sqrt((float)((float)(baseDir.x * baseDir.x + baseDir.z * baseDir.z)))) * 57.29577951308232));
        return new Vec3d[]{this.getDirectionFromYawPitch(baseYaw - 10.0f, basePitch), baseDir, this.getDirectionFromYawPitch(baseYaw + 10.0f, basePitch)};
    }

    private Vec3d getDirectionFromYawPitch(float yawDeg, float pitchDeg) {
        float yaw = yawDeg * ((float)Math.PI / 180);
        float pitch = pitchDeg * ((float)Math.PI / 180);
        float x2 = MathHelper.sin((float)(-yaw - (float)Math.PI)) * -MathHelper.cos((float)(-pitch));
        float y2 = MathHelper.sin((float)(-pitch));
        float z2 = MathHelper.cos((float)(-yaw - (float)Math.PI)) * -MathHelper.cos((float)(-pitch));
        return new Vec3d((double)x2, (double)y2, (double)z2).normalize();
    }

    private PredictionResult predict(PlayerEntity player, ProjectileParams params, Vec3d startPos, Vec3d direction, boolean inHand) {
        Vec3d pos = startPos;
        Vec3d motion = direction.normalize().multiply(params.velocity);
        ArrayList<Vec3d> points = new ArrayList<Vec3d>();
        points.add(pos);
        Entity entityHit = null;
        int ticks = 0;
        for (int i2 = 0; i2 < 150; ++i2) {
            BlockHitResult blockHit;
            Vec3d prev = pos;
            Vec3d next = pos.add(motion);
            ticks = i2;
            if (entityHit == null) {
                entityHit = this.checkEntityCollision(player, next);
            }
            if ((blockHit = Trajectories.mc.world.raycast(new RaycastContext(prev, next, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, (Entity)player))).getType() == HitResult.Type.BLOCK) {
                points.add(blockHit.getPos());
                return new PredictionResult(points, blockHit, blockHit.getPos(), entityHit, ticks);
            }
            points.add(next);
            pos = next;
            boolean inWater = Trajectories.mc.world.getBlockState(BlockPos.ofFloored((Position)pos)).isOf(Blocks.WATER);
            double drag = inWater ? 0.8 : params.drag;
            motion = motion.multiply(drag).subtract(0.0, params.gravity, 0.0);
            if (pos.y <= (double)Trajectories.mc.world.getBottomY()) break;
        }
        return new PredictionResult(points, null, (Vec3d)points.get(points.size() - 1), entityHit, ticks);
    }

    private Entity checkEntityCollision(PlayerEntity player, Vec3d pos) {
        Box search = new Box(pos, pos).expand(1.0);
        Entity closest = null;
        double closestDistance = Double.MAX_VALUE;
        for (Entity entity2 : Trajectories.mc.world.getOtherEntities((Entity)player, search, entity -> entity != null && entity.isAlive() && entity.canHit())) {
            double distance = entity2.squaredDistanceTo(pos);
            if (!(distance < closestDistance)) continue;
            closestDistance = distance;
            closest = entity2;
        }
        return closest;
    }

    private record ProjectileParams(double velocity, double gravity, double drag) {
    }

    private record PredictionResult(List<Vec3d> points, BlockHitResult blockHit, Vec3d hitPos, Entity entityHit, int ticks) {
    }

    private record Predicted(Entity entity, List<Vec3d> vectors, int ticks, Entity collidedEntity) {
    }
}

