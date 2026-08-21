package zov.alphadlc.module.list.render;

import com.google.common.eventbus.Subscribe;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.Vector2f;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import zov.alphadlc.event.list.EventHUD;
import zov.alphadlc.event.list.EventHandledScreen;
import zov.alphadlc.module.Module;
import zov.alphadlc.module.ModuleCategory;
import zov.alphadlc.module.ModuleInformation;
import zov.alphadlc.module.settings.BooleanSetting;
import zov.alphadlc.module.settings.SliderSetting;
import zov.alphadlc.util.render.math.ProjectionUtil;
import zov.alphadlc.util.render.providers.ColorProvider;
import zov.alphadlc.util.render.renderers.DrawUtil;

import java.util.List;

@ModuleInformation(moduleName = "ShulkerView", moduleDesc = "Показывает содержимое шалкеров и контейнеров", moduleCategory = ModuleCategory.RENDER)
public class ShulkerView extends Module {

    private final BooleanSetting showHover = new BooleanSetting("В инвентаре", true);
    private final BooleanSetting showGround = new BooleanSetting("На земле", true);
    private final SliderSetting scale = new SliderSetting("Масштаб", 0.75, 0.4, 1.5, 0.05);

    private static final int CELL = 18;
    private static final int COLS = 9;
    private static final float PAD = 3f;

    @Subscribe
    private void onHandledScreen(EventHandledScreen e) {
        if (!showHover.getValue()) return;
        Slot slot = e.getSlotHover();
        if (slot == null || !slot.hasStack()) return;

        List<ItemStack> contents = getContents(slot.getStack());
        if (contents.isEmpty()) return;

        float sc = scale.getFloatValue();
        float w = (COLS * CELL + PAD * 2) * sc;
        float h = (rows(contents.size()) * CELL + PAD * 2) * sc;

        float x = e.getMouseX() + 8;
        float y = e.getMouseY() + 8;

        if (x + w > mc.getWindow().getScaledWidth()) x = e.getMouseX() - w - 8;
        if (y + h > mc.getWindow().getScaledHeight()) y = e.getMouseY() - h - 8;

        draw(e.getDrawContext(), contents, x, y, sc);
    }

    @Subscribe
    private void onHud(EventHUD e) {
        if (!showGround.getValue() || mc.world == null) return;

        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof ItemEntity item)) continue;

            List<ItemStack> contents = getContents(item.getStack());
            if (contents.isEmpty()) continue;

            Vector2f pos = ProjectionUtil.project(entity.getPos().add(0, entity.getHeight() + 0.3, 0));
            if (pos.getX() == Float.MAX_VALUE) continue;

            float sc = scale.getFloatValue() * 0.6f;
            float w = (COLS * CELL + PAD * 2) * sc;
            draw(e.getDrawContext(), contents, pos.getX() - w / 2f, pos.getY(), sc);
        }
    }

    private List<ItemStack> getContents(ItemStack stack) {
        ContainerComponent component = stack.get(DataComponentTypes.CONTAINER);
        return component != null ? component.stream().toList() : List.of();
    }

    private int rows(int count) {
        return Math.max(1, (int) Math.ceil(count / (double) COLS));
    }

    private void draw(DrawContext ctx, List<ItemStack> stacks, float x, float y, float sc) {
        float w = (COLS * CELL + PAD * 2) * sc;
        float h = (rows(stacks.size()) * CELL + PAD * 2) * sc;

        DrawUtil.drawRound(x, y, w, h, 4f, ColorProvider.rgba(18, 18, 24, 225));

        ctx.getMatrices().push();
        ctx.getMatrices().translate(x + PAD * sc, y + PAD * sc, 400);
        ctx.getMatrices().scale(sc, sc, 1);

        int i = 0;
        for (ItemStack stack : stacks) {
            int cx = (i % COLS) * CELL;
            int cy = (i / COLS) * CELL;
            ctx.drawItem(stack, cx, cy);
            ctx.drawStackOverlay(mc.textRenderer, stack, cx, cy);
            i++;
        }

        ctx.getMatrices().pop();
    }
}
