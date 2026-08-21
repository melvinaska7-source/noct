package zov.alphadlc.module.list.render.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import zov.alphadlc.util.IMinecraft;
import zov.alphadlc.util.base.Instance;
import zov.alphadlc.util.render.math.Animation;
import zov.alphadlc.util.render.math.Easing;
import zov.alphadlc.util.render.msdf.Fonts;
import zov.alphadlc.util.render.providers.ColorProvider;
import zov.alphadlc.util.render.renderers.DrawUtil;
import zov.alphadlc.util.render.renderers.IRenderer;

import java.util.concurrent.CopyOnWriteArrayList;

public class NotificationsElement implements IMinecraft {

    private static final CopyOnWriteArrayList<Notification> notifications = new CopyOnWriteArrayList<>();

    // Шрифт MSDF не содержит глифа пробела, поэтому отступы между словами задаём в пикселях
    private static final float TEXT_GAP = 3f;

    public void post(String name, boolean enabled) {
        Notification n = new Notification(name, enabled);
        n.moduleName = name;
        n.statusText = enabled ? "Включен" : "Выключен";
        notifications.add(0, n);
    }

    public void postWarning(String text) {
        notifications.add(0, new Notification(text, true, true));
    }

    public void postTotem(Text tagText, boolean enchanted) {
        MutableText full = Text.literal("Игрок ").setStyle(Style.EMPTY.withColor(0xFFFFFF))
                .append(tagText)
                .append(Text.literal(" потерял Тотем").setStyle(Style.EMPTY.withColor(0xFFFFFF)));
        Notification n = new Notification(full.getString(), true, true);
        n.isTotem = true;
        n.enchanted = enchanted;
        n.totemTagText = full;
        notifications.add(0, n);
    }

    public void render(DrawContext context) {
        Interface iface = Instance.get(Interface.class);
        float baseX = iface != null ? iface.getNotificationsCenterX()
                : MinecraftClient.getInstance().getWindow().getScaledWidth() / 2f;
        float baseY = iface != null ? iface.getNotificationsY()
                : (MinecraftClient.getInstance().getWindow().getScaledHeight() / 2f) + 20f;
        float offset = 0;

        // Масштаб уведомлений применяем через DEFAULT_MATRIX (шейпы/текст берут именно его,
        // а не матрицу контекста), масштабируя весь стек вокруг якоря.
        float scale = iface != null ? iface.getNotificationsScale() : 1f;
        boolean scaled = Math.abs(scale - 1f) > 0.001f;
        if (scaled) {
            IRenderer.DEFAULT_MATRIX.identity()
                    .translate(baseX, baseY, 0f)
                    .scale(scale, scale, 1f)
                    .translate(-baseX, -baseY, 0f);
        }

        for (Notification n : notifications) {
            if (System.currentTimeMillis() - n.time > n.duration && n.anim.getValue() <= 0.01) {
                notifications.remove(n);
                continue;
            }

            boolean expiring = System.currentTimeMillis() - n.time > n.duration;
            n.anim.run(expiring ? 0 : 1);

            double animValue = n.anim.getValue();
            if (animValue <= 0.01) continue;

            float clampedAlpha = (float) Math.max(0.0, Math.min(1.0, animValue));
            int alphaInt = (int) (255 * clampedAlpha);

            float height = 14.5f;
            String fullText;
            String iconCode;
            int iconColor;
            float iconSize = n.isWarning ? 8f : 9f;

            if (n.isWarning) {
                fullText = n.customText;
                iconCode = "G";
                iconColor = ColorProvider.rgba(255, 255, 255, alphaInt);
            } else {
                fullText = n.name;
                iconCode = n.enabled ? "J" : "K";
                iconColor = ColorProvider.rgba(255, 255, 255, alphaInt);
            }

            float toggleW = 15f;
            float toggleH = 8f;
            
            // Вычисляем ширину в зависимости от типа уведомления
            float width;
            if (n.isWarning) {
                float textWidth = Fonts.SFMEDIUM.get().getWidth(n.customText, 7f);
                float iconWidth = Fonts.ICONS_NURIK.get().getWidth(iconCode, iconSize);
                width = iconWidth + textWidth + 22f;
            } else if (n.isTotem) {
                float textWidth = Fonts.SFMEDIUM.get().getWidth(n.totemTagText.getString(), 7f);
                width = 10f + textWidth + 16f;
            } else {
                // Обычное уведомление: "Модуль [название] [Включен/Выключен]" + переключатель
                String prefix = "Модуль";
                float prefixWidth = Fonts.SFMEDIUM.get().getWidth(prefix, 7f);
                float moduleWidth = Fonts.SFMEDIUM.get().getWidth(n.moduleName, 7f);
                float statusWidth = Fonts.SFMEDIUM.get().getWidth(n.statusText, 7f);
                float totalTextWidth = prefixWidth + TEXT_GAP + moduleWidth + TEXT_GAP + statusWidth;
                width = totalTextWidth + toggleW + 16f;
            }

            // Центрируем уведомление вокруг точки-якоря (по середине), а не по левому краю
            float x = baseX - width / 2f;
            float y = baseY + offset;
            float cx = x + width / 2f;

            context.getMatrices().push();
            context.getMatrices().translate(cx, y + height / 2f, 0);
            context.getMatrices().scale((float) animValue, (float) animValue, 1f);
            context.getMatrices().translate(-cx, -(y + height / 2f), 0);

            Interface interfaceModule = iface;
            if (interfaceModule != null) interfaceModule.drawNotifBackground(context, x, y, width, height, 2.5f, clampedAlpha);

            if (n.isWarning && !n.isTotem) {
                float warningTextWidth = Fonts.SFMEDIUM.get().getWidth(fullText, 7f);
                float warningIconWidth = Fonts.ICONS_NURIK.get().getWidth(iconCode, iconSize);
                float warningTotalWidth = warningIconWidth + 5f + warningTextWidth;
                float warningIconX = x + (width - warningTotalWidth) / 2f;
                DrawUtil.drawText(Fonts.ICONS_NURIK.get(), iconCode, warningIconX, y + 4, iconColor, iconSize);
            }

            // Центрирование по высоте: drawText рисует текст со смещением +2, поэтому
            // верх глифа = textY + 2, и для 7px текста в блоке height центр даёт (height-7)/2.
            float textY = y + (height - 7f) / 2f - 0.5f;
            if (n.isTotem && n.totemTagText != null) {
                // Тотем — текст, затем иконка тотема ПОСЛЕ текста
                float textWidth_full = Fonts.SFMEDIUM.get().getWidth(n.totemTagText.getString(), 7f);
                float totalWidth = textWidth_full + 4f + 10f; // текст + отступ + иконка
                float textX = x + (width - totalWidth) / 2f;
                DrawUtil.drawText(Fonts.SFMEDIUM.get(), n.totemTagText, textX, textY, 7f, alphaInt);

                context.getMatrices().push();
                context.getMatrices().translate(textX + textWidth_full + 4f, y + 2.5f, 0);
                context.getMatrices().scale(0.6f, 0.6f, 1f);
                context.drawItem(new ItemStack(Items.TOTEM_OF_UNDYING), 0, 0);
                context.getMatrices().pop();
            } else if (n.isWarning) {
                float warningTextWidth = Fonts.SFMEDIUM.get().getWidth(fullText, 7f);
                float warningIconWidth = Fonts.ICONS_NURIK.get().getWidth(iconCode, iconSize);
                float warningTotalWidth = warningIconWidth + 5f + warningTextWidth;
                float warningTextX = x + (width - warningTotalWidth) / 2f + warningIconWidth + 5f;
                DrawUtil.drawText(Fonts.SFMEDIUM.get(), fullText, warningTextX, textY, ColorProvider.rgba(255, 255, 255, alphaInt), 7f);
            } else {
                // Обычное уведомление: "Модуль [название] [Включен/Выключен]"
                String prefix = "Модуль";
                String moduleName = n.moduleName;
                String status = n.statusText;
                
                float prefixWidth = Fonts.SFMEDIUM.get().getWidth(prefix, 7f);
                float moduleWidth = Fonts.SFMEDIUM.get().getWidth(moduleName, 7f);
                float statusWidth = Fonts.SFMEDIUM.get().getWidth(status, 7f);
                float totalTextWidth = prefixWidth + TEXT_GAP + moduleWidth + TEXT_GAP + statusWidth;
                
                // Цвет статуса: зеленый для "Включен", красный для "Выключен"
                int statusColor = n.enabled 
                    ? ColorProvider.rgba(0, 255, 100, alphaInt)  // зеленый
                    : ColorProvider.rgba(255, 80, 80, alphaInt);  // красный
                
                float totalModuleWidth = totalTextWidth + 5f + toggleW;
                float startX = x + (width - totalModuleWidth) / 2f;
                
                // Рисуем части текста с явными пиксельными отступами (в шрифте нет глифа пробела)
                float currentX = startX;
                DrawUtil.drawText(Fonts.SFMEDIUM.get(), prefix, currentX, textY, ColorProvider.rgba(255, 255, 255, alphaInt), 7f);
                currentX += prefixWidth + TEXT_GAP;
                DrawUtil.drawText(Fonts.SFMEDIUM.get(), moduleName, currentX, textY, ColorProvider.rgba(255, 255, 255, alphaInt), 7f);
                currentX += moduleWidth + TEXT_GAP;
                DrawUtil.drawText(Fonts.SFMEDIUM.get(), status, currentX, textY, statusColor, 7f);

                float tX = startX + totalTextWidth + 5f;
                float tY = y + (height - toggleH) / 2f;
                float anim = n.enabled ? 1f : 0f;

                if (interfaceModule != null) {
                    interfaceModule.drawNewToggle(tX, tY, toggleW, toggleH, n.enabled, anim, alphaInt);
                }
            }

            context.getMatrices().pop();
            offset += (height + 3) * clampedAlpha;
        }

        if (scaled) {
            IRenderer.DEFAULT_MATRIX.identity();
        }
    }

    private static class Notification {
        String name;
        boolean enabled;
        long time;
        long duration = 1000;
        Animation anim = new Animation(Easing.BACK_OUT, 300);

        boolean isWarning = false;
        String customText;
        boolean isTotem = false;
        boolean enchanted = false;
        Text totemTagText = null;
        
        String moduleName = "";
        String statusText = "";

        public Notification(String name, boolean enabled) {
            this.name = name;
            this.enabled = enabled;
            this.time = System.currentTimeMillis();
        }

        public Notification(String customText, boolean enabled, boolean isWarning) {
            this.customText = customText;
            this.enabled = enabled;
            this.isWarning = isWarning;
            this.time = System.currentTimeMillis();
            this.duration = 2000;
        }
    }
}