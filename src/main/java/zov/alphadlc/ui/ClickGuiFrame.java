package zov.alphadlc.ui;

import com.mojang.blaze3d.systems.RenderSystem;
import lombok.Getter;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4fStack;
import org.lwjgl.glfw.GLFW;
import zov.alphadlc.module.ModuleCategory;
import zov.alphadlc.module.list.render.ClickGui;
import zov.alphadlc.module.settings.ItemModelSetting;
import zov.alphadlc.ui.component.ItemModelGalleryPopup;
import zov.alphadlc.ui.component.SearchField;
import zov.alphadlc.util.IMinecraft;
import zov.alphadlc.util.cursor.CursorManager;
import zov.alphadlc.util.render.helper.HoverUtil;
import zov.alphadlc.util.render.math.Animation;
import zov.alphadlc.util.render.math.Easing;
import zov.alphadlc.util.render.msdf.Fonts;
import zov.alphadlc.util.render.providers.ColorProvider;
import zov.alphadlc.util.render.renderers.DrawUtil;

import java.util.ArrayList;
import java.util.List;

@Getter
public class ClickGuiFrame extends Screen implements IMinecraft {

    private final List<Panel> panels = new ArrayList<>();
    private final SearchField searchField;
    private final ThemeEditor themeEditor = new ThemeEditor();
    private ItemModelGalleryPopup itemModelGallery;

    // Плавное описание модуля
    private final Animation descAnim = new Animation(Easing.QUINTIC_OUT, 220);
    private String lastDesc = null;

    // Анимация закрытия
    private boolean closing = false;

    // === АНИМАЦИЯ ОТКРЫТИЯ ВСЕГО GUI ===
    private final Animation globalOpenAnim = new Animation(Easing.BACK_OUT, 450);
    private boolean firstRender = true;

    // Курсоры
    private long handCursor, iBeamCursor, pointingCursor, arrowCursor;
    private boolean cursorsCreated = false;
    private long currentCursor = 0L;

    // Кэш поиска
    private String cachedRawQuery = null;
    private String cachedNormalizedQuery = "";

    private void ensureCursors() {
        if (cursorsCreated) return;
        handCursor = GLFW.glfwCreateStandardCursor(GLFW.GLFW_HAND_CURSOR);
        iBeamCursor = GLFW.glfwCreateStandardCursor(GLFW.GLFW_IBEAM_CURSOR);
        pointingCursor = GLFW.glfwCreateStandardCursor(GLFW.GLFW_POINTING_HAND_CURSOR);
        arrowCursor = GLFW.glfwCreateStandardCursor(GLFW.GLFW_ARROW_CURSOR);
        cursorsCreated = true;
    }

    private void applyCursor(long cursor) {
        if (cursor == currentCursor) return;
        GLFW.glfwSetCursor(mc.getWindow().getHandle(), cursor);
        currentCursor = cursor;
    }

    // === ЗАПУСК АНИМАЦИИ ОТКРЫТИЯ ===
    public void playOpenAnimation() {
        closing = false;
        firstRender = true;
        globalOpenAnim.reset(0f);
        itemModelGallery = null;

        for (int i = 0; i < panels.size(); i++) {
            Panel panel = panels.get(i);
            panel.slideAnim.reset(0f);
            panel.slideDir = 1; // +1 = снизу
        }
        themeEditor.resetAppear();
        searchField.resetAppear();
    }

    public ClickGuiFrame() {
        super(Text.of("Avalora Frame"));
        searchField = new SearchField("Search...");
        for (ModuleCategory category : ModuleCategory.values()) {
            panels.add(new Panel(category, this));
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        CursorManager.reset();
        CursorManager.resetIBeam();
        CursorManager.resetClick();

        int windowWidth = mc.getWindow().getScaledWidth();
        int windowHeight = mc.getWindow().getScaledHeight();

        // === ГЛОБАЛЬНАЯ АНИМАЦИЯ ОТКРЫТИЯ ===
        if (firstRender) {
            globalOpenAnim.run(1f);
            firstRender = false;
        } else {
            globalOpenAnim.run(closing ? 0f : 1f);
        }

        float globalProgress = (float) globalOpenAnim.getValue();

        if (closing && globalProgress < 0.02f) {
            closing = false;
            close();
            return;
        }

        float guiScale = guiScale();
        float centerX = windowWidth / 2f;
        float centerY = windowHeight / 2f;
        int mouseGx = (int) ((mouseX - centerX) / guiScale + centerX);
        int mouseGy = (int) ((mouseY - centerY) / guiScale + centerY);
        mouseX = mouseGx;
        mouseY = mouseGy;

        zov.alphadlc.util.render.math.Scissor.setGuiTransform(guiScale, centerX, centerY);

        Matrix4fStack modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        modelView.translate(centerX, centerY, 0f);
        modelView.scale(guiScale, guiScale, 1f);
        modelView.translate(-centerX, -centerY, 0f);

        float panelWidth = 120f;
        float spacing = 4f;
        float panelHeight = 270f;
        float panelTotalWidth = panels.size() * (panelWidth + spacing) - spacing;

        float startX = (windowWidth - panelTotalWidth) / 2f;
        float panelY = (windowHeight - panelHeight) / 2f;

        // === АНИМАЦИЯ ВЫЛЕТА ПАНЕЛЕЙ ===
        float offscreen = windowHeight / 2f + panelHeight;

        for (int i = 0; i < panels.size(); i++) {
            Panel panel = panels.get(i);

            // Задержка для каждой панели (stagger)
            float panelDelay = i * 0.04f;
            float panelProgress;

            if (!closing) {
                panelProgress = MathHelper.clamp((globalProgress - panelDelay) / (1f - panelDelay * panels.size()), 0f, 1f);
            } else {
                float reverseDelay = (panels.size() - 1 - i) * 0.03f;
                panelProgress = MathHelper.clamp((globalProgress - reverseDelay) / 0.7f, 0f, 1f);
            }

            // Применяем easing через Animation (которая сама использует Easing)
            // Создаём временную анимацию для easing-вычисления
            panel.slideAnim.setValue(panelProgress);

            float slide = MathHelper.clamp(panel.slideAnim.getValue(), 0f, 1f);
            float yOffset = (1f - slide) * panel.slideDir * offscreen;

            // Эффект подпрыгивания
            float bounce = 0f;
            if (!closing && slide > 0.01f && slide < 0.99f) {
                bounce = (float) Math.sin(slide * Math.PI) * 3f * (1f - slide);
            }

            panel.setX(startX + i * (panelWidth + spacing));
            panel.setY(panelY + yOffset - bounce);
            panel.setWidth(panelWidth);
            panel.setHeight(panelHeight);

            panel.render(context, mouseX, mouseY, delta);
        }

        // === ПОИСК ===
        float searchW = 90;
        float searchH = 18;
        float searchX = windowWidth / 2f - searchW / 2f;
        float searchY = panelY + panelHeight + 35;

        searchField.setBounds(searchX, searchY, searchW, searchH);
        searchField.render(context, mouseX, mouseY, delta);

        // === ОПИСАНИЕ МОДУЛЯ ===
        String hoveredDesc = null;
        for (Panel panel : panels) {
            boolean isMouseInPanel = HoverUtil.isHovered(mouseX, mouseY, panel.getX(), panel.getY(), panel.getWidth(), panel.getHeight());
            for (ModuleComponent component : panel.getModuleComponents()) {
                if (component.isHovered() && isMouseInPanel && searchField.isEmpty()) {
                    String desc = component.getModule().getDesc();
                    if (desc != null && !desc.isEmpty()) hoveredDesc = desc;
                }
            }
        }
        if (hoveredDesc != null) lastDesc = hoveredDesc;

        descAnim.run(hoveredDesc != null);
        float da = (float) descAnim.getValue();
        if (da > 0.01f && lastDesc != null) {
            float size = 7.5f;
            float padX = 8f;
            float padY = 5.5f;
            float textWidth = Fonts.SFREGULAR.get().getWidth(lastDesc, size);
            float tooltipW = textWidth + padX * 2f;
            float tooltipH = size + padY * 2f;
            float tooltipX = MathHelper.clamp(windowWidth / 2f - tooltipW / 2f, 4f, windowWidth - tooltipW - 4f);
            float tooltipY = panelY - tooltipH - 8f;

            int a = (int) (255 * da);
            float cx = tooltipX + tooltipW / 2f;
            float cy = tooltipY + tooltipH / 2f;
            float scale = 0.92f + 0.08f * da;

            context.getMatrices().push();
            context.getMatrices().translate(cx, cy, 0);
            context.getMatrices().scale(scale, scale, 1f);
            context.getMatrices().translate(-cx, -cy, 0);

            DrawUtil.drawRoundBlur(tooltipX, tooltipY, tooltipW, tooltipH, 4f, 
                ColorProvider.rgba(200, 200, 200, (int) (255 * da)), 12f);
            DrawUtil.drawRound(tooltipX, tooltipY, tooltipW, tooltipH, 4f, 
                ColorProvider.setAlpha(ColorProvider.getColorClickGui(), (int) (130 * da)));
            DrawUtil.drawRound(tooltipX - 0.5f, tooltipY - 0.5f, tooltipW + 1f, tooltipH + 1f, 4.5f, 
                ColorProvider.rgba(48, 66, 122, (int) (90 * da)));
            DrawUtil.drawText(Fonts.SFREGULAR.get(), lastDesc, tooltipX + padX, tooltipY + (tooltipH - size) / 2f + 0.2f,
                    ColorProvider.setAlpha(ColorProvider.getColorText(), a), size);

            context.getMatrices().pop();
        }

        themeEditor.render(context, mouseX, mouseY, delta);
        if (itemModelGallery != null) {
            itemModelGallery.render(context, mouseX, mouseY, delta);
        }

        modelView.popMatrix();
        zov.alphadlc.util.render.math.Scissor.resetGuiTransform();

        ensureCursors();
        long desiredCursor;
        if (CursorManager.shouldBeHand()) desiredCursor = handCursor;
        else if (CursorManager.shouldIBeam()) desiredCursor = iBeamCursor;
        else if (CursorManager.shouldClick()) desiredCursor = pointingCursor;
        else desiredCursor = arrowCursor;
        applyCursor(desiredCursor);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (itemModelGallery != null) {
            itemModelGallery.mouseClicked(mouseX, mouseY, button);
            if (!itemModelGallery.contains(mouseX, mouseY)) {
                itemModelGallery = null;
            }
            return true;
        }
        for (Panel panel : panels) {
            panel.mouseClicked(mouseX, mouseY, button);
        }
        searchField.mouseClicked(mouseX, mouseY, button);
        themeEditor.mouseClicked(mouseX, mouseY, button);
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        for (Panel panel : panels) {
            panel.mouseReleased(mouseX, mouseY, button);
        }
        searchField.mouseReleased(mouseX, mouseY, button);
        themeEditor.mouseReleased(mouseX, mouseY, button);
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        for (Panel panel : panels) {
            panel.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT || keyCode == GLFW.GLFW_KEY_LEFT_SHIFT) {
            closing = true;
            globalOpenAnim.run(0f);
            return true;
        }
        for (Panel panel : panels) {
            panel.keyPressed(keyCode, scanCode, modifiers);
        }
        searchField.keyPressed(keyCode, scanCode, modifiers);
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        searchField.charTyped(chr, modifiers);
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void close() {
        closing = true;
        globalOpenAnim.run(0f);
    }

    public void forceClose() {
        super.close();
    }

    public void openItemModelGallery(ItemModelSetting setting) {
        itemModelGallery = new ItemModelGalleryPopup(setting);
    }

    public boolean searchCheck(String moduleName) {
        String raw = searchField.getText();
        if (raw.equals(cachedRawQuery)) {
            return !cachedNormalizedQuery.isEmpty() && !moduleName.toLowerCase().contains(cachedNormalizedQuery);
        }
        cachedRawQuery = raw;
        cachedNormalizedQuery = raw.toLowerCase().replaceAll("\s+", "");
        return !cachedNormalizedQuery.isEmpty() && !moduleName.toLowerCase().replaceAll("\s+", "").contains(cachedNormalizedQuery);
    }

    private float guiScale() {
        // Получаем scale из модуля ClickGui
        // ClickGui — это Module, у него есть поле size (SliderSetting)
        // Нужно получить экземпляр модуля
        for (var module : zov.alphadlc.AlphaDLC.getInstance().getModuleStorage().getModules()) {
            if (module instanceof ClickGui clickGui) {
                return clickGui.size.getValue().floatValue();
            }
        }
        return 1.0f;
    }
}
