package polar.ru.client.ui.clickgui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import polar.ru.api.utils.animation.AnimationUtils;
import polar.ru.api.utils.animation.Easings;

public class ClickGuiScreen extends Screen {

    private final ClickGuiDropdownRenderer renderer = new ClickGuiDropdownRenderer();
    private final AnimationUtils openAnim = new AnimationUtils(0.0f, 8.0f, Easings.CUBIC_OUT);
    private boolean closing = false;

    public ClickGuiScreen() {
        super(Text.literal("ClickGui"));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (closing) {
            openAnim.update(0.0f);
            if (openAnim.getValue() <= 0.01f) {
                closing = false;
                super.close();
                return;
            }
        } else {
            openAnim.update(1.0f);
        }

        var window = client.getWindow();
        int screenWidth = window.getScaledWidth();
        int screenHeight = window.getScaledHeight();

        // Затемнение фона
        int overlayAlpha = (int) (80 * openAnim.getValue());
        context.fill(0, 0, screenWidth, screenHeight, (overlayAlpha << 24) | 0x000000);

        // Масштабирование
        context.getMatrices().push();
        float scale = openAnim.getValue();
        float centerX = screenWidth / 2f;
        float centerY = screenHeight / 2f;
        context.getMatrices().translate(centerX, centerY, 0);
        context.getMatrices().scale(scale, scale, 1f);
        context.getMatrices().translate(-centerX, -centerY, 0);

        renderer.render(context, mouseX, mouseY, window);

        context.getMatrices().pop();
    }

    @Override
    public void close() {
        if (!closing) {
            closing = true;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        renderer.mouseClicked(mouseX, mouseY, button);
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        renderer.mouseReleased(mouseX, mouseY, button);
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        renderer.mouseScrolled(mouseX, mouseY, verticalAmount);
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        renderer.keyPressed(keyCode, scanCode, modifiers);
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        renderer.charTyped(chr, modifiers);
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}