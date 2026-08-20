package polar.ru.client.modules.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;
import java.lang.reflect.Field;
import java.util.ArrayList;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import polar.ru.api.utils.color.ColorUtils;
import polar.ru.api.utils.render.RenderUtils;
import polar.ru.api.utils.render.fonts.msdf.Font;
import polar.ru.api.utils.render.fonts.msdf.Fonts;
import polar.ru.client.modules.Module;

public class ShulkerPreview
extends Module {
    public static ShulkerPreview INSTANCE = new ShulkerPreview();
    private static final float RECT_RADIUS = 5.0f;
    private static final int SLOT_SIZE = 18;
    private static final int PADDING = 7;
    private static final int ROWS = 3;
    private static final int COLS = 9;
    private static final int TITLE_HEIGHT = 14;
    private static final int SLOT_BG_COLOR = -7631989;
    private Field guiLeftField;
    private Field guiTopField;
    private static ShulkerPreview instance;

    public ShulkerPreview() {
        super("ShulkerPreview", "Показывает содержимое шалкера при наведении + CTRL", Module.ModuleCategory.RENDER);
        this.initReflection();
        instance = this;
    }

    public static ShulkerPreview getInstance() {
        return instance;
    }

    private void initReflection() {
        try {
            for (Field field : HandledScreen.class.getDeclaredFields()) {
                if (field.getType() != Integer.TYPE) continue;
                field.setAccessible(true);
                String name = field.getName();
                if (name.equals("x") || name.equals("x") || name.contains("Left") || name.contains("guiLeft")) {
                    this.guiLeftField = field;
                    continue;
                }
                if (!name.equals("y") && !name.equals("y") && !name.contains("Top") && !name.contains("guiTop")) continue;
                this.guiTopField = field;
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    private int getGuiLeft(HandledScreen<?> screen) {
        try {
            if (this.guiLeftField != null) {
                return this.guiLeftField.getInt(screen);
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
        return (mc.getWindow().getScaledWidth() - 176) / 2;
    }

    private int getGuiTop(HandledScreen<?> screen) {
        try {
            if (this.guiTopField != null) {
                return this.guiTopField.getInt(screen);
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
        return (mc.getWindow().getScaledHeight() - 166) / 2;
    }

    public void renderFromMixin(DrawContext context, int mouseX, int mouseY) {
        boolean isCtrlPressed;
        if (!this.isEnable()) {
            return;
        }
        if (mc == null || ShulkerPreview.mc.player == null || ShulkerPreview.mc.currentScreen == null) {
            return;
        }
        Screen var_437_2 = ShulkerPreview.mc.currentScreen;
        if (!(var_437_2 instanceof HandledScreen)) {
            return;
        }
        HandledScreen handledScreen = (HandledScreen)var_437_2;
        long handle = mc.getWindow().getHandle();
        boolean bl = isCtrlPressed = GLFW.glfwGetKey((long)handle, (int)341) == 1;
        if (!isCtrlPressed) {
            return;
        }
        Slot hoveredSlot = this.getHoveredSlot(handledScreen);
        if (hoveredSlot == null) {
            return;
        }
        ItemStack stack = hoveredSlot.getStack();
        if (!this.isShulkerBox(stack)) {
            return;
        }
        ContainerComponent container = (ContainerComponent)stack.get(DataComponentTypes.CONTAINER);
        if (container == null) {
            return;
        }
        this.renderShulkerPreview(context, stack, container, mouseX, mouseY);
    }

    private Slot getHoveredSlot(HandledScreen<?> screen) {
        try {
            ScreenHandler handler = screen.getScreenHandler();
            if (handler == null || handler.slots == null) {
                return null;
            }
            double mouseX = ShulkerPreview.mc.mouse.getX() * (double)mc.getWindow().getScaledWidth() / (double)mc.getWindow().getWidth();
            double mouseY = ShulkerPreview.mc.mouse.getY() * (double)mc.getWindow().getScaledHeight() / (double)mc.getWindow().getHeight();
            int guiLeft = this.getGuiLeft(screen);
            int guiTop = this.getGuiTop(screen);
            for (Slot slot : handler.slots) {
                int slotX = guiLeft + slot.x;
                int slotY = guiTop + slot.y;
                if (!(mouseX >= (double)slotX) || !(mouseX < (double)(slotX + 16)) || !(mouseY >= (double)slotY) || !(mouseY < (double)(slotY + 16))) continue;
                return slot;
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
        return null;
    }

    private boolean isShulkerBox(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        return stack.getItem() == Items.SHULKER_BOX || stack.getItem() == Items.WHITE_SHULKER_BOX || stack.getItem() == Items.ORANGE_SHULKER_BOX || stack.getItem() == Items.MAGENTA_SHULKER_BOX || stack.getItem() == Items.LIGHT_BLUE_SHULKER_BOX || stack.getItem() == Items.YELLOW_SHULKER_BOX || stack.getItem() == Items.LIME_SHULKER_BOX || stack.getItem() == Items.PINK_SHULKER_BOX || stack.getItem() == Items.GRAY_SHULKER_BOX || stack.getItem() == Items.LIGHT_GRAY_SHULKER_BOX || stack.getItem() == Items.CYAN_SHULKER_BOX || stack.getItem() == Items.PURPLE_SHULKER_BOX || stack.getItem() == Items.BLUE_SHULKER_BOX || stack.getItem() == Items.BROWN_SHULKER_BOX || stack.getItem() == Items.GREEN_SHULKER_BOX || stack.getItem() == Items.RED_SHULKER_BOX || stack.getItem() == Items.BLACK_SHULKER_BOX;
    }

    private int getShulkerColor(ItemStack stack) {
        if (stack.getItem() == Items.SHULKER_BOX) {
            return -6394435;
        }
        if (stack.getItem() == Items.WHITE_SHULKER_BOX) {
            return -1;
        }
        if (stack.getItem() == Items.ORANGE_SHULKER_BOX) {
            return -425955;
        }
        if (stack.getItem() == Items.MAGENTA_SHULKER_BOX) {
            return -3715395;
        }
        if (stack.getItem() == Items.LIGHT_BLUE_SHULKER_BOX) {
            return -12930086;
        }
        if (stack.getItem() == Items.YELLOW_SHULKER_BOX) {
            return -75715;
        }
        if (stack.getItem() == Items.LIME_SHULKER_BOX) {
            return -8337633;
        }
        if (stack.getItem() == Items.PINK_SHULKER_BOX) {
            return -816214;
        }
        if (stack.getItem() == Items.GRAY_SHULKER_BOX) {
            return -12103854;
        }
        if (stack.getItem() == Items.LIGHT_GRAY_SHULKER_BOX) {
            return -6447721;
        }
        if (stack.getItem() == Items.CYAN_SHULKER_BOX) {
            return -15295332;
        }
        if (stack.getItem() == Items.PURPLE_SHULKER_BOX) {
            return -7785800;
        }
        if (stack.getItem() == Items.BLUE_SHULKER_BOX) {
            return -12827478;
        }
        if (stack.getItem() == Items.BROWN_SHULKER_BOX) {
            return -8170446;
        }
        if (stack.getItem() == Items.GREEN_SHULKER_BOX) {
            return -10585066;
        }
        if (stack.getItem() == Items.RED_SHULKER_BOX) {
            return -5231066;
        }
        if (stack.getItem() == Items.BLACK_SHULKER_BOX) {
            return -14869215;
        }
        return -6394435;
    }

    private void renderShulkerPreview(DrawContext context, ItemStack shulkerItem, ContainerComponent container, float mouseX, float mouseY) {
        MatrixStack matrices = context.getMatrices();
        int screenWidth = context.getScaledWindowWidth();
        int screenHeight = context.getScaledWindowHeight();
        float contentWidth = 162.0f;
        float contentHeight = 54.0f;
        float totalWidth = contentWidth + 14.0f;
        float totalHeight = contentHeight + 14.0f + 14.0f;
        float x2 = mouseX + 12.0f;
        float y2 = mouseY - 12.0f;
        if (x2 + totalWidth > (float)screenWidth) {
            x2 = mouseX - totalWidth - 4.0f;
        }
        if (y2 + totalHeight > (float)screenHeight) {
            y2 = (float)screenHeight - totalHeight - 4.0f;
        }
        if (y2 < 4.0f) {
            y2 = 4.0f;
        }
        if (x2 < 4.0f) {
            x2 = 4.0f;
        }
        int shulkerColor = this.getShulkerColor(shulkerItem);
        int bgColor = ColorUtils.applyAlpha(shulkerColor, 0.85f);
        int darkerColor = this.darkenColor(shulkerColor, 0.6f);
        int lighterColor = this.lightenColor(shulkerColor, 1.3f);
        matrices.push();
        GL11.glClear((int)256);
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        matrices.translate(0.0f, 0.0f, 500.0f);
        RenderUtils.drawBlur(matrices, x2 - 2.0f, y2 - 2.0f, totalWidth + 4.0f, totalHeight + 4.0f, 7.0f, 8.0f, -1);
        context.fill((int)x2, (int)y2, (int)(x2 + totalWidth), (int)(y2 + totalHeight), bgColor);
        context.fill((int)x2, (int)y2, (int)(x2 + totalWidth), (int)(y2 + 2.0f), lighterColor);
        context.fill((int)x2, (int)(y2 + totalHeight - 2.0f), (int)(x2 + totalWidth), (int)(y2 + totalHeight), darkerColor);
        context.fill((int)x2, (int)y2, (int)(x2 + 2.0f), (int)(y2 + totalHeight), lighterColor);
        context.fill((int)(x2 + totalWidth - 2.0f), (int)y2, (int)(x2 + totalWidth), (int)(y2 + totalHeight), darkerColor);
        Font font = Fonts.getFont("sf_regular", 12);
        if (font != null) {
            String title = shulkerItem.getName().getString();
            float titleX = x2 + 7.0f;
            float titleY = y2 + 7.0f - 1.0f;
            int textColor = this.isColorDark(shulkerColor) ? -1 : -15066598;
            font.drawString(matrices, title, titleX, titleY, textColor);
        }
        float slotsX = x2 + 7.0f;
        float slotsY = y2 + 7.0f + 14.0f - 2.0f;
        int slotAreaBg = this.darkenColor(shulkerColor, 0.5f);
        context.fill((int)(slotsX - 1.0f), (int)(slotsY - 1.0f), (int)(slotsX + contentWidth + 1.0f), (int)(slotsY + contentHeight + 1.0f), slotAreaBg);
        ArrayList items = new ArrayList();
        container.stream().forEach(items::add);
        for (int i2 = 0; i2 < 27; ++i2) {
            ItemStack itemStack;
            int row = i2 / 9;
            int col = i2 % 9;
            int slotX = (int)(slotsX + (float)(col * 18));
            int slotY = (int)(slotsY + (float)(row * 18));
            context.fill(slotX, slotY, slotX + 18 - 2, slotY + 18 - 2, -7631989);
            context.fill(slotX, slotY, slotX + 18 - 2, slotY + 1, -11184811);
            context.fill(slotX, slotY, slotX + 1, slotY + 18 - 2, -11184811);
            context.fill(slotX, slotY + 18 - 3, slotX + 18 - 2, slotY + 18 - 2, -1);
            context.fill(slotX + 18 - 3, slotY, slotX + 18 - 2, slotY + 18 - 2, -1);
            if (i2 >= items.size() || (itemStack = (ItemStack)items.get(i2)).isEmpty()) continue;
            context.drawItem(itemStack, slotX, slotY);
            context.drawStackOverlay(ShulkerPreview.mc.textRenderer, itemStack, slotX, slotY);
        }
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        matrices.pop();
    }

    private int darkenColor(int color, float factor) {
        int a2 = color >> 24 & 0xFF;
        int r2 = (int)((float)(color >> 16 & 0xFF) * factor);
        int g2 = (int)((float)(color >> 8 & 0xFF) * factor);
        int b2 = (int)((float)(color & 0xFF) * factor);
        return a2 << 24 | Math.min(255, r2) << 16 | Math.min(255, g2) << 8 | Math.min(255, b2);
    }

    private int lightenColor(int color, float factor) {
        int a2 = color >> 24 & 0xFF;
        int r2 = (int)Math.min(255.0f, (float)(color >> 16 & 0xFF) * factor);
        int g2 = (int)Math.min(255.0f, (float)(color >> 8 & 0xFF) * factor);
        int b2 = (int)Math.min(255.0f, (float)(color & 0xFF) * factor);
        return a2 << 24 | r2 << 16 | g2 << 8 | b2;
    }

    private boolean isColorDark(int color) {
        int r2 = color >> 16 & 0xFF;
        int g2 = color >> 8 & 0xFF;
        int b2 = color & 0xFF;
        double luminance = (0.299 * (double)r2 + 0.587 * (double)g2 + 0.114 * (double)b2) / 255.0;
        return luminance < 0.5;
    }
}

