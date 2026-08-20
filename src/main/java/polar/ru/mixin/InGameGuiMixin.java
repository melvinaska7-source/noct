package polar.ru.mixin;

import java.util.Comparator;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.AbstractTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.Team;
import net.minecraft.scoreboard.number.NumberFormat;
import net.minecraft.scoreboard.number.StyledNumberFormat;
import net.minecraft.text.MutableText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import polar.ru.api.QClient;
import polar.ru.api.events.EventInvoker;
import polar.ru.api.events.implement.EventRender;
import polar.ru.api.storages.implement.helpertstorages.enumvar.ModuleClass;
import polar.ru.api.utils.SidebarEntry;
import polar.ru.api.utils.animation.AnimationUtils;
import polar.ru.api.utils.animation.Easings;
import polar.ru.client.modules.impl.misc.NameProtect;

@Mixin(value={InGameHud.class})
public class InGameGuiMixin
implements QClient {
    private static final AnimationUtils statusBarsAnimation = new AnimationUtils(0.0f, 12.5f, Easings.CUBIC_OUT);
    private static final AnimationUtils experienceAnimation = new AnimationUtils(0.0f, 12.5f, Easings.CUBIC_OUT);
    private static float lastHotbarX = 0.0f;
    private static float lastHotbarY = 0.0f;
    private static final int DOMAIN_COLOR = 15557921;
    @Shadow
    @Final
    private MinecraftClient client;

    @Inject(method={"render"}, at={@At(value="HEAD")})
    private void render(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (EventInvoker.hasListeners(EventRender.Default.class)) {
            new EventRender.Default(context, tickCounter.getTickDelta(true)).call();
        }
    }

    @Inject(method={"renderHotbar"}, at={@At(value="HEAD")}, cancellable=true)
    private void polar$hideVanillaHotbar(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (ModuleClass.INSTANCE != null && ModuleClass.interfaceModule != null && ModuleClass.interfaceModule.isEnable() && ModuleClass.interfaceModule.isHotbarEnabled()) {
            ci.cancel();
        }
    }

    @Inject(method={"renderScoreboardSidebar"}, at={@At(value="HEAD")}, cancellable=true)
    private void polar$hideVanillaScoreboard(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (ModuleClass.INSTANCE != null && ModuleClass.interfaceModule != null && ModuleClass.interfaceModule.isEnable() && ModuleClass.interfaceModule.isScoreboardEnabled()) {
            ci.cancel();
        }
    }

    @Inject(method={"renderStatusBars"}, at={@At(value="HEAD")})
    private void polar$renderCustomStatusBars(DrawContext context, CallbackInfo ci) {
        if (ModuleClass.INSTANCE != null && ModuleClass.interfaceModule != null && ModuleClass.interfaceModule.isEnable() && ModuleClass.interfaceModule.isHotbarEnabled()) {
            float hotbarX = ModuleClass.interfaceModule.getHotbarX();
            float hotbarY = ModuleClass.interfaceModule.getHotbarY();
            float vanillaX = context.getScaledWindowWidth() / 2 - 91;
            float vanillaY = context.getScaledWindowHeight() - 39;
            float offsetX = hotbarX - vanillaX + 4.0f;
            float offsetY = hotbarY - vanillaY - 20.0f;
            context.getMatrices().push();
            context.getMatrices().translate(offsetX, offsetY, 0.0f);
        }
    }

    @Inject(method={"renderStatusBars"}, at={@At(value="TAIL")})
    private void polar$restoreStatusBarsMatrix(DrawContext context, CallbackInfo ci) {
        if (ModuleClass.INSTANCE != null && ModuleClass.interfaceModule != null && ModuleClass.interfaceModule.isEnable() && ModuleClass.interfaceModule.isHotbarEnabled()) {
            context.getMatrices().pop();
        }
    }

    @Inject(method={"renderExperienceBar"}, at={@At(value="HEAD")})
    private void polar$renderCustomExperienceBar(DrawContext context, int x2, CallbackInfo ci) {
        if (ModuleClass.INSTANCE != null && ModuleClass.interfaceModule != null && ModuleClass.interfaceModule.isEnable() && ModuleClass.interfaceModule.isHotbarEnabled()) {
            float hotbarX = ModuleClass.interfaceModule.getHotbarX();
            float hotbarY = ModuleClass.interfaceModule.getHotbarY();
            float vanillaX = context.getScaledWindowWidth() / 2 - 91;
            float vanillaY = context.getScaledWindowHeight() - 29;
            float offsetX = hotbarX - vanillaX + 4.0f;
            float offsetY = hotbarY - 10.0f - vanillaY;
            context.getMatrices().push();
            context.getMatrices().translate(offsetX, offsetY, 0.0f);
        }
    }

    @Inject(method={"renderExperienceBar"}, at={@At(value="TAIL")})
    private void polar$restoreExperienceBarMatrix(DrawContext context, int x2, CallbackInfo ci) {
        if (ModuleClass.INSTANCE != null && ModuleClass.interfaceModule != null && ModuleClass.interfaceModule.isEnable() && ModuleClass.interfaceModule.isHotbarEnabled()) {
            context.getMatrices().pop();
        }
    }

    @Shadow
    private PlayerEntity getCameraPlayer() {
        return null;
    }

    @Inject(method={"renderScoreboardSidebar"}, at={@At(value="HEAD")}, cancellable=true)
    private void polar$renderPatchedScoreboard(DrawContext drawContext, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (!this.polar$shouldPatchScoreboard() || this.client.world == null) {
            return;
        }
        Scoreboard scoreboard = this.client.world.getScoreboard();
        ScoreboardObjective objective = scoreboard.getObjectiveForSlot(net.minecraft.scoreboard.ScoreboardDisplaySlot.SIDEBAR);
        if (objective == null) {
            return;
        }
        try {
            int titleWidth;
            Text title;
            NumberFormat numberFormat = objective.getNumberFormatOr((NumberFormat)StyledNumberFormat.RED);
            List<SidebarEntry> lines = scoreboard.getScoreboardEntries(objective).stream().filter(entry -> !entry.hidden()).sorted(Comparator.comparing(ScoreboardEntry::value).reversed().thenComparing(ScoreboardEntry::owner, String.CASE_INSENSITIVE_ORDER)).limit(15L).map(entry -> {
                try {
                    Team team = scoreboard.getScoreHolderTeam(entry.owner());
                    Text name = this.polar$patchText((Text)Team.decorateName((AbstractTeam)team, (Text)entry.name()));
                    MutableText score = entry.formatted(numberFormat);
                    int scoreWidth = this.client.textRenderer.getWidth((StringVisitable)score);
                    return new SidebarEntry(name, (Text)score, scoreWidth);
                }
                catch (Exception e2) {
                    MutableText fallback = Text.literal((String)"???");
                    return new SidebarEntry((Text)fallback, (Text)fallback, 0);
                }
            }).toList();
            try {
                title = this.polar$patchText(objective.getDisplayName());
            }
            catch (Exception e2) {
                title = Text.literal((String)"???");
            }
            int maxWidth = titleWidth = this.client.textRenderer.getWidth((StringVisitable)title);
            int separatorWidth = this.client.textRenderer.getWidth(": ");
            for (SidebarEntry line : lines) {
                maxWidth = Math.max(maxWidth, this.client.textRenderer.getWidth((StringVisitable)line.name) + (line.scoreWidth > 0 ? separatorWidth + line.scoreWidth : 0));
            }
            int lineCount = lines.size();
            int totalHeight = lineCount * 9;
            int bottom = drawContext.getScaledWindowHeight() / 2 + totalHeight / 3;
            int left = drawContext.getScaledWindowWidth() - maxWidth - 3;
            int right = drawContext.getScaledWindowWidth() - 1;
            int bodyColor = this.client.options.getTextBackgroundColor(0.3f);
            int headerColor = this.client.options.getTextBackgroundColor(0.4f);
            int top = bottom - lineCount * 9;
            drawContext.fill(left - 2, top - 10, right, top - 1, headerColor);
            drawContext.fill(left - 2, top - 1, right, bottom, bodyColor);
            drawContext.drawText(this.client.textRenderer, title, left + maxWidth / 2 - titleWidth / 2, top - 9, -1, false);
            for (int index = 0; index < lineCount; ++index) {
                SidebarEntry line = lines.get(index);
                int y2 = bottom - (lineCount - index) * 9;
                drawContext.drawText(this.client.textRenderer, line.name, left, y2, -1, false);
                drawContext.drawText(this.client.textRenderer, line.score, right - line.scoreWidth, y2, -1, false);
            }
            ci.cancel();
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    private boolean polar$shouldPatchScoreboard() {
        return ModuleClass.INSTANCE != null && ModuleClass.nameProtect != null && ModuleClass.nameProtect.isEnable();
    }

    private Text polar$patchText(Text text) {
        NameProtect nameProtect = ModuleClass.nameProtect;
        Text patched = nameProtect.patchText(text);
        String patchedString = patched.getString();
        if (nameProtect.shouldHideGrief()) {
            if (patchedString.contains("Анархия-")) {
                patchedString = patchedString.replaceAll("Анархия-\\d+", "polardlc.fun");
            }
            if (patchedString.contains("ГРИФ #")) {
                patchedString = patchedString.replaceAll("ГРИФ #\\d+", "polardlc.fun");
            }
        }
        if ((patchedString = this.polar$sanitizeForIdentifier(patchedString)).equals(patched.getString())) {
            return patched;
        }
        return Text.literal((String)patchedString).setStyle(patched.getStyle().withColor(TextColor.fromRgb((int)15557921)));
    }

    private String polar$sanitizeForIdentifier(String input) {
        if (input == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(input.length());
        for (int i2 = 0; i2 < input.length(); ++i2) {
            char c2 = input.charAt(i2);
            if (c2 < ' ' || c2 == '\u007f') continue;
            sb.append(c2);
        }
        return sb.toString();
    }
}

