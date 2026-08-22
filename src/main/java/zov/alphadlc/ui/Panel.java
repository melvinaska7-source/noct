package zov.alphadlc.ui;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;
import zov.alphadlc.AlphaDLC;
import zov.alphadlc.module.ModuleCategory;
import zov.alphadlc.module.settings.ItemModelSetting;
import zov.alphadlc.ui.component.Component;
import zov.alphadlc.util.IMinecraft;
import zov.alphadlc.util.render.helper.HoverUtil;
import zov.alphadlc.util.render.math.Animation;
import zov.alphadlc.util.render.math.Easing;
import zov.alphadlc.util.render.math.Scissor;
import zov.alphadlc.util.render.msdf.Fonts;
import zov.alphadlc.util.render.providers.ColorProvider;
import zov.alphadlc.util.render.renderers.DrawUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Getter
@Setter
public class Panel implements IMinecraft {
    public float x, y, width, height;
    public final ModuleCategory category;
    public List<ModuleComponent> moduleComponents = new ArrayList<>();
    private Animation animation = new Animation(Easing.QUINTIC_OUT, 350);
    private Animation animationAlpha = new Animation(Easing.BOUNCE_OUT, 350);
    private final Animation scrollbarAnim = new Animation(Easing.CUBIC_IN_OUT, 220);
    float scroll;
    float maxScroll;

    private final ClickGuiFrame parent;

    private String cachedTitle;
    private String cachedIcon;
    private float cachedTitleWidth = -1f;
    private float cachedIconWidth = -1f;

    public Panel(ModuleCategory category, ClickGuiFrame parent) {
        this.category = category;
        this.parent = parent;
        AlphaDLC.getInstance().getModuleStorage().getModules().stream()
                .filter(m -> m.getCategory() == this.category)
                .sorted(Comparator.comparing(m -> m.getName().toLowerCase()))
                .forEach(m -> moduleComponents.add(new ModuleComponent(m, this)));
    }

    public void clampScroll() {
        if (maxScroll > 0) {
            scroll = MathHelper.clamp(scroll, -maxScroll, 0);
        } else {
            scroll = 0;
        }
    }

    public void render(DrawContext context, int mouseX, int mouseY, float partialTicks) {
        float alphaRatio = MathHelper.clamp(animationAlpha.getValue(), 0f, 1f);
        float alpha = Math.min(255 * alphaRatio, 255);
        float cornerRadius = 8f;
        float headerHeight = 25f;

        int panelColor = ColorProvider.setAlpha(ColorProvider.getColorClickGui(), (int)(130 * alphaRatio));

        DrawUtil.drawRoundBlur(x, y, width, height, cornerRadius, 
            ColorProvider.rgba(200, 200, 200, (int)(255 * alphaRatio)), 14f);
        DrawUtil.drawRound(x - 1f, y - 1f, width + 2f, height + 2f, cornerRadius + 0.5f, 
            ColorProvider.rgba(48, 66, 122, (int)(70 * alphaRatio)));
        DrawUtil.drawRound(x, y, width, height, cornerRadius, panelColor);

        DrawUtil.drawRound(x, y, width, headerHeight, 
            new org.joml.Vector4f(cornerRadius, 0, 0, cornerRadius), 
            ColorProvider.setAlpha(ColorProvider.getColorHeaderBg(), (int)(45 * alphaRatio)));

        float iconSize = 8.5f;
        if (cachedTitleWidth < 0f) {
            String title = category.name();
            cachedTitle = title.substring(0, 1).toUpperCase() + title.substring(1).toLowerCase();
            cachedIcon = switch (category) {
                case COMBAT -> "a";
                case MOVEMENT -> "b";
                case RENDER -> "c";
                case PLAYER -> "d";
                case MISC -> "e";
            };
            cachedTitleWidth = Fonts.SFREGULAR.get().getWidth(cachedTitle, 8.5f);
            cachedIconWidth = Fonts.ICONS_MINCED.get().getWidth(cachedIcon, iconSize);
        }

        String capitalizedTitle = cachedTitle;
        float titleWidth = cachedTitleWidth;
        String categoryIcon = cachedIcon;
        float iconWidth = cachedIconWidth;
        float totalWidth = iconWidth + 3f + titleWidth;
        float startX = x + width / 2f - totalWidth / 2f - 1f;

        float titleY = y + headerHeight / 2f - 8.5f / 2f;
        float iconY = titleY + 1f;
        DrawUtil.drawText(Fonts.ICONS_MINCED.get(), categoryIcon, startX, iconY, 
            ColorProvider.setAlpha(ColorProvider.getColorIcons(), (int) alpha), iconSize);
        DrawUtil.drawText(Fonts.SFREGULAR.get(), capitalizedTitle, startX + iconWidth + 3f, titleY, 
            ColorProvider.setAlpha(ColorProvider.getColorHeaderText(), (int) alpha), 8.5f);

        float offset = 2f;
        clampScroll();
        animation.run(scroll);

        Scissor.push();
        Scissor.setFromComponentCoordinates(x, y + headerHeight, width, height - headerHeight - 4);

        for (ModuleComponent component : moduleComponents) {
            if (parent.searchCheck(component.getModule().getName())) {
                continue;
            }

            float sideMargin = 6f;
            component.setX(x + sideMargin);
            component.setY((float) (y + headerHeight + offset + animation.getValue()));
            component.setWidth(width - sideMargin * 2f);

            float baseHeight = 19f;
            float extraHeight = 0;
            if (component.getAnimation().getValue() > 0.01f) {
                extraHeight = 3f;
                for (Component comp : component.getComponents()) {
                    float visibleProgress = MathHelper.clamp(comp.getAlphaAnimSetting().getValue(), 0f, 1f);
                    if (comp.isVisible() || visibleProgress > 0f) {
                        extraHeight += comp.getHeight() * visibleProgress;
                    }
                }
                extraHeight += 5f;
            }
            component.setHeight(baseHeight + (extraHeight * (float) component.getAnimation().getValue()));

            Scissor.setFromComponentCoordinates(x, y + headerHeight, width, height - headerHeight - 4);
            component.render(context, mouseX, mouseY, partialTicks);
            Scissor.setFromComponentCoordinates(x, y + headerHeight, width, height - headerHeight - 4);

            offset += component.getHeight() + 5f;
        }
        maxScroll = Math.max(0, offset - (height - headerHeight - 8));
        scrollbarAnim.run(maxScroll > 0f);

        if (scrollbarAnim.getValue() > 0.01f && maxScroll > 0) {
            float scrollbarWidth = 2.5f;
            float scrollbarHeight = (height - headerHeight - 8) * ((height - headerHeight - 8) / offset);
            scrollbarHeight = Math.max(scrollbarHeight, 15f);
            float scrollbarY = y + headerHeight + 4 + (-scroll / maxScroll) * (height - headerHeight - 8 - scrollbarHeight - 8);
            float scrollbarX = x + width - scrollbarWidth - 3f;

            int sbAlpha = (int)(100 * scrollbarAnim.getValue() * alphaRatio);
            DrawUtil.drawRound(scrollbarX, scrollbarY, scrollbarWidth, scrollbarHeight, scrollbarWidth / 2f, 
                ColorProvider.rgba(100, 110, 140, sbAlpha));
        }

        Scissor.unset();
        Scissor.pop();
    }

    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (HoverUtil.isHovered(mouseX, mouseY, x, y + 20, width, height - 20)) {
            for (ModuleComponent moduleComponent : moduleComponents) {
                if (!parent.searchCheck(moduleComponent.getModule().getName())) {
                    moduleComponent.mouseClicked(mouseX, mouseY, button);
                }
            }
        }
    }

    public void mouseReleased(double mouseX, double mouseY, int button) {
        for (ModuleComponent moduleComponent : moduleComponents) {
            if (!parent.searchCheck(moduleComponent.getModule().getName())) {
                moduleComponent.mouseReleased(mouseX, mouseY, button);
            }
        }
    }

    public void mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (HoverUtil.isHovered(mouseX, mouseY, x, y, width, height)) {
            scroll += (float) (verticalAmount * 30f);
            clampScroll();
        }
    }

    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        for (ModuleComponent moduleComponent : moduleComponents) {
            moduleComponent.keyPressed(keyCode, scanCode, modifiers);
        }
    }

    public void openItemModelGallery(ItemModelSetting setting) {
        parent.openItemModelGallery(setting);
    }
}
