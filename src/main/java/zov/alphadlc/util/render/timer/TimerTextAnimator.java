package zov.alphadlc.util.render.timer;

import zov.alphadlc.util.render.msdf.MsdfFont;
import zov.alphadlc.util.render.renderers.DrawUtil;

import java.util.HashMap;
import java.util.Map;

public final class TimerTextAnimator {
    private static final long ANIM_MS = 170L;
    private static final float SLIDE_PX = 5.0F;
    private static final Map<String, Entry> ENTRIES = new HashMap<>();
    private static final ThreadLocal<char[]> CHAR_BUF = ThreadLocal.withInitial(() -> new char[1]);

    private TimerTextAnimator() {
    }

    private static String singleCharString(char c) {
        char[] buf = CHAR_BUF.get();
        buf[0] = c;
        return new String(buf);
    }

    public static float getAnimatedWidth(MsdfFont font, String key, String text, float size) {
        Entry entry = ENTRIES.get(key);
        String previous = entry != null ? entry.previous : text;
        String current = entry != null ? entry.current : text;
        if (previous.length() != current.length()) {
            return font.getWidth(current, size);
        }
        int maxLen = Math.max(previous.length(), current.length());
        float width = 0f;
        for (int i = 0; i < maxLen; i++) {
            char oldChar = i < previous.length() ? previous.charAt(i) : '\0';
            char newChar = i < current.length() ? current.charAt(i) : '\0';
            float oldW = oldChar == '\0' ? 0f : font.getWidth(singleCharString(oldChar), size);
            float newW = newChar == '\0' ? 0f : font.getWidth(singleCharString(newChar), size);
            width += Math.max(oldW, newW);
        }
        return width;
    }

    public static void draw(MsdfFont font, String key, String text, float x, float y, int color, float size) {
        long now = System.currentTimeMillis();
        Entry entry = ENTRIES.computeIfAbsent(key, k -> new Entry(text));
        if (!text.equals(entry.current)) {
            if (text.length() != entry.current.length()) {
                entry.previous = text;
            } else {
                entry.previous = entry.current;
            }
            entry.current = text;
            entry.changedAt = now;
        }
        entry.lastUse = now;
        cleanup(now);

        String previous = entry.previous;
        String current = entry.current;
        if (previous.length() != current.length()) {
            DrawUtil.drawText(font, current, x, y, color, size);
            return;
        }

        float progress = Math.min(1.0F, (now - entry.changedAt) / (float) ANIM_MS);
        if (progress >= 1.0F) {
            DrawUtil.drawText(font, current, x, y, color, size);
            return;
        }

        int baseAlpha = (color >>> 24) & 0xFF;
        if (baseAlpha == 0) {
            baseAlpha = 255;
        }

        int maxLen = Math.max(previous.length(), current.length());
        float cursor = x;

        for (int i = 0; i < maxLen; i++) {
            char oldChar = i < previous.length() ? previous.charAt(i) : '\0';
            char newChar = i < current.length() ? current.charAt(i) : '\0';

            float oldW = oldChar == '\0' ? 0.0F : font.getWidth(singleCharString(oldChar), size);
            float newW = newChar == '\0' ? 0.0F : font.getWidth(singleCharString(newChar), size);
            float charW = Math.max(oldW, newW);

            boolean animate = Character.isDigit(oldChar) && Character.isDigit(newChar) && oldChar != newChar && progress < 1.0F;
            if (animate) {
                int oldAlpha = (int) (baseAlpha * (1.0F - progress));
                int newAlpha = (int) (baseAlpha * progress);
                DrawUtil.drawText(font, singleCharString(oldChar), cursor, y - progress * SLIDE_PX, (oldAlpha << 24) | (color & 0xFFFFFF), size);
                DrawUtil.drawText(font, singleCharString(newChar), cursor, y + (1.0F - progress) * SLIDE_PX, (newAlpha << 24) | (color & 0xFFFFFF), size);
            } else if (newChar != '\0') {
                DrawUtil.drawText(font, singleCharString(newChar), cursor, y, color, size);
            }

            cursor += charW;
        }
    }

    private static void cleanup(long now) {
        if (ENTRIES.size() < 512) {
            return;
        }
        ENTRIES.entrySet().removeIf(e -> now - e.getValue().lastUse > 10_000L);
    }

    private static final class Entry {
        private String previous;
        private String current;
        private long changedAt;
        private long lastUse;

        private Entry(String text) {
            this.previous = text;
            this.current = text;
            this.changedAt = System.currentTimeMillis();
            this.lastUse = this.changedAt;
        }
    }
}
