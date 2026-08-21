package polar.ru.client.ui.clickgui;

import net.minecraft.client.gui.DrawContext;

public abstract class ClickGuiDropdownSetting {

    protected float x, y;
    protected float width;
    private float height;

    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public void setWidth(float width) {
        this.width = width;
    }

    public float getHeight() {
        return height;
    }

    protected void setHeight(float height) {
        this.height = height;
    }

    public abstract void render(DrawContext context, int mouseX, int mouseY);
    public abstract void mouseClicked(double mouseX, double mouseY, int button);
    public abstract void mouseReleased(double mouseX, double mouseY, int button);
    public abstract void keyPressed(int keyCode, int scanCode, int modifiers);
    public abstract void charTyped(char chr, int modifiers);
    public abstract boolean isVisible();
}