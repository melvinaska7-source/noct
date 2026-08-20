package polar.ru.api.utils.math;

public class HoveringUtils {
    public static boolean isHovering(float x2, float y2, float width, float height, int mouseX, int mouseY) {
        return (float)mouseX >= x2 && (float)mouseY >= y2 && (float)mouseX < x2 + width && (float)mouseY < y2 + height;
    }

    public static boolean isInRegion(int mouseX, int mouseY, int x2, int y2, int width, int height) {
        return mouseX >= x2 && mouseX <= x2 + width && mouseY >= y2 && mouseY <= y2 + height;
    }

    public static boolean isInRegion(double mouseX, double mouseY, float x2, float y2, float width, float height) {
        return mouseX >= (double)x2 && mouseX <= (double)(x2 + width) && mouseY >= (double)y2 && mouseY <= (double)(y2 + height);
    }

    public static boolean isHovering(float x2, float y2, float width, float height, double mouseX, double mouseY) {
        return mouseX >= (double)x2 && mouseY >= (double)y2 && mouseX < (double)(x2 + width) && mouseY < (double)(y2 + height);
    }

    public static boolean isInRegion(double mouseX, double mouseY, int x2, int y2, int width, int height) {
        return mouseX >= (double)x2 && mouseX <= (double)(x2 + width) && mouseY >= (double)y2 && mouseY <= (double)(y2 + height);
    }

    public static boolean isHovered(double mouseX, double mouseY, double x2, double y2, double width, double height) {
        return mouseX > x2 && mouseX < x2 + width && mouseY > y2 && mouseY < y2 + height;
    }
}

