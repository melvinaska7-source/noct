package polar.ru.client.modules.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.Locale;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.joml.Matrix4f;
import polar.ru.api.events.EventLink;
import polar.ru.api.events.implement.Event3DRender;
import polar.ru.client.modules.Module;
import polar.ru.client.modules.settings.implement.BooleanSetting;
import polar.ru.client.modules.settings.implement.ListSetting;
import polar.ru.polar;

public class ItemRadius
extends Module {
    private static final float TRANSITION_DURATION = 0.5f;
    public static ItemRadius INSTANCE = new ItemRadius();
    private final ListSetting items = new ListSetting("Предметы", new BooleanSetting("Дезка", true), new BooleanSetting("Явка", true), new BooleanSetting("Огненый Заряд", true), new BooleanSetting("Божья Аура", true), new BooleanSetting("Трапка", true), new BooleanSetting("Пласт", true));
    private int currentOutlineColor = -1;
    private int targetOutlineColor = -1;
    private float transitionTimer;
    private boolean lastPlayersInRadius;
    private Vec3d plastSmoothedCenter = Vec3d.ZERO;
    private float plastSmoothedYawDeg;
    private float plastSmoothedPitchDeg;
    private boolean plastHasSmoothedPose;

    public ItemRadius() {
        super("ItemRadius", "Показывает радиус действия предметов в руке", Module.ModuleCategory.RENDER);
        this.addSettings(this.items);
    }

    @Override
    public void onDisable() {
        this.plastHasSmoothedPose = false;
        this.transitionTimer = 0.0f;
        this.lastPlayersInRadius = false;
        this.currentOutlineColor = -1;
        this.targetOutlineColor = -1;
        super.onDisable();
    }

    @EventLink
    public void onRender3D(Event3DRender event) {
        ItemStack stack;
        if (!this.isEnable() || ItemRadius.mc.player == null || ItemRadius.mc.world == null) {
            return;
        }
        ItemStack main = ItemRadius.mc.player.getMainHandStack();
        ItemStack off = ItemRadius.mc.player.getOffHandStack();
        float tickDelta = event.getTickDelta();
        boolean handled = false;
        if (this.items.is("Дезка") && (ItemRadius.isHolding(main, Items.ENDER_EYE) || ItemRadius.isHolding(off, Items.ENDER_EYE))) {
            ItemStack var_1799_2 = stack = ItemRadius.isHolding(main, Items.ENDER_EYE) ? main : off;
            if (ItemRadius.matchesServerItem(stack, "дезор")) {
                handled = true;
                this.renderCircleItem(event, tickDelta, 10.0f, true);
            }
        }
        if (!handled && this.items.is("Явка") && (ItemRadius.isHolding(main, Items.SUGAR) || ItemRadius.isHolding(off, Items.SUGAR))) {
            ItemStack var_1799_3 = stack = ItemRadius.isHolding(main, Items.SUGAR) ? main : off;
            if (ItemRadius.matchesServerItem(stack, "явн", "пыл")) {
                handled = true;
                this.renderCircleItem(event, tickDelta, 10.0f, true);
            }
        }
        if (!handled && this.items.is("Огненый Заряд") && (ItemRadius.isHolding(main, Items.FIRE_CHARGE) || ItemRadius.isHolding(off, Items.FIRE_CHARGE))) {
            ItemStack var_1799_4 = stack = ItemRadius.isHolding(main, Items.FIRE_CHARGE) ? main : off;
            if (ItemRadius.matchesServerItem(stack, "взрыв", "штучк", "заряд")) {
                handled = true;
                this.renderCircleItem(event, tickDelta, 10.0f, true);
            }
        }
        if (!handled && this.items.is("Божья Аура") && (ItemRadius.isHolding(main, Items.PHANTOM_MEMBRANE) || ItemRadius.isHolding(off, Items.PHANTOM_MEMBRANE))) {
            ItemStack var_1799_5 = stack = ItemRadius.isHolding(main, Items.PHANTOM_MEMBRANE) ? main : off;
            if (ItemRadius.matchesServerItem(stack, "бож", "аур")) {
                handled = true;
                this.renderAuraItem(event, tickDelta);
            }
        }
        if (!handled && this.items.is("Трапка") && (ItemRadius.isHolding(main, Items.NETHERITE_SCRAP) || ItemRadius.isHolding(off, Items.NETHERITE_SCRAP))) {
            ItemStack var_1799_6 = stack = ItemRadius.isHolding(main, Items.NETHERITE_SCRAP) ? main : off;
            if (ItemRadius.matchesServerItem(stack, "трап")) {
                handled = true;
                Vec3d playerPos = this.getLerpedPlayerPos(tickDelta);
                Vec3d cubeCenter = new Vec3d(playerPos.x, playerPos.y + 0.5 + 1.625, playerPos.z);
                boolean playersInRadius = this.checkPlayersInRadius((PlayerEntity)ItemRadius.mc.player, cubeCenter, 2.5);
                this.updateOutlineColor(playersInRadius, tickDelta);
                this.renderCubeOutline(event, cubeCenter, 4.0f, this.currentOutlineColor);
            }
        }
        if (!handled && this.items.is("Пласт") && (ItemRadius.isHolding(main, Items.DRIED_KELP) || ItemRadius.isHolding(off, Items.DRIED_KELP))) {
            ItemStack var_1799_7 = stack = ItemRadius.isHolding(main, Items.DRIED_KELP) ? main : off;
            if (ItemRadius.matchesServerItem(stack, "пласт")) {
                handled = true;
                PlanePose pose = this.smoothPlastPose(this.computePlanePose(tickDelta), tickDelta);
                boolean playersInRadius = this.checkPlayersInRadius((PlayerEntity)ItemRadius.mc.player, pose.center, 2.5);
                if (playersInRadius) {
                    this.renderPlaneFill(event, pose, 0x66FF0000);
                    this.renderPlaneOutline(event, pose, -65536);
                } else {
                    this.renderPlaneOutline(event, pose, -16711936);
                }
            }
        }
        if (!handled) {
            this.plastHasSmoothedPose = false;
        }
    }

    private void renderCircleItem(Event3DRender event, float tickDelta, float radius, boolean fillOnPlayers) {
        Vec3d center = this.getCircleCenter(tickDelta);
        boolean playersInRadius = this.checkPlayersInRadius((PlayerEntity)ItemRadius.mc.player, center, radius);
        this.updateOutlineColor(playersInRadius, tickDelta);
        if (fillOnPlayers && playersInRadius) {
            this.renderCircleFill(event, center, radius, 0x3300FF00);
        }
        this.renderCircleOutline(event, center, radius, this.currentOutlineColor);
    }

    private void renderAuraItem(Event3DRender event, float tickDelta) {
        float radius = 2.0f;
        Vec3d center = this.getCircleCenter(tickDelta);
        boolean playersInRadius = this.checkPlayersInRadius((PlayerEntity)ItemRadius.mc.player, center, radius);
        boolean friendsInRadius = this.checkFriendsInRadius((PlayerEntity)ItemRadius.mc.player, center, radius);
        this.updateOutlineColor(playersInRadius, tickDelta);
        if (friendsInRadius) {
            this.renderCircleFill(event, center, radius, 0x3300FF00);
        }
        this.renderCircleOutline(event, center, radius, this.currentOutlineColor);
    }

    private void renderCircleFill(Event3DRender event, Vec3d center, float radius, int fillColor) {
        MatrixStack matrices = event.getMatrices();
        Vec3d camera = event.getCamera().getPos();
        float y2 = (float)(center.y + (double)ItemRadius.mc.player.getHeight());
        matrices.push();
        matrices.translate(center.x - camera.x, (double)y2 - camera.y, center.z - camera.z);
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        int r2 = fillColor >> 16 & 0xFF;
        int g2 = fillColor >> 8 & 0xFF;
        int b2 = fillColor & 0xFF;
        int a2 = fillColor >> 24 & 0xFF;
        this.beginFillRender();
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
        int stepDeg = 5;
        float prevX = 0.0f;
        float prevZ = 0.0f;
        boolean hasPrev = false;
        for (int deg = 0; deg <= 360; deg += stepDeg) {
            double rad = Math.toRadians(deg);
            float x2 = MathHelper.sin((float)((float)rad)) * radius;
            float z2 = -MathHelper.cos((float)((float)rad)) * radius;
            if (!hasPrev) {
                prevX = x2;
                prevZ = z2;
                hasPrev = true;
                continue;
            }
            buffer.vertex(matrix, 0.0f, 0.0f, 0.0f).color(r2, g2, b2, a2);
            buffer.vertex(matrix, prevX, 0.0f, prevZ).color(r2, g2, b2, a2);
            buffer.vertex(matrix, x2, 0.0f, z2).color(r2, g2, b2, a2);
            prevX = x2;
            prevZ = z2;
        }
        BufferRenderer.drawWithGlobalProgram((BuiltBuffer)buffer.end());
        this.endFillRender();
        matrices.pop();
    }

    private void renderCircleOutline(Event3DRender event, Vec3d center, float radius, int outlineColor) {
        MatrixStack matrices = event.getMatrices();
        Vec3d camera = event.getCamera().getPos();
        float y2 = (float)(center.y + (double)ItemRadius.mc.player.getHeight());
        matrices.push();
        matrices.translate(center.x - camera.x, (double)y2 - camera.y, center.z - camera.z);
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        int r2 = outlineColor >> 16 & 0xFF;
        int g2 = outlineColor >> 8 & 0xFF;
        int b2 = outlineColor & 0xFF;
        int a2 = outlineColor >> 24 & 0xFF;
        this.beginLineRender();
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        int stepDeg = 5;
        float firstX = 0.0f;
        float firstZ = 0.0f;
        float prevX = 0.0f;
        float prevZ = 0.0f;
        boolean hasPrev = false;
        for (int deg = 0; deg <= 360; deg += stepDeg) {
            double rad = Math.toRadians(deg);
            float x2 = MathHelper.sin((float)((float)rad)) * radius;
            float z2 = -MathHelper.cos((float)((float)rad)) * radius;
            if (!hasPrev) {
                firstX = x2;
                firstZ = z2;
                prevX = x2;
                prevZ = z2;
                hasPrev = true;
                continue;
            }
            buffer.vertex(matrix, prevX, 0.0f, prevZ).color(r2, g2, b2, a2);
            buffer.vertex(matrix, x2, 0.0f, z2).color(r2, g2, b2, a2);
            prevX = x2;
            prevZ = z2;
        }
        if (hasPrev) {
            buffer.vertex(matrix, prevX, 0.0f, prevZ).color(r2, g2, b2, a2);
            buffer.vertex(matrix, firstX, 0.0f, firstZ).color(r2, g2, b2, a2);
        }
        BufferRenderer.drawWithGlobalProgram((BuiltBuffer)buffer.end());
        this.endLineRender();
        matrices.pop();
    }

    private void renderCubeOutline(Event3DRender event, Vec3d center, float size, int outlineColor) {
        MatrixStack matrices = event.getMatrices();
        Vec3d camera = event.getCamera().getPos();
        matrices.push();
        matrices.translate(center.x - camera.x, center.y - camera.y, center.z - camera.z);
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        float half = size / 2.0f;
        this.beginLineRender();
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        ItemRadius.drawBoxEdges(buffer, matrix, -half, -half, -half, half, half, half, outlineColor);
        BufferRenderer.drawWithGlobalProgram((BuiltBuffer)buffer.end());
        this.endLineRender();
        matrices.pop();
    }

    private void renderPlaneFill(Event3DRender event, PlanePose pose, int fillColor) {
        MatrixStack matrices = event.getMatrices();
        Vec3d camera = event.getCamera().getPos();
        matrices.push();
        matrices.translate(pose.center.x - camera.x, pose.center.y - camera.y, pose.center.z - camera.z);
        this.applyPlaneRotation(matrices, pose);
        float halfW = 2.0f;
        float halfH = 2.0f;
        float halfT = 0.75f;
        this.beginFillRender();
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        ItemRadius.drawBoxFaces(buffer, matrices.peek().getPositionMatrix(), -halfW, -halfH, -halfT, halfW, halfH, halfT, fillColor);
        BufferRenderer.drawWithGlobalProgram((BuiltBuffer)buffer.end());
        this.endFillRender();
        matrices.pop();
    }

    private void renderPlaneOutline(Event3DRender event, PlanePose pose, int outlineColor) {
        MatrixStack matrices = event.getMatrices();
        Vec3d camera = event.getCamera().getPos();
        matrices.push();
        matrices.translate(pose.center.x - camera.x, pose.center.y - camera.y, pose.center.z - camera.z);
        this.applyPlaneRotation(matrices, pose);
        float halfW = 2.0f;
        float halfH = 2.0f;
        float halfT = 0.75f;
        this.beginLineRender();
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        ItemRadius.drawBoxEdges(buffer, matrices.peek().getPositionMatrix(), -halfW, -halfH, -halfT, halfW, halfH, halfT, outlineColor);
        BufferRenderer.drawWithGlobalProgram((BuiltBuffer)buffer.end());
        this.endLineRender();
        matrices.pop();
    }

    private void applyPlaneRotation(MatrixStack matrices, PlanePose pose) {
        if (Math.abs(pose.pitchDeg) > 0.001f) {
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pose.pitchDeg));
        }
        if (Math.abs(pose.yawDeg) > 0.001f) {
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(pose.yawDeg));
        }
    }

    private PlanePose smoothPlastPose(PlanePose target, float tickDelta) {
        if (!this.plastHasSmoothedPose) {
            this.plastSmoothedCenter = target.center;
            this.plastSmoothedYawDeg = target.yawDeg;
            this.plastSmoothedPitchDeg = target.pitchDeg;
            this.plastHasSmoothedPose = true;
            return target;
        }
        float speed = 12.0f;
        float t2 = 1.0f - (float)Math.exp(-speed * Math.max(0.0f, tickDelta));
        this.plastSmoothedCenter = this.plastSmoothedCenter.lerp(target.center, (double)t2);
        this.plastSmoothedYawDeg = ItemRadius.lerpAngleDeg(this.plastSmoothedYawDeg, target.yawDeg, t2);
        this.plastSmoothedPitchDeg = ItemRadius.lerpAngleDeg(this.plastSmoothedPitchDeg, target.pitchDeg, t2);
        return new PlanePose(this.plastSmoothedCenter, this.plastSmoothedYawDeg, this.plastSmoothedPitchDeg);
    }

    private PlanePose computePlanePose(float tickDelta) {
        float pitchDeg;
        float yawDeg;
        Vec3d center;
        Vec3d playerPos = this.getLerpedPlayerPos(tickDelta);
        Vec3d start = playerPos.add(0.0, (double)ItemRadius.mc.player.getEyeHeight(ItemRadius.mc.player.getPose()), 0.0);
        Vec3d lookVec = ItemRadius.mc.player.getRotationVec(tickDelta);
        Vec3d end = start.add(lookVec.multiply(4.0));
        BlockHitResult hit = ItemRadius.mc.world.raycast(new RaycastContext(start, end, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, (Entity)ItemRadius.mc.player));
        float pitch = ItemRadius.mc.player.getPitch(tickDelta);
        boolean isLookingDown = pitch > 45.0f;
        boolean isLookingUp = pitch < -45.0f;
        boolean isLookingHorizontal = !isLookingDown && !isLookingUp;
        float thickness = 1.5f;
        float halfThickness = thickness / 2.0f;
        if (hit.getType() == HitResult.Type.BLOCK && hit.getPos().distanceTo(start) <= 4.0) {
            Vec3d hitPos = hit.getPos();
            Direction face = hit.getSide();
            if (isLookingDown) {
                center = new Vec3d(Math.floor(hitPos.x) + 0.5, Math.floor(hitPos.y + 1.0) - 1.8 + (double)halfThickness, Math.floor(hitPos.z) + 0.5);
                yawDeg = 0.0f;
                pitchDeg = 90.0f;
            } else if (isLookingUp) {
                center = new Vec3d(Math.floor(hitPos.x) + 0.5, Math.floor(hitPos.y) - (double)halfThickness + 1.6, Math.floor(hitPos.z) + 0.5);
                yawDeg = 0.0f;
                pitchDeg = -90.0f;
            } else {
                double offsetX = face.getOffsetX() != 0 ? (double)((float)face.getOffsetX() * halfThickness) : 0.0;
                double offsetZ = face.getOffsetZ() != 0 ? (double)((float)face.getOffsetZ() * halfThickness) : 0.0;
                center = new Vec3d(Math.floor(hitPos.x) + 0.5 + offsetX, Math.floor(hitPos.y) + 0.5 + 1.6, Math.floor(hitPos.z) + 0.5 + offsetZ);
                yawDeg = switch (face) {
                    case Direction.NORTH -> 180.0f;
                    case Direction.SOUTH -> 0.0f;
                    case Direction.WEST -> 90.0f;
                    case Direction.EAST -> -90.0f;
                    default -> -ItemRadius.mc.player.getYaw(tickDelta);
                };
                pitchDeg = 0.0f;
            }
        } else {
            Vec3d approx = start.add(lookVec.multiply(4.0));
            double y2 = Math.floor(approx.y) + (isLookingDown ? -1.8 + (double)halfThickness : (isLookingUp ? (double)(-halfThickness) + 1.6 : 2.1));
            center = new Vec3d(Math.floor(approx.x) + 0.5, y2, Math.floor(approx.z) + 0.5);
            yawDeg = -ItemRadius.mc.player.getYaw(tickDelta);
            pitchDeg = 0.0f;
        }
        if (!isLookingHorizontal) {
            yawDeg = -ItemRadius.mc.player.getYaw(tickDelta);
        }
        return new PlanePose(center, yawDeg, pitchDeg);
    }

    private void updateOutlineColor(boolean playersInRadius, float tickDelta) {
        if (playersInRadius != this.lastPlayersInRadius) {
            this.transitionTimer = 0.0f;
            this.lastPlayersInRadius = playersInRadius;
        }
        int baseOutline = -1;
        int lightOutline = -16711936;
        this.targetOutlineColor = playersInRadius ? lightOutline : baseOutline;
        float step = tickDelta / 0.5f;
        this.transitionTimer = Math.min(this.transitionTimer + step, 1.0f);
        this.currentOutlineColor = ItemRadius.lerpColor(this.currentOutlineColor, this.targetOutlineColor, this.transitionTimer);
    }

    private static int lerpColor(int startColor, int endColor, float t2) {
        int startA = startColor >> 24 & 0xFF;
        int startR = startColor >> 16 & 0xFF;
        int startG = startColor >> 8 & 0xFF;
        int startB = startColor & 0xFF;
        int endA = endColor >> 24 & 0xFF;
        int endR = endColor >> 16 & 0xFF;
        int endG = endColor >> 8 & 0xFF;
        int endB = endColor & 0xFF;
        int a2 = (int)((float)startA + (float)(endA - startA) * t2);
        int r2 = (int)((float)startR + (float)(endR - startR) * t2);
        int g2 = (int)((float)startG + (float)(endG - startG) * t2);
        int b2 = (int)((float)startB + (float)(endB - startB) * t2);
        return a2 << 24 | r2 << 16 | g2 << 8 | b2;
    }

    private static float lerpAngleDeg(float from, float to, float t2) {
        float delta = MathHelper.wrapDegrees((float)(to - from));
        return from + delta * t2;
    }

    private static boolean isHolding(ItemStack stack, Item item) {
        return stack != null && !stack.isEmpty() && stack.isOf(item);
    }

    private static boolean matchesServerItem(ItemStack stack, String ... keywords) {
        if (stack == null || stack.isEmpty() || keywords == null || keywords.length == 0) {
            return false;
        }
        String searchable = ItemRadius.collectItemText(stack);
        for (String keyword : keywords) {
            if (keyword == null || keyword.isEmpty() || !searchable.contains(keyword.toLowerCase(Locale.ROOT))) continue;
            return true;
        }
        return false;
    }

    private static String collectItemText(ItemStack stack) {
        StringBuilder builder = new StringBuilder(stack.getName().getString().toLowerCase(Locale.ROOT));
        LoreComponent lore = (LoreComponent)stack.get(DataComponentTypes.LORE);
        if (lore != null) {
            for (Text line : lore.lines()) {
                builder.append(' ').append(line.getString().toLowerCase(Locale.ROOT));
            }
        }
        return builder.toString();
    }

    private boolean checkPlayersInRadius(PlayerEntity self, Vec3d centerPos, double radius) {
        if (ItemRadius.mc.world == null) {
            return false;
        }
        double r2 = radius * radius;
        for (PlayerEntity player : ItemRadius.mc.world.getPlayers()) {
            if (player == null || player == self || !(player.squaredDistanceTo(centerPos) <= r2)) continue;
            return true;
        }
        return false;
    }

    private boolean checkFriendsInRadius(PlayerEntity self, Vec3d centerPos, double radius) {
        if (ItemRadius.mc.world == null || polar.INSTANCE == null || polar.INSTANCE.friendStorage == null) {
            return false;
        }
        double r2 = radius * radius;
        for (PlayerEntity player : ItemRadius.mc.world.getPlayers()) {
            String name;
            if (player == null || player == self || player.squaredDistanceTo(centerPos) > r2 || (name = player.getName().getString()) == null || !polar.INSTANCE.friendStorage.isFriend(name)) continue;
            return true;
        }
        return false;
    }

    private Vec3d getLerpedPlayerPos(float tickDelta) {
        if (ItemRadius.mc.player == null) {
            return Vec3d.ZERO;
        }
        double x2 = MathHelper.lerp((double)tickDelta, (double)ItemRadius.mc.player.lastRenderX, (double)ItemRadius.mc.player.getX());
        double y2 = MathHelper.lerp((double)tickDelta, (double)ItemRadius.mc.player.lastRenderY, (double)ItemRadius.mc.player.getY());
        double z2 = MathHelper.lerp((double)tickDelta, (double)ItemRadius.mc.player.lastRenderZ, (double)ItemRadius.mc.player.getZ());
        return new Vec3d(x2, y2, z2);
    }

    private Vec3d getCircleCenter(float tickDelta) {
        return this.getLerpedPlayerPos(tickDelta).add(0.0, -1.4, 0.0);
    }

    private static void drawBoxFaces(BufferBuilder buffer, Matrix4f matrix, float minX, float minY, float minZ, float maxX, float maxY, float maxZ, int color) {
        int r2 = color >> 16 & 0xFF;
        int g2 = color >> 8 & 0xFF;
        int b2 = color & 0xFF;
        int a2 = color >> 24 & 0xFF;
        buffer.vertex(matrix, minX, minY, maxZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, maxX, minY, maxZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, minX, maxY, maxZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, maxX, minY, minZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, minX, minY, minZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, minX, maxY, minZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, maxX, maxY, minZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, minX, maxY, minZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, minX, maxY, maxZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, maxX, maxY, minZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, minX, minY, maxZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, minX, minY, minZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, maxX, minY, minZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, maxX, minY, maxZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, minX, minY, minZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, minX, minY, maxZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, minX, maxY, maxZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, minX, maxY, minZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, maxX, minY, maxZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, maxX, minY, minZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, maxX, maxY, minZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r2, g2, b2, a2);
    }

    private static void drawBoxEdges(BufferBuilder buffer, Matrix4f matrix, float minX, float minY, float minZ, float maxX, float maxY, float maxZ, int color) {
        ItemRadius.drawLine(buffer, matrix, minX, minY, minZ, maxX, minY, minZ, color);
        ItemRadius.drawLine(buffer, matrix, maxX, minY, minZ, maxX, minY, maxZ, color);
        ItemRadius.drawLine(buffer, matrix, maxX, minY, maxZ, minX, minY, maxZ, color);
        ItemRadius.drawLine(buffer, matrix, minX, minY, maxZ, minX, minY, minZ, color);
        ItemRadius.drawLine(buffer, matrix, minX, maxY, minZ, maxX, maxY, minZ, color);
        ItemRadius.drawLine(buffer, matrix, maxX, maxY, minZ, maxX, maxY, maxZ, color);
        ItemRadius.drawLine(buffer, matrix, maxX, maxY, maxZ, minX, maxY, maxZ, color);
        ItemRadius.drawLine(buffer, matrix, minX, maxY, maxZ, minX, maxY, minZ, color);
        ItemRadius.drawLine(buffer, matrix, minX, minY, minZ, minX, maxY, minZ, color);
        ItemRadius.drawLine(buffer, matrix, maxX, minY, minZ, maxX, maxY, minZ, color);
        ItemRadius.drawLine(buffer, matrix, maxX, minY, maxZ, maxX, maxY, maxZ, color);
        ItemRadius.drawLine(buffer, matrix, minX, minY, maxZ, minX, maxY, maxZ, color);
    }

    private static void drawLine(BufferBuilder buffer, Matrix4f matrix, float x1, float y1, float z1, float x2, float y2, float z2, int color) {
        int r2 = color >> 16 & 0xFF;
        int g2 = color >> 8 & 0xFF;
        int b2 = color & 0xFF;
        int a2 = color >> 24 & 0xFF;
        buffer.vertex(matrix, x1, y1, z1).color(r2, g2, b2, a2);
        buffer.vertex(matrix, x2, y2, z2).color(r2, g2, b2, a2);
    }

    private void beginLineRender() {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask((boolean)false);
        RenderSystem.setShader((ShaderProgramKey)ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.lineWidth((float)2.0f);
    }

    private void endLineRender() {
        RenderSystem.lineWidth((float)1.0f);
        RenderSystem.depthMask((boolean)true);
        RenderSystem.enableCull();
    }

    private void beginFillRender() {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask((boolean)false);
        RenderSystem.setShader((ShaderProgramKey)ShaderProgramKeys.POSITION_COLOR);
    }

    private void endFillRender() {
        RenderSystem.depthMask((boolean)true);
        RenderSystem.enableCull();
    }

    private record PlanePose(Vec3d center, float yawDeg, float pitchDeg) {
    }
}

