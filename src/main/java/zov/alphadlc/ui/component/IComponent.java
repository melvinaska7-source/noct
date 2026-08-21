package zov.alphadlc.ui.component;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;

public interface IComponent {
    default void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {}
    default void render(DrawContext context, int mouseX, int mouseY, float partialTicks) {
        render(context.getMatrices(), mouseX, mouseY, partialTicks);
    }
    default void mouseClicked(double mouseX, double mouseY, int button) {}
    default void mouseReleased(double mouseX, double mouseY, int button) {}
    default void mouseScrolled(double mouseX, double mouseY, double delta) {}
    default void mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        mouseScrolled(mouseX, mouseY, verticalAmount);
    }
    default void keyPressed(int keyCode, int scanCode, int modifiers) {}
    default void charTyped(char chr, int modifiers) {}
}