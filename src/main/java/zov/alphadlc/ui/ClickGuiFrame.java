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

    // Анимация закрытия (панели улетают обратно, после чего экран закрывается)
    private boolean closing = false;

    // === НОВОЕ: Глобальная анимация открытия GUI из центра ===
    // BACK_OUT даёт эффект "выстрела" — быстрый старт, небольшой перелёт за границу, возврат
    private final Animation openScaleAnim = new Animation(Easing.BACK_OUT, 420);
    private final Animation openAlphaAnim = new Animation(Easing.QUINTIC_OUT, 300);

    // Курсоры создаём один раз (лениво) и меняем только при смене состояния —
    // раньше glfwCreateStandardCursor() дёргался каждый кадр без освобождения (нативная утечка).
    private long handCursor, iBeamCursor, pointingCursor, arrowCursor;
    private boolean cursorsCreated = false;
    private long currentCursor = 0L; // последний установленный handle (0 = системный по умолчанию)

    // Кэш нормализованной строки поиска, чтобы не гонять replaceAll()/toLowerCase()
    // для каждого модуля каждый кадр.
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

    // Сброс анимации появления — панели вылетают снизу/сверху к центру
    public void playOpenAnimation() {
        closing = false;
        itemModelGallery = null;
        // === НОВОЕ: Сброс глобальной анимации масштаба ===
        openScaleAnim.reset(0f);
        openAlphaAnim.reset(0f);
        for (Panel panel : panels) {
            panel.slideAnim.reset(0f);
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

        // Масштаб GUI из настройки модуля: масштабируем всё вокруг центра экрана.
        // Все шейпы/текст/блюр рисуются через глобальный шейдер (drawWithGlobalProgram),
        // который берёт ModelViewMat из RenderSystem.getModelViewStack(), поэтому масштаб
        // применяем именно к нему — тогда масштабируется РЕАЛЬНО всё. Области отсечения
        // масштабируем отдельно через Scissor, координаты мыши — обратным преобразованием.
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

        // === НОВОЕ: Глобальная анимация открытия/закрытия GUI ===
        // При открытии: масштаб 0→1 с эффектом BACK_OUT (выстрел из центра)
        // При закрытии: масштаб 1→0 с QUINTIC_OUT (плавное исчезновение)
        openScaleAnim.run(closing ? 0f : 1f);
        openAlphaAnim.run(closing ? 0f : 1f);
        float openProgress = MathHelper.clamp((float) openScaleAnim.getValue(), 0f, 1f);
        float openAlpha = MathHelper.clamp((float) openAlphaAnim.getValue(), 0f, 1f);

        // Применяем масштаб из центра экрана
        // BACK_OUT может дать значение > 1 (перелёт), что создаёт эффект "выстрела"
        modelView.translate(centerX, centerY, 0f);
        modelView.scale(openProgress, openProgress, 1f);
        modelView.translate(-centerX, -centerY, 0f);

        float panelWidth = 120f;
        float spacing = 4f;
        float panelHeight = 270f;
        float panelTotalWidth = panels.size() * (panelWidth + spacing) - spacing;

        float startX = (windowWidth - panelTotalWidth) / 2f;
        float panelY = (windowHeight - panelHeight) / 2f;

        float offscreen = windowHeight / 2f + panelHeight;
        for (int i = 0; i < panels.size(); i++) {
            Panel panel = panels.get(i);
            // Чётные вылетают снизу, нечётные — сверху
            panel.slideDir = (i % 2 == 0) ? 1 : -1;
            panel.slideAnim.run(closing ? 0f : 1f);
            float slide = MathHelper.clamp(panel.slideAnim.getValue(), 0f, 1f);
            // Умножаем slide на openAlpha для синхронизации с глобальной анимацией
            float effectiveSlide = slide * openAlpha;
            float yOffset = (1f - effectiveSlide) * panel.slideDir * offscreen;

            panel.setX(startX + i * (panelWidth + spacing));
            panel.setY(panelY + yOffset);
            panel.setWidth(panelWidth);
            panel.setHeight(panelHeight);

            panel.render(context, mouseX, mouseY, delta);
        }

        // Когда анимация закрытия завершилась — закрываем экран
        if (closing) {
            boolean allClosed = true;
            for (Panel panel : panels) {
                if (panel.slideAnim.getValue() > 0.02f) {
                    allClosed = false;
                    break;
                }
            }
            // Также ждём завершения глобальной анимации
            if (openScaleAnim.getValue() > 0.02f) {
                allClosed = false;
            }
            if (allClosed) {
                closing = false;
                modelView.popMatrix();
                zov.alphadlc.util.render.math.Scissor.resetGuiTransform();
                close();
                return;
            }
        }

        float searchW = 90;
        float searchH = 18;
        float searchX = windowWidth / 2f - searchW / 2f;
        float searchY = panelY + panelHeight + 35;

        searchField.setBounds(searchX, searchY, searchW, searchH);
        searchField.render(context, mouseX, mouseY, delta);

        // Определяем описание модуля под курсором
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

            DrawUtil.drawRoundBlur(tooltipX, tooltipY, tooltipW, tooltipH, 4f, ColorProvider.rgba(200, 200, 200, (int) (255 * da)), 12f);
            DrawUtil.drawRound(tooltipX, tooltipY, tooltipW, tooltipH, 4f, ColorProvider.setAlpha(ColorProvider.getColorClickGui(), (int) (130 * da)));
            DrawUtil.drawRound(tooltipX - 0.5f, tooltipY - 0.5f, tooltipW + 1f, tooltipH + 1f, 4.5f, ColorProvider.rgba(48, 66, 122, (int) (90 * da)));
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
            if (itemModelGallery.mouseClicked(mouseX, mouseY, button)) return true;
            itemModelGallery = null;
            return true;
        }
        if (themeEditor.mouseClicked(mouseX, mouseY, button)) return true;

        int windowWidth = mc.getWindow().getScaledWidth();
        int windowHeight = mc.getWindow().getScaledHeight();
        float guiScale = guiScale();
        float centerX = windowWidth / 2f;
        float centerY = windowHeight / 2f;
        float openProgress = MathHelper.clamp((float) openScaleAnim.getValue(), 0.01f, 1f);
        int mouseGx = (int) ((mouseX - centerX) / (guiScale * openProgress) + centerX);
        int mouseGy = (int) ((mouseY - centerY) / (guiScale * openProgress) + centerY);
        mouseX = mouseGx;
        mouseY = mouseGy;

        searchField.mouseClicked(mouseX, mouseY, button);
        for (Panel panel : panels) {
            panel.mouseClicked(mouseX, mouseY, button);
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        int windowWidth = mc.getWindow().getScaledWidth();
        int windowHeight = mc.getWindow().getScaledHeight();
        float guiScale = guiScale();
        float centerX = windowWidth / 2f;
        float centerY = windowHeight / 2f;
        float openProgress = MathHelper.clamp((float) openScaleAnim.getValue(), 0.01f, 1f);
        int mouseGx = (int) ((mouseX - centerX) / (guiScale * openProgress) + centerX);
        int mouseGy = (int) ((mouseY - centerY) / (guiScale * openProgress) + centerY);
        mouseX = mouseGx;
        mouseY = mouseGy;

        for (Panel panel : panels) {
            panel.mouseReleased(mouseX, mouseY, button);
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int windowWidth = mc.getWindow().getScaledWidth();
        int windowHeight = mc.getWindow().getScaledHeight();
        float guiScale = guiScale();
        float centerX = windowWidth / 2f;
        float centerY = windowHeight / 2f;
        float openProgress = MathHelper.clamp((float) openScaleAnim.getValue(), 0.01f, 1f);
        int mouseGx = (int) ((mouseX - centerX) / (guiScale * openProgress) + centerX);
        int mouseGy = (int) ((mouseY - centerY) / (guiScale * openProgress) + centerY);
        mouseX = mouseGx;
        mouseY = mouseGy;

        for (Panel panel : panels) {
            panel.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (searchField.keyPressed(keyCode, scanCode, modifiers)) return true;
        for (Panel panel : panels) {
            if (panel.keyPressed(keyCode, scanCode, modifiers)) return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (searchField.charTyped(chr, modifiers)) return true;
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void close() {
        if (closing) return;
        closing = true;
        // === НОВОЕ: Запускаем анимацию закрытия ===
        openScaleAnim.animateTo(0f);
        openAlphaAnim.animateTo(0f);
        for (Panel panel : panels) {
            panel.slideAnim.animateTo(0f);
        }
    }

    public void openItemModelGallery(ItemModelSetting setting) {
        itemModelGallery = new ItemModelGalleryPopup(setting, () -> itemModelGallery = null);
    }

    public boolean searchCheck(String moduleName) {
        String raw = searchField.text;
        if (raw.isEmpty()) {
            cachedRawQuery = null;
            cachedNormalizedQuery = "";
            return false;
        }
        if (!raw.equals(cachedRawQuery)) {
            cachedRawQuery = raw;
            cachedNormalizedQuery = raw.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
        }
        String normalizedModule = moduleName.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
        return !normalizedModule.contains(cachedNormalizedQuery);
    }

    private float guiScale() {
        return zov.alphadlc.module.list.render.ClickGui.getInstance().scale.getValue().floatValue();
    }
}
