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

        float panelWidth = 120f;
        float spacing = 4f;
        float panelHeight = 270f;  // Уменьшена высота с 280f до 270f
        float panelTotalWidth = panels.size() * (panelWidth + spacing) - spacing;

        float startX = (windowWidth - panelTotalWidth) / 2f;
        float panelY = (windowHeight - panelHeight) / 2f;  // ровно по центру вертикально

        for (int i = 0; i < panels.size(); i++) {
            Panel panel = panels.get(i);
            panel.setX(startX + i * (panelWidth + spacing));
            panel.setY(panelY);
            panel.setWidth(panelWidth);
            panel.setHeight(panelHeight);

            panel.render(context, mouseX, mouseY, delta);
        }

        float searchW = 90;
        float searchH = 18;
        float searchX = windowWidth / 2f - searchW / 2f;
        float searchY = panelY + panelHeight + 35;  // увеличено с 25 (поиск ниже)

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
            // Держим окно в пределах экрана, чтобы текст не выходил за рамки
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
            // Полупрозрачный тёмно-синий фон поверх матового блюра
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
    public void removed() {
        super.removed();
        // Возвращаем системный курсор и освобождаем созданные нативные хэндлы.
        if (cursorsCreated) {
            GLFW.glfwSetCursor(mc.getWindow().getHandle(), 0L);
            GLFW.glfwDestroyCursor(handCursor);
            GLFW.glfwDestroyCursor(iBeamCursor);
            GLFW.glfwDestroyCursor(pointingCursor);
            GLFW.glfwDestroyCursor(arrowCursor);
            cursorsCreated = false;
            currentCursor = 0L;
        }
    }

    public boolean searchCheck(String text) {
        if (searchField.isEmpty()) return false;
        String raw = searchField.text;
        // Нормализуем строку поиска только когда она изменилась, а не для каждого модуля каждый кадр.
        if (!raw.equals(cachedRawQuery)) {
            cachedRawQuery = raw;
            cachedNormalizedQuery = raw.replaceAll(" ", "").toLowerCase();
        }
        return !text.replaceAll(" ", "").toLowerCase().contains(cachedNormalizedQuery);
    }

    public void openItemModelGallery(ItemModelSetting setting) {
        itemModelGallery = new ItemModelGalleryPopup(setting);
    }

    private float guiScale() {
        ClickGui module = zov.alphadlc.util.base.Instance.get(ClickGui.class);
        return module != null ? (float) module.size.getValue() : 1f;
    }

    // Перевод экранных координат мыши в масштабированное пространство GUI
    private double scaleMouseX(double mouseX) {
        float s = guiScale();
        double cx = mc.getWindow().getScaledWidth() / 2.0;
        return (mouseX - cx) / s + cx;
    }

    private double scaleMouseY(double mouseY) {
        float s = guiScale();
        double cy = mc.getWindow().getScaledHeight() / 2.0;
        return (mouseY - cy) / s + cy;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        mouseX = scaleMouseX(mouseX);
        mouseY = scaleMouseY(mouseY);
        if (itemModelGallery != null) {
            if (!itemModelGallery.contains(mouseX, mouseY)) {
                itemModelGallery = null;
            } else {
                itemModelGallery.mouseClicked(mouseX, mouseY, button);
            }
            return true;
        }
        // Менеджер тем имеет приоритет (он поверх остального UI)
        if (themeEditor.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        searchField.mouseClicked(mouseX, mouseY, button);

        if (searchField.isEmpty()) {
            for (Panel panel : panels) {
                if (HoverUtil.isHovered(mouseX, mouseY, panel.getX(), panel.getY(), panel.getWidth(), panel.getHeight())) {
                    panel.mouseClicked(mouseX, mouseY, button);
                }
            }
        } else {
            for (Panel panel : panels) {
                panel.mouseClicked(mouseX, mouseY, button);
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        mouseX = scaleMouseX(mouseX);
        mouseY = scaleMouseY(mouseY);
        if (itemModelGallery != null) {
            itemModelGallery.mouseReleased(mouseX, mouseY, button);
            return true;
        }
        themeEditor.mouseReleased(mouseX, mouseY, button);
        for (Panel panel : panels) {
            panel.mouseReleased(mouseX, mouseY, button);
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        mouseX = scaleMouseX(mouseX);
        mouseY = scaleMouseY(mouseY);
        if (itemModelGallery != null) {
            itemModelGallery.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
            return true;
        }
        for (Panel panel : panels) {
            panel.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (itemModelGallery != null) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                itemModelGallery = null;
            } else {
                itemModelGallery.keyPressed(keyCode, scanCode, modifiers);
            }
            return true;
        }
        // Проверяем, не биндится ли какой-то модуль в данный момент
        boolean anyModuleBinding = false;
        for (Panel panel : panels) {
            for (ModuleComponent component : panel.getModuleComponents()) {
                if (component.isBinding()) {
                    anyModuleBinding = true;
                    break;
                }
            }
            if (anyModuleBinding) break;
        }

        // Если модуль биндится и нажат ESC, не закрываем GUI
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && anyModuleBinding) {
            // Передаем нажатие панелям для обработки бинда
            for (Panel panel : panels) {
                panel.keyPressed(keyCode, scanCode, modifiers);
            }
            return true; // Останавливаем дальнейшую обработку ESC
        }

        // Пока поле поиска в фокусе — все нажатия уходят в него и не утекают в игру/модули.
        // ESC при этом лишь снимает фокус, а не закрывает GUI.
        if (searchField.isFocused()) {
            searchField.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            ensureCursors();
            applyCursor(arrowCursor);
            super.close(); // ← СТАРЫЙ СПОСОБ ЗАКРЫТИЯ, работает!
            return true;
        }
        for (Panel panel : panels) {
            panel.keyPressed(keyCode, scanCode, modifiers);
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (itemModelGallery != null) {
            itemModelGallery.charTyped(chr, modifiers);
            return true;
        }
        if (searchField.isFocused()) {
            searchField.charTyped(chr, modifiers);
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
