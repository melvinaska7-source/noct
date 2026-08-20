package polar.ru.client.modules.impl.misc;

import java.lang.invoke.StringConcatFactory;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import polar.ru.api.utils.replace.ReplaceUtils;
import polar.ru.client.modules.Module;
import polar.ru.client.modules.settings.implement.BooleanSetting;
import polar.ru.client.modules.settings.implement.TextSetting;
import polar.ru.mixin.ChatScreenAccessor;
import polar.ru.polar;

public class NameProtect
extends Module {
    public static final NameProtect INSTANCE = new NameProtect();
    private final BooleanSetting friends = new BooleanSetting("Скрывать друзей", true);
    private final BooleanSetting grief = new BooleanSetting("Скрывать информацию", false);
    private final TextSetting nickname = new TextSetting("Никнейм", "polardlc.fun", 32);
    private static final int PATCH_CACHE_LIMIT = 512;
    private final Map<String, String> patchCache = new LinkedHashMap<String, String>(512, 0.75f, true){

        @Override
        protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
            return this.size() > 512;
        }
    };

    private NameProtect() {
        super("NameProtect", "Скрывает никнеймы", Module.ModuleCategory.MISC);
        this.addSettings(this.friends, this.grief, this.nickname);
    }

    public String patch(String text) {
        if (text == null) {
            return null;
        }
        if (!this.shouldPatch()) {
            return text;
        }
        String cacheKey = this.getPatchCacheKey(text);
        String cached = this.patchCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        String out = text;
        String replacement = this.getReplacementName();
        out = this.replaceIgnoreCase(out, mc.getSession().getUsername(), replacement);
        if (this.friends.isState() && polar.INSTANCE != null && polar.INSTANCE.friendStorage != null) {
            for (String friend : polar.INSTANCE.friendStorage.getFriends()) {
                out = this.replaceIgnoreCase(out, friend, replacement);
            }
        }
        out = this.patchGrief(out);
        this.patchCache.put(cacheKey, out);
        return out;
    }

    public String patchIncomingText(String text) {
        return this.patch(text);
    }

    public Text patchText(Text text) {
        if (text == null) {
            return null;
        }
        if (!this.shouldPatch()) {
            return text;
        }
        Text output = text;
        String replacement = this.getReplacementName();
        output = ReplaceUtils.replace(output, mc.getSession().getUsername(), replacement);
        if (this.friends.isState() && polar.INSTANCE != null && polar.INSTANCE.friendStorage != null) {
            for (String friend : polar.INSTANCE.friendStorage.getFriends()) {
                output = ReplaceUtils.replace(output, friend, replacement);
            }
        }
        return output;
    }

    public String getReplacementName() {
        String value = this.nickname.get();
        return value == null || value.isBlank() ? "polar" : value;
    }

    public boolean shouldHideGrief() {
        return this.grief.isState();
    }

    private String replaceIgnoreCase(String text, String target, String replacement) {
        if (text == null || target == null || target.isEmpty()) {
            return text;
        }
        int firstIndex = this.indexOfIgnoreCase(text, target, 0);
        if (firstIndex < 0) {
            return text;
        }
        StringBuilder out = new StringBuilder(text.length() + replacement.length());
        int from = 0;
        int index = firstIndex;
        while (index >= 0) {
            out.append(text, from, index).append(replacement);
            from = index + target.length();
            index = this.indexOfIgnoreCase(text, target, from);
        }
        out.append(text, from, text.length());
        return out.toString();
    }

    private int indexOfIgnoreCase(String text, String target, int from) {
        int max = text.length() - target.length();
        for (int i2 = Math.max(0, from); i2 <= max; ++i2) {
            if (!text.regionMatches(true, i2, target, 0, target.length())) continue;
            return i2;
        }
        return -1;
    }

    private String patchGrief(String text) {
        if (text == null || !this.grief.isState()) {
            return text;
        }
        String out = text.replaceAll("Анархия-\\d+", "PolarDLC.fun");
        out = out.replaceAll("ГРИФ #\\d+", "PolarDLC.fun");
        return out;
    }

    private String getPatchCacheKey(String text) {
        String username = mc != null && mc.getSession() != null ? mc.getSession().getUsername() : "";
        int friendsHash = 0;
        if (this.friends.isState() && polar.INSTANCE != null && polar.INSTANCE.friendStorage != null) {
            List<String> friendList = polar.INSTANCE.friendStorage.getFriends();
            friendsHash = friendList.hashCode();
        }
        return username + ":" + this.getReplacementName() + ":" + this.friends.isState() + ":" + this.grief.isState() + ":" + friendsHash + ":" + text;
    }

    private boolean shouldPatch() {
        return this.isEnable() && mc != null && NameProtect.mc.player != null && NameProtect.mc.world != null && !this.isFriendRemoveInputActive();
    }

    private boolean isFriendRemoveInputActive() {
        Screen var_437_2 = NameProtect.mc.currentScreen;
        if (!(var_437_2 instanceof ChatScreen)) {
            return false;
        }
        ChatScreen chatScreen = (ChatScreen)var_437_2;
        TextFieldWidget chatField = ((ChatScreenAccessor)chatScreen).polar$getChatField();
        if (chatField == null) {
            return false;
        }
        String input = chatField.getText();
        if (input == null) {
            return false;
        }
        String normalized = input.trim().toLowerCase();
        String prefix = polar.INSTANCE != null && polar.INSTANCE.commandStorage != null ? polar.INSTANCE.commandStorage.getPrefix().toLowerCase() : ".";
        return normalized.startsWith(prefix + "friend remove");
    }
}

