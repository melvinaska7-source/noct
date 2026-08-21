package zov.alphadlc.module.list.render;

import com.google.common.eventbus.Subscribe;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.Vector2f;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.consume.UseAction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import zov.alphadlc.event.list.EventEntitySpawn;
import zov.alphadlc.event.list.EventHUD;
import zov.alphadlc.module.Module;
import zov.alphadlc.module.ModuleCategory;
import zov.alphadlc.module.ModuleInformation;
import zov.alphadlc.module.settings.BooleanSetting;
import zov.alphadlc.module.settings.SliderSetting;
import zov.alphadlc.util.render.math.ProjectionUtil;
import zov.alphadlc.util.render.providers.ColorProvider;
import zov.alphadlc.util.render.renderers.DrawUtil;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@ModuleInformation(moduleName = "UseIndicator", moduleDesc = "Иконка используемого предмета и вспышки фейерверков", moduleCategory = ModuleCategory.RENDER)
public class UseIndicator extends Module {

    private static final long FIREWORK_LIFETIME_MS = 1000L;
    private static final int MAX_FIREWORKS = 64;

    private final BooleanSetting players = new BooleanSetting("У игроков", true);
    private final BooleanSetting fireworks = new BooleanSetting("Фейерверки", true);
    private final SliderSetting scale = new SliderSetting("Масштаб", 1.0, 0.3, 2.0, 0.05);

    private final List<Spawn> fireworkSpawns = new ArrayList<>();

    private record Spawn(Vec3d pos, ItemStack stack, long expireAt) {}

    @Subscribe
    private void onEntitySpawn(EventEntitySpawn e) {
        if (!fireworks.getValue()) return;
        if (!(e.getEntity() instanceof FireworkRocketEntity rocket)) return;

        // Точка спавна ракеты в мире (не привязана к игроку).
        ItemStack icon = new ItemStack(Items.FIREWORK_ROCKET);
        synchronized (fireworkSpawns) {
            fireworkSpawns.add(new Spawn(rocket.getPos(), icon, System.currentTimeMillis() + FIREWORK_LIFETIME_MS));
            while (fireworkSpawns.size() > MAX_FIREWORKS) fireworkSpawns.remove(0);
        }
    }

    @Subscribe
    private void onHud(EventHUD e) {
        if (mc.world == null || mc.player == null) return;
        DrawContext ctx = e.getDrawContext();
        float tickDelta = mc.getRenderTickCounter().getTickDelta(true);
        float sc = scale.getFloatValue();

        if (players.getValue()) {
            for (PlayerEntity player : mc.world.getPlayers()) {
                if (!shouldRender(player)) continue;
                ItemStack stack = resolveConsumable(player);
                if (stack.isEmpty()) continue;

                Vec3d pos = player.getLerpedPos(tickDelta).add(0, player.getHeight() / 2.0, 0); // Посередине тела вместо над головой
                Vector2f screen = ProjectionUtil.project(pos, tickDelta);
                if (screen.getX() == Float.MAX_VALUE) continue;
                drawIcon(ctx, stack, screen.getX(), screen.getY(), sc);
            }
        }

        if (fireworks.getValue()) {
            long now = System.currentTimeMillis();
            synchronized (fireworkSpawns) {
                Iterator<Spawn> it = fireworkSpawns.iterator();
                while (it.hasNext()) {
                    Spawn spawn = it.next();
                    if (now >= spawn.expireAt()) {
                        it.remove();
                        continue;
                    }
                    Vector2f screen = ProjectionUtil.project(spawn.pos(), tickDelta);
                    if (screen.getX() == Float.MAX_VALUE) continue;
                    drawIcon(ctx, spawn.stack(), screen.getX(), screen.getY(), sc);
                }
            }
        }
    }

    private boolean shouldRender(LivingEntity entity) {
        if (!entity.isAlive() || !entity.isUsingItem()) return false;
        if (entity == mc.player && mc.options.getPerspective().isFirstPerson()) return false;
        if (entity.isInvisible() && entity != mc.player) return false;
        return !resolveConsumable(entity).isEmpty();
    }

    private ItemStack resolveConsumable(LivingEntity entity) {
        if (!entity.isUsingItem()) return ItemStack.EMPTY;

        ItemStack active = entity.getActiveItem();
        if (isConsumable(active)) return active;

        Hand hand = entity.getActiveHand();
        if (hand != null) {
            ItemStack handStack = entity.getStackInHand(hand);
            if (isConsumable(handStack)) return handStack;
        }
        return ItemStack.EMPTY;
    }

    private boolean isConsumable(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        UseAction action = stack.getUseAction();
        return action == UseAction.EAT || action == UseAction.DRINK;
    }

    private void drawIcon(DrawContext ctx, ItemStack stack, float x, float y, float sc) {
        float radius = 9f * sc;
        DrawUtil.drawRound(x - radius, y - radius, radius * 2f, radius * 2f, radius, ColorProvider.rgba(16, 16, 20, 205));

        float itemSize = 16f * sc;
        ctx.getMatrices().push();
        ctx.getMatrices().translate(x - itemSize / 2f, y - itemSize / 2f, 300);
        ctx.getMatrices().scale(sc, sc, 1);
        ctx.drawItem(stack, 0, 0);
        ctx.getMatrices().pop();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        synchronized (fireworkSpawns) {
            fireworkSpawns.clear();
        }
    }
}
